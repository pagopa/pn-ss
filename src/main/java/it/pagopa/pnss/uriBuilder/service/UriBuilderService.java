package it.pagopa.pnss.uriBuilder.service;

import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import org.springframework.stereotype.Service;
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

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
public class UriBuilderService {

    public static final String PN_NOTIFICATION_ATTACHMENTS ="PN_NOTIFICATION_ATTACHMENTS";
    public static final String PN_AAR="PN_AAR";
    public static final String PN_LEGAL_FACTS="PN_LEGAL_FACTS";
    public static final String PN_EXTERNAL_LEGAL_FACTS="PN_EXTERNAL_LEGAL_FACTS";
    public static final String PN_DOWNTIME_LEGAL_FACTS="PN_DOWNTIME_LEGAL_FACTS";
    public static final BigDecimal MAX_RECOVER_COLD = new BigDecimal(259200);
    public static final String BUCKET_STAGING="Staging";
    public static final String BUCKET_HOT ="Hot";
    List <String> hotStatus;
    List <String> coldStatus;
    Map<String,String> mapDocumentTypeToBucket ;

    public UriBuilderService() {

        mapDocumentTypeToBucket.put(PN_NOTIFICATION_ATTACHMENTS,BUCKET_HOT);
        mapDocumentTypeToBucket.put(PN_AAR,BUCKET_HOT);
        mapDocumentTypeToBucket.put(PN_LEGAL_FACTS,BUCKET_STAGING);
        mapDocumentTypeToBucket.put(PN_EXTERNAL_LEGAL_FACTS,BUCKET_HOT);
        mapDocumentTypeToBucket.put(PN_DOWNTIME_LEGAL_FACTS,BUCKET_STAGING);
        hotStatus = Arrays.asList("cold");
        coldStatus = Arrays.asList("hot");
    }

    public FileCreationResponse createUriForUploadFile(String contentType, String documentType, String status) {
        FileCreationResponse response = new FileCreationResponse();
        String keyName = createKeyName(documentType);
        response.setKey(keyName);
        String secret=null;
        response.setSecret(secret);

        PresignedPutObjectRequest presignedRequest = builsUploadUrl( documentType,keyName,contentType);
        String myURL = presignedRequest.url().toString();
        System.out.println("Presigned URL to upload a file to: " +myURL);
        System.out.println("Which HTTP method needs to be used when uploading a file: " + presignedRequest.httpRequest().method());

        // chiamare l'api di Gestore repository per salvare i dati
        //todo


        response.setUploadUrl(myURL);
        response.setUploadMethod(extractUploadMethod(presignedRequest.httpRequest().method()));

        return response;

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
        Region region = Region.EU_CENTRAL_1;
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

    private String createKeyName(String documentType) {
        UUID temp = UUID.randomUUID();
        String uuidString = Long.toHexString(temp.getMostSignificantBits())
                + Long.toHexString(temp.getLeastSignificantBits());
        return documentType+"-"+uuidString;
    }

    public FileDownloadResponse createUriForDownloadFile(String fileKey) {
        // chiamare l'api di  GestoreRepositori per recupero dati
        //todo
        FileDownloadResponse downloadResponse = new FileDownloadResponse();
        downloadResponse.setChecksum("");
        downloadResponse.setContentLength(BigDecimal.TEN);
        downloadResponse.setContentType("");
        downloadResponse.setDocumentStatus("");
        downloadResponse.setDocumentType("");

        downloadResponse.setKey(fileKey);
        downloadResponse.setRetentionUntil(new Date());
        downloadResponse.setVersionId("");
        // status valore di ritorno dalla chiamata verso gestore repository
        String status = "";
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
