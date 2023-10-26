package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.CurrentStatus;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.repositorymanager.service.impl.DocumentsConfigsServiceImpl;
import it.pagopa.pnss.repositorymanager.service.impl.StorageConfigurationsServiceImpl;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
public class DocumentsConfigsServiceImplTest {

    @Autowired
    StorageConfigurationsServiceImpl storageConfigurationsService;

    @Autowired
    DocumentsConfigsServiceImpl documentsConfigsService;

    @MockBean
    S3ServiceImpl s3Service;

    @MockBean
    DocTypesService docTypesService;

    @Test
    void getDocumentsConfigsOk(){
        List<Transition> transitions = new ArrayList<>();
        transitions.add(Transition.builder().days(1).build());
        transitions.add(Transition.builder().days(2).build());

        List<DocumentType> documentTypeList = new ArrayList<>();
        createListDocType(documentTypeList);

        LifecycleRule lifecycleRule = LifecycleRule.builder()
                .id("01")
                .filter(LifecycleRuleFilter.builder()
                        .and(LifecycleRuleAndOperator.builder()
                                .prefix("prefix")
                                .tags(Tag.builder()
                                        .key("storageType")
                                        .value("value")
                                        .build())
                                .build())
                        .build())
                .expiration(LifecycleExpiration.builder().days(500).build())
                .transitions(transitions)
                .build();

        GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();

        when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));
        when(docTypesService.getAllDocumentType()).thenReturn(Mono.just(documentTypeList));

        var testMono = documentsConfigsService.getDocumentsConfigs();

        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    void createListDocType(List<DocumentType> listDocTypes){
        DocumentType documentType1 = new DocumentType();
        DocumentType documentType2 = new DocumentType();

        documentType1.setTipoDocumento("PN_NOTIFICATION_ATTACHMENTS");
        documentType2.setTipoDocumento("PN_LEGAL_FACTS");

        documentType1.setInitialStatus("PRELOADED");
        documentType1.setInitialStatus("SAVED");

        CurrentStatus documentTypeConfigurationStatuses = new CurrentStatus();
        documentTypeConfigurationStatuses.setStorage("AVAILABLE");

        List<String> allowedStatusTransitions = new ArrayList<>();
        allowedStatusTransitions.add("ATTACHED");
        documentTypeConfigurationStatuses.setAllowedStatusTransitions(allowedStatusTransitions);

        documentType1.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));
        documentType2.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

        documentType1.setInformationClassification(DocumentType.InformationClassificationEnum.HC);
        documentType2.setInformationClassification(DocumentType.InformationClassificationEnum.HC);

        documentType1.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        documentType2.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));

        documentType1.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);
        documentType2.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);

        documentType1.setChecksum(DocumentType.ChecksumEnum.SHA256);
        documentType2.setChecksum(DocumentType.ChecksumEnum.SHA256);

        listDocTypes.add(documentType1);
        listDocTypes.add(documentType2);
    }

}
