package it.pagopa.pnss.uribuilder.service;

import com.amazonaws.SdkClientException;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.*;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.exception.QueryParamException;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class UriBuilderService {

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

    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;

    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;

    @Value("${uri.builder.get-file.can-execute-patch}")
    private boolean canExecutePatch;

    private final UserConfigurationClientCall userConfigurationClientCall;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;
    private final DocTypesClientCall docTypesClientCall;
    private final S3Service s3Service;
    private final S3Presigner s3Presigner;
    private static final String AMAZONERROR = "Error AMAZON AmazonServiceException ";
    private static final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));

    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall,
                             BucketName bucketName, DocTypesClientCall docTypesClientCall, S3Service s3Service, S3Presigner s3Presigner) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.docTypesClientCall = docTypesClientCall;
        this.s3Service = s3Service;
        this.s3Presigner = s3Presigner;
    }

    private Mono<String> getBucketName(DocumentType docType) {
        var transformations = docType.getTransformations();
        if (transformations == null || transformations.isEmpty()) {
            return Mono.just(bucketName.ssHotName());
        } else return Mono.just(bucketName.ssStageName());
    }

    public Mono<FileCreationResponse> createUriForUploadFile(String xPagopaSafestorageCxId, FileCreationRequest request,
                                                             String checksumValue, String xTraceIdValue) {

        log.debug(LogUtils.INVOKING_METHOD, CREATE_URI_FOR_UPLOAD_FILE, Stream.of(xPagopaSafestorageCxId, request, checksumValue, xTraceIdValue).toList());
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
                                            .flatMap(fileExtension -> {
                                                var documentKeyTmp = String.format("%s%s",
                                                                                   GenerateRandoKeyFile.getInstance()
                                                                                                       .createKeyName(documentType),
                                                                                                        fileExtension);
                                                DocumentInput documentInput = new DocumentInput().contentType(request.getContentType())
                                                        .documentKey(documentKeyTmp)
                                                        .documentState(
                                                                initialNewDocumentState)
                                                        .clientShortCode(
                                                                xPagopaSafestorageCxId)
                                                        .documentType(request.getDocumentType());
                                                return documentClientCall.postDocument(documentInput)
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

                                                             buildsUploadUrl(insertedDocument.getDocument(),
                                                                             checksumValue,
                                                                             metadata,
                                                                             xTraceIdValue).map(presignedPutObjectRequest -> {
                                                                 log.info(LogUtils.INSERTED_DATA_IN_DYNAMODB_TABLE, managerDynamoTableName.documentiName());
                                                                 FileCreationResponse response = new FileCreationResponse();
                                                                 response.setKey(insertedDocument.getDocument().getDocumentKey());
                                                                 response.setSecret(secret.toString());
                                                                 response.setUploadUrl(presignedPutObjectRequest.url().toString());
                                                                 response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest()
                                                                                                                                       .method()));
                                                                 return response;
                                                             })

                                                    )
                                            .doOnSuccess(fileCreationResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, CREATE_URI_FOR_UPLOAD_FILE, fileCreationResponse));
    }

    private Mono<Boolean> validationField(String contentType, String documentType, String xTraceIdValue) {
        return Mono.justOrEmpty(contentType)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_CONTENT_TYPE)))
                .handle((s, sink) -> {
                    final String VALIDATION_FIELD = "contentType, documentType, xTraceIdValue";

                    log.logChecking(VALIDATION_FIELD);

                    if (contentType.isBlank()) {
                        log.logCheckingOutcome(VALIDATION_FIELD, false, MISSING_CONTENT_TYPE);
                        sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_CONTENT_TYPE));
                    } else if (documentType == null || documentType.isBlank()) {
                        log.logCheckingOutcome(VALIDATION_FIELD, false, MISSING_DOCUMENT_TYPE);
                        sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType : Is missing"));
                    } else if (xTraceIdValue == null || xTraceIdValue.isBlank()) {
                        log.logCheckingOutcome(VALIDATION_FIELD, false, MISSING_TRACE_ID);
                        sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_TRACE_ID));
                    } else {
                        log.logCheckingOutcome(VALIDATION_FIELD, true);
                        sink.next(contentType);
                    }
                })
                .flatMap(mono -> docTypesClientCall.getdocTypes(documentType))
                .onErrorResume(DocumentTypeNotPresentException.class, e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType :" + documentType + " - Not valid")))
                .map(o -> true);
    }

    private FileCreationResponse.UploadMethodEnum extractUploadMethod(SdkHttpMethod method) {
        if (method.equals(SdkHttpMethod.POST)) {
            return FileCreationResponse.UploadMethodEnum.POST;
        }
        return FileCreationResponse.UploadMethodEnum.PUT;
    }

    private Mono<PresignedPutObjectRequest> buildsUploadUrl(Document document, String checksumValue, Map<String, String> secret, String xTraceIdValue) {

        log.debug(LogUtils.INVOKING_METHOD + ARG, BUILDS_UPLOAD_URL, document, checksumValue);

        var documentType = document.getDocumentType();
        var documentState = document.getDocumentState();
        var documentKey = document.getDocumentKey();
        var contentType = document.getContentType();
        var checksumType = documentType.getChecksum();

        return getBucketName(documentType).flatMap(buckName -> signBucket(
                        buckName,
                        documentKey,
                        documentState,
                        documentType.getTipoDocumento(),
                        contentType,
                        secret,
                        checksumType,
                        checksumValue,
                        xTraceIdValue))

                .onErrorResume(ChecksumException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        throwable.getMessage())))
                .onErrorResume(AwsServiceException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, AMAZONERROR)))
                .onErrorResume(throwable -> {
                    log.error("buildsUploadUrl() : Errore generico: {}", throwable.getMessage(), throwable);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico"));
                });

    }

    private Mono<PresignedPutObjectRequest> signBucket(String bucketName, String documentKey,
                                                       String documentState, String documentType, String contenType,
                                                       Map<String, String> secret, it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType.ChecksumEnum checksumType, String checksumValue, String xTraceIdValue) {

        log.debug(LogUtils.INVOKING_METHOD, SIGN_BUCKET, Stream.of(bucketName, documentKey, documentState, documentType, contenType, checksumType, checksumValue).toList());

        if (queryParamPresignedUrlTraceId == null || queryParamPresignedUrlTraceId.isBlank()) {
            return Mono.error(new QueryParamException("Property \"queryParam.presignedUrl.traceId\" non impostata"));
        }

        return Mono.just(checksumType)
                   .flatMap(checksumTypeToEvaluate -> {

                       PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
                               .bucket(bucketName)
                               .key(documentKey)
                               .contentType(contenType)
                               .metadata(secret)
                               .overrideConfiguration(awsRequestOverrideConfiguration -> awsRequestOverrideConfiguration.putRawQueryParameter(
                                       queryParamPresignedUrlTraceId,
                                       xTraceIdValue));

                       if (ChecksumEnum.MD5.name().equals(checksumTypeToEvaluate.name())) {
                           return Mono.just(putObjectRequest.contentMD5(checksumValue)
                                                            .build());
                       } else if (headerChecksumSha256 != null && !headerChecksumSha256.isBlank() && secret != null &&
                                  ChecksumEnum.SHA256.name().equals(checksumTypeToEvaluate.name())) {
                           return Mono.just(putObjectRequest.checksumSHA256(checksumValue)
                                                            .build());
                       }else if (ChecksumEnum.NONE.name().equals(checksumTypeToEvaluate.name())){
                           return Mono.just(putObjectRequest.build());
                       }else {
                            return Mono.error(new ChecksumException(
                                   "Non e' stato possibile impostare il ChecksumValue nella PutObjectRequest"));
                       }
                   })
                   .map(putObjectRequest -> PutObjectPresignRequest.builder()
                                                                   .signatureDuration(Duration.ofMinutes(Long.parseLong(duration)))
                                                                   .putObjectRequest(putObjectRequest)
                                                                   .build())
                .flatMap(putObjectPresignRequest -> {
                    log.info(CLIENT_METHOD_INVOCATION, PRESIGN_PUT_OBJECT, putObjectPresignRequest);
                    return Mono.just(s3Presigner.presignPutObject(putObjectPresignRequest))
                            .doOnNext(result -> log.debug(CLIENT_METHOD_RETURN, PRESIGN_PUT_OBJECT, result));
                });
    }

    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId, String xTraceIdValue, Boolean metadataOnly) {
        log.debug(LogUtils.INVOKING_METHOD, CREATE_URI_FOR_DOWNLOAD_FILE, Stream.of(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly).toList());

        log.logChecking(X_TRACE_ID_VALUE);
        if (xTraceIdValue == null || StringUtils.isBlank(xTraceIdValue)) {
            String errorMsg = String.format("Header %s is missing", queryParamPresignedUrlTraceId);
            log.logCheckingOutcome(X_TRACE_ID_VALUE, false, errorMsg);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg));
        }
        log.logCheckingOutcome(X_TRACE_ID_VALUE, true);

        return Mono.fromCallable(this::validationFieldCreateUri)//
                .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))//
                .flatMap(userConfigurationResponse -> {
                    List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();

                    return documentClientCall.getDocument(fileKey)
                            .onErrorResume(DocumentKeyNotPresentException.class//
                                    , throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document key not found : " + fileKey)))
                            .flatMap(documentResponse -> {
                                var document = documentResponse.getDocument();
                                var documentType = document.getDocumentType();

                                if (!canRead.contains(documentType.getTipoDocumento())) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN//
                                            , String.format("Client : %s not has privilege for read document type %s", xPagopaSafestorageCxId, documentType)));
                                } else if (document.getDocumentState().equalsIgnoreCase(DELETED)) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.GONE//
                                            , "Document has been deleted"));
                                } else if (document.getDocumentState().equalsIgnoreCase(STAGED)) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND//
                                            , "Document not found"));
                                } else return Mono.just(document);

                            });
                })
                .cast(Document.class)
                .flatMap(document -> {
                    if (canExecutePatch && (document.getDocumentState().equalsIgnoreCase(BOOKED) || StringUtils.isBlank(document.getRetentionUntil()))) {
                        log.info(LogUtils.CLIENT_METHOD_INVOCATION + LogUtils.ARG, "s3Service.headObject()", fileKey, bucketName.ssHotName());
                        return s3Service.headObject(fileKey, bucketName.ssHotName())
                                .onErrorResume(software.amazon.awssdk.services.s3.model.NoSuchKeyException.class, throwable -> Mono.error(new S3BucketException.NoSuchKeyException(fileKey)))
                                .flatMap(headObjectResponse -> {
                                    DocumentChanges documentChanges;
                                    if (document.getDocumentState().equalsIgnoreCase(BOOKED)) {
                                        log.debug(">> after check presence in createUriForDownloadFile {}", headObjectResponse);// HeadObjectResponse
                                        documentChanges = fixBookedDocument(document, headObjectResponse);
                                    } else
                                        documentChanges = new DocumentChanges().retentionUntil(DATE_TIME_FORMATTER.format(headObjectResponse.objectLockRetainUntilDate()));
                                    return documentClientCall.patchDocument(defaultInternalClientIdValue, defaultInternalApiKeyValue, document.getDocumentKey(), documentChanges)
                                            .map(DocumentResponse::getDocument);
                                });
                    }
                    else return Mono.just(document);
                })
                .flatMap(doc -> getFileDownloadResponse(fileKey, xTraceIdValue, doc, metadataOnly != null && metadataOnly))//
                .onErrorResume(S3BucketException.NoSuchKeyException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document is missing from bucket")))
                .doOnSuccess(fileDownloadResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, CREATE_URI_FOR_DOWNLOAD_FILE, fileDownloadResponse));
    }

    private DocumentChanges fixBookedDocument(Document document, HeadObjectResponse hor) {

        DocumentChanges documentChanges = new DocumentChanges();

        if (document.getCheckSum() == null) {
            if (ChecksumEnum.MD5.equals(document.getDocumentType().getChecksum())) {
                documentChanges.setCheckSum(hor.sseCustomerKeyMD5());
            } else if (ChecksumEnum.SHA256.equals(document.getDocumentType().getChecksum())) {
                documentChanges.setCheckSum(hor.checksumSHA256());
            }
        }
        if (document.getContentLenght() == null && hor.contentLength() != null) {
            documentChanges.setContentLenght(new BigDecimal(hor.contentLength()));
        }

        documentChanges.setRetentionUntil(DATE_TIME_FORMATTER.format(hor.objectLockRetainUntilDate()));
        documentChanges.setDocumentState(AVAILABLE);
        documentChanges.setLastStatusChangeTimestamp(OffsetDateTime.now());
        return documentChanges;
    }

    @NotNull
    private Mono<FileDownloadResponse> getFileDownloadResponse(String fileKey, String xTraceIdValue, Document doc, Boolean metadataOnly) {
        final String GET_FILE_DOWNLOAD_RESPONSE = "UriBuilderService.getFileDownloadResponse()";

        log.debug(LogUtils.INVOKING_METHOD, GET_FILE_DOWNLOAD_RESPONSE, Stream.of(fileKey, xTraceIdValue, doc, metadataOnly).toList());
        // Creazione della FileDownloadInfo. Se metadataOnly=true, la FileDownloadInfo
        // non viene creata e viene ritornato un Mono.empty()
        return createFileDownloadInfo(fileKey, xTraceIdValue, doc.getDocumentState(), metadataOnly)
                .map(fileDownloadInfo -> new FileDownloadResponse().download(fileDownloadInfo))
                .switchIfEmpty(Mono.just(new FileDownloadResponse()))
                .map(fileDownloadResponse ->
                        fileDownloadResponse
                                .checksum(doc.getCheckSum() != null ? doc.getCheckSum() : null)
                                .contentLength(doc.getContentLenght())
                                .documentStatus(doc.getDocumentLogicalState() != null ? doc.getDocumentLogicalState() : "")
                                .retentionUntil(doc.getRetentionUntil() != null ? Date.from(Instant.from(DATE_TIME_FORMATTER.parse(doc.getRetentionUntil()))) : null)
                                .contentType(doc.getContentType())
                                .documentType(doc.getDocumentType().getTipoDocumento())
                                .key(fileKey)
                                .versionId(null))
                .onErrorResume(DateTimeException.class, throwable ->
                {
                    log.error("getFileDownloadResponse() : errore nel parsing o nella formattazione della data = {}", throwable.getMessage(), throwable);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, throwable.getMessage()));
                })
                .cast(FileDownloadResponse.class)
                .doOnSuccess(fileDownloadResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, GET_FILE_DOWNLOAD_RESPONSE, fileDownloadResponse));
    }

    private Mono<Boolean> validationFieldCreateUri() {
        return Mono.just(true);
    }

    public Mono<FileDownloadInfo> createFileDownloadInfo(String fileKey, String xTraceIdValue, String status, boolean metadataOnly ) throws S3BucketException.NoSuchKeyException{
        log.debug(LogUtils.INVOKING_METHOD, "UriBuilderService.createFileDownloadInfo()", metadataOnly);
        if (Boolean.TRUE.equals(metadataOnly))
            return Mono.empty();
        if (!status.equalsIgnoreCase(TECHNICAL_STATUS_FREEZED)) {
            return getPresignedUrl(bucketName.ssHotName(), fileKey, xTraceIdValue);
        } else {
            return recoverDocumentFromBucket(bucketName.ssHotName(), fileKey);
        }
    }
    private Mono<FileDownloadInfo> recoverDocumentFromBucket(String bucketName, String keyName) throws S3BucketException.NoSuchKeyException {

        log.debug(LogUtils.INVOKING_METHOD + LogUtils.ARG, "UriBuilderService.recoverDocumentFromBucket()", bucketName, keyName);

        RestoreRequest restoreRequest = RestoreRequest.builder()
                .days(stayHotTime)
                .glacierJobParameters(builder -> builder.tier(Tier.STANDARD))
                .build();

        log.info(LogUtils.CLIENT_METHOD_INVOCATION + LogUtils.ARG + LogUtils.ARG, "s3Service.restoreObject()", keyName, bucketName, restoreRequest);
        return s3Service.restoreObject(keyName, bucketName, restoreRequest)
                //Eccezioni S3: RestoreAlreadyInProgress viene ignorata.
                .onErrorResume(AwsServiceException.class, ase ->
                {
                    if (ase.awsErrorDetails().errorCode().equalsIgnoreCase("RestoreAlreadyInProgress")) {
                        log.debug(" Errore AMAZON RestoreAlreadyInProgress S3Exception", ase);
                        return Mono.empty();
                    }
                    else if (ase.awsErrorDetails().errorCode().equalsIgnoreCase("NoSuchKey")) {
                        log.debug(" Errore AMAZON NoSuchKey S3Exception ", ase);
                        return Mono.error(new S3BucketException.NoSuchKeyException(keyName));
                    } else {
                        log.error(" Errore AMAZON S3Exception", ase);
                        return Mono.error(new ResponseStatusException(HttpStatus.valueOf(ase.statusCode()), AMAZONERROR + "- " + ase.awsErrorDetails().errorMessage()));
                    }
                })
                //Eccezioni dell'SDK.
                .onErrorResume(SdkClientException.class, sce ->
                {
                    log.error(" Errore AMAZON SdkClientException", sce);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Errore AMAZON S3Exception - " + sce.getMessage()));
                })
                .thenReturn(new FileDownloadInfo().retryAfter(maxRestoreTimeCold));
    }


    private Mono<FileDownloadInfo> getPresignedUrl(String bucketName, String keyName, String xTraceIdValue) throws S3BucketException.NoSuchKeyException {

        final String GET_PRESIGNED_URL = "UriBuilderService.getPresignedUrl()";

        log.debug(LogUtils.INVOKING_METHOD + LogUtils.ARG + LogUtils.ARG, GET_PRESIGNED_URL, bucketName, keyName, xTraceIdValue);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .overrideConfiguration(awsRequestOverrideConfiguration -> awsRequestOverrideConfiguration.putRawQueryParameter(
                        queryParamPresignedUrlTraceId,
                        xTraceIdValue))
                .build();

        log.info(LogUtils.CLIENT_METHOD_INVOCATION + LogUtils.ARG, "s3Service.presignGetObject()", getObjectRequest, Duration.ofMinutes(Long.parseLong(duration)));
        return s3Service.presignGetObject(getObjectRequest, Duration.ofMinutes(Long.parseLong(duration)))
                .map(presignedRequest -> new FileDownloadInfo().url(presignedRequest.url().toString()))
                //Eccezioni S3
                .onErrorResume(S3Exception.class, ase ->
                {
                    if (ase.awsErrorDetails().errorCode().equalsIgnoreCase("NoSuchKey")) {
                        log.error(" Errore AMAZON NoSuchKey S3Exception ", ase);
                        return Mono.error(new S3BucketException.NoSuchKeyException(keyName));
                    } else {
                        log.error(" Errore AMAZON S3Exception", ase);
                        return Mono.error(new ResponseStatusException(HttpStatus.valueOf(ase.statusCode()), AMAZONERROR + "- " + ase.awsErrorDetails().errorMessage()));
                    }
                })
                //Eccezioni dell'SDK
                .onErrorResume(SdkClientException.class, sce ->
                {
                    log.error(" Errore AMAZON SdkClientException", sce);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Errore AMAZON SdkClientException - " + sce.getMessage()));
                });
    }

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[256];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    private String getFileExtension(String contentType) {
        try {
            MimeType mimeType = MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(contentType);
            if (mimeType == null)
                return "";
            return mimeType.getExtension();
        } catch (MimeTypeException exception) {
            return "";
        }
    }

}
