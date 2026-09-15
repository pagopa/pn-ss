package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class PnSsConfigTest {

    @Test
    void logConfiguration_whenAllAreasArePopulated_doesNotThrow() {
        PnSsConfig pnSsConfig = new PnSsConfig();

        PnSsConfig.Bucket bucket = new PnSsConfig.Bucket();
        bucket.setHotName("hotName");
        bucket.setStageName("stageName");
        pnSsConfig.setBucket(bucket);

        PnSsConfig.Dynamo dynamo = new PnSsConfig.Dynamo();
        PnSsConfig.Dynamo.RepositoryManager repositoryManager = new PnSsConfig.Dynamo.RepositoryManager();
        repositoryManager.setAnagraficaClientName("anagraficaClientName");
        repositoryManager.setTipologieDocumentiName("tipologieDocumentiName");
        repositoryManager.setDocumentiName("documentiName");
        repositoryManager.setScadenzaDocumentiName("scadenzaDocumentiName");
        repositoryManager.setTagsName("tagsName");
        dynamo.setRepositoryManager(repositoryManager);
        PnSsConfig.Dynamo.EventStream eventStream = new PnSsConfig.Dynamo.EventStream();
        eventStream.setDocumentName("documentName");
        eventStream.setTableMetadata("tableMetadata");
        dynamo.setEventStream(eventStream);
        PnSsConfig.Dynamo.RetryStrategy dynamoRetryStrategy = new PnSsConfig.Dynamo.RetryStrategy();
        dynamoRetryStrategy.setMaxAttempts(3L);
        dynamoRetryStrategy.setMinBackoff(3L);
        dynamo.setRetryStrategy(dynamoRetryStrategy);
        pnSsConfig.setDynamo(dynamo);

        PnSsConfig.Sqs sqs = new PnSsConfig.Sqs();
        PnSsConfig.Sqs.Availability availability = new PnSsConfig.Sqs.Availability();
        availability.setSqsName("sqsName");
        sqs.setAvailability(availability);
        pnSsConfig.setSqs(sqs);

        PnSsConfig.EventBridge eventBridge = new PnSsConfig.EventBridge();
        eventBridge.setDisponibilitaDocumentiName("disponibilitaDocumentiName");
        pnSsConfig.setEventBridge(eventBridge);

        PnSsConfig.Endpoint endpoint = new PnSsConfig.Endpoint();
        PnSsConfig.Endpoint.StateMachine stateMachine = new PnSsConfig.Endpoint.StateMachine();
        stateMachine.setContainerBaseUrl("containerBaseUrl");
        stateMachine.setValidate("validate");
        endpoint.setStateMachine(stateMachine);
        pnSsConfig.setEndpoint(endpoint);

        PnSsConfig.GestoreRepository gestoreRepository = new PnSsConfig.GestoreRepository();
        PnSsConfig.GestoreRepository.RetryStrategy gestoreRepositoryRetryStrategy = new PnSsConfig.GestoreRepository.RetryStrategy();
        gestoreRepositoryRetryStrategy.setMaxAttempts(3L);
        gestoreRepositoryRetryStrategy.setMinBackoff(3L);
        gestoreRepository.setRetryStrategy(gestoreRepositoryRetryStrategy);
        pnSsConfig.setGestoreRepository(gestoreRepository);

        PnSsConfig.S3 s3 = new PnSsConfig.S3();
        PnSsConfig.S3.RetryStrategy s3RetryStrategy = new PnSsConfig.S3.RetryStrategy();
        s3RetryStrategy.setMaxAttempts(3L);
        s3RetryStrategy.setMinBackoff(3L);
        s3.setRetryStrategy(s3RetryStrategy);
        pnSsConfig.setS3(s3);

        assertThatCode(pnSsConfig::logConfiguration).doesNotThrowAnyException();
    }
}
