package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.transformation.service.TransformationService;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

@Component
@CustomLog
public class TransformationHandler {
    private final TransformationService transformationService;

    public TransformationHandler(TransformationService transformationService) {
        this.transformationService = transformationService;
    }

    @SqsListener(value = "${pn.ss.transformation.queues.staging}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void processAndPublishTransformation(S3EventNotification event, Acknowledgment acknowledgment) {
        transformationService.handleS3Event(event.getRecords().get(0))
                .doOnSuccess(result -> acknowledgment.acknowledge())
                .subscribe();
    }

}
