package it.pagopa.pnss.uribuilder.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.exception.PatchDocumentException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.uribuilder.rest.constant.ResultCodeWithDescription;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.text.SimpleDateFormat;
import java.util.stream.Stream;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.exception.ScadenzaDocumentiCallException;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.s3.model.*;

import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.common.constant.Constant.DELETED;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class FileMetadataUpdateService {

    private final UserConfigurationClientCall userConfigClientCall;
    private final DocumentClientCall docClientCall;
    private final DocTypesClientCall docTypesClientCall;
    private final RetryBackoffSpec gestoreRepositoryRetryStrategy;
    private final S3Service s3Service;
    private final BucketName bucketName;
    private final ScadenzaDocumentiClientCall scadenzaDocumentiClientCall;

    public FileMetadataUpdateService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall, DocTypesClientCall docTypesClientCall, RetryBackoffSpec gestoreRepositoryRetryStrategy, S3Service s3Service, BucketName bucketName, ScadenzaDocumentiClientCall scadenzaDocumentiClientCall) {
        this.userConfigClientCall = userConfigurationClientCall;
        this.docClientCall = documentClientCall;
        this.docTypesClientCall = docTypesClientCall;
        this.gestoreRepositoryRetryStrategy = gestoreRepositoryRetryStrategy;
        this.s3Service = s3Service;
        this.bucketName = bucketName;
        this.scadenzaDocumentiClientCall = scadenzaDocumentiClientCall;
    }

    public Mono<OperationResultCodeResponse> updateMetadata(String fileKey, String xPagopaSafestorageCxId, UpdateFileMetadataRequest request, String authPagopaSafestorageCxId, String authApiKey) {

        log.debug(INVOKING_METHOD, UPDATE_METADATA, Stream.of(fileKey, xPagopaSafestorageCxId, request).toList());

        var retentionUntil = request.getRetentionUntil();
        var logicalState = request.getStatus();

        return docClientCall.getDocument(fileKey)
                .retryWhen(gestoreRepositoryRetryStrategy)
                .flatMap(documentResponse -> Mono.zipDelayError(userConfigClientCall.getUser(xPagopaSafestorageCxId), Mono.just(documentResponse)))
                .handle(((objects, synchronousSink) -> {

                    var userConfiguration = objects.getT1().getUserConfiguration();
                    var document = objects.getT2().getDocument();
                    var tipoDocumento = document.getDocumentType().getTipoDocumento();

                    log.logChecking(USER_CONFIGURATION);
                    if (userConfiguration == null || userConfiguration.getCanModifyStatus() == null || !userConfiguration.getCanModifyStatus().contains(tipoDocumento)) {
                        String errorMsg = String.format("Client '%s' not has privilege for change document type '%s'",
                                xPagopaSafestorageCxId,
                                tipoDocumento);
                        log.logCheckingOutcome(USER_CONFIGURATION, false, errorMsg);
                        synchronousSink.error(new ResponseStatusException(HttpStatus.FORBIDDEN, errorMsg));
                    } else {
                        log.logCheckingOutcome(USER_CONFIGURATION, true);
                        synchronousSink.next(document);
                    }
                }))
                            .flatMap(object -> {
                                Document document = (Document) object;
                                String documentType = document.getDocumentType().getTipoDocumento();

                                Mono<String> checkedStatus;
                                if (logicalState != null && !logicalState.isBlank()) {
                                    checkedStatus = checkLookUp(documentType, logicalState);
                                } else {
                                    checkedStatus = Mono.just("");
                                }
                                return Mono.zipDelayError(Mono.just(document),  checkedStatus);
                            }).flatMap(objects -> {

                    Document document = objects.getT1();
                    DocumentType documentType = document.getDocumentType();
                    String technicalStatus = objects.getT2();
                    DocumentChanges documentChanges = new DocumentChanges();

                    boolean isStatusPresent = false;

                    if (request.getStatus() != null && !StringUtils.isBlank(request.getStatus())) {
                        if (documentType.getStatuses() != null) {
                            isStatusPresent = documentType.getStatuses().containsKey(logicalState);
                        }
                        if (!isStatusPresent) {
                            log.debug("{} : Status '{}' not found for document" + " key {}",
                                      UPDATE_METADATA,
                                      request.getStatus(),
                                      fileKey);
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                          "Status not found for document key : " + fileKey));
                        }

                        if (StringUtils.isEmpty(technicalStatus)) {
                            log.debug("{} : Technical status not found " +
                                      "for document key {}", UPDATE_METADATA, fileKey);
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                          "Technical status not found for document key : " + fileKey));
                        }

                    }
                    documentChanges.setDocumentState(technicalStatus);

                    if (retentionUntil != null) {
                        documentChanges.setRetentionUntil(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(retentionUntil));
                    }

                    return Mono.just(documentChanges);
                })
                .flatMap(documentChanges ->  docClientCall.patchDocument(authPagopaSafestorageCxId, authApiKey, fileKey, documentChanges)
                        .retryWhen(gestoreRepositoryRetryStrategy)
                        .flatMap(documentResponsePatch -> {
                            if (retentionUntil != null) {
                                return updateS3ObjectTags(fileKey, documentResponsePatch.getDocument().getDocumentState(), documentResponsePatch.getDocument().getDocumentType())
                                        .flatMap(putObjectTaggingResponse -> scadenzaDocumentiClientCall.insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput()
                                                .documentKey(fileKey)
                                                .retentionUntil(retentionUntil.toInstant().getEpochSecond())))
                                        .thenReturn(documentResponsePatch);
                            }
                            return Mono.just(documentResponsePatch);
                        })
                        .flatMap(documentResponsePatch -> {
                    OperationResultCodeResponse resp = new OperationResultCodeResponse();
                    resp.setResultCode(ResultCodeWithDescription.OK.getResultCode());
                    resp.setResultDescription(ResultCodeWithDescription.OK.getDescription());
                    return Mono.just(resp);
                }))

                            .onErrorResume(PatchDocumentException.class, e -> {
                                log.debug(
                                        "{} : rilevata una PatchDocumentException : " +
										"errore = {}",
                                        UPDATE_METADATA,
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(e.getStatusCode(), e.getMessage()));
                            })
                            .onErrorResume(ScadenzaDocumentiCallException.class, e -> {
                                log.debug(
                                        "{} : rilevata una ScadenzaDocumentiCallException : " +
                                                "errore = {}",
                                        UPDATE_METADATA,
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.valueOf(e.getCode()), e.getMessage()));
                            })
                            .onErrorResume(NoSuchKeyException.class, e -> {
                                log.debug(
                                        "{} : rilevata una NoSuchKeyException" +
                                                " : errore" +
                                                " = " + "{}",
                                        UPDATE_METADATA,
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                            })
                            .onErrorResume(DocumentKeyNotPresentException.class, e -> {
                                log.debug(
                                        "{} : rilevata una DocumentKeyNotPresentException" +
										" : errore" +
                                        " = " + "{}",
                                        UPDATE_METADATA,
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                            })

                            .onErrorResume(InvalidNextStatusException.class, e -> {
                                log.debug(
                                        "{} : rilevata una InvalidNextStatusException : " +
										"errore" +
                                        " = " + "{}",
                                        UPDATE_METADATA,
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                            })

                            .onErrorResume(e -> {
                                log.error("{} : errore generico = {}", UPDATE_METADATA, e.getMessage(), e);
                                return Mono.error(e);
                            })
                            .doOnSuccess(operationResultCodeResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, UPDATE_METADATA, operationResultCodeResponse));
    }

    private Mono<String> checkLookUp(String documentType, String logicalState) {
        return docTypesClientCall.getdocTypes(documentType)
                                 .retryWhen(gestoreRepositoryRetryStrategy)
                                 .map(item -> item.getDocType().getStatuses().get(logicalState).getTechnicalState())//
                                 .onErrorResume(NullPointerException.class, e -> {
                                     String errorMsg =
                                             String.format("Status %s is not valid for DocumentType %s", logicalState, documentType);
                                     log.debug("NullPointerException: {}", errorMsg);
                                     return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg));
                                 });
    }

    private Mono<PutObjectTaggingResponse> updateS3ObjectTags(String fileKey, String documentState, DocumentType documentType) {
        return Flux.fromIterable(documentType.getStatuses().values())
                .filter(currentStatus -> currentStatus.getTechnicalState().equals(documentState))
                .map(CurrentStatus::getStorage)
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technical status not found for document key : " + fileKey)))
                .map(storage -> {
                    Tag freezeTag = Tag.builder().key(STORAGE_FREEZE).value(storage).build();
                    return Tagging.builder().tagSet(freezeTag).build();
                })
                .flatMap(tagging -> s3Service.putObjectTagging(fileKey, bucketName.ssHotName(), tagging));
    }

}
