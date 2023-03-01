package it.pagopa.pnss.repositorymanager.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import it.pagopa.pnss.transformation.service.CommonS3ObjectService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Service
@Slf4j
public class DocumentServiceImpl extends CommonS3ObjectService implements DocumentService {

	private final ObjectMapper objectMapper;
	private final DynamoDbAsyncTable<DocumentEntity> documentEntityDynamoDbAsyncTable;
	private final DocTypesService docTypesService;
	private final RetentionService retentionService;
	private final BucketName bucketName;

	public DocumentServiceImpl(ObjectMapper objectMapper, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
			RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, DocTypesService docTypesService,
			RetentionService retentionService, BucketName bucketName) {
		this.docTypesService = docTypesService;
		this.documentEntityDynamoDbAsyncTable = dynamoDbEnhancedAsyncClient
				.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
		this.objectMapper = objectMapper;
		this.retentionService = retentionService;
		this.bucketName = bucketName;
	}

	private Mono<DocumentEntity> getErrorIdDocNotFoundException(String documentKey) {
		log.error("getErrorIdDocNotFoundException() : document with documentKey \"{}\" not found", documentKey);
		return Mono.error(new DocumentKeyNotPresentException(documentKey));
	}

	@Override
	public Mono<Document> getDocument(String documentKey) {
		log.info("getDocument() : IN : documentKey {}", documentKey);
		return Mono
				.fromCompletionStage(
						documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
				.switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
				.doOnError(DocumentKeyNotPresentException.class, throwable -> log.error(throwable.getMessage()))
				.map(docTypeEntity -> objectMapper.convertValue(docTypeEntity, Document.class));
	}

	@Override
	public Mono<Document> insertDocument(DocumentInput documentInput) {
		log.info("insertDocument() : IN : documentInput : {}", documentInput);
		Document resp = new Document();
		if (documentInput == null) {
			throw new RepositoryManagerException("Document is null");
		}
		if (documentInput.getDocumentKey() == null || documentInput.getDocumentKey().isBlank()) {
			throw new RepositoryManagerException("Document Key is null");
		}
		String key = documentInput.getDocumentType();
		return Mono
				.fromCompletionStage(documentEntityDynamoDbAsyncTable
						.getItem(Key.builder().partitionValue(documentInput.getDocumentKey()).build()))
				.handle((documentFounded, sink) -> {
					if (documentFounded != null) {
						log.error("insertDocument() : document founded : {}", documentFounded);
						sink.error(new ItemAlreadyPresent(documentInput.getDocumentKey()));
					}
				}).doOnError(ItemAlreadyPresent.class, throwable -> log.error(throwable.getMessage()))
				.switchIfEmpty(Mono.just(documentInput)).flatMap(o -> docTypesService.getDocType(key)).flatMap(o -> {
					resp.setDocumentType(o);
					resp.setDocumentKey(documentInput.getDocumentKey());
					resp.setDocumentState(documentInput.getDocumentState());
					resp.setCheckSum(documentInput.getCheckSum());
					resp.setRetentionUntil(documentInput.getRetentionUntil());
					resp.setContentLenght(documentInput.getContentLenght());
					resp.setContentType(documentInput.getContentType());
					resp.setDocumentLogicalState(documentInput.getDocumentLogicalState());
					resp.setClientShortCode(documentInput.getClientShortCode());
					DocumentEntity documentEntityInput = objectMapper.convertValue(resp, DocumentEntity.class);
					return Mono.fromCompletionStage(
							documentEntityDynamoDbAsyncTable.putItem(builder -> builder.item(documentEntityInput)));
				}).thenReturn(resp);
	}

	@Override
	public Mono<Document> patchDocument(
			String documentKey, DocumentChanges documentChanges,
			String authPagopaSafestorageCxId, String authApiKey) {
		log.info("patchDocument() : IN : documentKey : {} , documentChanges {}", documentKey, documentChanges);
		
		return Mono
				.fromCompletionStage(
						documentEntityDynamoDbAsyncTable.getItem(Key.builder().partitionValue(documentKey).build()))
				.switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
				.doOnError(DocumentKeyNotPresentException.class, throwable -> log.error(throwable.getMessage()))
				.map(documentEntityStored -> {
					if (documentChanges == null) {
						return documentEntityStored;
					}
					log.info("patchDocument() : documentEntityStored : {}", documentEntityStored);
					if (documentChanges.getDocumentState() != null) {
						// stato tecnico
						documentEntityStored.setDocumentState(documentChanges.getDocumentState());
						// stato logico
						if (documentChanges.getDocumentState().equalsIgnoreCase("available")) {
							if (documentEntityStored.getDocumentType().getTipoDocumento()
									.equalsIgnoreCase("PN_NOTIFICATION_ATTACHMENTS")) {
								documentEntityStored.setDocumentLogicalState("PRELOADED");
							} else {
								documentEntityStored.setDocumentLogicalState("SAVED");
							}
						}
						if (documentChanges.getDocumentState().equalsIgnoreCase("attached")) {
							if (documentEntityStored.getDocumentType().getTipoDocumento()
									.equalsIgnoreCase("PN_NOTIFICATION_ATTACHMENTS")) {
								documentEntityStored.setDocumentLogicalState("ATTACHED");
							} else {
								throw new IllegalDocumentStateException(
										"Document State inserted is invalid for present document type");
							}
						}
					}
					if (documentChanges.getRetentionUntil() != null) {
						documentEntityStored.setRetentionUntil(documentChanges.getRetentionUntil());
					}
					if (documentChanges.getCheckSum() != null) {
						documentEntityStored.setCheckSum(documentChanges.getCheckSum());
					}
					if (documentChanges.getContentLenght() != null) {
						documentEntityStored.setContentLenght(documentChanges.getContentLenght());
					}
					log.info("patchDocument() : documentEntity for patch : {}", documentEntityStored);
					return documentEntityStored;
				})
				.doOnError(IllegalArgumentException.class, throwable -> log.error(throwable.getMessage()))
				.flatMap(documentEntityStored -> {
					log.info("patchDocument() : retention period : PRE");
					return retentionService.setRetentionPeriodInBucketObjectMetadata(
																			authPagopaSafestorageCxId, authApiKey, 
																			documentEntityStored);
				})
				.zipWhen(documentUpdated -> Mono
						.fromCompletionStage(documentEntityDynamoDbAsyncTable.updateItem(documentUpdated)))
				.map(objects -> objectMapper.convertValue(objects.getT2(), Document.class));
	}

	@Override
	public Mono<Document> deleteDocument(String documentKey) {
		log.info("deleteDocument() : IN : documentKey {}", documentKey);
		Key typeKey = Key.builder().partitionValue(documentKey).build();

		return Mono.fromCompletionStage(documentEntityDynamoDbAsyncTable.getItem(typeKey))
				.switchIfEmpty(getErrorIdDocNotFoundException(documentKey))
				.doOnError(DocumentKeyNotPresentException.class, throwable -> log.error(throwable.getMessage()))
				.zipWhen(documentToDelete -> Mono
						.fromCompletionStage(documentEntityDynamoDbAsyncTable.deleteItem(typeKey)))
				.map(objects -> objectMapper.convertValue(objects.getT2(), Document.class));
	}
}
