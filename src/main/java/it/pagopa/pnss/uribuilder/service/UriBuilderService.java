package it.pagopa.pnss.uribuilder.service;

import com.amazonaws.SdkClientException;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.*;
import it.pagopa.pnss.common.exception.ContentTypeNotFoundException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.QueryParamException;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.uribuilder.utils.GenerateRandoKeyFile;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static it.pagopa.pnss.common.constant.Constant.*;

@Service
@Slf4j
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

    private final UserConfigurationClientCall userConfigurationClientCall;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;
    private final DocTypesClientCall docTypesClientCall;
    private final S3Service s3Service;
    private final S3Presigner s3Presigner;
    private static final String AMAZONERROR = "Error AMAZON AmazonServiceException ";

    private final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));

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
                                                var documentKeyTmp = GenerateRandoKeyFile.getInstance().createEncodedKeyName(documentType, fileExtension);
                                                log.debug("createUriForUploadFile(): documentKeyTmp = {} : ", documentKeyTmp);
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

                                                             buildsUploadUrl(insertedDocument.getDocument(),
                                                                             checksumValue,
                                                                             metadata,
                                                                             xTraceIdValue).map(presignedPutObjectRequest -> {
                                                                 FileCreationResponse response = new FileCreationResponse();
                                                                 response.setKey(URLEncoder.encode(insertedDocument.getDocument().getDocumentKey(), StandardCharsets.UTF_8));
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
        return Mono.justOrEmpty(contentType)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ContentType : Is missing")))
                .handle((s, sink) -> {
                    if (contentType.isBlank()) {
                        sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ContentType : Is missing"));
                    } else if (documentType == null || documentType.isBlank()) {
                        sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType : Is missing"));
                    } else if (xTraceIdValue == null || xTraceIdValue.isBlank()) {
                        sink.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, queryParamPresignedUrlTraceId + " : Is missing"));
                    } else {
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

        var documentType = document.getDocumentType();
        var documentState = document.getDocumentState();
        var documentKey = document.getDocumentKey();
        var contentType = document.getContentType();
        var checksumType = documentType.getChecksum();

        log.info("buildsUploadUrl() : START : " + "documentType {} : documentState {} : documentKey {} : " +
                        "contentType {} : secret {} : checksumType {} : checksumValue {}",
                documentType,
                documentState,
                URLDecoder.decode(documentKey, StandardCharsets.UTF_8),
                contentType,
                secret,
                checksumType,
                checksumValue);

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
                                                       Map<String, String> secret, ChecksumEnum checksumType, String checksumValue, String xTraceIdValue) {

        String decodedDocumentKey = URLDecoder.decode(documentKey, StandardCharsets.UTF_8);
        log.info("signBucket() : START : s3Presigner IN : " + "bucketName {} : keyName {} : " +
                  "documentState {} : documentType {} : contenType {} : " + "secret {} : checksumType{} : checksumValue {}",
                  bucketName,
                  decodedDocumentKey,
                  documentState,
                  documentType,
                  contenType,
                  secret,
                  checksumType,
                  checksumValue);
        log.debug("signBucket() : sign bucket {}", duration);

        if (queryParamPresignedUrlTraceId == null || queryParamPresignedUrlTraceId.isBlank()) {
            return Mono.error(new QueryParamException("Property \"queryParam.presignedUrl.traceId\" non impostata"));
        }

        return Mono.just(checksumType)
                   .flatMap(checksumTypeToEvaluate -> {

                       PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
                               .bucket(bucketName)
                               .key(decodedDocumentKey)
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
                   .flatMap(putObjectPresignRequest -> Mono.just(s3Presigner.presignPutObject(putObjectPresignRequest)));
    }

    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId, String xTraceIdValue, Boolean metadataOnly) {

        if (xTraceIdValue == null || StringUtils.isBlank(xTraceIdValue)) {
            String errorMsg = String.format("Header %s is missing", queryParamPresignedUrlTraceId);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg));
        }
        String decodedFileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);

        return Mono.fromCallable(this::validationFieldCreateUri)//
                .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))//
                .doOnSuccess(o ->
                    log.debug("--- REST FINE  CHIAMATA USER CONFIGURATION: {}", o)//UserConfigurationResponse
                    )//
                .flatMap(userConfigurationResponse -> {
                    List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();

                    return documentClientCall.getDocument(fileKey)
                            .onErrorResume(DocumentKeyNotPresentException.class//
                                    , throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document key not found : " + decodedFileKey)))
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
                                } else if (document.getDocumentState().equalsIgnoreCase(BOOKED)) {
                                    log.debug(">> before check presence in createUriForDownloadFile");
                                    return s3Service.headObject(decodedFileKey, bucketName.ssHotName())//
                                            .doOnSuccess(o -> {
                                                log.debug(">> after check presence in createUriForDownloadFile {}", o);// HeadObjectResponse
                                                fixBookedDocument(document, o);
                                            })//
                                            .thenReturn(document);
                                } else
                                    return Mono.just(document);

                            })//
                            .onErrorResume(software.amazon.awssdk.services.s3.model.NoSuchKeyException.class//
                                    , throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")))
                            .doOnSuccess(o ->
                                log.debug("---  FINE  CHECK PERMESSI LETTURA {}", o)//Document
                                );
                })//
                .cast(Document.class)//
                .flatMap(doc -> getFileDownloadResponse(fileKey, xTraceIdValue, doc, metadataOnly != null && metadataOnly))//
                .onErrorResume(S3BucketException.NoSuchKeyException.class//
                        , throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document is missing from bucket")))
                .doOnNext(o ->
                    log.info("--- RECUPERO PRESIGNED URL OK {}", o)//FileDownloadResponse
                    );
    }

    private void fixBookedDocument(Document document, HeadObjectResponse hor) {

        String decodedDocumentKey = URLDecoder.decode(document.getDocumentKey(), StandardCharsets.UTF_8);
        if (document.getCheckSum() == null) {
        	log.debug("fixBookedDocument() on {} - checksum null", decodedDocumentKey);
        	if (ChecksumEnum.MD5.equals(document.getDocumentType().getChecksum())) {
            	log.debug("fixBookedDocument() on {} - checksum MD5", decodedDocumentKey);
                document.setCheckSum(hor.sseCustomerKeyMD5());
            } else if (ChecksumEnum.SHA256.equals(document.getDocumentType().getChecksum())) {
            	log.debug("fixBookedDocument() on {} - checksum SHA256", decodedDocumentKey);
                document.setCheckSum(hor.checksumSHA256());
            }
        	log.debug("fixBookedDocument() on {} - new checksum {}", decodedDocumentKey, document.getCheckSum());
        }
        if (document.getContentLenght() == null && hor.contentLength() != null) {
        	log.debug("fixBookedDocument() on {} - contentLength null", decodedDocumentKey);
            document.setContentLenght(new BigDecimal(hor.contentLength()));
        	log.debug("fixBookedDocument() on {} - new contentLength {}", decodedDocumentKey, document.getContentLenght());
        }
        if (document.getRetentionUntil() == null && hor.objectLockRetainUntilDate() != null) {
        	log.debug("fixBookedDocument() on {} - retentionUntil null", decodedDocumentKey);
            document.setRetentionUntil(DATE_TIME_FORMATTER.format(hor.objectLockRetainUntilDate()));
        	log.debug("fixBookedDocument() on {} - retentionUntil {}", decodedDocumentKey, document.getRetentionUntil());
        }
    }

    @NotNull
    private Mono<FileDownloadResponse> getFileDownloadResponse(String fileKey, String xTraceIdValue, Document doc, Boolean metadataOnly) {

        // Creazione della FileDownloadInfo. Se metadataOnly=true, la FileDownloadInfo
        // non viene creata e viene ritornato un Mono.empty()
        return createFileDownloadInfo(fileKey, xTraceIdValue, doc.getDocumentState(), metadataOnly)
                .map(fileDownloadInfo -> new FileDownloadResponse().download(fileDownloadInfo))
                .switchIfEmpty(Mono.just(new FileDownloadResponse()))
                .map(fileDownloadResponse ->
                {
                    var checksum = doc.getCheckSum() != null ? doc.getCheckSum() : null;
                    var contentLength = doc.getContentLenght();
                    //var retentionInstant = Instant.from(DATE_TIME_FORMATTER.parse(doc.getRetentionUntil()));

                    // NOTA: deve essere restituito lo stato logico, piuttosto che lo stato tecnico
                    if (doc.getDocumentLogicalState() != null) {
                        fileDownloadResponse.setDocumentStatus(doc.getDocumentLogicalState());
                    } else {
                        fileDownloadResponse.setDocumentStatus("");
                    }

                    return fileDownloadResponse
                            .checksum(checksum)
                            .contentLength(contentLength)
                            //.retentionUntil(Date.from(retentionInstant))
                            .contentType(doc.getContentType())
                            .documentType(doc.getDocumentType().getTipoDocumento())
                            .key(fileKey)
                            .versionId(null);
                })

                //Check sul parsing corretto della retentionUntil
                .flatMap(fileDownloadResponse ->
                {
                    if (!doc.getDocumentState().equalsIgnoreCase(BOOKED)) {
                        if (!StringUtils.isBlank(doc.getRetentionUntil())) {
                            var retentionInstant = Instant.from(DATE_TIME_FORMATTER.parse(doc.getRetentionUntil()));
                            return Mono.just(fileDownloadResponse.retentionUntil((Date.from(retentionInstant))));
                        } else {
                            log.debug("before check presence in getFileDownloadResponse");
                            return s3Service.headObject(URLDecoder.decode(fileKey, StandardCharsets.UTF_8), bucketName.ssHotName())//
                                    .map(HeadObjectResponse::objectLockRetainUntilDate)
                                    .flatMap(retentionInstant ->
                                        documentClientCall.patchDocument(defaultInternalClientIdValue//
                                                , defaultInternalApiKeyValue//
                                                , fileKey//
                                                , new DocumentChanges().retentionUntil(DATE_TIME_FORMATTER.format(retentionInstant))).thenReturn(retentionInstant))
                                    .map(retentionInstant -> fileDownloadResponse.retentionUntil(Date.from(retentionInstant)));
                        }
                    } else {
                        if (!StringUtils.isBlank(doc.getRetentionUntil())) {
                            var retentionInstant = Instant.from(DATE_TIME_FORMATTER.parse(doc.getRetentionUntil()));
                            return Mono.just(fileDownloadResponse.retentionUntil((Date.from(retentionInstant))));
                        } else {
                            return Mono.just(fileDownloadResponse);
                        }
                    }
                })
                .onErrorResume(DateTimeException.class, throwable ->
                {
                    log.error("getFileDownloadResponse() : errore nel parsing o nella formattazione della data = {}", throwable.getMessage(), throwable);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, throwable.getMessage()));
                })
                .cast(FileDownloadResponse.class);
    }

    private Mono<Boolean> validationFieldCreateUri() {
        return Mono.just(true);
    }

    public Mono<FileDownloadInfo> createFileDownloadInfo(String fileKey, String xTraceIdValue, String status, boolean metadataOnly ) throws S3BucketException.NoSuchKeyException{
        log.info("INIZIO RECUPERO URL DOWNLOAD - metadata {}", metadataOnly);
        if (Boolean.TRUE.equals(metadataOnly))
            return Mono.empty();
        if (!status.equalsIgnoreCase(TECHNICAL_STATUS_FREEZED)) {
            return getPresignedUrl(bucketName.ssHotName(), fileKey, xTraceIdValue);
        } else {
            return recoverDocumentFromBucket(bucketName.ssHotName(), fileKey);
        }
    }
    private Mono<FileDownloadInfo> recoverDocumentFromBucket(String bucketName, String keyName) throws S3BucketException.NoSuchKeyException {

        String decodedKeyName = URLDecoder.decode(keyName, StandardCharsets.UTF_8);
        log.info("--- STARTING RESTORE DOCUMENT : " + decodedKeyName);

        RestoreRequest restoreRequest = RestoreRequest.builder()
                .days(stayHotTime)
                .glacierJobParameters(GlacierJobParameters.builder().tier(Tier.STANDARD).build())
                .build();

        return s3Service.restoreObject(decodedKeyName, bucketName, restoreRequest)
                //Eccezioni S3: RestoreAlreadyInProgress viene ignorata.
                .onErrorResume(AwsServiceException.class, ase ->
                {
                    if (ase.awsErrorDetails().errorCode().equalsIgnoreCase("RestoreAlreadyInProgress")) {
                        log.debug(" Errore AMAZON RestoreAlreadyInProgress S3Exception", ase);
                        return Mono.empty();
                    }
                    else if (ase.awsErrorDetails().errorCode().equalsIgnoreCase("NoSuchKey")) {
                        log.error(" Errore AMAZON NoSuchKey S3Exception ", ase);
                        return Mono.error(new S3BucketException.NoSuchKeyException(decodedKeyName));
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

        String decodedKeyName = URLDecoder.decode(keyName, StandardCharsets.UTF_8);
        log.info("---> STARTING GET PRESIGNED URL <--- , fileKey : {}", decodedKeyName);

        log.debug("INIZIO CREAZIONE getObjectRequest");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(decodedKeyName)
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
                        return Mono.error(new S3BucketException.NoSuchKeyException(decodedKeyName));
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
