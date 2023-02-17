package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.Constant;
import it.pagopa.pnss.common.client.DocumentClientCall;

import it.pagopa.pnss.common.client.exception.*;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;


@Service
@Slf4j
public class OrchestratorSignDocument {

    SignServiceSoap signServiceSoap;
    UploadObjectService uploadObjectService;
    DownloadObjectService downloadObjectService;
    DocumentClientCall documentClientCall;

    public OrchestratorSignDocument(SignServiceSoap signServiceSoap, UploadObjectService uploadObjectService, DownloadObjectService downloadObjectService, DocumentClientCall documentClientCall) {
        this.signServiceSoap = signServiceSoap;
        this.uploadObjectService = uploadObjectService;
        this.downloadObjectService = downloadObjectService;
        this.documentClientCall = documentClientCall;
    }


    public Mono<Void> incomingMessageFlow(String key) {
        log.info("chiamo la document con keyname :"+key);
        return Mono.fromCallable(() -> validationField())
                .flatMap(voidMono -> documentClientCall.getdocument(key))
                .map(documentResponse -> {
                    try {
                        ResponseBytes<GetObjectResponse> objectResponse = downloadObjectService.execute(key);
                        byte[] fileInput = objectResponse.asByteArray();
                        Document doc = documentResponse.getDocument();
                        String contentType = doc.getContentType();
                        SignReturnV2 signReturnV2 = null;
                        try {
                            if (contentType.equals(Constant.APPLICATION_PDF)) {
                                signReturnV2 = signServiceSoap.singnPdfDocument(fileInput, false);
                            } else {
                                signReturnV2 = signServiceSoap.pkcs7signV2(fileInput, false);
                            }

                        } catch (TypeOfTransportNotImplemented_Exception e) {
                            throw new ArubaSignException(key);
                        } catch (JAXBException e) {
                            throw new ArubaSignException(key);
                        } catch (MalformedURLException e) {
                            throw new ArubaSignException(key);
                        }
                        byte[] fileSigned = signReturnV2.getBinaryoutput();
                        PutObjectResponse putObjectResponse = uploadObjectService.execute(key, fileSigned);

                        doc.setDocumentState(Document.DocumentStateEnum.STAGED);
                        documentClientCall.updatedocument(doc);
                    }catch (NoSuchBucketException nsbe){
                        throw new S3BucketException.BucketNotPresentException(nsbe.getMessage());
                    }catch (NoSuchKeyException nske){
                        throw new S3BucketException.NoSuchKeyException(key);
                    }

                    return Mono.empty();
                })
                .retryWhen(Retry.max(10)
                        .filter(ArubaSignException.class::isInstance)
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                            throw new ArubaSignExceptionLimitCall(key);
                        })).then();






        // readDocument from bucket

        // UploadDocument

    }

    private Mono<Boolean> validationField() {
        return Mono.just(true);
    }


}
