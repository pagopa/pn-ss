package it.pagopa.pnss.transformation.sqsread;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;


@Service
@Slf4j
public class SQSConsumerService {

    OrchestratorSignDocument orchestrator;

    public SQSConsumerService(OrchestratorSignDocument orchestrator) {
        this.orchestrator = orchestrator;
    }

    @SqsListener(value = SIGN_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiesta(final S3ObjectCreated s3ObjectCreated, final Acknowledgment acknowledgment) {
        orchestrator.incomingMessageFlow(s3ObjectCreated, acknowledgment).subscribe();
    }


}
