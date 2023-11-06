package it.pagopa.pnss.repositorymanager.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import static it.pagopa.pnss.common.utils.LogUtils.DOC_TYPE_TABLE;
import static it.pagopa.pnss.common.utils.LogUtils.SUCCESSFUL_OPERATION_LABEL;

@Service
@CustomLog
public class DocTypesServiceImpl implements DocTypesService {

    private final ObjectMapper objectMapper;

    private final DynamoDbAsyncTable<DocTypeEntity> docTypeEntityDynamoDbAsyncTable;

    @Autowired
    RepositoryManagerDynamoTableName managerDynamoTableName;

    public DocTypesServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.objectMapper = objectMapper;
        this.docTypeEntityDynamoDbAsyncTable = dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.tipologieDocumentiName(),
                                                                                 TableSchema.fromBean(DocTypeEntity.class));
    }

    private Mono<DocTypeEntity> getErrorIdDocTypeNotFoundException(String typeId) {
        return Mono.error(new DocumentTypeNotPresentException(typeId));
    }
    
    private List<DocumentType> convert(List<DocTypeEntity> listEntity) {
    	List<DocumentType> listDto = new ArrayList<>();
    	if (listEntity == null || listEntity.isEmpty()) {
    		return listDto;
    	}
    	listEntity.forEach(entity -> listDto.add(objectMapper.convertValue(entity, DocumentType.class)));
    	return listDto;
    }

    @Override
    public Mono<DocumentType> getDocType(String typeId) {
        final String GET_DOC_TYPE = "DocTypesService.getDocType()";
        log.debug(LogUtils.INVOKING_METHOD, GET_DOC_TYPE, typeId);
        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(typeId).build()))
                   .doOnNext(result -> log.logGetDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), typeId, result))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class))
                   .doOnSuccess(documentType -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_DOC_TYPE, documentType));
    }

    @Override
    public Mono<List<DocumentType>> getAllDocumentType() {
        final String GET_ALL_DOCUMENT_TYPE = "DocTypesService.getAllDocumentType()";
        log.debug(LogUtils.INVOKING_METHOD, GET_ALL_DOCUMENT_TYPE, "");

        return Mono.from(docTypeEntityDynamoDbAsyncTable.scan())
        		   .map(Page::items)
        		   .switchIfEmpty(Mono.empty())
        		   .map(this::convert)
                   .doOnSuccess(documentTypes -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_ALL_DOCUMENT_TYPE, documentTypes));
    }

    @Override
    public Mono<DocumentType> insertDocType(DocumentType docTypeInput) {
        final String INSERT_DOC_TYPE = "DocTypesService.insertDocType()";
        log.debug(LogUtils.INVOKING_METHOD, INSERT_DOC_TYPE, docTypeInput);

        final String DOC_TYPE_INPUT = "docTypeInput in DocTypesServiceImpl insertDocType()";

        log.logChecking(DOC_TYPE_INPUT);
        if (docTypeInput == null) {
            String errorMsg = "The object DocumentType is null";
            log.logCheckingOutcome(DOC_TYPE_INPUT, false, errorMsg);
            throw new RepositoryManagerException(errorMsg);
        }
        if (docTypeInput.getTipoDocumento() == null) {
            String errorMsg = "The attribute 'tipoDocumento' is null";
            log.logCheckingOutcome(DOC_TYPE_INPUT, false, errorMsg);
            throw new RepositoryManagerException(errorMsg);
        }
        log.logCheckingOutcome(DOC_TYPE_INPUT, true);
        
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                   .partitionValue(docTypeInput.getTipoDocumento())
                                                                                   .build()))
                .doOnNext(result -> log.logGetDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), docTypeInput.getTipoDocumento(), result))
                .flatMap(foundedDocumentType -> Mono.error(new ItemAlreadyPresent(docTypeInput.getTipoDocumento())))
                .doOnError(ItemAlreadyPresent.class, throwable -> log.error("Error in DocTypesServiceImpl.insertDocType(): ItemAlreadyPresent - '{}'", throwable.getMessage()))
                .switchIfEmpty(Mono.just(docTypeInput))
                .flatMap(unused -> {
                    log.logPuttingDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), docTypeEntityInput);
                    return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                            docTypeEntityInput)));
                })
                .doOnNext(result -> log.logPutDoneDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName()))
                .doOnSuccess(unused -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_DOC_TYPE, docTypeInput))
                .thenReturn(docTypeInput);
    }

    @Override
    public Mono<DocumentType> updateDocType(String typeId, DocumentType docTypeInput) {
        final String UPDATE_DOC_TYPE = "DocTypesService.updateDocType()";
        log.debug(LogUtils.INVOKING_METHOD, UPDATE_DOC_TYPE, typeId);

        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
        docTypeEntityInput.setTipoDocumento(typeId);

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(typeId).build()))
                   .doOnNext(result -> log.logGetDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), typeId, result))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error("Error in DocTypesServiceImpl.updateDocType(): DocumentTypeNotPresentException - '{}'", throwable.getMessage()))
                   .zipWhen(unused -> Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.updateItem(docTypeEntityInput)))
                   .doOnNext(result -> log.logUpdateDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), result.getT2()))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), DocumentType.class))
                   .doOnSuccess(documentType -> log.info(SUCCESSFUL_OPERATION_LABEL, UPDATE_DOC_TYPE, documentType));
    }

    @Override
    public Mono<DocumentType> deleteDocType(String typeId) {
        final String DELETE_DOC_TYPE = "DocTypesService.deleteDocType()";
        log.debug(LogUtils.INVOKING_METHOD, DELETE_DOC_TYPE, typeId);

        Key typeKey = Key.builder().partitionValue(typeId).build();

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(typeKey))
                   .doOnNext(result -> log.logGetDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), typeId, result))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error("Error in DocTypesServiceImpl.deleteDocType(): DocumentTypeNotPresentException - '{}'", throwable.getMessage()))
                   .zipWhen(docTypeToDelete -> Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.deleteItem(typeKey)))
                   .doOnNext(result -> log.logDeleteDynamoDBEntity(managerDynamoTableName.tipologieDocumentiName(), typeId, result.getT2()))
                   .map(objects -> objectMapper.convertValue(objects.getT1(), DocumentType.class))
                   .doOnSuccess(documentType -> log.info(SUCCESSFUL_OPERATION_LABEL, DELETE_DOC_TYPE, documentType));
    }
}
