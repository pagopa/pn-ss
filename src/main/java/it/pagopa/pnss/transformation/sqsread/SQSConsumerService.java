package it.pagopa.pnss.transformation.sqsread;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;

import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.configurationproperties.QueueName;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    QueueName queName;
    @SqsListener(value = "${s3.queue.sign-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void lavorazioneRichiesta(@Payload S3ObjectCreated s3ObjectCreated) {
        ObjectMapper mapper = new ObjectMapper();
        String s3String = s3ObjectCreated.toString();
        try {
            s3String =mapper.writeValueAsString(s3ObjectCreated);
        } catch (JsonProcessingException e) {
           log.error("Impossibile recupero JSON ", e );
        }
        log.info("PAYLOAD :" +s3String);
        if (s3ObjectCreated==null ||
                s3ObjectCreated.getDetailObject() ==null ||
                s3ObjectCreated.getDetailObject().getObject()==null ||
                StringUtils.isEmpty(s3ObjectCreated.getDetailObject().getObject().getKey())){
            log.error("ERROR key name not present in S3ObjectCreated");
            return ;
        }
        String key = s3ObjectCreated.getDetailObject().getObject().getKey();
        String bucketName = s3ObjectCreated.getDetailObject().getBucket().getName();
        log.info("Ricevuto messaggio "+s3ObjectCreated.toString());

        try {
            orchestrator.incomingMessageFlow(key, bucketName).block();

        }catch(ArubaSignExceptionLimitCall arubaEx){
            log.error("Impossible connect wit aruba for key "+arubaEx.getMessage(),arubaEx);
            throw new ArubaSignExceptionLimitCall("");
        }catch(DocumentKeyNotPresentException docNotPresent){
            log.error("Document  Key  not present in repository "+ docNotPresent.getMessage(),docNotPresent);

        }catch(NoSuchBucketException bucketError){
            log.error("Error in S3  "+bucketError.getMessage(),bucketError);
            throw new RuntimeException("");
        }catch(Exception e){
            log.error("Generic Exception  "+e.getMessage());
            throw new RuntimeException("");
        }



    }


}
