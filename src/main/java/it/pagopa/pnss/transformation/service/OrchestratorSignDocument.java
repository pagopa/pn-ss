package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static it.pagopa.pnss.common.QueueNameConstant.MAXIMUM_LISTENING_TIME;
import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;


@Service
@Slf4j
public class OrchestratorSignDocument {


    public Flow.Publisher<Object> incomingMessageFlow(S3ObjectCreated s3ObjectCreated, Acknowledgment acknowledgment) {
        return null;
    }
}
