package it.pagopa.pnss.repositoryManager.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
	
	// @Value("PnSsBucketName") // nome della variabile di ambiente
	@Value("dgs-bing-ss-pnssbucket-27myu2kp62x9")
	private String pnSsBucketName;
	
	public void getLifecycleConfiguration() {
		log.info("getLifecycleConfiguration() : START");
		
		S3Client s3 = getS3Client();
		
        GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest = GetBucketLifecycleConfigurationRequest.builder()
                .bucket(pnSsBucketName)
//                .expectedBucketOwner(accountId)
                .build();
        
        GetBucketLifecycleConfigurationResponse response = s3.getBucketLifecycleConfiguration(getBucketLifecycleConfigurationRequest);
        List<LifecycleRule> newList = new ArrayList<>();
        List<LifecycleRule> rules = response.rules();
        log.info("getLifecycleConfiguration() : rules : {}", rules);
        for (LifecycleRule rule: rules) {
            newList.add(rule);
        }
	}
}
