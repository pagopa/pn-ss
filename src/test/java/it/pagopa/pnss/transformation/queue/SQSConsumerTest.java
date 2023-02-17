package it.pagopa.pnss.transformation.queue;


import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import it.pagopa.pnss.transformation.sqsread.SQSConsumerService;
import org.junit.Assert;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;


import java.util.List;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class SQSConsumerTest {



    @Autowired
    SQSConsumerService service;


    @Autowired
    AmazonSQSAsync amazonSQSAsync;
    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;




    @Test
    public void readDataFromCodaOk(){
    	
//    	AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
//    	String queue_url = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
//    	List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();
//    	for (Message m : messages) {
//    		// Pasticcio coi messaggi
//    	    sqs.deleteMessage(queueUrl, m.getReceiptHandle());
//    	}
    	
     //   amazonSQSAsync = amazonSQSAsync();
     //   sqsTemplate   = new QueueMessagingTemplate(amazonSQSAsync);
        S3ObjectCreated s3obj = new S3ObjectCreated();
        queueMessagingTemplate.convertAndSend(SIGN_QUEUE_NAME, s3obj);


        await().atMost(3, SECONDS).untilAsserted(() -> {
            assertThat(numberOfMessagesInQueue()).isEqualTo(0);
            assertThat(numberOfMessagesNotVisibleInQueue()).isEqualTo(0);

        });
    }

    private Integer numberOfMessagesInQueue() {
        GetQueueAttributesResult queue_attrs = getGetQueueAttributesResult();

        return Integer.parseInt(queue_attrs.getAttributes().get("ApproximateNumberOfMessages"));
    }

    private Integer numberOfMessagesNotVisibleInQueue() {
        GetQueueAttributesResult queue_attrs = getGetQueueAttributesResult();
        return Integer.parseInt(queue_attrs.getAttributes().get("ApproximateNumberOfMessagesNotVisible"));
    }

    private GetQueueAttributesResult getGetQueueAttributesResult() {
        GetQueueAttributesRequest attr = new GetQueueAttributesRequest(
                SIGN_QUEUE_NAME,
                List.of(QueueAttributeName.ALL.toString()));
        GetQueueAttributesResult queue_attrs = amazonSQSAsync.getQueueAttributes(attr);
        return queue_attrs;
    }








}
