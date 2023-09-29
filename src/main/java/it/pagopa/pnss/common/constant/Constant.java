package it.pagopa.pnss.common.constant;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import static org.springframework.http.MediaType.*;

public final class Constant {

    private Constant() {
        throw new IllegalStateException("Constant is a constant class");
    }

    //DOCUMENT STATUSES
    public static final String PRELOADED = "preloaded";
    public static final String ATTACHED = "attached";
    public static final String SAVED = "saved";
    public static final String BOOKED = "booked";
    public static final String FREEZED = "freezed";
    public static final String AVAILABLE = "available";
    public static final String STAGED = "staged";
    public static final String DELETED = "deleted";

    //LOGS CONSTANTS
    public static final String STARTING_PROCESS = "Starting '{}' Process";
    public static final String CHECKING_VALIDATION_PROCESS = "Checking '{}'";
    public static final String VALIDATION_PROCESS_PASSED = "'{}' passed";
    public static final String VALIDATION_PROCESS_FAILED = "'{}' failed error = '{}'";
    public static final String STARTING_PROCESS_ON = "Starting '{}' Process on '{}'";
    public static final String ENDING_PROCESS = "Ending '{}' Process";
    public static final String ENDING_PROCESS_ON = "Ending '{}' Process on '{}'";
    public static final String ENDING_PROCESS_WITH_ERROR = "Ending '{}' Process with error = '{}' - '{}'";
    public static final String INSERTING_DATA_IN_DYNAMODB_TABLE = "Inserting data '{}' in DynamoDB table '{}'";
    public static final String INSERTED_DATA_IN_DYNAMODB_TABLE = "Inserted data in DynamoDB table '{}'";
    public static final String UPDATING_DATA_IN_DYNAMODB_TABLE = "Updating data '{}' in DynamoDB table '{}'";
    public static final String UPDATED_DATA_IN_DYNAMODB_TABLE = "Updated data in DynamoDB table '{}'";
    public static final String DELETING_DATA_IN_DYNAMODB_TABLE = "Deleting data '{}' in DynamoDB table '{}'";
    public static final String DELETED_DATA_IN_DYNAMODB_TABLE = "Deleted data in DynamoDB table '{}'";
    public static final String INVOKING_METHOD = "Invoking operation '{}' with args: '{}'";
    public static final String INVOKING_EXTERNAL_SERVICE = "Invoking external service '{}'. Waiting Sync response.";
    public static final String SUCCESSFUL_OPERATION_LABEL = "Successful operation on '{}': '{}' = '{}'";
    public static final String INVOKING_INTERNAL_SERVICE = "Invoking internal service '{}' '{}'. Waiting Sync response.";
    public static final String CLIENT_METHOD_INVOCATION = "Client method '{}' - args: '{}'";
    public static final String ARG = " - '{}'";

    //OTHER
    public static final String STORAGE_TYPE = "storageType";
    public static final String EVENT_BUS_SOURCE_AVAILABLE_DOCUMENT = "SafeStorageOutcomeEvent";
    public static final String GESTORE_DISPONIBILITA_EVENT_NAME = "GESTORE DISPONIBILITA";
    public static final String IMAGE_TIFF_VALUE = "image/tiff";
}
