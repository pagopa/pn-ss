package it.pagopa.pnss.transformation.sqsread;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.configurationproperties.QueueName;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.OrchestratorSignDocument;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;


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
    	log.info("SQSConsumerService.lavorazioneRichiesta() : START");
    	
        ObjectMapper mapper = new ObjectMapper();
        String s3String = s3ObjectCreated.toString();
        try {
            s3String =mapper.writeValueAsString(s3ObjectCreated);
        } catch (JsonProcessingException e) {
           log.debug("SQSConsumerService.lavorazioneRichiesta() : Impossibile recupero JSON ", e );
        }
        log.debug("PAYLOAD :" +s3String);
        if (s3ObjectCreated==null ||
                s3ObjectCreated.getDetailObject() ==null ||
                s3ObjectCreated.getDetailObject().getObject()==null ||
                StringUtils.isEmpty(s3ObjectCreated.getDetailObject().getObject().getKey())){
            log.debug("SQSConsumerService.lavorazioneRichiesta() : ERROR key name not present in S3ObjectCreated");
            return ;
        }
        String key = s3ObjectCreated.getDetailObject().getObject().getKey();
        String bucketName = s3ObjectCreated.getDetailObject().getBucket().getName();
        log.debug("SQSConsumerService.lavorazioneRichiesta() : Ricevuto messaggio "+s3ObjectCreated.toString());

        try {
            orchestrator.incomingMessageFlow(key, bucketName, true).block();

        }catch(ArubaSignExceptionLimitCall arubaEx){
            log.error("Impossible connect wit aruba for key "+arubaEx.getMessage(),arubaEx);
            throw new ArubaSignExceptionLimitCall("");
        }catch(DocumentKeyNotPresentException docNotPresent){
            log.error("Document  Key  not present in repository "+ docNotPresent.getMessage(),docNotPresent);

        }catch(NoSuchBucketException bucketError){
            log.error("Error in S3  "+bucketError.getMessage(),bucketError);
            throw new RuntimeException("");
        }catch(RetentionException e){
            log.error("Retention Exception {}", e.getMessage(), e);
            throw new RuntimeException("Retention Exception",e);
        }catch(Exception e){
            log.error("Generic Exception  "+e.getMessage());
            throw new RuntimeException(e.getMessage());
        }



    }


}
