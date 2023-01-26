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

import it.pagopa.pnss.repositoryManager.dto.ChecksumEnumDTO;
import it.pagopa.pnss.repositoryManager.dto.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.dto.TimestampedEnumDTO;
import it.pagopa.pnss.repositoryManager.dto.TipoDocumentoEnum;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocTypesInternalApiControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_URL = "http://localhost:8080/doc-type";

	private static final ChecksumEnumDTO PARTITION_ID = ChecksumEnumDTO.MD5;
	private static final ChecksumEnumDTO NO_EXISTENT_PARTITION_ID = ChecksumEnumDTO.SHA256;
	private static final TipoDocumentoEnum SORT_KEY = TipoDocumentoEnum.PN_LEGAL_FACTS;

	@Test
	@Order(1)
	// Codice test: DTSS.101.1
	public void postItem() {

		DocTypesInput docTypesInput = new DocTypesInput();
		docTypesInput.setCheckSum(PARTITION_ID);
		docTypesInput.setTipoDocumento(SORT_KEY);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTipoTrasformazione("tipoTrasformazione1");
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimestampedEnumDTO.STANDARD);
		
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

		DocTypesInput docTypesInput = new DocTypesInput();
		//docTypesInput.setCheckSum(PARTITION_ID);
		docTypesInput.setTipoDocumento(SORT_KEY);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTipoTrasformazione("tipoTrasformazione1");
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimestampedEnumDTO.STANDARD);
		
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
			.expectBody(DocTypesOutput.class);

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
			.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

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
		
		DocTypesInput docTypesInput = new DocTypesInput();
		docTypesInput.setCheckSum(PARTITION_ID);
		docTypesInput.setTipoDocumento(SORT_KEY);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTipoTrasformazione("tipoTrasformazione1");
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimestampedEnumDTO.STANDARD);
		
		webTestClient.put()
			         .uri(BASE_URL)
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
		
		DocTypesInput docTypesInput = new DocTypesInput();
		docTypesInput.setCheckSum(NO_EXISTENT_PARTITION_ID);
		docTypesInput.setTipoDocumento(SORT_KEY);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTipoTrasformazione("tipoTrasformazione1");
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimestampedEnumDTO.STANDARD);
		
		webTestClient.put()
			         .uri(BASE_URL)
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
		
		DocTypesInput docTypesInput = new DocTypesInput();
		//docTypesInput.setCheckSum(NO_EXISTENT_PARTITION_ID);
		docTypesInput.setTipoDocumento(SORT_KEY);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTipoTrasformazione("tipoTrasformazione1");
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setTimeStamped(TimestampedEnumDTO.STANDARD);
		
		webTestClient.put()
			         .uri(BASE_URL)
			         .accept(APPLICATION_JSON)
			         .contentType(APPLICATION_JSON)
			         .body(BodyInserters.fromValue(docTypesInput))
			         .exchange()
			         .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		System.out.println("\n Test 8 (putItemNoExistentKey) passed \n");

	}

	@Test
	@Order(9)
	// codice test: DTSS.103.1
	public void deleteItem() {
		
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID.name())
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(DocTypesOutput.class);
	    
	    System.out.println("\n Test 9 (deleteItem) passed \n");

	}
	
	@Test
	@Order(10)
	// codice test: DTSS.103.2
	public void deleteItemNoExistentKey() {
		
		webTestClient.delete()
			.uri(BASE_URL+"/"+NO_EXISTENT_PARTITION_ID.name())
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
