package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import it.pagopa.pnss.repositoryManager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositoryManager.service.StorageConfigurationsService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


/** Fare riferimento al file "\pn-ss\docs\openapi\pn-safestorage-v1-api.yaml" */
@Service
@Slf4j
public class DocumentsConfigsServiceImpl implements DocumentsConfigsService {
	
	@Autowired
	private DocTypesService docTypesService;
	@SuppressWarnings("unused")
	@Autowired
	private StorageConfigurationsService storageConfigurationsService;
	@Autowired
    private ObjectMapper objectMapper;

	@Override
	public Mono<DocumentTypesConfigurations> getAllDocumentType() {
		
		// recupero la lista "documentsTypes"
		List<DocumentTypeConfiguration> listDocTypeConf = new ArrayList<>();
		docTypesService.getAllDocType().map(
				docTypeInternal -> listDocTypeConf.add(objectMapper.convertValue(docTypeInternal, DocumentTypeConfiguration.class)));
		log.info("getAllDocumentType() : listDocTypeConf : {}",listDocTypeConf.toString());
		
		// recupero la lista "storageConfigurations"
		// TODO attivita' in sospeso

		DocumentTypesConfigurations result = new DocumentTypesConfigurations();
		result.setDocumentsTypes(listDocTypeConf);
		return Mono.just(result);
	}

}
