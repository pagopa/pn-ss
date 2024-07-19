package it.pagopa.pnss.uribuilder.service;

import com.amazonaws.SdkClientException;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.*;
import it.pagopa.pnss.common.exception.RestoreRequestDateNotFound;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.common.exception.InvalidConfigurationException;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pnss.repositorymanager.exception.IndexingLimitException;
import it.pagopa.pnss.repositorymanager.exception.QueryParamException;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import it.pagopa.pnss.uribuilder.rest.constant.GetFilePatchConfiguration;
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
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Predicate;

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

    @Value("${amz.restore.request.date.header.name}")
    private String restoreRequestDateHeaderName;

    private final GetFilePatchConfiguration getFileWithPatchConfiguration;
    private final UserConfigurationClientCall userConfigurationClientCall;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;
    private final DocTypesClientCall docTypesClientCall;
    private final TagsClientCall tagsClientCall;
    private final S3Service s3Service;
    private final AdditionalFileTagsService additionalFileTagsService;
    private final S3Presigner s3Presigner;
    private final RetryBackoffSpec gestoreRepositoryRetryStrategy;
    private static final String AMAZONERROR = "Error AMAZON AmazonServiceException ";
    private static final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String SEPARATORE = "~";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));
    private final IndexingConfiguration indexingConfiguration;


    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall,
                             BucketName bucketName, DocTypesClientCall docTypesClientCall, TagsClientCall tagsClientCall, S3Service s3Service, S3Presigner s3Presigner, @Value("${uri.builder.get.file.with.patch.configuration}") String getFileWithPatchConfigValue, AdditionalFileTagsService additionalFileTagsService, RetryBackoffSpec gestoreRepositoryRetryStrategy, IndexingConfiguration indexingConfiguration) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.docTypesClientCall = docTypesClientCall;
        this.tagsClientCall = tagsClientCall;
        this.s3Service = s3Service;
        this.s3Presigner = s3Presigner;
        this.getFileWithPatchConfiguration= GetFilePatchConfiguration.valueOf(getFileWithPatchConfigValue);
        this.additionalFileTagsService = additionalFileTagsService;
        this.gestoreRepositoryRetryStrategy = gestoreRepositoryRetryStrategy;
        this.indexingConfiguration = indexingConfiguration;
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

        // Creazione di secret e metadata
        var secret = new ArrayList<String>();
        secret.add(generateSecret());
        var metadata = new HashMap<String, String>();
        metadata.put("secret", secret.toString());

        return validateLimits(request)
                .flatMap(validatedRequest -> {
                    // Verifica dei permessi di creazione
                    return validationField(contentType, documentType, xTraceIdValue).then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId)
                            .retryWhen(gestoreRepositoryRetryStrategy)
                            .handle((userConfiguration, synchronousSink) -> {
                                if (!userConfiguration.getUserConfiguration().getCanCreate().contains(documentType)) {
                                    synchronousSink.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                            String.format("Client '%s' does not have privilege to create document type '%s'",
                                                    xPagopaSafestorageCxId, documentType)));
                                }  else if (validatedRequest.getTags() != null && !validatedRequest.getTags().isEmpty() && !userConfiguration.getUserConfiguration().getCanWriteTags()) {
                                    synchronousSink.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                            String.format("Client '%s' does not have privilege to write tags", xPagopaSafestorageCxId)));
                                } else {
                                    synchronousSink.next(userConfiguration);
                                }
                            })
                               .flatMap(userConfiguration -> {
                                        // Creazione del documento con o senza tags
                                        var fileExtension = getFileExtension(contentType);
                                        var documentKeyTmp = String.format("%s%s",
                                                GenerateRandoKeyFile.getInstance().createKeyName(documentType),
                                                fileExtension);
                                        DocumentInput documentInput = new DocumentInput().contentType(request.getContentType())
                                                .documentKey(documentKeyTmp)
                                                .documentState(initialNewDocumentState)
                                                .clientShortCode(xPagopaSafestorageCxId)
                                                .documentType(request.getDocumentType());

                                        return documentClientCall.postDocument(documentInput)
                                                .retryWhen(Retry.max(10)
                                                        .filter(DocumentkeyPresentException.class::isInstance)
                                                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                                                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Non è stato possibile produrre una chiave per user " + xPagopaSafestorageCxId);
                                                        }))
                                                .flatMap(insertedDocument -> buildsUploadUrl(insertedDocument.getDocument(),
                                                        checksumValue, metadata, xTraceIdValue)
                                                        .flatMap(presignedPutObjectRequest -> {
                                                            if (validatedRequest.getTags() != null && !validatedRequest.getTags().isEmpty()) {
                                                                // Chiamata a putTags usando documentKey ottenuto da postDocument
                                                                TagsChanges tagsChanges = new TagsChanges().SET(validatedRequest.getTags());
                                                                return tagsClientCall.putTags(insertedDocument.getDocument().getDocumentKey(), tagsChanges)
                                                                        .doOnSuccess(tagsResponse -> log.info("PutTags successful for document key: {}", insertedDocument.getDocument().getDocumentKey()))
                                                                        .map(tagsResponse -> {
                                                                            FileCreationResponse response = new FileCreationResponse();
                                                                            response.setKey(insertedDocument.getDocument().getDocumentKey());
                                                                            response.setSecret(secret.toString());
                                                                            response.setUploadUrl(presignedPutObjectRequest.url().toString());
                                                                            response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest().method()));
                                                                            return response;
                                                                        });
                                                            } else {
                                                                // Nessun tag da scrivere
                                                                FileCreationResponse response = new FileCreationResponse();
                                                                response.setKey(insertedDocument.getDocument().getDocumentKey());
                                                                response.setSecret(secret.toString());
                                                                response.setUploadUrl(presignedPutObjectRequest.url().toString());
                                                                response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest().method()));
                                                                return Mono.just(response);
                                                            }
                                                        }));
                                    }))
                            .doOnSuccess(fileCreationResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, CREATE_URI_FOR_UPLOAD_FILE, fileCreationResponse));
                });
    }


    private Mono<FileCreationRequest> validateLimits(FileCreationRequest request) {
        Map<String, List<String>> tags = request.getTags();

        if(tags == null) {
            tags = Collections.emptyMap();
        }
            // Verifica MaxTagsPerRequest
            if (tags.size() > indexingConfiguration.getIndexingLimits().getMaxTagsPerRequest()) {
                return Mono.error(new IndexingLimitException("MaxTagsPerRequest", tags.size(), indexingConfiguration.getIndexingLimits().getMaxTagsPerRequest()));
            }

            // Verifica MaxValuesPerTagPerRequest
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                if (entry.getValue().size() > indexingConfiguration.getIndexingLimits().getMaxValuesPerTagPerRequest()) {
                    return Mono.error(new IndexingLimitException("MaxValuesPerTagPerRequest", entry.getValue().size(), indexingConfiguration.getIndexingLimits().getMaxValuesPerTagPerRequest()));
                }
            }
            return Mono.just(request);
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
                .flatMap(mono -> docTypesClientCall.getdocTypes(documentType).retryWhen(gestoreRepositoryRetryStrategy))
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
                                                       Map<String, String> secret, DocumentType.ChecksumEnum checksumType, String checksumValue, String xTraceIdValue) {

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
                    log.debug(CLIENT_METHOD_INVOCATION, PRESIGN_PUT_OBJECT, putObjectPresignRequest);
                    return Mono.just(s3Presigner.presignPutObject(putObjectPresignRequest))
                            .doOnNext(result -> log.debug(CLIENT_METHOD_RETURN, PRESIGN_PUT_OBJECT, result));
                });
    }

    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId, String xTraceIdValue, Boolean metadataOnly, Boolean tags) {
        log.debug(LogUtils.INVOKING_METHOD, CREATE_URI_FOR_DOWNLOAD_FILE, Stream.of(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly).toList());
        log.logChecking(X_TRACE_ID_VALUE);

        if (xTraceIdValue == null || StringUtils.isBlank(xTraceIdValue)) {
            String errorMsg = String.format("Header %s is missing", queryParamPresignedUrlTraceId);
            log.logCheckingOutcome(X_TRACE_ID_VALUE, false, errorMsg);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg));
        }
        log.logCheckingOutcome(X_TRACE_ID_VALUE, true);

        return Mono.fromCallable(this::validationFieldCreateUri)
                .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId)
                        .retryWhen(gestoreRepositoryRetryStrategy))
                .zipWhen(userConfigurationResponse -> {
                    List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();
                    boolean canReadTags = userConfigurationResponse.getUserConfiguration().getCanReadTags() != null
                            ? userConfigurationResponse.getUserConfiguration().getCanReadTags()
                            : true;

                    return documentClientCall.getDocument(fileKey)
                            .retryWhen(gestoreRepositoryRetryStrategy)
                            .onErrorResume(DocumentKeyNotPresentException.class,
                                    throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document key not found : " + fileKey)))
                            .flatMap(documentResponse -> checkDocumentPermissions(canRead, documentResponse.getDocument(), xPagopaSafestorageCxId, canReadTags, tags));
                })
                .flatMap(tuple -> {
                    UserConfigurationResponse userConfigurationResponse = tuple.getT1();
                    Document document = tuple.getT2();
                    return handleDocumentState(document, userConfigurationResponse);
                })
                .flatMap(doc -> getFileDownloadResponse(fileKey, xTraceIdValue, doc, metadataOnly != null && metadataOnly)
                        .flatMap(fileDownloadResponse -> setFileTags(tags, doc.getDocumentKey(), xPagopaSafestorageCxId, fileDownloadResponse)))
                .onErrorResume(S3BucketException.NoSuchKeyException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document is missing from bucket")))
                .onErrorResume(InvalidConfigurationException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage())))
                .doOnSuccess(fileDownloadResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, CREATE_URI_FOR_DOWNLOAD_FILE, fileDownloadResponse));
    }

    private Mono<Document> checkDocumentPermissions(List<String> canRead, Document document, String xPagopaSafestorageCxId, boolean canReadTags, Boolean tags) {
        var documentType = document.getDocumentType();
        if (!canRead.contains(documentType.getTipoDocumento())) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                    String.format("Client : %s not has privilege for read document type %s", xPagopaSafestorageCxId, documentType)));
        } else if (document.getDocumentState().equalsIgnoreCase(DELETED)) {
            return Mono.error(new ResponseStatusException(HttpStatus.GONE,
                    "Document has been deleted"));
        } else if (document.getDocumentState().equalsIgnoreCase(STAGED)) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Document not found"));
        } else if (tags != null && tags && !canReadTags) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                    String.format("Client '%s' does not have privilege to read tags", xPagopaSafestorageCxId)));
        } else {
            return Mono.just(document);
        }
    }

    private Mono<Document> handleDocumentState(Document document, UserConfigurationResponse userConfigurationResponse) {
        boolean isBooked = document.getDocumentState().equalsIgnoreCase(BOOKED);
        boolean hasRetentionUntilNull = StringUtils.isBlank(document.getRetentionUntil());

        if (isBooked || hasRetentionUntilNull) {
            log.info(CLIENT_METHOD_INVOCATION + ARG, "s3Service.headObject()", document.getDocumentKey(), bucketName.ssHotName());

            return s3Service.headObject(document.getDocumentKey(), bucketName.ssHotName())
                    .onErrorResume(NoSuchKeyException.class, throwable -> Mono.error(new S3BucketException.NoSuchKeyException(document.getDocumentKey())))
                    .map(headObjectResponse -> {
                        DocumentChanges documentChanges;
                        if (isBooked) {
                            log.debug(">> after check presence in createUriForDownloadFile {}", headObjectResponse);
                            documentChanges = fixBookedDocument(document, headObjectResponse);
                        } else {
                            String retentionUntil = DATE_TIME_FORMATTER.format(headObjectResponse.objectLockRetainUntilDate());
                            document.setRetentionUntil(retentionUntil);
                            documentChanges = new DocumentChanges().retentionUntil(retentionUntil);
                        }
                        return documentChanges;
                    })
                    .filter(documentChanges -> !isBooked || canExecutePatch(getFileWithPatchConfiguration, userConfigurationResponse.getUserConfiguration()))
                    .flatMap(documentChanges -> documentClientCall.patchDocument(defaultInternalClientIdValue, defaultInternalApiKeyValue, document.getDocumentKey(), documentChanges)
                            .retryWhen(gestoreRepositoryRetryStrategy)
                            .map(DocumentResponse::getDocument))
                    .defaultIfEmpty(document);
        } else {
            return Mono.just(document);
        }
    }

    /**
     * Metodo che prende in ingresso una serie di parametri
     * al fine di reperire la lista di tag di un documento
     * e aggiungerli alla response
     *
     * @param tags booleano che indica se restituire o meno la lista di tag
     * @param documentKey chiave del documento
     * @param xPagopaSafestorageCxId identificativo univoco del client
     * @param fileDownloadResponse oggetto della response
     * @return fileDownloadResponse con l'aggiunta dei tag
     */
    private Mono<FileDownloadResponse> setFileTags(Boolean tags, String documentKey, String xPagopaSafestorageCxId, FileDownloadResponse fileDownloadResponse) {
        if (tags != null && tags) {
            return additionalFileTagsService.getDocumentTags(documentKey, xPagopaSafestorageCxId)
                    .map(AdditionalFileTagsDto::getTags)
                    .map(tagList -> {
                        processTags(tagList);
                        fileDownloadResponse.setTags(tagList);
                        return fileDownloadResponse;
                    });
        } else {
            return Mono.just(fileDownloadResponse);
        }
    }

    /**
     * Metodo che prende in ingresso una mappa di tag
     * e la lavora in modo da restituire una nuova mappa
     * secondo la logica del metodo 'extractTagKey(param)'
     *
     * @param tags Mappa di stringa di una lista di stringhe di tag
     * @return Map<String, List<String>> dei tag privi di prefisso
     */
    public static Map<String, List<String>> processTags(Map<String, List<String>> tags) {
        if (tags == null) {
            return new HashMap<>();
        }
        return tags.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> extractTagKey(entry.getKey()),
                        Map.Entry::getValue
                ));
    }

    /**
     * Estrae la chiave del tag rimuovendo il prefisso composto da clientId e tilde.
     *
     * @param key Chiave del tag con prefisso.
     * @return Chiave senza prefisso.
     */
    private static String extractTagKey(String key) {
        if (key.contains(SEPARATORE)) {
            int tildeIndex = key.indexOf(SEPARATORE);
            if (tildeIndex != -1) {
                return key.substring(tildeIndex + 1);
            } else {
                return key;
            }
        }
        return key;
    }

    private DocumentChanges fixBookedDocument(Document document, HeadObjectResponse hor) {

        DocumentChanges documentChanges = new DocumentChanges();

        String checkSum;
        if (ChecksumEnum.MD5.equals(document.getDocumentType().getChecksum())) {
            checkSum = hor.sseCustomerKeyMD5();
            document.setCheckSum(checkSum);
            documentChanges.setCheckSum(checkSum);
        } else if (ChecksumEnum.SHA256.equals(document.getDocumentType().getChecksum())) {
            checkSum = hor.checksumSHA256();
            document.setCheckSum(checkSum);
            documentChanges.setCheckSum(checkSum);
        }

        BigDecimal contentLenght = new BigDecimal(hor.contentLength());
        document.setContentLenght(contentLenght);
        documentChanges.setContentLenght(contentLenght);

        String retentionUntil = DATE_TIME_FORMATTER.format(hor.objectLockRetainUntilDate());
        document.setRetentionUntil(retentionUntil);
        documentChanges.setRetentionUntil(retentionUntil);

        OffsetDateTime lastStatusChangeTimestamp = OffsetDateTime.now();
        document.setLastStatusChangeTimestamp(lastStatusChangeTimestamp);
        documentChanges.setLastStatusChangeTimestamp(lastStatusChangeTimestamp);

        document.setDocumentState(AVAILABLE);
        documentChanges.setDocumentState(AVAILABLE);

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
                .map(fileDownloadResponse -> {
                    var logicalState = doc.getDocumentType().getStatuses().entrySet().stream().filter(entry -> entry.getValue().getTechnicalState().equals(doc.getDocumentState())).map(Map.Entry::getKey).findFirst();
                    return fileDownloadResponse
                            .checksum(doc.getCheckSum() != null ? doc.getCheckSum() : null)
                            .contentLength(doc.getContentLenght())
                            .documentStatus(logicalState.orElse(""))
                            .retentionUntil(doc.getRetentionUntil() != null ? Date.from(Instant.from(DATE_TIME_FORMATTER.parse(doc.getRetentionUntil()))) : null)
                            .contentType(doc.getContentType())
                            .documentType(doc.getDocumentType().getTipoDocumento())
                            .key(fileKey)
                            .versionId(null);
                })
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

        log.debug(LogUtils.INVOKING_METHOD, RECOVER_DOCUMENT_FROM_BUCKET, Stream.of(bucketName, keyName).toList());

        RestoreRequest restoreRequest = RestoreRequest.builder()
                .days(stayHotTime)
                .glacierJobParameters(builder -> builder.tier(Tier.STANDARD))
                .build();

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
                .map(restoreObjectResponse -> new FileDownloadInfo().retryAfter(maxRestoreTimeCold))
                .switchIfEmpty(timeElapsedBetweenRequests(keyName, bucketName));
    }

    private Mono<FileDownloadInfo> timeElapsedBetweenRequests(String keyName, String bucketName) {
        return s3Service.headObject(keyName, bucketName)
                .map(headObjectResponse -> headObjectResponse.sdkHttpResponse().firstMatchingHeader(restoreRequestDateHeaderName).orElse(""))
                .filter(restoreRequestDate -> !restoreRequestDate.isBlank())
                .switchIfEmpty(Mono.error(new RestoreRequestDateNotFound(restoreRequestDateHeaderName)))
                .map(restoreRequestDate -> {
                    OffsetDateTime lastRestoreRequestDateTime = LocalDateTime.parse(restoreRequestDate, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)).atOffset(ZoneOffset.UTC);
                    BigDecimal elapsedTime = new BigDecimal(ChronoUnit.SECONDS.between(lastRestoreRequestDateTime, OffsetDateTime.now()));
                    return new FileDownloadInfo().retryAfter(maxRestoreTimeCold.subtract(elapsedTime).signum() == -1 ? BigDecimal.valueOf(3600) : maxRestoreTimeCold.subtract(elapsedTime));
                });
    }

    private Mono<FileDownloadInfo> getPresignedUrl(String bucketName, String keyName, String xTraceIdValue) throws S3BucketException.NoSuchKeyException {

        log.debug(LogUtils.INVOKING_METHOD, GET_PRESIGNED_URL, Stream.of(bucketName, keyName, xTraceIdValue).toList());
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .overrideConfiguration(awsRequestOverrideConfiguration -> awsRequestOverrideConfiguration.putRawQueryParameter(
                        queryParamPresignedUrlTraceId,
                        xTraceIdValue))
                .build();

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

    private boolean canExecutePatch(GetFilePatchConfiguration configValue, UserConfiguration userConfiguration) {
        switch (configValue) {
            case ALL -> {
                return true;
            }
            case CLIENT_SPECIFIC -> {
                return Boolean.TRUE.equals(userConfiguration.getCanExecutePatch());
            }
            case NONE -> {
                return false;
            }
            default -> throw new InvalidConfigurationException(configValue.getValue());
        }
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
