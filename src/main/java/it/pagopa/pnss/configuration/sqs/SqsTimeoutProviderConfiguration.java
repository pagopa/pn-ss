package it.pagopa.pnss.configuration.sqs;



import it.pagopa.pnss.configurationproperties.SqsTimeoutConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsTimeoutProviderConfiguration {
    @Bean
    public SqsTimeoutProvider sqsTimeoutProvider(SqsAsyncClient sqsAsyncClient, SqsTimeoutConfigurationProperties config){
        return new SqsTimeoutProvider(sqsAsyncClient,config);
    }
}