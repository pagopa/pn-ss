package it.pagopa.pnss.uribuilder.service;

import static it.pagopa.pnss.common.Constant.ATTACHED;
import static it.pagopa.pnss.common.Constant.PN_AAR;
import static it.pagopa.pnss.common.Constant.PN_EXTERNAL_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_NOTIFICATION_ATTACHMENTS;
import static it.pagopa.pnss.common.Constant.PRELOADED;
import static it.pagopa.pnss.common.Constant.SAVED;
import static it.pagopa.pnss.common.Constant.technicalStatus_attached;
import static it.pagopa.pnss.common.Constant.technicalStatus_available;
import static java.util.Map.entry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class FileMetadataUpdateService {
	private final UserConfigurationClientCall userConfigClientCall;
	private final DocumentClientCall docClientCall;

	private Map<String, String> mapDocumentTypeLogicalStateToIntStatus;

	@PostConstruct
	public void createMap() {
		mapDocumentTypeLogicalStateToIntStatus = Map.ofEntries(
				entry(PN_NOTIFICATION_ATTACHMENTS + "-" + PRELOADED, technicalStatus_available),
				entry(PN_NOTIFICATION_ATTACHMENTS + "-" + ATTACHED, technicalStatus_attached),
				entry(PN_EXTERNAL_LEGAL_FACTS + "-" + SAVED, technicalStatus_available),
				entry(PN_LEGAL_FACTS + "-" + SAVED, technicalStatus_available),
				entry(PN_AAR + "-" + SAVED, technicalStatus_available)

		);
	}

	public FileMetadataUpdateService(UserConfigurationClientCall userConfigurationClientCall,
			DocumentClientCall documentClientCall) {
		this.userConfigClientCall = userConfigurationClientCall;
		this.docClientCall = documentClientCall;
	}

	public Mono<OperationResultCodeResponse> createUriForUploadFile(String fileKey, String xPagopaSafestorageCxId,
			UpdateFileMetadataRequest request, String authPagopaSafestorageCxId, String authApiKey) {
		log.info("createUriForUploadFile()");
		var retentionUntil = request.getRetentionUntil();
		var logicalState = request.getStatus();
		return Mono.fromCallable(() -> validationField(retentionUntil, fileKey, logicalState, xPagopaSafestorageCxId))
				   .flatMap(unused -> 
				   		   docClientCall.getdocument(fileKey)
			   							.flatMap(documentResponse -> {
		   										Document document = documentResponse.getDocument();
												String documentType = document.getDocumentType().getTipoDocumento();
												DocumentChanges docChanges = new DocumentChanges();
	
												return userConfigClientCall
														.getUser(xPagopaSafestorageCxId)
														.flatMap(userConfiguration -> {
																	if (userConfiguration.getUserConfiguration().getCanModifyStatus() == null 
																			|| !userConfiguration.getUserConfiguration().getCanModifyStatus().contains(documentType)) {
																		String errore = String.format("Client '%s' not has privilege for change document " + "type '%s'",
																									  xPagopaSafestorageCxId, documentType);
																		log.error("FileMetadataUpdateService.createUriForUploadFile() : errore = {}", errore);
																		throw new ResponseStatusException(
																				HttpStatus.FORBIDDEN,
																				errore);
																	}

																	boolean isStatusPresent = false;
																	if (!StringUtils.isBlank(request.getStatus())) {
																		log.info("FileMetadataUpdateService.createUriForUploadFile() : request.getStatus() ins't blank : CONTINUO");
																		if (document.getDocumentType() != null
																				&& document.getDocumentType().getStatuses() != null) {
																			isStatusPresent = document.getDocumentType().getStatuses().containsKey(logicalState);
																		}
																		if (!isStatusPresent) {
																			log.error("FileMetadataUpdateService.createUriForUploadFile() : Status not found for document key {}",
																					fileKey);
																			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
																					"Status not found for document key : " + fileKey);
																		}
																		
																		String technicalStatus = checkLookUp(documentType, logicalState);
																		if (StringUtils.isEmpty(technicalStatus)) {
																			log.error("FileMetadataUpdateService.createUriForUploadFile() : Technical status not found for document key {}",
																					fileKey);
																			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
																					"Technical status not found for document key : " + fileKey);
																		}
																		docChanges.setDocumentState(technicalStatus);
																	}
																	if (retentionUntil != null) {
																		docChanges.setRetentionUntil(
																				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(retentionUntil));
																	}

																	return docClientCall
																			.patchdocument(authPagopaSafestorageCxId, authApiKey, fileKey, docChanges)
																			.flatMap(documentResponsePatch -> {
																				OperationResultCodeResponse resp = new OperationResultCodeResponse();
																				resp.setResultCode(HttpStatus.OK.name());
																				resp.setResultDescription("");
																				return Mono.just(resp);
																			});
		
														});
			   							})
			   							.onErrorResume(WebClientResponseException.class, e -> {
											log.error("FileMetadataUpdateService.createUriForUploadFile() : rilevata una WebClientResponseException : errore = {}",
													e.getMessage(), e);
											return Mono.error(new ResponseStatusException(e.getStatusCode(), e.getMessage()));
										})
										.onErrorResume(DocumentKeyNotPresentException.class, e -> {
											log.error("FileMetadataUpdateService.createUriForUploadFile() : rilevata una DocumentKeyNotPresentException : errore = {}",
													e.getMessage(), e);
											return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
										})
										.onErrorResume(RuntimeException.class, e -> {
											log.error("FileMetadataUpdateService.createUriForUploadFile() : errore generico = {}",
													e.getMessage(), e);
											return Mono.error(e);
										})
				   );
	}

	private String checkLookUp(String documentType, String logicalState) {
		return mapDocumentTypeLogicalStateToIntStatus.get(documentType + "-" + logicalState);
	}

	private Mono<Boolean> validationField(
			Date retentionUntil, String fileKey, String status,
			String xPagopaSafestorageCxId) {
		return Mono.just(true);
	}
}
