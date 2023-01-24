package it.pagopa.pnss.repositoryManager.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pnss.repositoryManager.dto.ChecksumEnumDTO;
import it.pagopa.pnss.repositoryManager.dto.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.dto.TimestampedEnumDTO;
import it.pagopa.pnss.repositoryManager.model.DocTypesEntity;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputDescription;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocTypesControllerTest {
	
	 private static DynamoDbClient ddb;
	 private static ObjectMapper mapper = new ObjectMapper();
	 private static DynamoDbEnhancedClient enhancedClient;
	
	DocTypesEntity docTypeEntity = new DocTypesEntity();
	DocTypesService docTypeService = null; //new DocTypesService(enhancedClient, mapper);
	DocTypesController docTypesController = null;//new DocTypesController(docTypeService);
	DocTypesInput docTypesInput = new DocTypesInput();
	
	// Define the data members required for the test
	
    private static String tableName = "DocTypes";
    private static String name = "name";
    
    
    @BeforeAll
    public static void setUp() {

        //Create a DynamoDbClient object
        Region region = Region.EU_CENTRAL_1;
        ddb = DynamoDbClient.builder()
                .region(region)
                .build();

        // Create a DynamoDbEnhancedClient object
        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();

    }

    
	@Test
	@Order(1)
	public void whenInitializingAWSS3Service_thenNotNull() {
		assertNotNull(ddb);
		System.out.println("Test 1 passed");
	}

	@Test
	@Order(2)
	public void CreateTable() {

		String result = createTable(ddb, tableName, name);
		assertFalse(result.isEmpty());
		System.out.println("\n Test 2 passed");
	}

	@Test
	@Order(3)
	public void DescribeTable() {
		describeDymamoDBTable(ddb, tableName);
		System.out.println("\n Test 3 passed");
	}

	@Test
	@Order(4)
	public void PostItem() {
		docTypesInput.setName("provachiave1");
		docTypesInput.setCheckSum(ChecksumEnumDTO.MD5);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setLifeCycleTag("lifeCicle1");
		docTypesInput.setTimeStamped(TimestampedEnumDTO.NONE);
		Mono<ResponseEntity<DocTypesOutput>> response = docTypesController.postdocTypes(docTypesInput);
		// DocTypesOutput userOut = docTypeService.postUser(docTypesInput);
		Assertions.assertNotNull(response.block().getBody().getName());
	}

	@Test
	@Order(5)
	public void getItem() {

		String partitionId = "provachiave1";
		Mono<ResponseEntity<DocTypesOutput>> response = docTypesController.getdocTypes(partitionId);

		// DocumentOutput documentOut = documentService.postdocument(documentInput);
		System.out.println(response.block().getBody().getName());
		Assertions.assertNotNull(response.block().getStatusCode());
	}
    
    
	
    @Test
    @Order(6)
    public void putItem() {
  
    	docTypesInput.setName("provachiave1");
		docTypesInput.setCheckSum(ChecksumEnumDTO.MD5);
		docTypesInput.setDigitalSignature(true);
		docTypesInput.setInformationClassification(ConfidentialityLevelEnum.C);
		docTypesInput.setLifeCycleTag("lifeCicle2");
		docTypesInput.setTimeStamped(TimestampedEnumDTO.NONE);
        Mono<ResponseEntity<DocTypesOutput>> response = docTypesController.updatedocTypes(docTypesInput);
    	
        //DocumentOutput documentOut = documentService.postdocument(documentInput);
        System.out.println(response.block().getBody().getName());
        Assertions.assertNotNull(response.block().getStatusCode());
    }
    
    @Test
    @Order(7)
    public void deleteItem() {
  
    	String partitionId = "provachiave1";
        Mono<ResponseEntity<DocTypesOutput>> response = docTypesController.deletedocTypes(partitionId);
    	
        //DocumentOutput documentOut = documentService.postdocument(documentInput);
        System.out.println(response.block().getBody().getName());
        Assertions.assertNotNull(response.block().getStatusCode());
    }
	
//	@Test
//	void getUserConfigurationTestPositive() {
//		this.user.setName("provaNome");
//		this.user.setApiKey("0123");
//		this.handler.getUser(user.getName());
//		Assertions.assertEquals(this.user.getApiKey(), this.docTypesController.getUser(this.user.getName()));
//	    //Assertions.assertNotNull(handler.queryTable(user.getName()));
//	}
    
    
    public static void describeDymamoDBTable(DynamoDbClient ddb,String tableName ) {

        DescribeTableRequest request = DescribeTableRequest.builder()
            .tableName(tableName)
            .build();

        try {
            TableDescription tableInfo = ddb.describeTable(request).table();
            if (tableInfo != null) {
                System.out.format("Table name  : %s\n", tableInfo.tableName());
                System.out.format("Table ARN   : %s\n", tableInfo.tableArn());
                System.out.format("Status      : %s\n", tableInfo.tableStatus());
                System.out.format("Item count  : %d\n", tableInfo.itemCount().longValue());
                System.out.format("Size (bytes): %d\n", tableInfo.tableSizeBytes().longValue());

                ProvisionedThroughputDescription throughputInfo = tableInfo.provisionedThroughput();
                System.out.println("Throughput");
                System.out.format("  Read Capacity : %d\n", throughputInfo.readCapacityUnits().longValue());
                System.out.format("  Write Capacity: %d\n", throughputInfo.writeCapacityUnits().longValue());

                List<AttributeDefinition> attributes = tableInfo.attributeDefinitions();
                System.out.println("Attributes");

                for (AttributeDefinition a : attributes) {
                    System.out.format("  %s (%s)\n", a.attributeName(), a.attributeType());
                }
            }

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("\nDone!");
    }
    
    public static String createTable(DynamoDbClient ddb, String tableName, String key) {
        DynamoDbWaiter dbWaiter = ddb.waiter();
        CreateTableRequest request = CreateTableRequest.builder()
            .attributeDefinitions(AttributeDefinition.builder()
                .attributeName(key)
                .attributeType(ScalarAttributeType.S)
                .build())
            .keySchema(KeySchemaElement.builder()
                .attributeName(key)
                .keyType(KeyType.HASH)
                .build())
            .provisionedThroughput(ProvisionedThroughput.builder()
                .readCapacityUnits(new Long(5))
                .writeCapacityUnits(new Long(5))
                .build())
            .tableName(tableName)
            .build();

        String newTable ="";
        try {
            CreateTableResponse response = ddb.createTable(request);
            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                .tableName(tableName)
                .build();

            // Wait until the Amazon DynamoDB table is created.
            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);
            newTable = response.tableDescription().tableName();
            return newTable;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
       return "";
    }

}

