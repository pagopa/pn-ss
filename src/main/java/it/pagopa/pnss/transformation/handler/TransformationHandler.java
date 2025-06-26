package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.service.TransformationService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Component
@CustomLog
public class TransformationHandler {
    private final TransformationService transformationService;
    private final Semaphore signAndTimemarkSemaphore;
    private final Semaphore signSemaphore;
    @Value("${pn.ss.transformation.queues.sign-and-timemark}")
    private String signAndTimemarkQueueName;
    @Value("${pn.ss.transformation.queues.sign}")
    private String signQueueName;

    public TransformationHandler(TransformationService transformationService, TransformationProperties props) {
        this.transformationService = transformationService;
        this.signAndTimemarkSemaphore = new Semaphore(props.getMaxThreadPoolSize().getSignAndTimemark());
        this.signSemaphore = new Semaphore(props.getMaxThreadPoolSize().getSign());
    }

    @SqsListener(value = "${pn.ss.transformation.queues.staging}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void processAndPublishTransformation(S3EventNotificationMessage s3EventNotificationMessage, Acknowledgment acknowledgment) {
        String fileKey = s3EventNotificationMessage.getEventNotificationDetail().getObject().getKey();
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        log.logStartingProcess(PROCESS_TRANSFORMATION_EVENT);

        MDCUtils.addMDCToContextAndExecute(
                transformationService.handleS3Event(s3EventNotificationMessage)
                        .doOnSuccess(result -> {
                            log.logEndingProcess(PROCESS_TRANSFORMATION_EVENT);
                            acknowledgment.acknowledge();
                        })
                        .doOnError(throwable -> log.logEndingProcess(PROCESS_TRANSFORMATION_EVENT, false, throwable.getMessage()))
        ).subscribe();
    }

    @SqsListener(value = "${pn.ss.transformation.queues.sign-and-timemark}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void signAndTimemarkTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {
        acquireSemaphore(signAndTimemarkSemaphore);
        consumeTransformationMessage(transformationMessage, message -> transformationService.signAndTimemarkTransformation(message, true,signAndTimemarkQueueName), acknowledgment, SIGN_AND_TIMEMARK_TRANSFORMATION_SUBSCRIBER)
                .doFinally(signalType -> signAndTimemarkSemaphore.release())
                .subscribe();
    }

    @SqsListener(value = "${pn.ss.transformation.queues.sign}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void signTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {
        acquireSemaphore(signSemaphore);
        consumeTransformationMessage(transformationMessage, message -> transformationService.signAndTimemarkTransformation(message, false,signQueueName), acknowledgment, SIGN_TRANSFORMATION_SUBSCRIBER)
                .doFinally(signalType -> signSemaphore.release())
                .subscribe();
    }

    @SqsListener(value = "${pn.ss.transformation.queues.dummy}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void dummyTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {
        consumeTransformationMessage(transformationMessage, transformationService::dummyTransformation, acknowledgment, DUMMY_TRANSFORMATION_SUBSCRIBER).subscribe();
    }

    private Mono<?> consumeTransformationMessage(TransformationMessage transformationMessage, Function<TransformationMessage, Mono<?>> transformationFunction, Acknowledgment acknowledgment, String op) {
        MDCUtils.clearMDCKeys();
        MDC.put(MDC_CORR_ID_KEY, transformationMessage.getFileKey());
        log.logStartingProcess(op);
        return MDCUtils.addMDCToContextAndExecute(transformationFunction.apply(transformationMessage)
                .doOnSuccess(result -> {
                    log.logEndingProcess(op);
                    acknowledgment.acknowledge();
                })
                .doOnError(throwable -> log.logEndingProcess(op, false, throwable.getMessage())));
    }

    private void acquireSemaphore(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
