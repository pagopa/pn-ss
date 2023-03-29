package it.pagopa.pnss.uribuilder.service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class FileMetadataUpdateService {

    private final UserConfigurationClientCall userConfigClientCall;
    private final DocumentClientCall docClientCall;
    private final DocTypesClientCall docTypesClientCall;

    public FileMetadataUpdateService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall,
                                     DocTypesClientCall docTypesClientCall) {
        this.userConfigClientCall = userConfigurationClientCall;
        this.docClientCall = documentClientCall;
        this.docTypesClientCall = docTypesClientCall;
    }

    public Mono<OperationResultCodeResponse> createUriForUploadFile(String fileKey, String xPagopaSafestorageCxId,
                                                                    UpdateFileMetadataRequest request, String authPagopaSafestorageCxId,
                                                                    String authApiKey) {
        var retentionUntil = request.getRetentionUntil();
        var logicalState = request.getStatus();

        return Mono.fromCallable(() -> validationField(retentionUntil, fileKey, logicalState, xPagopaSafestorageCxId))

                   .flatMap(unused -> docClientCall.getdocument(fileKey))

                   .flatMap(documentResponse -> {
                       var document = documentResponse.getDocument();
                       var documentType = document.getDocumentType().getTipoDocumento();

                       return Mono.zip(userConfigClientCall.getUser(xPagopaSafestorageCxId), checkLookUp(documentType, logicalState))
                                  .map(objects -> Tuples.of(document, objects.getT1(), objects.getT2()));
                   })

                   .flatMap(objects -> {
                       var document = objects.getT1();
                       var documentType = document.getDocumentType();
                       var tipoDocumento = documentType.getTipoDocumento();
                       var userConfiguration = objects.getT2().getUserConfiguration();
                       var technicalStatus = objects.getT3();
                       var documentChanges = new DocumentChanges();

                       if (userConfiguration != null && (userConfiguration.getCanModifyStatus() == null ||
                                                         !userConfiguration.getCanModifyStatus().contains(tipoDocumento))) {
                           String errore = String.format("Client '%s' not has privilege for change document " + "type '%s'",
                                                         xPagopaSafestorageCxId,
                                                         tipoDocumento);
                           log.error("FileMetadataUpdateService.createUriForUploadFile() : errore = {}", errore);
                           return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, errore));
                       }

                       boolean isStatusPresent = false;

                       if (!StringUtils.isBlank(request.getStatus())) {
                           log.info("FileMetadataUpdateService.createUriForUploadFile() : request.getStatus() isn't blank :" + " CONTINUO");
                           if (documentType.getStatuses() != null) {
                               isStatusPresent = documentType.getStatuses().containsKey(logicalState);
                           }
                           if (!isStatusPresent) {
                               log.error("FileMetadataUpdateService.createUriForUploadFile() : Status not found for document" + " key {}",
                                         fileKey);
                               return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                             "Status not found for document key : " + fileKey));
                           }

                           if (StringUtils.isEmpty(technicalStatus)) {
                               log.error("FileMetadataUpdateService.createUriForUploadFile() : Technical status not found " +
                                         "for document key {}", fileKey);
                               return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                             "Technical status not found for document key : " + fileKey));
                           }
                           documentChanges.setDocumentState(technicalStatus);
                       }

                       if (retentionUntil != null) {
                           documentChanges.setRetentionUntil(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(retentionUntil));
                       }

                       return docClientCall.patchdocument(authPagopaSafestorageCxId, authApiKey, fileKey, documentChanges)
                                           .flatMap(documentResponsePatch -> {
                                               OperationResultCodeResponse resp = new OperationResultCodeResponse();
                                               resp.setResultCode(HttpStatus.OK.name());
                                               resp.setResultDescription("");
                                               return Mono.just(resp);
                                           });

                   })

                   .onErrorResume(WebClientResponseException.class, e -> {
                       log.error(
                               "FileMetadataUpdateService.createUriForUploadFile() : rilevata una WebClientResponseException : errore = {}",
                               e.getMessage(),
                               e);
                       return Mono.error(new ResponseStatusException(e.getStatusCode(), e.getMessage()));
                   })

                   .onErrorResume(DocumentKeyNotPresentException.class, e -> {
                       log.error(
                               "FileMetadataUpdateService.createUriForUploadFile() : rilevata una DocumentKeyNotPresentException : errore" +
                               " = " + "{}", e.getMessage(), e);
                       return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
                   })

                   .onErrorResume(RuntimeException.class, e -> {
                       log.error("FileMetadataUpdateService.createUriForUploadFile() : errore generico = {}", e.getMessage(), e);
                       return Mono.error(e);
                   });
    }

    private Mono<String> checkLookUp(String documentType, String logicalState) {
        return docTypesClientCall.getdocTypes(documentType)
                                 .map(item -> item.getDocType().getStatuses().get(logicalState).getTechnicalState());
    }

    private Mono<Boolean> validationField(Date retentionUntil, String fileKey, String status, String xPagopaSafestorageCxId) {
        return Mono.just(true);
    }
}
