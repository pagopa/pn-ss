package it.pagopa.pnss.transformation.queue;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.sqsread.SQSConsumerService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTestWebEnv
public class SQSConsumerTest {

    @Autowired
    SQSConsumerService service;

    @Test
    void testQueMessage(){

        //service.lavorazioneRichiesta();
    }
}
