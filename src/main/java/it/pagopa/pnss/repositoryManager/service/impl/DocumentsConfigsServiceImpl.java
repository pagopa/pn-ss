package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.rest.v1.dto.ConfidentialityLevel;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.TimestampedEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import it.pagopa.pnss.repositoryManager.exception.BucketException;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
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
	@Autowired
	private StorageConfigurationsService storageConfigurationsService;
	
	private DocumentTypeConfiguration getDocumentTypeConfiguration(DocumentType docType) {
		if (docType == null) {
			throw new RepositoryManagerException("DocType is null: can't convert in DocumentTypeConfiguration");
		}
		DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
		dtc.setName(docType.getTipoDocumento() != null ? docType.getTipoDocumento().getValue() : null);
		dtc.setInformationClassification(docType.getInformationClassification() != null ? ConfidentialityLevel.fromValue(docType.getInformationClassification().getValue()) : null);
		dtc.setDigitalSignature(docType.getDigitalSignature());
		dtc.setTimestamped(docType.getTimeStamped() != null ? TimestampedEnum.fromValue(docType.getTimeStamped().getValue()) : null);
		dtc.setChecksum(docType.getChecksum() != null ? ChecksumEnum.fromValue(docType.getChecksum().getValue()) : null);
		return dtc;
	}
	
	private StorageConfiguration getStorageConfiguration(LifecycleRuleDTO lifecycleRule) {
		if (lifecycleRule == null) {
			throw new BucketException("RifecycleRule is null: can't convert in StorageConfiguration");
		}
		StorageConfiguration sc = new StorageConfiguration();
		sc.setName(lifecycleRule.getName());
		sc.setRetentionPeriod(lifecycleRule.getExpirationDays());
		sc.setHotPeriod(lifecycleRule.getTransitionDays());
		return sc;
	}

	@Override
	public Mono<DocumentTypesConfigurations> getDocumentsConfigs() {
		
		DocumentTypesConfigurations dtc = new DocumentTypesConfigurations();
		dtc.setDocumentsTypes(new ArrayList<>());
		dtc.setStorageConfigurations(new ArrayList<>());

		// recupero la lista "documentsTypes"
		docTypesService.getAllDocumentType()
			.subscribe(elem -> {
				dtc.getDocumentsTypes().add(getDocumentTypeConfiguration(elem));
				log.info("getDocumentsConfigs() : elem docType {}",elem);
			});
		
		// recupero la lista "storageConfigurations"
		storageConfigurationsService.getLifecycleConfiguration()
			.subscribe(elem -> {
				dtc.getStorageConfigurations().add(getStorageConfiguration(elem));
				log.info("getDocumentsConfigs() : elem lyfeclicleRule {}",elem);
			});
		
		return Mono.just(dtc);
	}

}
