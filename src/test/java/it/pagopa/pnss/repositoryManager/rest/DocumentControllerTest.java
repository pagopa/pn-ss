package it.pagopa.pnss.repositoryManager.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.dto.DocumentStateEnum;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentControllerTest {
	
	@Autowired
	private WebTestClient webTestClient;

	private static final String BASE_URL = "http://localhost:8080//document";

	private static final String PARTITION_ID = "checksum1";
	
//	 private static DynamoDbClient ddb;
//	 private static ObjectMapper mapper = new ObjectMapper();
//	 private static DynamoDbEnhancedClient enhancedClient;
//	
//	DocumentEntity documentEntity = new DocumentEntity();
//	DocumentService documentService = null; //new DocumentService(enhancedClient, mapper);
//	DocumentController documentController = null; //new DocumentController(documentService);
//	DocumentInput documentInput = new DocumentInput();
//	
//	// Define the data members required for the test
//	
//	private static String tableName = "Document";
//	
//	private static String documentkey = "documentKey";
    
//	@BeforeAll
//    public static void setUp() {
//
//        //Create a DynamoDbClient object
//        Region region = Region.EU_CENTRAL_1;
//        ddb = DynamoDbClient.builder()
//                .region(region)
//                .build();
//
//        // Create a DynamoDbEnhancedClient object
//        enhancedClient = DynamoDbEnhancedClient.builder()
//                .dynamoDbClient(ddb)
//                .build();
//
//    }
    
//    @Test
//    @Order(1)
//    public void whenInitializingAWSS3Service_thenNotNull() {
//        assertNotNull(ddb);
//        System.out.println("Test 1 passed");
//    }

//    @Test
//    @Order(2)
//    public void CreateTable() {
//
//       String result = createTable(ddb, tableName, documentkey);
//       assertFalse(result.isEmpty());
//       System.out.println("\n Test 2 passed");
//    }
    
//    @Test
//    @Order(3)
//    public void DescribeTable() {
//       describeDymamoDBTable(ddb,tableName);
//       System.out.println("\n Test 3 passed");
//    }
    	
    @Test
    @Order(1)
    public void postItem() {
  
    	DocumentInput documentInput = new DocumentInput();
    	documentInput.setCheckSum(PARTITION_ID);
//    	documentInput.setContentLenght("prova content lenght 2");
//    	documentInput.setContentType("prova content type 2");
//    	documentInput.setDocumentKey("chiavedocumento2");
//    	documentInput.setDocumentState("stato prova");
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
//    	documentInput.setDocumentType("tipo prova");
    	documentInput.setRetentionPeriod("retention prova");

		webTestClient.post()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
		
		System.out.println("\n Test 1 (postItem) passed \n");
    	
//        Mono<ResponseEntity<DocumentOutput>> response = documentController.postdocument(documentInput);    	
//        Assertions.assertNotNull(response.block().getStatusCode());
    }
    
    @Test
    @Order(2)
    public void getItem() {
    	
		webTestClient.get()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(DocumentOutput.class);
	    
	    System.out.println("\n Test 2 (getItem) passed \n");
  
//    	String partitionId = "chiavedocumento2";
//        Mono<ResponseEntity<DocumentOutput>> response = documentController.getdocument(partitionId);
//    	
//        //DocumentOutput documentOut = documentService.postdocument(documentInput);
//        System.out.println(response.block().getBody().getCheckSum());
//        Assertions.assertNotNull(response.block().getStatusCode());
    }
	
    @Test
    @Order(3)
    public void putItem() {
  
    	DocumentInput documentInput = new DocumentInput();
    	documentInput.setCheckSum(PARTITION_ID);
//    	documentInput.setContentLenght("prova content lenght 3");
//    	documentInput.setContentType("prova content type 3");
//    	documentInput.setDocumentKey("chiavedocumento2");
//    	documentInput.setDocumentState("stato prova 3");
    	documentInput.setDocumentState(DocumentStateEnum.AVAILABLE);
//    	documentInput.setDocumentType("tipo prova 3");
    	documentInput.setRetentionPeriod("retention prova 3");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(documentInput))
	        .exchange()
	        .expectStatus().isOk();
	
		System.out.println("\n Test 3 (putItem) passed \n");
    	
//        Mono<ResponseEntity<DocumentOutput>> response = documentController.updatedocument(documentInput);
//    	
//        //DocumentOutput documentOut = documentService.postdocument(documentInput);
//        System.out.println(response.block().getBody().getCheckSum());
//        Assertions.assertNotNull(response.block().getStatusCode());
    }
    
    @Test
    @Order(4)
    public void deleteItem() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody(DocumentOutput.class);
	    
	    System.out.println("\n Test 4 (deleteItem) passed \n");
  
//    	String partitionId = "chiavedocumento2";
//        Mono<ResponseEntity<DocumentOutput>> response = documentController.deletedocument(partitionId);
//    	
//        //DocumentOutput documentOut = documentService.postdocument(documentInput);
//        System.out.println(response.block().getBody().getCheckSum());
//        Assertions.assertNotNull(response.block().getStatusCode());
    }
    
//    public static void describeDymamoDBTable(DynamoDbClient ddb,String tableName ) {
//
//        DescribeTableRequest request = DescribeTableRequest.builder()
//            .tableName(tableName)
//            .build();
//
//        try {
//            TableDescription tableInfo = ddb.describeTable(request).table();
//            if (tableInfo != null) {
//                System.out.format("Table name  : %s\n", tableInfo.tableName());
//                System.out.format("Table ARN   : %s\n", tableInfo.tableArn());
//                System.out.format("Status      : %s\n", tableInfo.tableStatus());
//                System.out.format("Item count  : %d\n", tableInfo.itemCount().longValue());
//                System.out.format("Size (bytes): %d\n", tableInfo.tableSizeBytes().longValue());
//
//                ProvisionedThroughputDescription throughputInfo = tableInfo.provisionedThroughput();
//                System.out.println("Throughput");
//                System.out.format("  Read Capacity : %d\n", throughputInfo.readCapacityUnits().longValue());
//                System.out.format("  Write Capacity: %d\n", throughputInfo.writeCapacityUnits().longValue());
//
//                List<AttributeDefinition> attributes = tableInfo.attributeDefinitions();
//                System.out.println("Attributes");
//
//                for (AttributeDefinition a : attributes) {
//                    System.out.format("  %s (%s)\n", a.attributeName(), a.attributeType());
//                }
//            }
//
//        } catch (DynamoDbException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }
//        System.out.println("\nDone!");
//    }
    
//    public static String createTable(DynamoDbClient ddb, String tableName, String key) {
//        DynamoDbWaiter dbWaiter = ddb.waiter();
//        CreateTableRequest request = CreateTableRequest.builder()
//            .attributeDefinitions(AttributeDefinition.builder()
//                .attributeName(key)
//                .attributeType(ScalarAttributeType.S)
//                .build())
//            .keySchema(KeySchemaElement.builder()
//                .attributeName(key)
//                .keyType(KeyType.HASH)
//                .build())
//            .provisionedThroughput(ProvisionedThroughput.builder()
//                .readCapacityUnits(new Long(5))
//                .writeCapacityUnits(new Long(5))
//                .build())
//            .tableName(tableName)
//            .build();
//
//        String newTable ="";
//        try {
//            CreateTableResponse response = ddb.createTable(request);
//            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
//                .tableName(tableName)
//                .build();
//
//            // Wait until the Amazon DynamoDB table is created.
//            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
//            waiterResponse.matched().response().ifPresent(System.out::println);
//            newTable = response.tableDescription().tableName();
//            return newTable;
//
//        } catch (DynamoDbException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }
//       return "";
//    }

}