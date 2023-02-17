package it.pagopa.pnss.transformation.sqsread;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;

import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;


import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;


@Component
@Slf4j
public class SQSConsumerService {

    OrchestratorSignDocument orchestrator;

    public SQSConsumerService(OrchestratorSignDocument orchestrator) {
        this.orchestrator = orchestrator;
    }

    @SqsListener(value = SIGN_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiesta(@Payload S3ObjectCreated s3ObjectCreated,  Acknowledgment ack) {

        if (s3ObjectCreated==null ||
                s3ObjectCreated.getObject()==null ||
                StringUtils.isEmpty(s3ObjectCreated.getObject().getKey())){
            log.error("ERROR key name not present in S3ObjectCreated");
            ack.acknowledge();
            return ;
        }
        String key = s3ObjectCreated.getObject().getKey();
        log.info("Ricevuto messaggio "+s3ObjectCreated.toString());
        try{
            orchestrator.incomingMessageFlow(key).subscribe();
            ack.acknowledge();
        }catch(ArubaSignExceptionLimitCall arubaEx){
           log.error("Impossible connect wit aruba for key "+arubaEx.getMessage(),arubaEx);
        }catch(DocumentKeyNotPresentException docNotPresent){
            log.error("Document  Key  not present in repository "+ docNotPresent.getMessage(),docNotPresent);
            ack.acknowledge();
        }catch(NoSuchBucketException bucketError ){
            log.error("Error in S3  "+bucketError.getMessage(),bucketError);
        }


    }


}
