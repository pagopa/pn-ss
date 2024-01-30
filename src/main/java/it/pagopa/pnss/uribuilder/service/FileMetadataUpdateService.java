package it.pagopa.pnss.uribuilder.service;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.ScadenzaDocumentiCallException;
import it.pagopa.pnss.common.exception.PatchDocumentException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.uribuilder.rest.constant.ResultCodeWithDescription;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class FileMetadataUpdateService {

    private final UserConfigurationClientCall userConfigClientCall;
    private final DocumentClientCall docClientCall;
    private final DocTypesClientCall docTypesClientCall;
    private final S3Service s3Service;
    private final BucketName bucketName;
    private final ScadenzaDocumentiClientCall scadenzaDocumentiClientCall;

    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

    public FileMetadataUpdateService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall, DocTypesClientCall docTypesClientCall, S3Service s3Service, BucketName bucketName, ScadenzaDocumentiClientCall scadenzaDocumentiClientCall) {
        this.userConfigClientCall = userConfigurationClientCall;
        this.docClientCall = documentClientCall;
        this.docTypesClientCall = docTypesClientCall;
        this.s3Service = s3Service;
        this.bucketName = bucketName;
        this.scadenzaDocumentiClientCall = scadenzaDocumentiClientCall;
    }

    public Mono<OperationResultCodeResponse> updateMetadata(String fileKey, String xPagopaSafestorageCxId, UpdateFileMetadataRequest request, String authPagopaSafestorageCxId, String authApiKey) {

        log.debug(INVOKING_METHOD, UPDATE_METADATA, Stream.of(fileKey, xPagopaSafestorageCxId, request).toList());

        var retentionUntil = request.getRetentionUntil();
        var logicalState = request.getStatus();

        return docClientCall.getDocument(fileKey)
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
                            log.debug("FileMetadataUpdateService.createUriForUploadFile() : Status '{}' not found for document" + " key {}",
                                      request.getStatus(),
                                      fileKey);
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                          "Status not found for document key : " + fileKey));
                        }

                        if (StringUtils.isEmpty(technicalStatus)) {
                            log.debug("FileMetadataUpdateService.createUriForUploadFile() : Technical status not found " +
                                      "for document key {}", fileKey);
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                          "Technical status not found for document key : " + fileKey));
                        }

                    }
                    documentChanges.setDocumentState(technicalStatus);

                    return Mono.zip(Mono.just(document), Mono.just(documentChanges));
                })
                .flatMap(tuple ->
                {
                    Document document = tuple.getT1();
                    DocumentType documentType = document.getDocumentType();
                    DocumentChanges documentChanges = tuple.getT2();

                    String technicalStateToCheck = logicalState != null ? documentChanges.getDocumentState() : document.getDocumentState();
                    return updateS3ObjectTags(fileKey, technicalStateToCheck, documentType, documentChanges, retentionUntil)
                            .thenReturn(documentChanges);

                })
                .flatMap(documentChanges ->  docClientCall.patchDocument(authPagopaSafestorageCxId, authApiKey, fileKey, documentChanges)
                .flatMap(documentResponsePatch -> {
                    OperationResultCodeResponse resp = new OperationResultCodeResponse();
                    resp.setResultCode(ResultCodeWithDescription.OK.getResultCode());
                    resp.setResultDescription(ResultCodeWithDescription.OK.getDescription());
                    return Mono.just(resp);
                }))

                            .onErrorResume(PatchDocumentException.class, e -> {
                                log.debug(
                                        "FileMetadataUpdateService.createUriForUploadFile() : rilevata una PatchDocumentException : " +
										"errore = {}",
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(e.getStatusCode(), e.getMessage()));
                            })
                            .onErrorResume(ScadenzaDocumentiCallException.class, e -> {
                                log.debug(
                                        "FileMetadataUpdateService.createUriForUploadFile() : rilevata una ScadenzaDocumentiCallException : " +
                                                "errore = {}",
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.valueOf(e.getCode()), e.getMessage()));
                            })
                            .onErrorResume(NoSuchKeyException.class, e -> {
                                log.debug(
                                        "FileMetadataUpdateService.createUriForUploadFile() : rilevata una NoSuchKeyException" +
                                                " : errore" +
                                                " = " + "{}",
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                            })
                            .onErrorResume(DocumentKeyNotPresentException.class, e -> {
                                log.debug(
                                        "FileMetadataUpdateService.createUriForUploadFile() : rilevata una DocumentKeyNotPresentException" +
										" : errore" +
                                        " = " + "{}",
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                            })

                            .onErrorResume(InvalidNextStatusException.class, e -> {
                                log.debug(
                                        "FileMetadataUpdateService.createUriForUploadFile() : rilevata una InvalidNextStatusException : " +
										"errore" +
                                        " = " + "{}",
                                        e.getMessage(),
                                        e);
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
                            })

                            .onErrorResume(e -> {
                                log.error("FileMetadataUpdateService.createUriForUploadFile() : errore generico = {}", e.getMessage(), e);
                                return Mono.error(e);
                            })
                            .doOnSuccess(operationResultCodeResponse -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL, UPDATE_METADATA, operationResultCodeResponse));
    }

    private Mono<String> checkLookUp(String documentType, String logicalState) {
        return docTypesClientCall.getdocTypes(documentType)//
                                 .map(item -> item.getDocType().getStatuses().get(logicalState).getTechnicalState())//
                                 .onErrorResume(NullPointerException.class, e -> {
                                     String errorMsg =
                                             String.format("Status %s is not valid for DocumentType %s", logicalState, documentType);
                                     log.debug("NullPointerException: {}", errorMsg);
                                     return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg));
                                 });
    }

    private Mono<PutObjectTaggingResponse> updateS3ObjectTags(String fileKey, String technicalStateToCheck, DocumentType documentType, DocumentChanges documentChanges, Date retentionUntil) {
        return Flux.fromIterable(documentType.getStatuses().values())
                .filter(currentStatus -> currentStatus.getTechnicalState().equals(technicalStateToCheck))
                .map(CurrentStatus::getStorage)
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technical status not found for document key : " + fileKey)))
                .flatMap(storage ->
                {
                    ArrayList<Tag> tagList = new ArrayList<>();
                    tagList.add(Tag.builder().key(STORAGE_FREEZE).value(storage).build());

                    if (documentChanges.getDocumentState() != null && (documentChanges.getDocumentState().equalsIgnoreCase(AVAILABLE) || documentChanges.getDocumentState().equalsIgnoreCase(ATTACHED))) {
                        //Add "storage_expiry" tag to the new tag list
                        tagList.add(Tag.builder().key(STORAGE_EXPIRY).value(storage).build());
                    } else if (retentionUntil != null) {
                        documentChanges.setRetentionUntil(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(retentionUntil));
                        //Doesn't add any additional tags. Other tags ("storageType" or "storage_expiry") will be removed.
                        return scadenzaDocumentiClientCall.insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput()
                                        .documentKey(fileKey)
                                        .retentionUntil(retentionUntil.toInstant().getEpochSecond()))
                                .thenReturn(tagList);
                    }
                    return Mono.just(tagList);
                })
                .flatMap(tags -> s3Service.putObjectTagging(fileKey, bucketName.ssHotName(), Tagging.builder().tagSet(tags).build()));
    }

}
