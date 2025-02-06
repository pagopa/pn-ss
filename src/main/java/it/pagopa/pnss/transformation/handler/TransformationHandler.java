package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import it.pagopa.pnss.transformation.service.TransformationService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
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

    public TransformationHandler(TransformationService transformationService, TransformationProperties props) {
        this.transformationService = transformationService;
        this.signAndTimemarkSemaphore = new Semaphore(props.getMaxThreadPoolSize().getSignAndTimemark());
        this.signSemaphore = new Semaphore(props.getMaxThreadPoolSize().getSign());
    }

    @SqsListener(value = "${pn.ss.transformation.queues.sign-and-timemark}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void signAndTimemarkTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {
        acquireSemaphore(signAndTimemarkSemaphore);
        consumeTransformationMessage(transformationMessage, message -> transformationService.signAndTimemarkTransformation(message, true), acknowledgment, SIGN_AND_TIMEMARK_TRANSFORMATION_SUBSCRIBER)
                .doFinally(signalType -> signAndTimemarkSemaphore.release())
                .subscribe();
    }

    @SqsListener(value = "${pn.ss.transformation.queues.sign}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void signTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {
        acquireSemaphore(signSemaphore);
        consumeTransformationMessage(transformationMessage, message -> transformationService.signAndTimemarkTransformation(message, false), acknowledgment, SIGN_TRANSFORMATION_SUBSCRIBER)
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
