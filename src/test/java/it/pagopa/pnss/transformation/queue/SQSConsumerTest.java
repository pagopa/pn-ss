package it.pagopa.pnss.transformation.queue;


import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class SQSConsumerTest {
//
//
//
//    @Autowired
//    TransformationService service;
//
//
//    @Autowired
//    AmazonSQSAsync amazonSQSAsync;
//    @Autowired
//    private QueueMessagingTemplate queueMessagingTemplate;
//    @Autowired
//    QueueName queName;
//    @MockBean
//    DocumentClientCall documentClientCall;
//
//    @Value("${test.aws.s3.endpoint:#{null}}")
//    String testAwsS3Endpoint;
//
//    @MockBean
//    OrchestratorSignDocument orchestrator;
//
//    @Test
//    public void testS3ErrorMessaggioscodatoOk() throws MalformedURLException, TypeOfTransportNotImplemented_Exception, JAXBException {
//
//        S3ObjectDto s3obj = new S3ObjectDto();
//
//        Mockito.doThrow(new ArubaSignExceptionLimitCall("")).
//                when(orchestrator).incomingMessageFlow(Mockito.any(),Mockito.any(),Mockito.any());
//
//        amazonSQSAsync.listQueues();
//        queueMessagingTemplate.convertAndSend(queName.signQueueName(),s3obj);
//
//
//        await().atMost(3, SECONDS).untilAsserted(() -> {
//            assertThat(numberOfMessagesInQueue()).isEqualTo(0);
//            assertThat(numberOfMessagesNotVisibleInQueue()).isEqualTo(0);
//
//        });
//    }
//
//
//
//
//
//    private Integer numberOfMessagesInQueue() {
//        GetQueueAttributesResult queue_attrs = getGetQueueAttributesResult();
//
//        return Integer.parseInt(queue_attrs.getAttributes().get("ApproximateNumberOfMessages"));
//    }
//
//    private Integer numberOfMessagesNotVisibleInQueue() {
//        GetQueueAttributesResult queue_attrs = getGetQueueAttributesResult();
//        return Integer.parseInt(queue_attrs.getAttributes().get("ApproximateNumberOfMessagesNotVisible"));
//    }
//
//    private GetQueueAttributesResult getGetQueueAttributesResult() {
//        GetQueueAttributesRequest attr = new GetQueueAttributesRequest(
//                queName.signQueueName(),
//                List.of(QueueAttributeName.ALL.toString()));
//        GetQueueAttributesResult queue_attrs = amazonSQSAsync.getQueueAttributes(attr);
//        return queue_attrs;
//    }
//
//
//
//
//
//

}
