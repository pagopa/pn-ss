package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCallImpl;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.concurrent.Future;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class TransformationServiceTest {

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private ArubaSignServiceCallImpl arubaSignServiceCall;

    @Test
    public void createIdentityOk() {

        Assertions.assertNotNull(arubaSignServiceCall.createIdentity());
    }
//    @Test
//    public void newStagingBucketObjectCreatedEventOk(){
//
//        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
//        Acknowledgment acknowledgment = new Acknowledgment() {
//            @Override
//            public Future<?> acknowledge() {
//                return null;
//            }
//        };
//
//        transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);
//        StepVerifier.create(transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment)).expectNextCount(0).verifyComplete();
//    }
}
