package it.pagopa.pnss.uriBuilder.service;

import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static it.pagopa.pnss.common.Constant.*;

@Service
@Slf4j
public class UriBuilderService {


    UserConfigurationClientCall userConfigurationClientCall;
    DocumentClientCall documentClientCall;

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
    }

    List <String> hotStatus;
    List <String> coldStatus;
    Map<String,String> mapDocumentTypeToBucket ;

    @PostConstruct
    public void createMap() {
        mapDocumentTypeToBucket= new HashMap<>();
        mapDocumentTypeToBucket.put(PN_NOTIFICATION_ATTACHMENTS,BUCKET_HOT);
        mapDocumentTypeToBucket.put(PN_AAR,BUCKET_HOT);
        mapDocumentTypeToBucket.put(PN_LEGAL_FACTS,BUCKET_STAGING);
        mapDocumentTypeToBucket.put(PN_EXTERNAL_LEGAL_FACTS,BUCKET_HOT);
        mapDocumentTypeToBucket.put(PN_DOWNTIME_LEGAL_FACTS,BUCKET_STAGING);
        hotStatus = Arrays.asList("cold");
        coldStatus = Arrays.asList("hot");
    }

    public FileCreationResponse createUriForUploadFile(String xPagopaSafestorageCxId, String contentType, String documentType, String status) throws InterruptedException {

        ResponseEntity<UserConfigurationOutput> userResponse = userConfigurationClientCall.getUser(xPagopaSafestorageCxId);
        UserConfigurationOutput user = null;
        if (userResponse!=null ){
            user = userResponse.getBody();
        }

        if (user == null ){
            log.info("Utente NON TROVATO "+ xPagopaSafestorageCxId);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User Not Found : " + xPagopaSafestorageCxId);
        }

        List<String> canRead = user.getCanRead();
        if (!canRead.contains(documentType)){
            log.info("Utente :"+ xPagopaSafestorageCxId +" non abilitato a caricare documenti di tipo "+documentType);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Client : "+ xPagopaSafestorageCxId+  " not has privilege for download document type " +documentType);
        }
        log.info("Utente :"+ xPagopaSafestorageCxId +" abilitato a  "+documentType);

        FileCreationResponse response = new FileCreationResponse();
        GenerateRandoKeyFile g = GenerateRandoKeyFile.getInstance();
        log.info("Utente :"+ xPagopaSafestorageCxId +" docuemntType   "+documentType + "Inizio produzione chiave ");

        String keyName = g.createKeyName(documentType);
        int riprova = 0;
        while (keyPresent(keyName) && riprova <10){
            log.info( "keyname : "+ keyName + " gia presente riprovo a calcolare");
            Thread.sleep(1000);
            keyName = g.createKeyName(documentType);
            riprova++;
        }
        if (riprova>10 && keyPresent(keyName)){
            log.info( " NON e' stato possibile produrre un key ");
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Non e' stato possibile produrre una chiave per user " + xPagopaSafestorageCxId);

        }
        log.info("Utente :"+ xPagopaSafestorageCxId +" docuemntType   "+documentType + " keyname : "+ keyName);

        response.setKey(keyName);
        String secret=null;
        response.setSecret(secret);

        PresignedPutObjectRequest presignedRequest = builsUploadUrl( documentType,keyName,contentType);
        String myURL = presignedRequest.url().toString();
        log.info("Presigned URL to upload a file to: " +myURL);
        log.info("Which HTTP method needs to be used when uploading a file: " + presignedRequest.httpRequest().method());

        // chiamare l'api di Gestore repository per salvare i dati
        //todo


        response.setUploadUrl(myURL);
        response.setUploadMethod(extractUploadMethod(presignedRequest.httpRequest().method()));

        DocumentInput documentRepositoryDto = new DocumentInput();
        log.info("Upload dati documento su gestore repository: " +myURL);

        documentClientCall.updatedocument(documentRepositoryDto);

        return response;

    }

    private boolean keyPresent(String keyName) {
        ResponseEntity<DocumentOutput> block = documentClientCall.getdocument(keyName);
        DocumentOutput doc = block!=null ? block.getBody(): null;
        return doc != null ? true : false ;
    }

    private FileCreationResponse.UploadMethodEnum extractUploadMethod(SdkHttpMethod method) {
       if(method.equals(SdkHttpMethod.POST)){
           return FileCreationResponse.UploadMethodEnum.POST;
       }
       return FileCreationResponse.UploadMethodEnum.PUT;
    }

    private PresignedPutObjectRequest  builsUploadUrl(String documentType, String keyName, String contentType) {

        String bucketName = mapDocumentTypeToBucket.get(documentType);

        S3Presigner presigner = getS3Presigner();
        return  signBucket(presigner, bucketName, keyName,contentType);

    }

    private S3Presigner getS3Presigner() {
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
        Region region = EU_CENTRAL_1;
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private PresignedPutObjectRequest signBucket(S3Presigner presigner, String bucketName, String keyName, String contenType) {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .contentType(contenType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return presignedRequest;

    }



    public FileDownloadResponse createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId) {
        // chiamare l'api di  GestoreRepositori per recupero dati
        //todo

        ResponseEntity<UserConfigurationOutput> userResponse = userConfigurationClientCall.getUser(xPagopaSafestorageCxId);
        UserConfigurationOutput user = null;
        if (userResponse!=null ){
            user = userResponse.getBody();
        }
        if (user == null ){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User Not Found : " + xPagopaSafestorageCxId);
        }
        ResponseEntity<DocumentOutput> block = documentClientCall.getdocument(fileKey);
        DocumentOutput doc = block!=null ? block.getBody(): null;
        if (doc==null ){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Document key Not Found : " + fileKey);
        }
        List<String> canRead = user.getCanRead();
// TODO: 26/01/2023  mettere doc.getDocumentType
        String typeDocument = doc.getCheckSum();
        if (!canRead.contains(typeDocument)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Client : "+ xPagopaSafestorageCxId+  " not has privilege for download document type " +typeDocument);
        }


        FileDownloadResponse downloadResponse = new FileDownloadResponse();
        downloadResponse.setChecksum(doc.getCheckSum());
        downloadResponse.setContentLength(BigDecimal.TEN);
        downloadResponse.setContentType("");
        downloadResponse.setDocumentStatus(doc.getDocumentState().value());
        downloadResponse.setDocumentType("");

        downloadResponse.setKey(fileKey);
        downloadResponse.setRetentionUntil(new Date());
        downloadResponse.setVersionId("");

         downloadResponse.setDownload(createFileDownloadInfo(fileKey,downloadResponse.getDocumentStatus(), downloadResponse.getDocumentType()));

        return downloadResponse;

    }

    private FileDownloadInfo createFileDownloadInfo(String fileKey, String status,  String documentType) {
        S3Presigner presigner = getS3Presigner();
        String bucketName = mapDocumentTypeToBucket.get(documentType);
        FileDownloadInfo fileDOwnloadInfo = null;
        if (hotStatus.contains(status)){
            fileDOwnloadInfo =getPresignedUrl(presigner, bucketName,  fileKey );
        }else{
            fileDOwnloadInfo =recoverDocumentFromBucket( presigner,  bucketName,fileKey);
        }

         return fileDOwnloadInfo;

    }

    private FileDownloadInfo recoverDocumentFromBucket(S3Presigner presigner, String bucketName, String fileKey) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        // mettere codice per far partire il recupero del file
        //todo
        fdinfo.setRetryAfter(MAX_RECOVER_COLD );
        return fdinfo;
    }

    private FileDownloadInfo getPresignedUrl(S3Presigner presigner, String bucketName, String keyName) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);

        String theUrl = presignedGetObjectRequest.url().toString();
        fdinfo.setUrl(theUrl);
        return fdinfo;

    }
}
