package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.Constant;
import it.pagopa.pnss.common.client.DocumentClientCall;

import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static it.pagopa.pnss.common.Constant.STAGED;
import static it.pagopa.pnss.common.QueueNameConstant.MAXIMUM_LISTENING_TIME;


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

    public <T> Mono<Void> incomingMessageFlow(S3ObjectCreated queuePayload, Acknowledgment acknowledgment) {
        return Mono.just(queuePayload)
                .doOnNext(message -> this.signDocument(queuePayload))
                .flatMap(unused -> Mono.fromFuture(CompletableFuture.supplyAsync(acknowledgment::acknowledge)))
                .timeout(Duration.ofSeconds(MAXIMUM_LISTENING_TIME))
                .doOnError(throwable -> log.error("Maximum listening time on incoming message queue exceed"))
                .then();
    }

    public void signDocument(S3ObjectCreated s3Object){
        String key = s3Object.getObject().getKey();
        // readDocument from bucket
        ResponseBytes<GetObjectResponse> objectResponse = downloadObjectService.execute(key);
        byte[] fileInput = objectResponse.asByteArray();

        // readDocument from DB
        ResponseEntity<Document> getdocument = null ; //documentClientCall.getdocument(key);
        Document doc = getdocument.getBody();
        if (getdocument==null || getdocument.getBody()==null || getdocument.getBody().getContentType().isEmpty()){

        }

        String contentType = doc.getContentType();
        SignReturnV2 signReturnV2 = null;
        try {
            if (contentType.equals(Constant.APPLICATION_PDF)) {
                signReturnV2 = signServiceSoap.singnPdfDocument(fileInput, false);
            }else {
                signReturnV2 = signServiceSoap.pkcs7signV2(fileInput, false);
            }

            } catch (TypeOfTransportNotImplemented_Exception e) {
                throw new RuntimeException(e);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
        }
        // UploadDocument
        byte[] fileSigned = signReturnV2.getBinaryoutput();
        PutObjectResponse putObjectResponse=  uploadObjectService.execute(key,fileSigned);

        doc.setDocumentState(STAGED);
        documentClientCall.updatedocument(doc);

    }
}
