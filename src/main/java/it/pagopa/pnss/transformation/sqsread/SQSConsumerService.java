package it.pagopa.pnss.transformation.sqsread;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;

import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;


@Component
@Slf4j
public class SQSConsumerService {

    OrchestratorSignDocument orchestrator;

    public SQSConsumerService(OrchestratorSignDocument orchestrator) {
        this.orchestrator = orchestrator;
    }

    @SqsListener(value = SIGN_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void lavorazioneRichiesta(@Payload S3ObjectCreated s3ObjectCreated) {

        if (s3ObjectCreated==null ||
                s3ObjectCreated.getObject()==null ||
                StringUtils.isEmpty(s3ObjectCreated.getObject().getKey())){
            log.error("ERROR key name not present in S3ObjectCreated");
            return ;
        }
        String key = s3ObjectCreated.getObject().getKey();
        log.info("Ricevuto messaggio "+s3ObjectCreated.toString());
        orchestrator.incomingMessageFlow(key).subscribe();

    }


}
