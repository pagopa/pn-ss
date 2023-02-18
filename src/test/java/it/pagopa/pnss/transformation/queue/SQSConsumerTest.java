package it.pagopa.pnss.transformation.queue;


import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;

import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.QueueName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.Detail;
import it.pagopa.pnss.transformation.model.Oggetto;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import it.pagopa.pnss.transformation.sqsread.SQSConsumerService;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import org.junit.Assert;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;

import software.amazon.awssdk.services.sqs.model.QueueAttributeName;


import javax.xml.bind.JAXBException;

import java.net.MalformedURLException;

import java.util.List;


import static it.pagopa.pnss.common.Constant.APPLICATION_PDF;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;


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
    @Autowired
    QueueName queName;
    @MockBean
    DocumentClientCall documentClientCall;

    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    @MockBean
    OrchestratorSignDocument orchestrator;

    @Test
    public void testS3ErrorMessaggioscodatoOk() throws MalformedURLException, TypeOfTransportNotImplemented_Exception, JAXBException {

        S3ObjectCreated s3obj = new S3ObjectCreated();

        Mockito.doThrow(new ArubaSignExceptionLimitCall("")).
                when(orchestrator).incomingMessageFlow(Mockito.any());

        amazonSQSAsync.listQueues();
        queueMessagingTemplate.convertAndSend(queName.signQueueName(),s3obj);


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
                queName.signQueueName(),
                List.of(QueueAttributeName.ALL.toString()));
        GetQueueAttributesResult queue_attrs = amazonSQSAsync.getQueueAttributes(attr);
        return queue_attrs;
    }







}
