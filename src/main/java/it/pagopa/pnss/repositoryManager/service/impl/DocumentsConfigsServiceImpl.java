package it.pagopa.pnss.repositorymanager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.repositorymanager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositorymanager.service.StorageConfigurationsService;


/** Fare riferimento al file "\pn-ss\docs\openapi\pn-safestorage-v1-api.yaml" */
@Service
public class DocumentsConfigsServiceImpl implements DocumentsConfigsService {
	
	@Autowired
	private DocTypesService docTypesService;
	@SuppressWarnings("unused")
	@Autowired
	private StorageConfigurationsService storageConfigurationsService;
	@Autowired
    private ObjectMapper objectMapper;

	@Override
	public DocumentTypesConfigurations getAllDocumentType() {
		
		// recupero la lista "documentsTypes"
		List<DocumentTypeConfiguration> listDocTypeConf = new ArrayList<>();
		List<DocumentType> listDocTypes = docTypesService.getAllDocType();
		listDocTypes.forEach(docType -> 
			listDocTypeConf.add(objectMapper.convertValue(docType, DocumentTypeConfiguration.class))
		);
		
		// recupero la lista "storageConfigurations"
		// TODO attivita' in sospeso

		DocumentTypesConfigurations result = new DocumentTypesConfigurations();
		result.setDocumentsTypes(listDocTypeConf);
		return result;
	}

}
