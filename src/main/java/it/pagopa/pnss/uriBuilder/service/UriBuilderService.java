package it.pagopa.pnss.uriBuilder.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.uriBuilder.client.GetRepositoryClient;
import it.pagopa.pnss.uriBuilder.model.DocumentRepositoryDto;
import org.aspectj.lang.annotation.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import com.amazonaws.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class UriBuilderService {

    @Autowired
    GetRepositoryClient gGetRepositoryClient;

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
    Region region = Region.EU_CENTRAL_1;
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

        DocumentRepositoryDto documentRepositoryDto = new DocumentRepositoryDto();
        gGetRepositoryClient.upLoadDocument(documentRepositoryDto);

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
        try {
            S3Presigner presigner = getS3Presigner();
            return  signBucket(presigner, bucketName, keyName,contentType);

        }catch (S3Exception e) {

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "S3Exception -> Message  "+ e.getMessage());
        }catch (Exception e) {

            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Generic Exception -> Message  "+ e.getMessage());
        }

    }

    private S3Presigner getS3Presigner() {
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();

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


        ResponseEntity <DocumentRepositoryDto> d = gGetRepositoryClient.retrieveDocument(fileKey);
        DocumentRepositoryDto doc = d!=null ? d.getBody(): null;

        FileDownloadResponse downloadResponse = new FileDownloadResponse();
        downloadResponse.setChecksum("");
        downloadResponse.setContentLength(BigDecimal.TEN);
        downloadResponse.setContentType("");
        downloadResponse.setDocumentStatus("");
        downloadResponse.setDocumentType("");

        downloadResponse.setKey(fileKey);
        downloadResponse.setRetentionUntil(new Date());
        downloadResponse.setVersionId("");

         downloadResponse.setDownload(createFileDownloadInfo(fileKey,downloadResponse.getDocumentStatus(), downloadResponse.getDocumentType()));

        return downloadResponse;

    }

    private FileDownloadInfo createFileDownloadInfo(String fileKey, String status,  String documentType) {

        String bucketName = mapDocumentTypeToBucket.get(documentType);
        FileDownloadInfo fileDOwnloadInfo = null;
        if (hotStatus.contains(status)){
            fileDOwnloadInfo =getPresignedUrl(bucketName,  fileKey );
        }else{
            fileDOwnloadInfo =recoverDocumentFromBucket(   bucketName,fileKey);
        }

         return fileDOwnloadInfo;

    }

    private FileDownloadInfo recoverDocumentFromBucket(String bucketName, String keyName) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        // mettere codice per far partire il recupero del file
        //todo

        try {
            AmazonS3 s3Client = createS3();
            fdinfo.setRetryAfter(MAX_RECOVER_COLD );
            com.amazonaws.services.s3.model.RestoreObjectRequest requestRestore = new com.amazonaws.services.s3.model.RestoreObjectRequest(bucketName, keyName, 2);
            s3Client.restoreObjectV2(requestRestore);
            ObjectMetadata response = s3Client.getObjectMetadata(bucketName, keyName);
            Boolean restoreFlag = response.getOngoingRestore();
            System.out.format("Restoration status: %s.\n",
                    restoreFlag ? "in progress" : "not in progress (finished or failed)");
            fdinfo.setRetryAfter(MAX_RECOVER_COLD);        ;
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "AmazonServiceException -> Message  "+ e.getMessage() + " The call was transmitted successfully, but Amazon S3 couldn't process ");

        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "ResponseStatusException -> Message  "+ e.getMessage() + " Amazon S3 couldn't be contacted for a response, or the client couldn't parse the response from Amazon S3. ");

        }
        return fdinfo;
    }

    private AmazonS3 createS3() {
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new com.amazonaws.auth.profile.ProfileCredentialsProvider())
                .withRegion(region.id())
                .build();
        return s3Client;
    }

    private FileDownloadInfo getPresignedUrl( String bucketName, String keyName) {
        S3Presigner presigner = getS3Presigner();
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
