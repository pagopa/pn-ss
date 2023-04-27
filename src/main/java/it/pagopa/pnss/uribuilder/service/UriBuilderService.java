package it.pagopa.pnss.uribuilder.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.ChecksumException;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.common.exception.ContentTypeNotFoundException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.QueryParamException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.transformation.service.CommonS3ObjectService;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

import static it.pagopa.pnss.common.constant.Constant.*;

@Service
@Slf4j
public class UriBuilderService extends CommonS3ObjectService {

    @Value("${uri.builder.presigned.url.duration.minutes}")
    String duration;

    @Value("${uri.builder.stay.Hot.Bucket.tyme.days}")
    Integer stayHotTime;

    @Value("${header.presignUrl.checksum-sha256:#{null}}")
    String headerChecksumSha256;

    @Value("${presignedUrl.initial.newDocument.state}")
    String initialNewDocumentState;

    @Value("${test.aws.s3.endpoint:#{null}}")
    private String testAwsS3Endpoint;

    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    String queryParamPresignedUrlTraceId;

    @Value("${max.restore.time.cold}")
    BigDecimal maxRestoreTimeCold;

    private final UserConfigurationClientCall userConfigurationClientCall;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;
    private final DocTypesClientCall docTypesClientCall;
    private final DocTypesService docTypesService;
    private static final String AMAZONERROR = "Error AMAZON AmazonServiceException ";

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall,
                             BucketName bucketName, DocTypesClientCall docTypesClientCall, DocTypesService docTypesService) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.docTypesClientCall = docTypesClientCall;
        this.docTypesService = docTypesService;
    }

    private Mono<String> getBucketName(String docType) {

        return docTypesClientCall.getdocTypes(docType).map(documentTypeResponse -> {
            var transformations = documentTypeResponse.getDocType().getTransformations();
            if (transformations == null || transformations.isEmpty()) {
                return bucketName.ssHotName();
            } else return bucketName.ssStageName();
        });
    }

    public Mono<FileCreationResponse> createUriForUploadFile(String xPagopaSafestorageCxId, FileCreationRequest request,
                                                             String checksumValue, String xTraceIdValue) {

        var contentType = request.getContentType();
        var documentType = request.getDocumentType();

        // NOTA : in questo modo, sono immutabili
        var secret = new ArrayList<String>();
        secret.add(generateSecret());
        var metadata = new HashMap<String, String>();
        metadata.put("secret", secret.toString());

        return validationField(contentType, documentType, xTraceIdValue)
                .flatMap(booleanMono -> userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                                            .handle((userConfiguration, synchronousSink) -> {
                                                if (!userConfiguration.getUserConfiguration().getCanCreate().contains(documentType)) {
                                                    synchronousSink.error((new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                                                                       String.format(
                                                                                                               "Client '%s' not has " +
                                                                                                               "privilege for upload " +
                                                                                                               "document " +
                                                                                                               "type '%s'",
                                                                                                               xPagopaSafestorageCxId,
                                                                                                               documentType))));
                                                }
                                                else {
                                                    synchronousSink.next(userConfiguration);
                                                }
                                            })
                                            .map(unused-> getFileExtension(contentType))
                                            .onErrorResume(ContentTypeNotFoundException.class, e-> Mono.just(""))
                                            .flatMap(fileExtension -> {
                                                var documentKeyTmp = String.format("%s%s",
                                                                                   GenerateRandoKeyFile.getInstance()
                                                                                                       .createKeyName(documentType),
                                                                                                        fileExtension);
                                                log.info("createUriForUploadFile(): documentKeyTmp = {} : ", documentKeyTmp);
                                                return documentClientCall.postDocument(new DocumentInput().contentType(request.getContentType())
                                                                                                          .documentKey(documentKeyTmp)
                                                                                                          .documentState(
                                                                                                                  initialNewDocumentState)
                                                                                                          .clientShortCode(
                                                                                                                  xPagopaSafestorageCxId)
                                                                                                          .documentType(request.getDocumentType()))
                                                                         .retryWhen(Retry.max(10)
                                                                                         .filter(DocumentkeyPresentException.class::isInstance)
                                                                                         .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                                                                                             throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                                                               "Non e' " +
                                                                                                                               "stato " +
                                                                                                                               "possibile" +
                                                                                                                               " " +
                                                                                                                               "produrre " +
                                                                                                                               "una " +
                                                                                                                               "chiave " +
                                                                                                                               "per " +
                                                                                                                               "user " +
                                                                                                                               xPagopaSafestorageCxId);
                                                                                         }));
                                            })
                                            .flatMap(insertedDocument ->
//                       PresignedPutObjectRequest presignedPutObjectRequest =
                                                             buildsUploadUrl(documentType,
                                                                             insertedDocument.getDocument().getDocumentState(),
                                                                             insertedDocument.getDocument().getDocumentKey(),
                                                                             contentType,
                                                                             metadata,
                                                                             insertedDocument.getDocument().getDocumentType().getChecksum(),
                                                                             checksumValue, xTraceIdValue).map(presignedPutObjectRequest -> {
                                                                 FileCreationResponse response = new FileCreationResponse();
                                                                 response.setKey(insertedDocument.getDocument().getDocumentKey());
                                                                 response.setSecret(secret.toString());
                                                                 response.setUploadUrl(presignedPutObjectRequest.url().toString());
                                                                 response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest()
                                                                                                                                       .method()));
                                                                 return response;
                                                             })

                                                    )
                                            .doOnNext(o -> log.info("--- RECUPERO PRESIGNED URL OK "));
    }

    private Mono<Boolean> validationField(String contentType, String documentType, String xTraceIdValue) {
        return Mono.justOrEmpty(contentType).handle((s, sink) -> {
            if (contentType.isBlank()) {
                sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ContentType : Is missing"));
            } else 
            	if (documentType == null || documentType.isBlank()) {
                sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType : Is missing"));
            } else if (xTraceIdValue == null || xTraceIdValue.isBlank()) {
                sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, queryParamPresignedUrlTraceId + " : Is missing"));
            } 
            else {
                sink.next(contentType);
            }
        }).flatMap(mono -> docTypesService.getAllDocumentType()).handle((documentTypes, sink) -> {
            if (documentTypes.stream().noneMatch(item -> item.getTipoDocumento().equals(documentType))) {
                sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType :" + documentType + " - Not valid"));
            } else {
                sink.next(documentTypes);
            }
        }).map(o -> true).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ContentType : Is missing")));
    }

    private FileCreationResponse.UploadMethodEnum extractUploadMethod(SdkHttpMethod method) {
        if (method.equals(SdkHttpMethod.POST)) {
            return FileCreationResponse.UploadMethodEnum.POST;
        }
        return FileCreationResponse.UploadMethodEnum.PUT;
    }

    private Mono<PresignedPutObjectRequest> buildsUploadUrl(String documentType, String documentState, String documentKey,
                                                            String contentType, Map<String, String> secret, ChecksumEnum checksumType,
                                                            String checksumValue, String xTraceIdValue) {
        log.info("buildsUploadUrl() : START : " + "documentType {} : documentState {} : documentKey {} : " +
                 "contentType {} : secret {} : checksumType {} : checksumValue {}",
                 documentType,
                 documentState,
                 documentKey,
                 contentType,
                 secret,
                 checksumType,
                 checksumValue);

        S3Presigner presigner = getS3Presigner();
        return getBucketName(documentType).flatMap(buckName -> signBucket(presigner,
                                                                          buckName,
                                                                          documentKey,
                                                                          documentState,
                                                                          documentType,
                                                                          contentType,
                                                                          secret,
                                                                          checksumType,
                                                                          checksumValue,
                                                                          xTraceIdValue))
//        return signBucket(presigner,
//        				  getBucketName(documentType),
//        				  documentKey,
//        				  documentState,
//        				  documentType,
//        				  contentType,
//        				  secret,
//        				  checksumType,
//        				  checksumValue)
                                          .onErrorResume(ChecksumException.class, throvable -> {
                                              log.error("buildsUploadUrl() : Errore impostazione ChecksumValue = {}",
                                                        throvable.getMessage(),
                                                        throvable);
                                              return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                                            throvable.getMessage()));
                                          }).onErrorResume(AmazonServiceException.class, throvable -> {
                    log.error("buildsUploadUrl() : " + AMAZONERROR + "= {}", throvable.getMessage(), throvable);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, AMAZONERROR));
                }).onErrorResume(ResponseStatusException.class, throvable -> {
                    log.error("buildsUploadUrl() : Errore AMAZON SdkClientException = {}", throvable.getMessage(), throvable);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, AMAZONERROR));
                }).onErrorResume(Exception.class, throvable -> {
                    log.error("buildsUploadUrl() : Errore generico: {}", throvable.getMessage(), throvable);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico"));
                });

    }

    private Mono<PresignedPutObjectRequest> signBucket(S3Presigner s3Presigner, String bucketName, String documentKey,
                                                       String documentState, String documentType, String contenType,
                                                       Map<String, String> secret, ChecksumEnum checksumType, String checksumValue, String xTraceIdValue) {

        log.debug("signBucket() : START : s3Presigner IN : " + "bucketName {} : keyName {} : " +
                  "documentState {} : documentType {} : contenType {} : " + "secret {} : checksumType{} : checksumValue {}",
                  bucketName,
                  documentKey,
                  documentState,
                  documentType,
                  contenType,
                  secret,
                  checksumType,
                  checksumValue);
        log.info("signBucket() : sign bucket {}", duration);

        if (checksumType == null || checksumValue == null || checksumValue.isBlank()) {
            return Mono.error(new ChecksumException("Non e' stato possibile impostare il ChecksumValue nella PutObjectRequest"));
        }
        if (queryParamPresignedUrlTraceId == null || queryParamPresignedUrlTraceId.isBlank()) {
            return Mono.error(new QueryParamException("Property \"queryParam.presignedUrl.traceId\" non impostata"));
        }

        return Mono.just(checksumType)
                   .flatMap(checksumTypeToEvaluate -> {
                       if (ChecksumEnum.MD5.name().equals(checksumTypeToEvaluate.name())) {
                           return Mono.just(PutObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(documentKey)
                                                            .contentType(contenType)
                                                            .metadata(secret)
                                                            .contentMD5(checksumValue)
                                                            //.tagging(storageType)
                                                            // Aggiungere queryParam custom alle presigned URL di upload e download
                                                            .overrideConfiguration(awsRequestOverrideConfiguration -> awsRequestOverrideConfiguration.putRawQueryParameter(
                                                                    queryParamPresignedUrlTraceId,
                                                                    xTraceIdValue))
                                                            .build());
                       } else if (headerChecksumSha256 != null && !headerChecksumSha256.isBlank() && secret != null &&
                                  ChecksumEnum.SHA256.name().equals(checksumTypeToEvaluate.name())) {
                           return Mono.just(PutObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(documentKey)
                                                            .contentType(contenType)
                                                            .metadata(secret)
                                                            .checksumSHA256(checksumValue)
                                                            //.tagging(storageType)
                                                            // Aggiungere queryParam custom alle presigned URL di upload e download
                                                            .overrideConfiguration(awsRequestOverrideConfiguration -> awsRequestOverrideConfiguration.putRawQueryParameter(
                                                                    queryParamPresignedUrlTraceId,
                                                                    xTraceIdValue))
                                                            .build());
                       } else {
                           return Mono.error(new ChecksumException(
                                   "Non e' stato possibile impostare il ChecksumValue nella PutObjectRequest"));
                       }
                   })
                   .map(putObjectRequest -> PutObjectPresignRequest.builder()
                                                                   .signatureDuration(Duration.ofMinutes(Long.parseLong(duration)))
                                                                   .putObjectRequest(putObjectRequest)
                                                                   .build())
                   .flatMap(putObjectPresignRequest -> Mono.just(s3Presigner.presignPutObject(putObjectPresignRequest)));
    }

    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId, String xTraceIdValue, Boolean metadataOnly) {
        if (xTraceIdValue== null || StringUtils.isBlank(xTraceIdValue)) {
            String errorMsg = String.format("Header %s is missing", queryParamPresignedUrlTraceId);
            log.error("NullPointerException: {}", errorMsg);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg));
        }
        return Mono.fromCallable(this::validationFieldCreateUri)
                   .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                   .doOnSuccess(o -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                   .flatMap(userConfigurationResponse -> {
                       List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();

                       return documentClientCall.getDocument(fileKey)
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
                               .handle((document, sink)->
                               {
                                   if(document.getDocumentState().equalsIgnoreCase(DELETED))
                                   {
                                       sink.error(new ResponseStatusException(HttpStatus.GONE));
                                   }
                                   else {
                                       sink.next(document);
                                   }
                               })
                               .doOnSuccess(o -> log.info("---  FINE  CHECK PERMESSI LETTURA"));
                   })
                   .flatMap(doc -> getFileDownloadResponse(fileKey, xTraceIdValue,(Document) doc, metadataOnly))
                   .doOnNext(o -> log.info("--- RECUPERO PRESIGNED URL OK "))
                   .onErrorResume(RuntimeException.class, throwable -> {
                       log.error("createUriForDownloadFile() : erroe generico = {}", throwable.getMessage(), throwable);
                       return Mono.error(throwable);
                   });

    }

    @NotNull
    private Mono<FileDownloadResponse> getFileDownloadResponse(String fileKey, String xTraceIdValue, Document doc, Boolean metadataOnly) {

        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();

        return Mono.just(fileDownloadResponse)
                .map(unused ->
                {
                    BigDecimal contentLength = doc.getContentLenght();

                    // NOTA: deve essere restituito lo stato logico, piuttosto che lo stato tecnico
                    if (doc.getDocumentLogicalState() != null) {
                        fileDownloadResponse.setDocumentStatus(doc.getDocumentLogicalState());
                    } else {
                        fileDownloadResponse.setDocumentStatus("");
                    }

                    if ((Boolean.FALSE.equals(metadataOnly) || metadataOnly == null) && (doc.getDocumentState() == null || !(doc.getDocumentState()
                            .equalsIgnoreCase(
                                    TECHNICAL_STATUS_AVAILABLE) ||
                            doc.getDocumentState()
                                    .equalsIgnoreCase(
                                            TECHNICAL_STATUS_ATTACHED) ||
                            doc.getDocumentState()
                                    .equalsIgnoreCase(
                                            TECHNICAL_STATUS_FREEZED)))) {
                        throw (new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Document : " + doc.getDocumentKey() + " not has a valid state "));
                    }

                    if (doc.getRetentionUntil() != null && !doc.getRetentionUntil().isBlank()) {
                        try {
                            final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
                            fileDownloadResponse.setRetentionUntil(new SimpleDateFormat(PATTERN_FORMAT).parse(doc.getRetentionUntil()));
                        } catch (Exception e) {
                            log.error("getFileDownloadResponse() : errore = {}", e.getMessage(), e);
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                        }
                    }

                    return fileDownloadResponse
                            .checksum(doc.getCheckSum() != null ? doc.getCheckSum() : null)
                            .contentLength(contentLength)
                            .contentType(doc.getContentType())
                            .documentType(doc.getDocumentType().getTipoDocumento())
                            .key(fileKey)
                            .versionId(null);

                })
                .flatMap(unused ->
                        {
                            if (!metadataOnly)
                                return createFileDownloadInfo(fileKey, xTraceIdValue, doc.getDocumentState(), doc.getDocumentType().getTipoDocumento());
                            else return Mono.empty();
                        }
                )
                .map(fileDownloadResponse::download)
                .switchIfEmpty(Mono.just(fileDownloadResponse));


//        return createFileDownloadInfo(fileKey, xTraceIdValue, doc.getDocumentState(), doc.getDocumentType().getTipoDocumento()).map(fileDownloadInfo -> {
//            FileDownloadResponse downloadResponse = new FileDownloadResponse();
//            BigDecimal contentLength = doc.getContentLenght();
//
//            // NOTA: deve essere restituito lo stato logico, piuttosto che lo stato tecnico
//            if (doc.getDocumentLogicalState() != null) {
//                downloadResponse.setDocumentStatus(doc.getDocumentLogicalState());
//            } else {
//                downloadResponse.setDocumentStatus("");
//            }
//
//            downloadResponse
//                            .checksum(doc.getCheckSum() != null ? doc.getCheckSum() : null)
//                            .contentLength(contentLength)
//                            .contentType(doc.getContentType())
//                            .documentType(doc.getDocumentType().getTipoDocumento())
//                            .key(fileKey);
//
//
//            if (doc.getRetentionUntil() != null && !doc.getRetentionUntil().isBlank()) {
//                try {
//                    final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
//                    downloadResponse.setRetentionUntil(new SimpleDateFormat(PATTERN_FORMAT).parse(doc.getRetentionUntil()));
//                } catch (Exception e) {
//                    log.error("getFileDownloadResponse() : errore = {}", e.getMessage(), e);
//                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
//                }
//            }
//
//            downloadResponse.setVersionId(null);
//
//            if ((Boolean.FALSE.equals(metadataOnly) || metadataOnly == null) && (doc.getDocumentState() == null || !(doc.getDocumentState()
//                                                                                                                        .equalsIgnoreCase(
//                                                                                                                                TECHNICAL_STATUS_AVAILABLE) ||
//                                                                                                                     doc.getDocumentState()
//                                                                                                                        .equalsIgnoreCase(
//                                                                                                                                TECHNICAL_STATUS_ATTACHED) ||
//                                                                                                                     doc.getDocumentState()
//                                                                                                                        .equalsIgnoreCase(
//                                                                                                                                TECHNICAL_STATUS_FREEZED)))) {
//                throw (new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                                                   "Document : " + doc.getDocumentKey() + " not has a valid state "));
//            }
//
//            return downloadResponse;
//        });
    }

    private Mono<Boolean> validationFieldCreateUri() {
        return Mono.just(true);
    }

    private Mono<FileDownloadInfo> createFileDownloadInfo(String fileKey, String xTraceIdValue, String status, String documentType) {
            log.info("INIZIO RECUPERO URL DOWNLOAD ");
            if (!status.equalsIgnoreCase(TECHNICAL_STATUS_FREEZED)) {
                return Mono.just( getPresignedUrl(bucketName.ssHotName(), fileKey, xTraceIdValue));
            } else {
                return Mono.just( recoverDocumentFromBucket(bucketName.ssHotName(), fileKey) );
            }
    }

    private FileDownloadInfo recoverDocumentFromBucket(String bucketName, String keyName) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        // mettere codice per far partire il recupero del file
        log.info("--- RESTORE DOCUMENT : " + keyName);

        try {
            log.info("--- CREATION S3 CLIENT DOCUMENT : " + keyName);
            AmazonS3 s3Client = getAmazonS3();

            // Create and submit a request to restore an object from Glacier for two days.
            log.info("--- REQUIRE RESTORE OBJECT DOCUMENT : " + keyName);
            RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, keyName, stayHotTime);
            log.info("--- RESTORE OBJECT DOCUMENT : " + keyName);
            s3Client.restoreObjectV2(requestRestore);

            // Check the restoration status of the object.
            ObjectMetadata response = s3Client.getObjectMetadata(bucketName, keyName);
            Boolean restoreFlag = response.getOngoingRestore();
            log.info("--- RETENTION DATE " + response.getHttpExpiresDate() + " DOCUMENT " + keyName);
            log.info("Restore status: %s.\n", restoreFlag ? "in progress" : "not in progress (finished or failed)");
        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            if (!ase.getErrorCode().equalsIgnoreCase("RestoreAlreadyInProgress")) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, AMAZONERROR + "- " + ase.getErrorMessage());
            }
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON SdkClientException - " + sce.getMessage());
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico " + e.getMessage());

        }

        fdinfo.setRetryAfter(maxRestoreTimeCold);
        return fdinfo;
    }


    private FileDownloadInfo getPresignedUrl(String bucketName, String keyName, String xTraceIdValue) {

        try {
            S3Presigner presigner = getS3Presigner();
            FileDownloadInfo fdinfo = new FileDownloadInfo();
            log.info("INIZIO CREAZIONE OGGETTO  GetObjectRequest");
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                                .bucket(bucketName)
                                                                .key(keyName)
                                                                .overrideConfiguration(awsRequestOverrideConfiguration -> awsRequestOverrideConfiguration.putRawQueryParameter(
                                                                        queryParamPresignedUrlTraceId,
                                                                        xTraceIdValue))
                                                                .build();
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

        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Errore AMAZON AmazonServiceException - " + ase.getMessage());
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                              "Errore AMAZON AmazonServiceException - " + sce.getMessage());
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");

        }

    }

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[256];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    private String getFileExtension(String contentType)
    {
        try {
        	String mime = MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(contentType).getExtension();
        	if(mime == null) {
        		mime = "";
        	}
        	
        	return mime;
        }
        catch(MimeTypeException exception)
        {
            throw new ContentTypeNotFoundException(contentType);
        }
    }
}
