package it.pagopa.pnss.repositorymanager.service.impl;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.BucketException;
import it.pagopa.pnss.repositorymanager.service.StorageConfigurationsService;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.Tag;

import java.util.*;

import static it.pagopa.pnss.common.constant.Constant.STORAGE_EXPIRY;
import static it.pagopa.pnss.common.constant.Constant.STORAGE_FREEZE;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class StorageConfigurationsServiceImpl implements StorageConfigurationsService {

    private final S3Service s3Service;
    private final RetryBackoffSpec s3RetryStrategy;
    private final BucketName bucketName;

    public StorageConfigurationsServiceImpl(S3Service s3Service, RetryBackoffSpec s3RetryStrategy, BucketName bucketName) {
        this.s3Service = s3Service;
        this.s3RetryStrategy = s3RetryStrategy;
        this.bucketName = bucketName;
    }

    private String formatInYearsDays(Integer value) {
        if (value == null) {
            return null;
        }
        final int daysInYear = 365;
        int years = value / daysInYear;
        int days = value % daysInYear;
        StringBuilder sb = new StringBuilder();
        if (years != 0) {
            sb.append(years).append("y");
        }
        if (days != 0) {
            if (years != 0) {
                sb.append(" ");
            }
            sb.append(days).append("d");
        }
        return sb.toString();
    }

    private List<LifecycleRule> filter(List<LifecycleRule> listIn) {
        List<LifecycleRule> listOut = new ArrayList<>();
        if (listIn == null || listIn.isEmpty()) {
            return listOut;
        }
        listIn.forEach(rule -> {
            if (rule.filter() != null && rule.filter().and() != null && rule.filter().and().hasTags()) {
                listOut.add(rule);
            }
        });
        return listOut;
    }


    /**
     * Map the tags of the lifecycle rule with the fields of the DTO.
     * If the rule contains the expiry tag, the expirationDays is set with the value of the expiration rule field.
     * If the rule contains the freeze tag, the transitionDays field is set with the value of the transition rule field.
     * It then updates the map with the new DTO.
     *
     * @param rule       lifecycle rule
     * @param ruleDTOMap map of lifecycle rule DTO
     */
    private void mapLifecycleRuleDTO(LifecycleRule rule, Map<String, LifecycleRuleDTO> ruleDTOMap) {
        if (rule.hasTransitions() && rule.transitions().size() > 1) {
            log.warn("getLifecycleRuleDTO() : rule with name {} has {} transitions : the first is used",
                    rule.id(),
                    rule.transitions().size());
        }

        getTags(rule).forEach(tag -> {
            String storageType = tag.value();
            LifecycleRuleDTO dto = ruleDTOMap.getOrDefault(storageType, new LifecycleRuleDTO());
            boolean hasStorageTag = false;
            if (STORAGE_EXPIRY.equals(tag.key())) {
                dto.setExpirationDays(rule.expiration() != null ? formatInYearsDays(rule.expiration().days()) : null);
                hasStorageTag = true;
            }
            if (STORAGE_FREEZE.equals(tag.key())) {
                dto.setTransitionDays(rule.hasTransitions() ? formatInYearsDays(rule.transitions().get(0).days()) : dto.getExpirationDays());
                hasStorageTag = true;
            }
            if (hasStorageTag) {
                dto.setName(storageType);
                ruleDTOMap.put(storageType, dto);
            }
        });
    }

    /**
     * Get the tags list from a specific rule
     *
     * @param rule lifecycle rule
     * @return list of tags
     */
    private List<Tag> getTags(LifecycleRule rule) {
        List<Tag> tagList;
        if (rule.filter().tag() != null) {
            tagList = new ArrayList<>();
            tagList.add(rule.filter().tag());
        } else if (rule.filter().and().tags() != null) {
            tagList = rule.filter().and().tags();
        } else {
            String msgError = String.format("getLifecycleRuleDTO() : rule with id '%s' has no tags", rule.id());
            log.error(msgError);
            throw new IllegalArgumentException(msgError);
        }
        return tagList;
    }

    /**
     * Convert a list of LifecycleRule into a list of LifecycleRuleDTO
     * @param listIn list of LifecycleRule
     * @return list of LifecycleRuleDTO
     */
    private List<LifecycleRuleDTO> convert(List<LifecycleRule> listIn) {
        List<LifecycleRuleDTO> listOut = new ArrayList<>();

        if (listIn == null || listIn.isEmpty()) {
            return listOut;
        }

        Map<String,LifecycleRuleDTO> mapRule = new HashMap<>();
        listIn.forEach(rule -> mapLifecycleRuleDTO(rule, mapRule));
        return new ArrayList<>(mapRule.values());
    }

    @Override
    public Mono<List<LifecycleRuleDTO>> getLifecycleConfiguration() {
        final String GET_LIFECYCLE_CONFIGURATION="StorageConfigurationsService.getLifecycleConfiguration()";
        log.debug(INVOKING_METHOD, GET_LIFECYCLE_CONFIGURATION, "");
        return s3Service.getBucketLifecycleConfiguration(bucketName.ssHotName())
                .retryWhen(s3RetryStrategy)
                .handle((response, sink) -> {
                    if (response == null || response.rules() == null) {
                        sink.error(new BucketException("No Rules founded"));
                    } else sink.next(response);
                })
                .cast(GetBucketLifecycleConfigurationResponse.class)
                .map(response -> filter(response.rules()))
                .map(this::convert)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_LIFECYCLE_CONFIGURATION, result))
                .onErrorResume(throwable -> {
                    log.debug("getLifecycleConfiguration() : error", throwable);
                    return Mono.error(new BucketException(throwable.getMessage()));
                });
    }
}
