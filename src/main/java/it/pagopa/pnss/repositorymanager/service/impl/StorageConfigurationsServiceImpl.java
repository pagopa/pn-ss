package it.pagopa.pnss.repositorymanager.service.impl;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.BucketException;
import it.pagopa.pnss.repositorymanager.service.StorageConfigurationsService;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StorageConfigurationsServiceImpl implements StorageConfigurationsService {

    @Autowired
    private S3Service s3Service;
    private static final String TAG_KEY = "storageType";

    @Autowired
    private BucketName bucketName;

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

    private LifecycleRuleDTO getLifecycleRuleDTO(LifecycleRule rule) {
        log.info("getLifecycleRuleDTO() : START");
        LifecycleRuleDTO dto = new LifecycleRuleDTO();
        rule.filter().and().tags().forEach(tag -> {
            log.debug("getLifecycleRuleDTO() : tag : value for {} : {}", tag.key(), tag.value());
            if (TAG_KEY.equals(tag.key())) {
                dto.setName(tag.value());
            }
        });
        dto.setExpirationDays(rule.expiration() != null ? formatInYearsDays(rule.expiration().days()) : null);
        if (rule.hasTransitions() && rule.transitions().size() > 1) {
            log.warn("getLifecycleRuleDTO() : rule with name {} has {} transitions : the first is used",
                     rule.id(),
                     rule.transitions().size());
        }
        dto.setTransitionDays(rule.hasTransitions() ? formatInYearsDays(rule.transitions().get(0).days()) : dto.getExpirationDays());
        log.debug("getLifecycleRuleDTO() : dto : {}", dto);
        return dto;
    }

    private List<LifecycleRuleDTO> convert(List<LifecycleRule> listIn) {
        log.info("convert() : START");
        List<LifecycleRuleDTO> listOut = new ArrayList<>();
        if (listIn == null || listIn.isEmpty()) {
            return listOut;
        }
        listIn.forEach(rule -> listOut.add(getLifecycleRuleDTO(rule)));
        return listOut;
    }

    @Override
    public Mono<List<LifecycleRuleDTO>> getLifecycleConfiguration() {

        log.info("getLifecycleConfiguration() : START");
        return s3Service.getBucketLifecycleConfiguration(bucketName.ssHotName())
                .handle((response, sink) -> {
                    if (response == null || response.rules() == null) {
                        sink.error(new BucketException("No Rules founded"));
                    } else sink.next(response);
                })
                .cast(GetBucketLifecycleConfigurationResponse.class)
                .map(response -> filter(response.rules()))
                .map(this::convert)
                .onErrorResume(throwable -> {
                    log.debug("getLifecycleConfiguration() : error", throwable);
                    return Mono.error(new BucketException(throwable.getMessage()));
                });
    }
}
