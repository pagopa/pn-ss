package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
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

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
class StorageConfigurationsServiceImplTest {

    @Autowired
    StorageConfigurationsServiceImpl storageConfigurationsService;

    @MockBean
    S3ServiceImpl s3Service;

    @Test
    void getLifecycleConfigurationOk(){
        List<Transition> transitions = new ArrayList<>();
        transitions.add(Transition.builder().days(1).build());
        transitions.add(Transition.builder().days(2).build());

        String storageType = "STORAGE_TYPE";
        LifecycleRule expiryRule = LifecycleRule.builder().id("01").filter(LifecycleRuleFilter.builder().and(LifecycleRuleAndOperator.builder().prefix("prefix")
                                .tags(Tag.builder()
                                        .key(STORAGE_EXPIRY)
                                        .value(storageType)
                                        .build())
                                .build())
                        .build())
                .expiration(LifecycleExpiration.builder().days(500).build()).build();
        LifecycleRule freezeRule = LifecycleRule.builder().id("02").filter(LifecycleRuleFilter.builder().and(LifecycleRuleAndOperator.builder().prefix("prefix")
                                .tags(Tag.builder()
                                        .key(STORAGE_FREEZE)
                                        .value(storageType)
                                        .build())
                                .build())
                        .build()).transitions(transitions).build();

        GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(expiryRule, freezeRule)).build();

        when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));

        var testMono = storageConfigurationsService.getLifecycleConfiguration();

        StepVerifier.create(testMono).expectNextMatches(lifecycleRuleDTOS -> {
            LifecycleRuleDTO lifecycleRuleDTO = lifecycleRuleDTOS.get(0);
            return lifecycleRuleDTOS.size() == 1 && lifecycleRuleDTO.getExpirationDays() != null && lifecycleRuleDTO.getTransitionDays() != null;
        }).verifyComplete();
    }

}
