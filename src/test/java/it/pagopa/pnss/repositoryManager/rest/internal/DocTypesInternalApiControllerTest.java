package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TypeIdEnum;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocTypesInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_URL = "http://localhost:8080/safe-storage/internal/v1/doctypes";

	private static final TypeIdEnum PARTITION_ID = TypeIdEnum.NOTIFICATION_ATTACHMENTS;
	private static final TypeIdEnum NO_EXISTENT_PARTITION_ID = TypeIdEnum.EXTERNAL_LEGAL_FACTS;
	
	private DocumentType getDocumentType() {
		DocumentType docTypesInput = new DocumentType();
		docTypesInput.setTypeId(PARTITION_ID);
		docTypesInput.setChecksum(ChecksumEnum.MD5);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTipoTrasformazione("tipoTrasformazione1");
		docTypesInput.setInformationClassification(InformationClassificationEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimeStampedEnum.STANDARD);
		return docTypesInput;
	}

	@Test
	@Order(1)
	// Codice test: DTSS.101.1
	public void postItem() {

		DocumentType docTypesInput = getDocumentType();
		
		webTestClient.post()
					 .uri(BASE_URL)
					 .accept(APPLICATION_JSON)
					 .contentType(APPLICATION_JSON)
					 .body(BodyInserters.fromValue(docTypesInput))
					 .exchange()
					 .expectStatus().isOk();

		System.out.println("\n Test 1 (postItem) passed \n");

	}
	
	@Test
	@Order(2)
	// Codice test: DTSS.101.2
	public void postItemIncorrectParameters() {

		DocumentType docTypesInput = getDocumentType();
		docTypesInput.setTypeId(null);
		
		webTestClient.post()
					 .uri(BASE_URL)
					 .accept(APPLICATION_JSON)
					 .contentType(APPLICATION_JSON)
					 .body(BodyInserters.fromValue(docTypesInput))
					 .exchange()
		        .expectStatus()
		        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		System.out.println("\n Test 2 (postItemIncorrectParameters) passed \n");

	}

	@Test
	@Order(3)
	// codice test: DTSS.100.1
	public void getItem() {

		webTestClient.get()
			.uri(BASE_URL + "/" + PARTITION_ID.name())
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectBody(DocumentType.class);

		System.out.println("\n Test 3 (getItem) passed \n");

	}
	
	@Test
	@Order(4)
	// codice test: DTSS.100.2
	public void getItemNoExistentKey() {

		webTestClient.get()
			.uri(BASE_URL + "/" + NO_EXISTENT_PARTITION_ID.name())
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectBody().isEmpty();
		
		System.out.println("\n Test 4 (getItemNoExistentKey) passed \n");

	}
	
	@Test
	@Order(5)
	// codice test: DTSS.100.3
	public void getItemIncorrectParameters() {

		webTestClient.get()
			.uri(BASE_URL /*+ "/" + PARTITION_ID.name()*/)
			.accept(APPLICATION_JSON)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		System.out.println("\n Test 5 (getItemIncorrectParameters) passed \n");

	}

	@Test
	@Order(6)
	// codice test: DTSS.102.1
	public void putItem() {
		
		DocumentType docTypesInput = getDocumentType();
		docTypesInput.setChecksum(ChecksumEnum.SHA256);
		
		webTestClient.put()
			         .uri(BASE_URL + "/" + PARTITION_ID.getValue())
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesInput))
			         .exchange()
			         .expectStatus().isOk();

		System.out.println("\n Test 6 (putItem) passed \n");

	}
	
	@Test
	@Order(7)
	// codice test: DTSS.102.2
	public void putItemNoExistentKey() {
		
		DocumentType docTypesInput = getDocumentType();
		docTypesInput.setTypeId(NO_EXISTENT_PARTITION_ID);
		
		webTestClient.put()
			         .uri(BASE_URL + "/" + NO_EXISTENT_PARTITION_ID.name())
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesInput))
			         .exchange()
			         .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		System.out.println("\n Test 7 (putItemNoExistentKey) passed \n");

	}
	
	@Test
	@Order(8)
	// codice test: DTSS.102.3
	public void putItemIncorretctParameters() {
		
		DocumentType docTypesInput = getDocumentType();
		docTypesInput.setTypeId(NO_EXISTENT_PARTITION_ID);
		
		webTestClient.put()
			         .uri(BASE_URL /*+ "/" + NO_EXISTENT_PARTITION_ID.name()*/)
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesInput))
			         .exchange()
			         .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);

		System.out.println("\n Test 8 (putItemIncorretctParameters) passed \n");

	}

	@Test
	@Order(9)
	// codice test: DTSS.103.1
	public void deleteItem() {
		
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID.getValue())
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody().isEmpty();
	    
	    System.out.println("\n Test 9 (deleteItem) passed \n");

	}
	
	@Test
	@Order(10)
	// codice test: DTSS.103.2
	public void deleteItemNoExistentKey() {
		
		webTestClient.delete()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID.getValue())
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	    
	    System.out.println("\n Test 10 (deleteItemNoExistentKey) passed \n");

	}
	
	@Test
	@Order(11)
	// codice test: DTSS.103.3
	public void deleteItemIncorrectParameters() {
		
		webTestClient.delete()
			.uri(BASE_URL/*+"/"+NO_EXISTENT_PARTITION_ID.name()*/)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	    
	    System.out.println("\n Test 11 (deleteItemIncorrectParameters) passed \n");

	}

}
