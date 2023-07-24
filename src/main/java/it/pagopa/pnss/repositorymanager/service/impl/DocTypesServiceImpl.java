package it.pagopa.pnss.repositorymanager.service.impl;

import java.util.ArrayList;
import java.util.List;

import it.pagopa.pnss.common.constant.Constant;
import org.springframework.stereotype.Service;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

@Service
@Slf4j
public class DocTypesServiceImpl implements DocTypesService {

    private final ObjectMapper objectMapper;

    private final DynamoDbAsyncTable<DocTypeEntity> docTypeEntityDynamoDbAsyncTable;

    private final String TABLE_NAME = "DocTypeEntity";

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
    	listEntity.forEach(entity -> {
    		listDto.add(objectMapper.convertValue(entity, DocumentType.class));
    	});
    	return listDto;
    }

    @Override
    public Mono<DocumentType> getDocType(String typeId) {
        final String GET_DOC_TYPE = "DocTypesServiceImpl.getDocType()";
        log.debug(Constant.INVOKING_METHOD, GET_DOC_TYPE, typeId);
        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(typeId).build()))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.debug(throwable.getMessage()))
                   .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class))
                   .doOnSuccess(documentType -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, typeId, GET_DOC_TYPE, documentType));
    }

    @Override
//    public Flux<DocumentType> getAllDocumentType() {
    public Mono<List<DocumentType>> getAllDocumentType() {
        final String GET_ALL_DOCUMENT_TYPE = "DocTypesServiceImpl.getAllDocumentType()";
        log.debug(Constant.INVOKING_METHOD, GET_ALL_DOCUMENT_TYPE, "");

        return Mono.from(docTypeEntityDynamoDbAsyncTable.scan())
        		   .map(Page::items)
        		   .switchIfEmpty(Mono.empty())
        		   .map(this::convert)
                   .doOnSuccess(documentTypes -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, "", GET_ALL_DOCUMENT_TYPE, documentTypes));
    }

    @Override
    public Mono<DocumentType> insertDocType(DocumentType docTypeInput) {
        final String DOC_TYPE_INPUT = "docTypeInput in DocTypesServiceImpl insertDocType()";

        log.info(Constant.CHECKING_VALIDATION_PROCESS, DOC_TYPE_INPUT);
        if (docTypeInput == null) {
            log.warn(Constant.VALIDATION_PROCESS_FAILED, DOC_TYPE_INPUT, "The object DocumentType is null");
            throw new RepositoryManagerException("The object DocumentType is null");
        }
        if (docTypeInput.getTipoDocumento() == null) {
            log.warn(Constant.VALIDATION_PROCESS_FAILED, DOC_TYPE_INPUT, "The attribute tipoDocumento is null");
            throw new RepositoryManagerException("The attribute tipoDocumento is null");
        }
        log.info(Constant.VALIDATION_PROCESS_PASSED, DOC_TYPE_INPUT);
        
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                   .partitionValue(docTypeInput.getTipoDocumento())
                                                                                   .build()))
                .flatMap(foundedDocumentType -> Mono.error(new ItemAlreadyPresent(docTypeInput.getTipoDocumento())))
                .doOnError(ItemAlreadyPresent.class, throwable -> log.error("Error in DocTypesServiceImpl.insertDocType(): ItemAlreadyPresent - '{}'", throwable.getMessage()))
                .switchIfEmpty(Mono.just(docTypeInput))
                .flatMap(unused -> {
                    log.debug(Constant.INSERTING_DATA_IN_DYNAMODB_TABLE, docTypeEntityInput, TABLE_NAME);
                    return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                            docTypeEntityInput)));
                })
                .doOnSuccess(unused -> {
                    log.info(Constant.INSERTED_DATA_IN_DYNAMODB_TABLE, TABLE_NAME);
                    log.info(Constant.SUCCESSFUL_OPERATION_LABEL, docTypeInput.getTipoDocumento(), "DocTypesServiceImpl.insertDocType()", docTypeInput);
                })
                .thenReturn(docTypeInput);
    }

    @Override
    public Mono<DocumentType> updateDocType(String typeId, DocumentType docTypeInput) {
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
        docTypeEntityInput.setTipoDocumento(typeId);

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(typeId).build()))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error("Error in DocTypesServiceImpl.updateDocType(): DocumentTypeNotPresentException - '{}'", throwable.getMessage()))
                   .zipWhen(unused -> {
                       log.debug(Constant.UPDATING_DATA_IN_DYNAMODB_TABLE, docTypeEntityInput, TABLE_NAME);
                       return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.updateItem(docTypeEntityInput));
                   })
                   .doOnSuccess(unused -> {
                       log.info(Constant.UPDATED_DATA_IN_DYNAMODB_TABLE, TABLE_NAME);
                   })
                   .map(objects -> objectMapper.convertValue(objects.getT2(), DocumentType.class))
                   .doOnSuccess(documentType -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, typeId, "DocTypesServiceImpl.updateDocType()", documentType));
    }

    @Override
    public Mono<DocumentType> deleteDocType(String typeId) {
        Key typeKey = Key.builder().partitionValue(typeId).build();

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(typeKey))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error("Error in DocTypesServiceImpl.deleteDocType(): DocumentTypeNotPresentException - '{}'", throwable.getMessage()))
                   .zipWhen(docTypeToDelete -> {
                       log.debug(Constant.DELETING_DATA_IN_DYNAMODB_TABLE, typeId, TABLE_NAME);
                       return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.deleteItem(typeKey));
                   })
                   .doOnSuccess(unused ->{
                       log.info(Constant.DELETED_DATA_IN_DYNAMODB_TABLE, TABLE_NAME);
                   })
                   .map(objects -> objectMapper.convertValue(objects.getT1(), DocumentType.class))
                   .doOnSuccess(documentType -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, typeId, "DocTypesServiceImpl.deleteDocType()", documentType));
    }
}
