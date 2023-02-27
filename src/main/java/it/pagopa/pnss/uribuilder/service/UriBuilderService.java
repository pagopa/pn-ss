package it.pagopa.pnss.uribuilder.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.BucketName;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
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
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.Constant.*;
import static java.util.Map.entry;

@Service
@Slf4j
public class UriBuilderService {
	
	@Value("${object.lock.retention.mode}")
	private String objectLockRetentionMode;

    private final UserConfigurationClientCall userConfigurationClientCall;
    private final DocumentClientCall documentClientCall;
    private final AwsConfigurationProperties awsConfigurationProperties;
    private final BucketName bucketName;

    @Value("${uri.builder.presigned.url.duration.minutes}")
    String duration;
    
    private final RetentionService retentionService;

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall,
                             AwsConfigurationProperties awsConfigurationProperties, BucketName bucketName,
                             RetentionService retentionService) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
        this.awsConfigurationProperties = awsConfigurationProperties;
        this.bucketName = bucketName;
        this.retentionService = retentionService;
    }

    private Map<String, String> mapDocumentTypeToBucket;

    @PostConstruct
    public void createMap() {
        mapDocumentTypeToBucket = Map.ofEntries(entry(PN_NOTIFICATION_ATTACHMENTS, bucketName.ssHotName()),
                                                entry(PN_AAR, bucketName.ssHotName()),
                                                entry(PN_LEGAL_FACTS, bucketName.ssStageName()),
                                                entry(PN_EXTERNAL_LEGAL_FACTS, bucketName.ssHotName()),
                                                entry(PN_DOWNTIME_LEGAL_FACTS, bucketName.ssStageName()));
    }

    public Mono<FileCreationResponse> createUriForUploadFile(String xPagopaSafestorageCxId, FileCreationRequest request) {

        var contentType = request.getContentType();
        var documentType = request.getDocumentType();
        var status = request.getStatus();

        var secret = List.of(generateSecret());
        var metadata = Map.of("secret", secret.toString());

        return Mono.fromCallable(() -> validationField(contentType, documentType, status))
                   .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                   .handle((userConfiguration, synchronousSink) -> {
                       if (!userConfiguration.getUserConfiguration().getCanCreate().contains(documentType)) {
                           synchronousSink.error((new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                                              String.format(
                                                                                      "Client '%s' not has privilege for upload document " +
                                                                                      "type '%s'",
                                                                                      xPagopaSafestorageCxId,
                                                                                      documentType))));
                       } else {
                           synchronousSink.next(userConfiguration);
                       }
                   })
                   .doOnSuccess(object -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                   .then(documentClientCall.postDocument(new DocumentInput().contentType(contentType)
                                                                            .documentKey(GenerateRandoKeyFile.getInstance()
                                                                                                             .createKeyName(documentType))
                                                                            .documentState(BOOKED)
                                                                            .clientShortCode(xPagopaSafestorageCxId)
                                                                            .documentType(documentType))
                                           .retryWhen(Retry.max(10)
                                                           .filter(DocumentkeyPresentException.class::isInstance)
                                                           .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                                                               throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                                 "Non e' stato possibile " +
                                                                                                 "produrre una chiave per " + "user " +
                                                                                                 xPagopaSafestorageCxId);
                                                           })))
                   .map(insertedDocument -> {

                       PresignedPutObjectRequest presignedPutObjectRequest =
                               builsUploadUrl(documentType, 
                            		   		  insertedDocument.getDocument().getDocumentState(), 
                            		   		  insertedDocument.getDocument().getDocumentKey(), 
                            		   		  contentType, 
                            		   		  metadata);
                       String myURL = presignedPutObjectRequest.url().toString();
                       FileCreationResponse response = new FileCreationResponse();
                       response.setKey(insertedDocument.getDocument().getDocumentKey());
                       response.setSecret(secret.toString());

                       response.setUploadUrl(myURL);
                       response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest().method()));

                       return response;
                   })
                   .doOnNext(o -> log.info("--- RECUPERO PRESIGNED URL OK "));
    }

    private Mono<Boolean> validationField(String contentType, String documentType, String status) {

        if (!listaTipoDocumenti.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ContentType :" + contentType + " - Not valid");
        }
        if (!listaTipologieDoc.contains(documentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType :" + documentType + " - Not valid");
        }
        if (!status.equals("")) {
            if (!listaStatus.contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status :" + status + " - Not valid ");
            } else {
                if (!(documentType.equals("PN_NOTIFICATION_ATTACHMENTS") && status.equals("PRELOADED"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status :" + status + " - Not valid for documentType");
                }
            }
        }
        return Mono.just(true);
    }

    private FileCreationResponse.UploadMethodEnum extractUploadMethod(SdkHttpMethod method) {
        if (method.equals(SdkHttpMethod.POST)) {
            return FileCreationResponse.UploadMethodEnum.POST;
        }
        return FileCreationResponse.UploadMethodEnum.PUT;
    }

    private PresignedPutObjectRequest builsUploadUrl(String documentType, String documentState, String keyName, String contentType, Map<String, String> secret) {

        String bucketName = mapDocumentTypeToBucket.get(documentType);
        PresignedPutObjectRequest response;
        S3Presigner presigner = getS3Presigner();
        
        
        try {
            response = signBucket(presigner, bucketName, keyName, documentState, documentType, contentType, secret);
        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");
        }

        return response;
    }

    public S3Presigner getS3Presigner() {
        return S3Presigner.builder().region(Region.of(awsConfigurationProperties.regionCode())).build();
    }

    private PresignedPutObjectRequest signBucket(S3Presigner s3Presigner, String bucketName, 
    		String keyName, String documentState, String documentType,
    		String contenType, Map<String,String> secret) {
    	log.debug("signBucket() : START : s3Presigner IN : bucketName {} : keyName {} : documentState {} : documentType {} : contenType {} : secret {}",
    			bucketName, keyName, documentState, documentType, contenType, secret);
    	
        PutObjectRequest objectRequest = retentionService.getPutObjectForPresignRequest(
        		bucketName,keyName,contenType,secret,keyName,documentState,documentType);
        
        log.info("sign bucket {}", duration);
        PutObjectPresignRequest preSignRequest = PutObjectPresignRequest.builder()
                                                                        .signatureDuration(Duration.ofMinutes(Long.parseLong(duration)))
                                                                        .putObjectRequest(objectRequest)
                                                                        .build();
        return s3Presigner.presignPutObject(preSignRequest);
    }

    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly) {
        return Mono.fromCallable(() -> validationFieldCreateUri(fileKey, xPagopaSafestorageCxId))
                   .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                   .doOnSuccess(o -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                   .flatMap(userConfigurationResponse -> {
                       List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();

                       return documentClientCall.getdocument(fileKey)
                                                .onErrorResume(DocumentKeyNotPresentException.class,
                                                               throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                                                   "Document key Not " +
                                                                                                                   "Found : " + fileKey)))

                                                .map(documentResponse -> {
                                                    if (!canRead.contains(documentResponse.getDocument()
                                                                                          .getDocumentType()
                                                                                          .getTipoDocumento())) {
                                                        throw (new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                                                           "Client : " + xPagopaSafestorageCxId +
                                                                                           " not has privilege for read document type " +
                                                                                           documentResponse.getDocument()
                                                                                                           .getDocumentType()));
                                                    }

                                                    return documentResponse.getDocument();
                                                })
                                                .doOnSuccess(o -> log.info("---  FINE  CHECK PERMESSI LETTURA"));
                   })
                   .map(doc -> getFileDownloadResponse(fileKey, doc, metadataOnly))
                   .doOnNext(o -> log.info("--- RECUPERO PRESIGNE URL OK "));
    }

    @NotNull
    private FileDownloadResponse getFileDownloadResponse(String fileKey, Document doc, Boolean metadataOnly) {
        FileDownloadResponse downloadResponse = new FileDownloadResponse();

        BigDecimal contentLength = doc.getContentLenght();

        downloadResponse.setChecksum(doc.getCheckSum() != null ? doc.getCheckSum() : null);
        downloadResponse.setContentLength(contentLength);
        downloadResponse.setContentType(doc.getContentType());
        // NOTA: deve essere restituito lo satto logico, piuttosto che lo stato tecnico
        //downloadResponse.setDocumentStatus(doc.getDocumentState().getValue());
        if (doc.getDocumentLogicalState() != null) {
            downloadResponse.setDocumentStatus(doc.getDocumentLogicalState());
        } else {
            downloadResponse.setDocumentStatus("");
        }
        log.info("getFileDownloadResponse() : documentState {} : documentLogicalState {}",
                 doc.getDocumentState(),
                 doc.getDocumentLogicalState());
        downloadResponse.setDocumentType(doc.getDocumentType().getTipoDocumento());

        downloadResponse.setKey(fileKey);
        downloadResponse.setRetentionUntil(new Date());
        downloadResponse.setVersionId(null);

        if (Boolean.FALSE.equals(metadataOnly) || metadataOnly == null) {
            downloadResponse.setDownload(createFileDownloadInfo(fileKey,
                                                                downloadResponse.getDocumentStatus(),
                                                                downloadResponse.getDocumentType()));
        }
        return downloadResponse;
    }

    private Mono<Boolean> validationFieldCreateUri(String fileKey, String xPagopaSafestorageCxId) {
        return Mono.just(true);
    }

    private FileDownloadInfo createFileDownloadInfo(String fileKey, String status, String documentType) {
        FileDownloadInfo fileDOwnloadInfo = null;
        try {
            S3Presigner presigner = getS3Presigner();
            String bucketName = mapDocumentTypeToBucket.get(documentType);
            log.info("INIZIO RECUPERO URL DOWLOAND ");
            if (!status.equals(FREEZED)) {
                fileDOwnloadInfo = getPresignedUrl(presigner, bucketName, fileKey);
            } else {
                fileDOwnloadInfo = recoverDocumentFromBucket(presigner, bucketName, fileKey);
            }
        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");

        }

        return fileDOwnloadInfo;

    }

    private FileDownloadInfo recoverDocumentFromBucket(S3Presigner presigner, String bucketName, String fileKey) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        // mettere codice per far partire il recupero del file
        //todo
        fdinfo.setRetryAfter(MAX_RECOVER_COLD);
        return fdinfo;
    }

    private FileDownloadInfo getPresignedUrl(S3Presigner presigner, String bucketName, String keyName) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        log.info("INIZIO CREAZIONE OGGETTO  GetObjectRequest");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(keyName).build();
        log.info("FINE  CREAZIONE OGGETTO  GetObjectRequest");
        log.info("INIZIO  CREAZIONE OGGETTO  GetObjectPresignRequest");
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                                                                                 .signatureDuration(Duration.ofMinutes(Long.parseLong(
                                                                                         duration)))
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

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[256];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }
}
