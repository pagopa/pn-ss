package it.pagopa.pnss.repositorymanager.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import it.pagopa.pnss.repositorymanager.exception.BucketException;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.repositorymanager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositorymanager.service.StorageConfigurationsService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.SUCCESSFUL_OPERATION_LABEL;


/**
 * Fare riferimento al file "\pn-ss\docs\openapi\pn-safestorage-v1-api.yaml"
 */
@Service
@CustomLog
public class DocumentsConfigsServiceImpl implements DocumentsConfigsService {

    private final DocTypesService docTypesService;
    private final StorageConfigurationsService storageConfigurationsService;

    public DocumentsConfigsServiceImpl(DocTypesService docTypesService, StorageConfigurationsService storageConfigurationsService) {
        this.docTypesService = docTypesService;
        this.storageConfigurationsService = storageConfigurationsService;
    }

    private DocumentTypeConfiguration convertDocumentTypeConfiguration(DocumentType docType) {
        if (docType == null) {
            throw new RepositoryManagerException("DocType is null: can't convert in DocumentTypeConfiguration");
        }
        DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
        dtc.setName(docType.getTipoDocumento() != null ? docType.getTipoDocumento() : null);
        dtc.setInitialStatus(docType.getInitialStatus());
        if (docType.getStatuses() != null && !docType.getStatuses().isEmpty()) {
            Map<String, DocumentTypeConfigurationStatuses> statuses = new HashMap<>();
            dtc.setStatuses(statuses);
            Set<String> keySet = docType.getStatuses().keySet();
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
        dtc.setTransformations(docType.getTransformations());
        dtc.setTimestamped(docType.getTimeStamped() != null ? DocumentTypeConfiguration.TimestampedEnum.fromValue(docType.getTimeStamped().getValue()) : null);
        dtc.setChecksum(docType.getChecksum() != null ? DocumentTypeConfiguration.ChecksumEnum.fromValue(docType.getChecksum().getValue()) : null);
        return dtc;
    }

    private List<DocumentTypeConfiguration> convertDocumentTypeConfigurationList(List<DocumentType> listDocTypeDto) {
        List<DocumentTypeConfiguration> list = new ArrayList<>();
        listDocTypeDto.forEach(docTypeDto -> list.add(convertDocumentTypeConfiguration(docTypeDto)));
        return list;
    }

    private StorageConfiguration convertStorageConfiguration(LifecycleRuleDTO lifecycleRule) {
        if (lifecycleRule == null) {
            throw new BucketException("LifecycleRule is null: can't convert in StorageConfiguration");
        }
        StorageConfiguration sc = new StorageConfiguration();
        sc.setName(lifecycleRule.getName());
        sc.setRetentionPeriod(lifecycleRule.getExpirationDays());
        sc.setHotPeriod(lifecycleRule.getTransitionDays());
        return sc;
    }

    private List<StorageConfiguration> convert(List<LifecycleRuleDTO> listLifecycleRuleDto) {
        List<StorageConfiguration> list = new ArrayList<>();
        listLifecycleRuleDto.forEach(lifecycleRule -> list.add(convertStorageConfiguration(lifecycleRule)));
        return list;
    }

    @Override
    public Mono<DocumentTypesConfigurations> getDocumentsConfigs() {
        final String GET_DOCUMENTS_CONFIGS = "DocumentConfigsServiceImpl.getDocumentsConfigs()";
        log.debug(LogUtils.INVOKING_METHOD, GET_DOCUMENTS_CONFIGS, "");

        DocumentTypesConfigurations dtc = new DocumentTypesConfigurations();
        dtc.setDocumentsTypes(new ArrayList<>());
        dtc.setStorageConfigurations(new ArrayList<>());

        return storageConfigurationsService.getLifecycleConfiguration().doOnNext(lifecycleRuleList -> {
            log.debug("getDocumentsConfigs() : elem lifecycleRuleList {}", lifecycleRuleList);
            dtc.setStorageConfigurations(convert(lifecycleRuleList));
        }).flatMap(lifecycleRule -> docTypesService.getAllDocumentType()).doOnNext(documentTypeList -> {
            log.debug("getDocumentsConfigs() : elem documentTypeList {}", documentTypeList);
            dtc.setDocumentsTypes(convertDocumentTypeConfigurationList(documentTypeList));
        }).then(Mono.just(dtc)
         .doOnSuccess(documentTypesConfigurations -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_DOCUMENTS_CONFIGS, documentTypesConfigurations)));
    }
}