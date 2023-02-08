package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.rest.v1.dto.ConfidentialityLevel;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.TimestampedEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import it.pagopa.pnss.repositoryManager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositoryManager.service.StorageConfigurationsService;
import lombok.extern.slf4j.Slf4j;


/** Fare riferimento al file "\pn-ss\docs\openapi\pn-safestorage-v1-api.yaml" */
@Service
@Slf4j
public class DocumentsConfigsServiceImpl implements DocumentsConfigsService {
	
	@Autowired
	private DocTypesService docTypesService;
	@SuppressWarnings("unused")
	@Autowired
	private StorageConfigurationsService storageConfigurationsService;

	@Override
	public DocumentTypesConfigurations getAllDocumentType() {
		
		// recupero la lista "documentsTypes"
		List<DocumentTypeConfiguration> listDocTypeConf = new ArrayList<>();
//		docTypesService.getAllDocType().log().subscribe(dt -> {
//				listDocTypeConf.add(objectMapper.convertValue(dt, DocumentTypeConfiguration.class));
//			});
		List<DocumentType> listDocTypes = docTypesService.getAllDocType();
		listDocTypes.forEach(docType -> {
			DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
			dtc.setName(docType.getTipoDocumento() != null ? docType.getTipoDocumento().getValue() : null);
			dtc.setInformationClassification(docType.getInformationClassification() != null ? ConfidentialityLevel.fromValue(docType.getInformationClassification().getValue()) : null);
			dtc.setTimestamped(docType.getTimeStamped() != null ? TimestampedEnum.fromValue(docType.getTimeStamped().getValue()) : null);
			dtc.setChecksum(docType.getChecksum() != null ? ChecksumEnum.fromValue(docType.getChecksum().getValue()) : null);
			listDocTypeConf.add(dtc);
		});
		log.info("getAllDocumentType() : listDocTypeConf : {}", listDocTypeConf);
		
		// recupero la lista "storageConfigurations"
		storageConfigurationsService.getLifecycleConfiguration();

		DocumentTypesConfigurations result = new DocumentTypesConfigurations();
		result.setDocumentsTypes(listDocTypeConf);
		return result;
	}

}
