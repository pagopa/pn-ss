package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import it.pagopa.pnss.repositoryManager.service.StorageConfigurationsService;
import it.pagopa.pnss.transformation.service.CommonS3ObjectService;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;

@Service
@Slf4j
public class StorageConfigurationsImpl extends CommonS3ObjectService implements StorageConfigurationsService {
	
//	// @Value("PnSsBucketName") // nome della variabile di ambiente
//	@Value("dgs-bing-ss-pnssbucket-27myu2kp62x9")
//	private String pnSsBucketName;
	
	@Value("${S3.bucket.hot.name}")
	private String pnSsBucketName;
	
	public List<LifecycleRuleDTO> getLifecycleConfiguration() {
		log.info("getLifecycleConfiguration() : START");
		
		log.info("getLifecycleConfiguration() : pnSsBucketName : {}", pnSsBucketName);
        GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest = GetBucketLifecycleConfigurationRequest.builder()
                .bucket(pnSsBucketName)
//                .expectedBucketOwner(accountId)
                .build();
        
		S3Client s3 = getS3Client();
        GetBucketLifecycleConfigurationResponse response = s3.getBucketLifecycleConfiguration(getBucketLifecycleConfigurationRequest);
        
        List<LifecycleRuleDTO> dtoList = new ArrayList<>();
        List<LifecycleRule> rules = response.rules();
        log.info("getLifecycleConfiguration() : rules : {}", rules);
        for (LifecycleRule rule: rules) {
        	 log.info("getLifecycleConfiguration() : rule : {}", rule);
        	 
        	 LifecycleRuleDTO dto = new LifecycleRuleDTO();
        	 dto.setId(rule.id());
        	 dto.setExpirationDays(rule.expiration().days());
        	 dto.setTransitionDays(rule.transitions().get(0).days());
        	 
        	 log.info("getLifecycleConfiguration() : dto : {}", dto);
        	 dtoList.add(dto);
        }
        
        return dtoList;
	}
}
