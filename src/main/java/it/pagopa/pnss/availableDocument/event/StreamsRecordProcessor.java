package it.pagopa.pnss.availableDocument.event;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StreamsRecordProcessor implements IRecordProcessor {
    private Integer checkpointCounter;

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;
    private final String disponibilitaDocumentiEventBridge;

    public StreamsRecordProcessor(AmazonDynamoDB dynamoDBClient2, String tableName, String disponibilitaDocumentiEventBridge) {
        this.tableName = tableName;
        this.amazonDynamoDB = dynamoDBClient2;
        this.disponibilitaDocumentiEventBridge = disponibilitaDocumentiEventBridge;

    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        checkpointCounter = 0;
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        List<PutEventsRequestEntry> requestEntries = new ArrayList<>();
        for (Record record : processRecordsInput.getRecords()) {
            String data = new String(record.getData().array(), Charset.forName("UTF-8"));
            log.info(data);
            if (record instanceof RecordAdapter) {
                com.amazonaws.services.dynamodbv2.model.Record streamRecord = ((RecordAdapter) record)
                        .getInternalObject();
                PutEventsRequestEntry putEventsRequestEntry = null;
                try{
                    switch (streamRecord.getEventName()) {
                        case "INSERT":
                            break;
                        case "MODIFY":
                            ManageDynamoEvent mde = new ManageDynamoEvent();
                            putEventsRequestEntry = mde.manageItem(amazonDynamoDB, tableName,disponibilitaDocumentiEventBridge,
                                    streamRecord.getDynamodb().getNewImage(), streamRecord.getDynamodb().getOldImage());


                            break;
                        case "REMOVE":
                            break;


                    }
                    try {
                        processRecordsInput.getCheckpointer().checkpoint();
                        if (putEventsRequestEntry!=null ){
                            requestEntries.add(putEventsRequestEntry);
                        }
                    }
                    catch (Exception e) {
                        log.error("Errore nella commit dell'evento ",e );
                    }

                }catch (Exception ex){
                    log.error("Errore generico nella gestione dell'evento ",ex );
                }
            }
        }
        if (!requestEntries.isEmpty()){
            EventBridgeClient eventBridgeClient =
                    EventBridgeClient.builder()
                            //.credentialsProvider()
                            //.defaultsMode()
                            //.dualstackEnabled()
                            //.endpointOverride()
                            //.httpClient()
                            //.httpClientBuilder()
                            .build();
            PutEventsRequest eventsRequest = PutEventsRequest.builder()
                    .entries(requestEntries)
                    .build();
            PutEventsResponse result = eventBridgeClient.putEvents(eventsRequest );
        }

    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            try {
                shutdownInput.getCheckpointer().checkpoint();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}