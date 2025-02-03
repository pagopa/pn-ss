package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.transformation.model.dto.TransformationMessage;
import it.pagopa.pnss.transformation.service.TransformationService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
@CustomLog
public class TransformationHandler {
    private final TransformationService transformationService;
    private final Semaphore signAndTimemarkSemaphore;

    public TransformationHandler(TransformationService transformationService,
                                 @Value("${transformation.max-thread-pool-size.sign-and-timemark}") Integer signAndTimemarkMaxThreadPoolSize,
                                 @Value("${transformation.max-thread-pool-size.raster}") Integer rasterMaxThreadPoolSize) {
        this.transformationService = transformationService;
        this.signAndTimemarkSemaphore = new Semaphore(signAndTimemarkMaxThreadPoolSize);
    }

    @SqsListener
    void signAndTimemarkTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {}

    @SqsListener
    void signTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {}

    @SqsListener
    void dummyTransformationSubscriber(TransformationMessage transformationMessage, Acknowledgment acknowledgment) {}

    private void acquireSemaphore(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
