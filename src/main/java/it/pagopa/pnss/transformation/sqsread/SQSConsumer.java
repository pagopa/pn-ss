package it.pagopa.pnss.transformation.sqsread;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;
import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

@Component
@Slf4j
public class SQSConsumer {
    @Autowired
    OrchestratorSignDocument orchestrator;

//    @SqsListener(value = SIGN_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiesta(final S3ObjectCreated s3ObjectCreated, final Acknowledgment acknowledgment) {
        orchestrator.incomingMessageFlow(s3ObjectCreated, acknowledgment);//.subscribe();
    }


}
