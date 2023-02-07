package it.pagopa.pnss.uriBuilder.service;

import static it.pagopa.pnss.common.Constant.*;
import static it.pagopa.pnss.common.QueueNameConstant.BUCKET_HOT_NAME;
import static it.pagopa.pnss.common.QueueNameConstant.BUCKET_STAGE_NAME;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import it.pagopa.pn.template.rest.v1.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
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

@Service
@Slf4j
public class UriBuilderService {


    UserConfigurationClientCall userConfigurationClientCall;
    DocumentClientCall documentClientCall;

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
    }

    Map<String,String> mapDocumentTypeToBucket ;

    @PostConstruct
    public void createMap() {
        mapDocumentTypeToBucket= new HashMap<>();
        mapDocumentTypeToBucket.put(PN_NOTIFICATION_ATTACHMENTS,BUCKET_HOT_NAME);
        mapDocumentTypeToBucket.put(PN_AAR,BUCKET_HOT_NAME);
        mapDocumentTypeToBucket.put(PN_LEGAL_FACTS,BUCKET_STAGE_NAME);
        mapDocumentTypeToBucket.put(PN_EXTERNAL_LEGAL_FACTS,BUCKET_HOT_NAME);
        mapDocumentTypeToBucket.put(PN_DOWNTIME_LEGAL_FACTS,BUCKET_STAGE_NAME);

    }

    public ResponseEntity<FileCreationResponse> createUriForUploadFile(String xPagopaSafestorageCxId, FileCreationRequest request) {
        String contentType = request.getContentType();
        String documentType = request.getDocumentType();
        String status = request.getStatus();

        ResponseEntity ret = validationField(contentType, documentType, status);
        if (ret!=null){
            return ret;
        }
        log.info("--- REST INIZIO CHIAMATA USER CONFIGURATION");
        ResponseEntity<UserConfiguration> userResponse = userConfigurationClientCall.getUser(xPagopaSafestorageCxId);
        log.info("--- REST INIZIO CHIAMATA USER CONFIGURATION");
        UserConfiguration user = null;
        if (userResponse!=null ){
            user = userResponse.getBody();
        }
        log.info("--- CHECK UTENTE TROVATO ");
        if (user == null ){
            log.info("Utente NON TROVATO "+ xPagopaSafestorageCxId);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User Not Found : " + xPagopaSafestorageCxId);
        }
        log.info("--- UTENTE TROVATO ");

        log.info("CHECK Utente :"+ xPagopaSafestorageCxId +" abilitato a  "+documentType);

        List<String> canCreate = user.getCanCreate();
        if (!canCreate.contains(documentType)){
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

        Document documentRepositoryDto = new Document();
        log.info("Upload dati documento su gestore repository: " +myURL);
        documentRepositoryDto.setContentType(contentType);
        documentRepositoryDto.setDocumentKey(keyName);
        documentRepositoryDto.setDocumentState(Document.DocumentStateEnum.BOOKED);
        documentRepositoryDto.setDocumentType(retrieveDocType(documentType));
        //documentRepositoryDto.setRetentionPeriod();
        log.info("--- REST INIZIO UPDATE DOCUMENT ");
        documentClientCall.postdocument(documentRepositoryDto);
        log.info("--- REST FINE  UPDATE DOCUMENT ");
        return ResponseEntity.ok(response);

    }

    private ResponseEntity validationField(String contentType, String documentType, String status) {

        if(!listaTipoDocumenti.contains(contentType)){
            return ResponseEntity.badRequest().body("ContentType :"+contentType+" - Not valid");
        }
        if(!listaTipologieDoc.contains(documentType)){
            return ResponseEntity.badRequest().body("DocumentType :"+documentType+" - Not valid");
        }
        if (!status.equals("")){
            if (!listaStatus.contains(status)){
                return ResponseEntity.badRequest().body("status :"+status+" - Not valid ");
            }else{
                if (!(documentType.equals("PN_NOTIFICATION_ATTACHMENTS")&& status.equals("PRELOADED"))){
                    return ResponseEntity.badRequest().body("status :"+status+" - Not valid for documentType");
                }
            }
        }
        return null ;
    }

    private Document.DocumentTypeEnum retrieveDocType(String documentType) {
        if (documentType.equals(PN_DOWNTIME_LEGAL_FACTS)){
            //return Document.DocumentTypeEnum.PN_DOWNTIME_LEGAL_FACTS
        }
        if (documentType.equals(PN_NOTIFICATION_ATTACHMENTS)){
            return Document.DocumentTypeEnum.NOTIFICATION_ATTACHMENTS;
        }
        if (documentType.equals(PN_AAR)){
            return Document.DocumentTypeEnum.AAR;
        }
        if (documentType.equals(PN_LEGAL_FACTS)){
            return Document.DocumentTypeEnum.LEGAL_FACTS;
        }
        if (documentType.equals(PN_EXTERNAL_LEGAL_FACTS)){
            return Document.DocumentTypeEnum.EXTERNAL_LEGAL_FACTS;
        }
        return null;

    }

    private boolean keyPresent(String keyName) {
        ResponseEntity<Document> block = documentClientCall.getdocument(keyName);
        Document doc = block!=null ? block.getBody(): null;
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
        PresignedPutObjectRequest response = null;
        try{
            S3Presigner presigner = getS3Presigner();
            response =signBucket(presigner, bucketName, keyName,contentType);

        }catch (AmazonServiceException ase){
            log.error(" Errore AMAZON AmazonServiceException" ,ase );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException " );
        }catch (SdkClientException sce){
            log.error(" Errore AMAZON SdkClientException" ,sce );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException " );
        }catch (Exception e) {
            log.error(" Errore Generico" ,e );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico " );

        }

        return  response;

    }

    public static  S3Presigner getS3Presigner() {
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
        Region region = EU_CENTRAL_1;
        return S3Presigner.builder()
                .region(region)
                //.credentialsProvider(credentialsProvider)
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

        ResponseEntity<UserConfiguration> userResponse = userConfigurationClientCall.getUser(xPagopaSafestorageCxId);
        UserConfiguration user = null;
        if (userResponse!=null ){
            user = userResponse.getBody();
        }
        if (user == null ){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User Not Found : " + xPagopaSafestorageCxId);
        }
        ResponseEntity<Document> block = documentClientCall.getdocument(fileKey);
        Document doc = block!=null ? block.getBody(): null;
        if (doc==null ){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Document key Not Found : " + fileKey);
        }
        List<String> canRead = user.getCanRead();

        String typeDocument = doc.getDocumentType().getValue();
        if (!canRead.contains(typeDocument)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Client : "+ xPagopaSafestorageCxId+  " not has privilege for download document type " +typeDocument);
        }


        FileDownloadResponse downloadResponse = new FileDownloadResponse();

        BigDecimal contentLength = doc.getContentLenght();
//        BigDecimal fileLength = null;
//        if (StringUtils.isNotEmpty(contentLength)){
//            try {
//                fileLength = new BigDecimal(contentLength);
//            }catch (Exception e){
//                log.error("Unabel parse to bigdecimal value "+contentLength);
//            }
//        }
        downloadResponse.setChecksum(doc.getCheckSum().getValue());
        downloadResponse.setContentLength(contentLength);
        downloadResponse.setContentType(doc.getContentType());
        downloadResponse.setDocumentStatus(doc.getDocumentState().getValue());
        downloadResponse.setDocumentType(doc.getDocumentType().getValue());

        downloadResponse.setKey(fileKey);
        downloadResponse.setRetentionUntil(new Date());
        downloadResponse.setVersionId(null);

         downloadResponse.setDownload(createFileDownloadInfo(fileKey,downloadResponse.getDocumentStatus(), downloadResponse.getDocumentType()));

        return downloadResponse;

    }

    private FileDownloadInfo createFileDownloadInfo(String fileKey, String status,  String documentType) {
        FileDownloadInfo fileDOwnloadInfo = null;
        try{
            S3Presigner presigner = getS3Presigner();
            String bucketName = mapDocumentTypeToBucket.get(documentType);
            log.info("INIZIO RECUPERO URL DOWLOAND ");
            if (!Document.DocumentStateEnum.FREEZED.getValue().equals(status)){
                fileDOwnloadInfo =getPresignedUrl(presigner, bucketName,  fileKey );
            }else{
                fileDOwnloadInfo =recoverDocumentFromBucket( presigner,  bucketName,fileKey);
            }
        }catch (AmazonServiceException ase){
            log.error(" Errore AMAZON AmazonServiceException" ,ase );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException " );
        }catch (SdkClientException sce){
            log.error(" Errore AMAZON SdkClientException" ,sce );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException " );
        }catch (Exception e) {
            log.error(" Errore Generico" ,e );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico " );

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
        log.info("INIZIO CREAZIONE OGGETTO  GetObjectRequest");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();
        log.info("FINE  CREAZIONE OGGETTO  GetObjectRequest");
        log.info("INIZIO  CREAZIONE OGGETTO  GetObjectPresignRequest");
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
                .build();
        log.info("FINE  CREAZIONE OGGETTO  GetObjectPresignRequest");

        log.info("INIZIO  RECUPERO URL ");
        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
        log.info("FINE   RECUPERO URL ");

        String theUrl = presignedGetObjectRequest.url().toString();
        fdinfo.setUrl(theUrl);
        return fdinfo;

    }
}
