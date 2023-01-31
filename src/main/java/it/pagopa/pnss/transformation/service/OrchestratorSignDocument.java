package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pnss.common.Constant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.dto.DocumentDTO;
import it.pagopa.pnss.common.client.dto.DocumentStateEnumDTO;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.util.concurrent.Flow;



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

    public Flow.Publisher<Object> incomingMessageFlow(S3ObjectCreated s3ObjectCreated, Acknowledgment acknowledgment) {
        return null;
    }

    public void signDocument(S3ObjectCreated s3Object){
        String key = s3Object.getObject().getKey();
        // readDocument from bucket
        ResponseBytes<GetObjectResponse> objectResponse = downloadObjectService.execute(key);
        byte[] fileInput = objectResponse.asByteArray();

        // readDocument from DB
        ResponseEntity<DocumentDTO> getdocument = documentClientCall.getdocument(key);
        DocumentDTO doc = getdocument.getBody();
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

        doc.setDocumentState(DocumentStateEnumDTO.STAGED);
        documentClientCall.updatedocument(doc);

    }
}
