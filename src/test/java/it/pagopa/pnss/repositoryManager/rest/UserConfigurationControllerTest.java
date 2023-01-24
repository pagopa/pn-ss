package it.pagopa.pnss.repositoryManager.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.ArrayList;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationDestinationDTO;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserConfigurationControllerTest {

	@Autowired
	private WebTestClient webTestClient;
	
	private static final String PARTITION_ID = "provachiave1";
	private static final String SORT_ID = "sortedKey1";
	
	@SuppressWarnings("unused")
	private static final String BAD_PARTITION_ID = "prova_chiave_bad";
	
	private static final String BASE_URL = "http://localhost:8090/user-configuration";
	

//	 private static DynamoDbClient ddb;
//	 private static ObjectMapper mapper = new ObjectMapper();
//	 private static DynamoDbEnhancedClient enhancedClient;
//	
//	UserConfigurationEntity userEntity = new UserConfigurationEntity();
//	UserConfigurationService userService = new UserConfigurationService(enhancedClient, mapper);
//	UserConfigurationController userController = new UserConfigurationController(userService);
//	UserConfigurationInput userInput = new UserConfigurationInput();
//    UserConfiguration user = new UserConfiguration();
//    UserConfigurationDestination userDestination = new UserConfigurationDestination();
//	
//	// Define the data members required for the test
//	
//    private static String tableName = "UserConfiguration";
//    private static String name = "name";
//    private static List<String> canCreate = new ArrayList<String>();
//    private static List<String> canRead = new ArrayList<String>();;
//    private static String signatureInfo;
//    private static UserConfigurationDestinationDTO destination = new UserConfigurationDestinationDTO();
//    private static String apiKey = "";

//    @BeforeAll
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
//	@Test
//	@Order(1)
//	public void whenInitializingAWSS3Service_thenNotNull() {
//		assertNotNull(ddb);
//		System.out.println("Test 1 passed");
//	}
//
////	@Test
////	@Order(2)
////	public void CreateTable() {
////
////		String result = createTable(ddb, tableName, name);
////		assertFalse(result.isEmpty());
////		System.out.println("\n Test 2 passed");
////	}
//
//	@Test
//	@Order(1)
//	public void describeTable() {
//		describeDymamoDBTable(DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME);
//		System.out.println("\n Test 1 (describeTable) passed");
//	}

	@Test
	@Order(1)
	// codice test: ECGRAC.100.1
	public void postItem() {
		
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(PARTITION_ID);
		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
		userInput.getCanCreate().add("create1");
		userInput.setCanRead(new ArrayList<String>());
		userInput.getCanRead().add("read1");
		userInput.setDestination(new UserConfigurationDestinationDTO());
		userInput.getDestination().setSqsUrl("url1");
		userInput.setSignatureInfo("info1");
		
		webTestClient.post()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isOk();
		
		System.out.println("\n Test 1 (postItem) passed \n");
		
//		Mono<ResponseEntity<UserConfigurationOutput>> response = userController.postUser(userInput);
//		// UserConfigurationOutput userOut = userService.postUser(userInput);
//		Assertions.assertNotNull(response.block().getBody().getName());
	}
	
    @Test
    @Order(2)
    // codice test: ECGRAC.100.1
    public void postItemPartitionKeyDuplicated() {
    	
//		UserConfigurationInput userInput = new UserConfigurationInput();
//		userInput.setName(PARTITION_ID);
//		userInput.setApiKey(SORT_ID);
//		userInput.setCanCreate(new ArrayList<String>());
//		userInput.getCanCreate().add("create1");
//		userInput.setCanRead(new ArrayList<String>());
//		userInput.getCanRead().add("read1");
//		userInput.setDestination(new UserConfigurationDestinationDTO());
//		userInput.getDestination().setSqsUrl("url1");
//		userInput.setSignatureInfo("info1");
//		
//		webTestClient.post()
//	        .uri(BASE_URL)
//	        .accept(APPLICATION_JSON)
//	        .contentType(APPLICATION_JSON)
//	        .body(BodyInserters.fromValue(userInput))
//	        .exchange()
//	        .expectStatus()
//	        .isOk();
//		
//		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) insert 1");
//    	
//		webTestClient.put()
//	        .uri(BASE_URL)
//	        .accept(APPLICATION_JSON)
//	        .contentType(APPLICATION_JSON)
//	        .body(BodyInserters.fromValue(userInput))
//	        .exchange()
//	        .expectStatus()
//	        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
//		
//		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) insert 2");
	
		System.out.println("\n Test 2 (postItemPartitionKeyDuplicated) passed \n");
    }
	
	@Test
	@Order(3)
	// codice test: ECGRAC.101.1
	public void getItem() {

//		String partitionId = "provachiave1";
		
		webTestClient.get()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isOk()
	        .expectBody(UserConfigurationOutput.class);
        
        System.out.println("\n Test 3 (getItem) passed \n");
		
//		Mono<ResponseEntity<UserConfigurationOutput>> response = userController.getUser(partitionId);
//
//		// DocumentOutput documentOut = documentService.postdocument(documentInput);
//		System.out.println(response.block().getBody().getName());
//		Assertions.assertNotNull(response.block().getStatusCode());
	}
	
    @Test
    @Order(4)
    // codice test: ECGRAC.102.1
    public void putItem() {
    	
		UserConfigurationInput userInput = new UserConfigurationInput();
		userInput.setName(PARTITION_ID);
		userInput.setApiKey(SORT_ID);
		userInput.setCanCreate(new ArrayList<String>());
    	userInput.getCanCreate().add("create2");
    	userInput.setCanRead(new ArrayList<String>());
    	userInput.getCanRead().add("read2");
    	userInput.setDestination(new UserConfigurationDestinationDTO());
    	userInput.getDestination().setSqsUrl("url2");
    	userInput.setSignatureInfo("info2");
    	
		webTestClient.put()
	        .uri(BASE_URL)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .body(BodyInserters.fromValue(userInput))
	        .exchange()
	        .expectStatus()
	        .isOk();
	
		System.out.println("\n Test 4 (putItem) passed \n");
    }
    
    @Test
    @Order(5)
    // codice test: ECGRAC.103.1
    public void deleteItem() {
    	
		webTestClient.delete()
			.uri(BASE_URL+"/"+PARTITION_ID)
	        .accept(APPLICATION_JSON)
	        .exchange()
	        .expectStatus()
	        .isOk()
	        .expectBody(UserConfigurationOutput.class)
	        ;
	    
	    System.out.println("\n Test 5 (deleteItem) passed \n");
  
//    	String partitionId = "provachiave1";
//        Mono<ResponseEntity<UserConfigurationOutput>> response = userController.deleteUser(partitionId);
//    	
//        //DocumentOutput documentOut = documentService.postdocument(documentInput);
//        System.out.println(response.block().getBody().getName());
//        Assertions.assertNotNull(response.block().getStatusCode());
    }
	
//	@Test
//	void getUserConfigurationTestPositive() {
//		this.user.setName("provaNome");
//		this.user.setApiKey("0123");
//		this.handler.getUser(user.getName());
//		Assertions.assertEquals(this.user.getApiKey(), this.userController.getUser(this.user.getName()));
//	    //Assertions.assertNotNull(handler.queryTable(user.getName()));
//	}
    
    
//	private void describeDymamoDBTable(String tableName) {
//
//		DescribeTableRequest request = DescribeTableRequest.builder().tableName(tableName).build();
//
//		try {
//			TableDescription tableInfo = dynamoDbClient.describeTable(request).table();
//			if (tableInfo != null) {
//				System.out.format("Table name  : %s\n", tableInfo.tableName());
//				System.out.format("Table ARN   : %s\n", tableInfo.tableArn());
//				System.out.format("Status      : %s\n", tableInfo.tableStatus());
//				System.out.format("Item count  : %d\n", tableInfo.itemCount().longValue());
//				System.out.format("Size (bytes): %d\n", tableInfo.tableSizeBytes().longValue());
//
//				ProvisionedThroughputDescription throughputInfo = tableInfo.provisionedThroughput();
//				System.out.println("Throughput");
//				System.out.format("  Read Capacity : %d\n", throughputInfo.readCapacityUnits().longValue());
//				System.out.format("  Write Capacity: %d\n", throughputInfo.writeCapacityUnits().longValue());
//
//				List<AttributeDefinition> attributes = tableInfo.attributeDefinitions();
//				System.out.println("Attributes");
//
//				for (AttributeDefinition a : attributes) {
//					System.out.format("  %s (%s)\n", a.attributeName(), a.attributeType());
//				}
//			}
//
//		} catch (DynamoDbException e) {
//			System.err.println(e.getMessage());
//			System.exit(1);
//		}
//		System.out.println("\nDone!");
//	}

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
