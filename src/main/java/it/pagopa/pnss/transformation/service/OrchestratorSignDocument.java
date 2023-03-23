package it.pagopa.pnss.transformation.service;

import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pnss.common.Constant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;


@Service
@Slf4j
public class OrchestratorSignDocument {

    SignServiceSoap signServiceSoap;
    UploadObjectService uploadObjectService;
    DownloadObjectService downloadObjectService;

    DeleteObjectService deleteObjectService;
    DocumentClientCall documentClientCall;

    public OrchestratorSignDocument(SignServiceSoap signServiceSoap, UploadObjectService uploadObjectService, DownloadObjectService downloadObjectService, DocumentClientCall documentClientCall,DeleteObjectService deleteObjectService) {
        this.signServiceSoap = signServiceSoap;
        this.uploadObjectService = uploadObjectService;
        this.downloadObjectService = downloadObjectService;
        this.documentClientCall = documentClientCall;
        this.deleteObjectService = deleteObjectService;
    }


    public Mono<Void> incomingMessageFlow(String key, String bucketName, Boolean marcatura) {
    	log.info("OrchestratorSignDocument.incomingMessageFlow() : START");
        log.debug("OrchestratorSignDocument.incomingMessageFlow() : chiamo la document con keyname = {}", key);
        return Mono.fromCallable(this::validationField)
                .flatMap(voidMono -> documentClientCall.getdocument(key))
                .flatMap(documentResponse -> {
                    try {
                    	log.info("step 1");
                        ResponseBytes<GetObjectResponse> objectResponse = downloadObjectService.execute(key,bucketName);
                        byte[] fileInput = objectResponse.asByteArray();
                        Document doc = documentResponse.getDocument();
                        log.info("step 2: doc = {}", doc);
                        log.info("step 3: doc.getDocumentType().getDigitalSignature() = {}", doc.getDocumentType().getDigitalSignature());

                        if (!doc.getDocumentType().getDigitalSignature()){
                            return Mono.empty();
                        }

                        String contentType = doc.getContentType();
                        SignReturnV2 signReturnV2 = null;
                        try {
                            if (contentType.equals(Constant.APPLICATION_PDF)) {
                            	log.info("step 4: application pdf");
                                signReturnV2 = signServiceSoap.singnPdfDocument(fileInput, marcatura);
                            } else {
                            	log.info("step 4: not application pdf");
                                signReturnV2 = signServiceSoap.pkcs7signV2(fileInput, marcatura);
                            }
                            log.info("step 5: signReturnV2 = {}", signReturnV2);
                            log.info("step 6: signReturnV2.getStatus() = {}", signReturnV2.getStatus());

                        } catch (TypeOfTransportNotImplemented_Exception|JAXBException|MalformedURLException e) {
                        	log.error("step error",e);
                            throw new ArubaSignException(key);
                        }
                        log.debug("\n--- ARUBA RESPONSE "+
                                 "\n--- ARUBA RETURN CODE : "+signReturnV2.getReturnCode()+
                                 "\n--- ARUBA STATUS      : "+signReturnV2.getStatus()+
                                 "\n--- ARUBA DESCRIPTION : "+signReturnV2.getDescription());
                        if (signReturnV2.getStatus().equals("KO")){
                            throw new ArubaSignException(key);
                        }
                        byte[] fileSigned = signReturnV2.getBinaryoutput();
                        log.info("step 7: fileSigned");
                        
                        return uploadObjectService.execute(key, fileSigned);

                    }catch (NoSuchBucketException nsbe){
                        throw new S3BucketException.BucketNotPresentException(nsbe.getMessage());
                    }catch (NoSuchKeyException nske){
                        throw new S3BucketException.NoSuchKeyException(key);
                    }


                })
                .flatMap( response -> {
                	
                	log.info("step 8");
                    
                    DocumentChanges docChanges = new DocumentChanges();
                    docChanges.setDocumentState(Constant.AVAILABLE);
                    return deleteObjectService.execute(key,bucketName);

                })
                .retryWhen(Retry.max(10)
                        .filter(ArubaSignException.class::isInstance)
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                            throw new ArubaSignExceptionLimitCall(key);
                        }))
                .then();

        // readDocument from bucket

        // UploadDocument

    }

    private Mono<Boolean> validationField() {
        return Mono.just(true);
    }


}
