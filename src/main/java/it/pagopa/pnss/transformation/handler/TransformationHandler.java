package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pnss.transformation.model.dto.S3BucketOriginDetail;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.transformation.service.TransformationService;
import it.pagopa.pnss.transformation.utils.TransformationUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Component
@CustomLog
public class TransformationHandler {
    private final TransformationService transformationService;
    private final Semaphore signAndTimemarkSemaphore;
    private final Semaphore signSemaphore;
    private final S3Service s3Service;
    @Value("${pn.ss.transformation.queues.sign-and-timemark}")
    private String signAndTimemarkQueueName;
    @Value("${pn.ss.transformation.queues.sign}")
    private String signQueueName;


    public TransformationHandler(TransformationService transformationService, TransformationProperties props, S3Service s3Service) {
        this.transformationService = transformationService;
        this.signAndTimemarkSemaphore = new Semaphore(props.getMaxThreadPoolSize().getSignAndTimemark());
        this.signSemaphore = new Semaphore(props.getMaxThreadPoolSize().getSign());
        this.s3Service = s3Service;
    }

    /*
     * Gestisce un evento S3 ricevuto dalla coda SQS e decide se avviare o meno
     * il processo di trasformazione associato al file.
     * Per gli eventi di tipo PutObject viene verificata la presenza di più versioni
     * dello stesso file nel bucket:
     * - se il file ha più versioni, il processo di trasformazione viene saltato
     *   e il messaggio SQS viene confermato immediatamente;
     * - se il file ha una sola versione, viene avviata la trasformazione.
     */
    @SqsListener(value = "${pn.ss.transformation.queues.staging}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void processAndPublishTransformation(S3EventNotificationMessage s3EventNotificationMessage, Acknowledgment acknowledgment) {
        String fileKey = s3EventNotificationMessage.getEventNotificationDetail().getObject().getKey();
        String bucketName = Optional.ofNullable(s3EventNotificationMessage.getEventNotificationDetail().getS3BucketOriginDetail())
                .map(S3BucketOriginDetail::getName)
                .orElse("");
        String eventName = s3EventNotificationMessage.getEventNotificationDetail().getReason();
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        log.logStartingProcess(PROCESS_TRANSFORMATION_EVENT);
        Mono<Boolean> shouldSkipProcess = (TransformationUtils.PUT_OBJECT_REASON.equals(eventName != null ? eventName : ""))
                ? hasMultipleVersions(bucketName, fileKey)
                : Mono.just(false);
        MDCUtils.addMDCToContextAndExecute(
                shouldSkipProcess.flatMap(shouldSkipTransformation -> {
                            if (shouldSkipTransformation) {
                                log.info("File {} has multiple versions. Skipping transformation.", fileKey);
                                acknowledgment.acknowledge();
                                return Mono.empty();
                            }
                            return transformationService.handleS3Event(s3EventNotificationMessage)
                                    .doOnSuccess(r -> acknowledgment.acknowledge());
                        })
                        .doOnError(e -> log.logEndingProcess(PROCESS_TRANSFORMATION_EVENT, false, e.getMessage()))
                        .doFinally(s -> log.logEndingProcess(PROCESS_TRANSFORMATION_EVENT))).subscribe();
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

    /*
     * Controlla quante versioni ci sono per un file sul bucket.
     * Se ci sono v>1:
     * - vuol dire che sono state fatte più trasformazioni
     */
    private Mono<Boolean> hasMultipleVersions(String bucketName, String key) {
        if(bucketName == null || bucketName.isEmpty()) {
            log.info("Skipping check on multiple versions for file {}, bucketName is null", key);
            return Mono.just(Boolean.FALSE);
        }
        return s3Service.listObjectVersions(key, bucketName)
                .map(response ->
                        response.versions().stream()
                                .filter(v -> v.key().equals(key))
                                .count() > 1);
    }


}
