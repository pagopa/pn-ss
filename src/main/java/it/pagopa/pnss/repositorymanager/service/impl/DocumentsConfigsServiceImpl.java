package it.pagopa.pnss.repositorymanager.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.CurrentStatus;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.rest.v1.dto.ConfidentialityLevel;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.TimestampedEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfigurationStatuses;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import it.pagopa.pnss.repositorymanager.exception.BucketException;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.repositorymanager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositorymanager.service.StorageConfigurationsService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


/**
 * Fare riferimento al file "\pn-ss\docs\openapi\pn-safestorage-v1-api.yaml"
 */
@Service
@Slf4j
public class DocumentsConfigsServiceImpl implements DocumentsConfigsService {

    private final DocTypesService docTypesService;
    private final StorageConfigurationsService storageConfigurationsService;

    public DocumentsConfigsServiceImpl(DocTypesService docTypesService, StorageConfigurationsService storageConfigurationsService) {
        this.docTypesService = docTypesService;
        this.storageConfigurationsService = storageConfigurationsService;
    }

    private DocumentTypeConfiguration getDocumentTypeConfiguration(DocumentType docType) {
        if (docType == null) {
            throw new RepositoryManagerException("DocType is null: can't convert in DocumentTypeConfiguration");
        }
        DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
        dtc.setName(docType.getTipoDocumento() != null ? docType.getTipoDocumento() : null);
        dtc.setInitialStatus(docType.getInitialStatus());
        if (docType.getStatuses() != null &&  !docType.getStatuses().isEmpty()) {
        	Map<String, DocumentTypeConfigurationStatuses> statuses = new HashMap<>();
        	dtc.setStatuses(statuses);
        	Set<String> keySet =  docType.getStatuses().keySet();
        	keySet.forEach(key -> {
        		CurrentStatus dtCurrentStatus = docType.getStatuses().get(key);
        		DocumentTypeConfigurationStatuses dtcStatues = new DocumentTypeConfigurationStatuses();
        		dtcStatues.setStorage(dtCurrentStatus.getStorage());
        		dtcStatues.setAllowedStatusTransitions(dtCurrentStatus.getAllowedStatusTransitions());
        		dtc.getStatuses().put(key, dtcStatues);
        	});
        }
        dtc.setInformationClassification(
                docType.getInformationClassification() != null ? ConfidentialityLevel.fromValue(docType.getInformationClassification()
                                                                                                       .getValue()) : null);
        dtc.setDigitalSignature(docType.getDigitalSignature());
        dtc.setTimestamped(docType.getTimeStamped() != null ? TimestampedEnum.fromValue(docType.getTimeStamped().getValue()) : null);
        dtc.setChecksum(docType.getChecksum() != null ? ChecksumEnum.fromValue(docType.getChecksum().getValue()) : null);
        return dtc;
    }

    private StorageConfiguration getStorageConfiguration(LifecycleRuleDTO lifecycleRule) {
        if (lifecycleRule == null) {
            throw new BucketException("LifecycleRule is null: can't convert in StorageConfiguration");
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
        
        return storageConfigurationsService.getLifecycleConfiguration().doOnNext(lifecycleRuleDTO -> {
            log.info("getDocumentsConfigs() : elem lifecycleRule {}", lifecycleRuleDTO);
            dtc.getStorageConfigurations().add(getStorageConfiguration(lifecycleRuleDTO));
        }).flatMap(lifecycleRule -> docTypesService.getAllDocumentType()).doOnNext(documentType -> {
            log.info("getDocumentsConfigs() : elem docType {}", documentType);
            dtc.getDocumentsTypes().add(getDocumentTypeConfiguration(documentType));
        }).then(Mono.just(dtc));

        // NOTA: docTypesService.getAllDocumentType() potrebbe resituire una lista vuota
//        return docTypesService.getAllDocumentType().doOnNext(documentType -> {
//            log.info("getDocumentsConfigs() : elem docType {}", documentType);
//            dtc.getDocumentsTypes().add(getDocumentTypeConfiguration(documentType));
//        }).flatMap(documentType -> storageConfigurationsService.getLifecycleConfiguration()).doOnNext(lifecycleRuleDTO -> {
//            log.info("getDocumentsConfigs() : elem lifecycleRule {}", lifecycleRuleDTO);
//            dtc.getStorageConfigurations().add(getStorageConfiguration(lifecycleRuleDTO));
//        }).then(Mono.just(dtc));
    }
}
