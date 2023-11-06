package it.pagopa.pnss.common.utils;

import it.pagopa.pn.commons.utils.MDCUtils;

public class LogUtils {

    private LogUtils() {
        throw new IllegalStateException("LogUtils is a constant class");
    }

    //KEYS
    public static final String MDC_CORR_ID_KEY = "correlation_id";

    //LABELS
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
    public static final String SUCCESSFUL_OPERATION_LABEL = "Successful operation: '{}' = '{}'";
    public static final String INVOKING_INTERNAL_SERVICE = "Invoking internal service '{}' '{}'. Waiting Sync response.";
    public static final String CLIENT_METHOD_INVOCATION = "Client method '{}' - args: '{}'";
    public static final String CLIENT_METHOD_RETURN = "Return client method: {} = {}";
    public static final String ARG = " - '{}'";
    public static final String GET_FILE = "getFile()";
    public static final String CREATE_FILE = "createFile()";

    //DYNAMODB
    public static final String DOC_TYPE_TABLE = "DocTypeEntity";

    //URI BUILDER
    public static final String CREATE_URI_FOR_DOWNLOAD_FILE = "UriBuilderService.createUriForDownloadFile()";
    public static final String CREATE_URI_FOR_UPLOAD_FILE = "UriBuilderService.createUriForUploadFile()";
    public static final String BUILDS_UPLOAD_URL = "UriBuilderService.buildsUploadUrl()";
    public static final String SIGN_BUCKET = "UriBuilderService.signBucket()";
    public static final String GET_FILE_DOWNLOAD_RESPONSE = "UriBuilderService.getFileDownloadResponse()";

    //UPDATE METADATA
    public static final String UPDATE_FILE_METADATA = "updateFileMetadata()";
    public static final String UPDATE_METADATA = "FileMetadataUpdateService.updateMetadata()";

    public static final String MISSING_CONTENT_TYPE = "ContentType is missing";
    public static final String MISSING_DOCUMENT_TYPE = "Document type is missing";
    public static final String MISSING_TRACE_ID = "XTraceId is missing";

    // ----------------------------------------------------------------------------

    //REPOSITORY MANAGER
    public static final String REPOSITORY_MANAGER = "repositorymanager";

    //INTERNAL
    public static final String GET_DOC_TYPE = "getDocType()";
    public static final String INSERT_DOC_TYPE = "insertDocType()";
    public static final String UPDATE_DOC_TYPE = "updateDocType()";
    public static final String DELETE_DOC_TYPE = "deleteDocType()";

    //EXTERNAL
    public static final String GET_DOC_TYPES = "getDocTypes()";
    public static final String GET_USER = "getUser()";
    public static final String POST_DOCUMENT = "postDocument()";
    public static final String GET_DOCUMENT = "getDocument()";
    public static final String PATCH_DOCUMENT = "patchDocument()";
    public static final String GET_DOCUMENTS_CONFIGS = "getDocumentsConfigs()";



    // ---------------------------------------------------------------------------

    //S3
    public static final String PRESIGN_PUT_OBJECT = "presignPutObject()";
    public static final String PUT_OBJECT = "putObject()";
    public static final String PUT_OBJECT_RETENTION = "putObjectRetention()";
    public static final String PUT_OBJECT_TAGGING = "putObjectTagging()";
    public static final String GET_OBJECT = "getObject()";
    public static final String HEAD_OBJECT = "headObject()";
    public static final String GET_BUCKET_LIFECYCLE_CONFIGURATION = "getBucketLifecycleConfiguration()";
    public static final String PRESIGN_GET_OBJECT = "presignGetObject()";
    public static final String RESTORE_OBJECT = "restoreObject()";
    public static final String DELETE_OBJECT = "deleteObject()";

    //VALIDATION
    public static final String X_TRACE_ID_VALUE = "X_TRACE_ID_VALUE";
    public static final String USER_CONFIGURATION = "USER_CONFIGURATION";

    //TRANSFORMATION
    public static final String NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER = "newStagingBucketObjectCreatedListener()";
    public static final String NEW_STAGING_BUCKET_OBJECT_CREATED = "TransformationService.newStagingBucketObjectCreated()";
    public static final String OBJECT_TRANSFORMATION = "TransformationService.objectTransformation()";
    public static final String CHANGE_FROM_STAGING_BUCKET_TO_HOT_BUCKET = "TransformationService.changeFromStagingBucketToHotBucket()";

    //RETENTION SERVICE
    public static final String GET_RETENTION_UNTIL = "RetentionService.getRetentionUntil()";
    public static final String SET_RETENTION_PERIOD_IN_BUCKET_OBJECT_METADATA = "RetentionService.setRetentionPeriodInBucketObjectMetadata()";

}
