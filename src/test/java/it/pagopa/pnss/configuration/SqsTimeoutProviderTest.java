package it.pagopa.pnss.configuration;


import it.pagopa.pnss.configuration.sqs.SqsTimeoutProvider;
import it.pagopa.pnss.configurationproperties.SqsTimeoutConfigurationProperties;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@TestPropertySource(properties = {
        "pn.ss.sqs.timeout.percent=10",
        "pn.ss.sqs.timeout.managedQueues=test-queue",
        "pn.ss.sqs.timeout.default-seconds=86400"
})
class SqsTimeoutProviderTest {

    @MockBean
    private SqsAsyncClient sqsAsyncClient;
    @Autowired
    private SqsTimeoutProvider timeoutProvider;
    private static final Duration TIMEOUT_INACTIVE_DURATION = Duration.ofSeconds(86400);


    private void setMock(String queueUrl, String timeout) {
        when(sqsAsyncClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        GetQueueUrlResponse.builder().queueUrl(queueUrl).build()));

        when(sqsAsyncClient.getQueueAttributes(any(GetQueueAttributesRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(GetQueueAttributesResponse.builder()
                        .attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT, timeout)).build()));
    }

    @BeforeEach
    void setField(){
        ReflectionTestUtils.setField(timeoutProvider,"sqsAsyncClient",sqsAsyncClient);
    }

    @Test
    void loadManagedQueuesTest() {
        setMock("queueUrl", "30");

        timeoutProvider.initQueueTimeouts().block();

        Assertions.assertEquals(timeoutProvider.getTimeoutForQueue("test-queue"),Duration.ofSeconds(3));
    }

    @Test
    void timeoutDisabledTest() {
        SqsTimeoutConfigurationProperties props = new SqsTimeoutConfigurationProperties();
        props.setPercent(0);
        props.setManagedQueues(List.of("test-queue"));
        props.setDefaultSeconds(TIMEOUT_INACTIVE_DURATION.toSeconds());

        SqsTimeoutProvider isolatedProvider = new SqsTimeoutProvider(sqsAsyncClient, props);
        setMock("queueUrl", "30");

        isolatedProvider.initQueueTimeouts().block();

        Assertions.assertEquals(TIMEOUT_INACTIVE_DURATION, timeoutProvider.getTimeoutForQueue("test-queue"));
    }


    @Test
    void queueNotFoundTest() {
        Assertions.assertEquals(TIMEOUT_INACTIVE_DURATION, timeoutProvider.getTimeoutForQueue("unknown-queue"));
    }
}