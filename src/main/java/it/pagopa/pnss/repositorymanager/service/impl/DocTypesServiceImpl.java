package it.pagopa.pnss.repositorymanager.service.impl;

import java.util.ArrayList;
import java.util.List;

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
        log.info("getDocType() : IN : typeId {}", typeId);
        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(typeId).build()))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error(throwable.getMessage()))
                   .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
    }

    @Override
//    public Flux<DocumentType> getAllDocumentType() {
    public Mono<List<DocumentType>> getAllDocumentType() {
        log.info("getAllDocumentType() : START");
        
//        return Mono.from(docTypeEntityDynamoDbAsyncTable.scan())
//                .map(Page::items)
//                .flatMapMany(Flux::fromIterable)
//                .map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, DocumentType.class));
        
        return Mono.from(docTypeEntityDynamoDbAsyncTable.scan())
        		   .map(Page::items)
        		   .switchIfEmpty(Mono.empty())
        		   .map(this::convert);
    }

    @Override
    public Mono<DocumentType> insertDocType(DocumentType docTypeInput) {
        log.info("insertDocType() : IN : docTypeInput : {}", docTypeInput);

        if (docTypeInput == null) {
            throw new RepositoryManagerException("The object DocumentType is null");
        }
        if (docTypeInput.getTipoDocumento() == null) {
            throw new RepositoryManagerException("The attribute tipoDocumento is null");
        }
        
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder()
                                                                                   .partitionValue(docTypeInput.getTipoDocumento())
                                                                                   .build()))
                .flatMap(foundedDocumentType -> Mono.error(new ItemAlreadyPresent(docTypeInput.getTipoDocumento())))
                .doOnError(ItemAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                .switchIfEmpty(Mono.just(docTypeInput))
                .flatMap(unused -> Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.putItem(builder -> builder.item(
                        docTypeEntityInput))))
                .thenReturn(docTypeInput);
    }

    @Override
    public Mono<DocumentType> updateDocType(String typeId, DocumentType docTypeInput) {
        log.info("updateDocType() : IN : typeId : {} , docTypeInput {}", typeId, docTypeInput);
        DocTypeEntity docTypeEntityInput = objectMapper.convertValue(docTypeInput, DocTypeEntity.class);
        docTypeEntityInput.setTipoDocumento(typeId);

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(typeId).build()))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error(throwable.getMessage()))
                   .zipWhen(unused -> Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.updateItem(docTypeEntityInput)))
                   .map(objects -> objectMapper.convertValue(objects.getT2(), DocumentType.class));
    }

    @Override
    public Mono<DocumentType> deleteDocType(String typeId) {
        log.info("deleteDocType() : IN : typeId : {}", typeId);
        Key typeKey = Key.builder().partitionValue(typeId).build();

        return Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.getItem(typeKey))
                   .switchIfEmpty(getErrorIdDocTypeNotFoundException(typeId))
                   .doOnError(DocumentTypeNotPresentException.class, throwable -> log.error(throwable.getMessage()))
                   .zipWhen(docTypeToDelete -> Mono.fromCompletionStage(docTypeEntityDynamoDbAsyncTable.deleteItem(typeKey)))
                   .map(objects -> objectMapper.convertValue(objects.getT1(), DocumentType.class));
    }
}
