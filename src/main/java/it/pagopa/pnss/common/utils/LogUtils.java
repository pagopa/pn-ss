package it.pagopa.pnss.common.utils;

import it.pagopa.pn.commons.utils.MDCUtils;

public class LogUtils {

    private LogUtils() {
        throw new IllegalStateException("LogUtils is a constant class");
    }

    //KEYS
    public static final String MDC_CORR_ID_KEY = MDCUtils.MDC_CX_ID_KEY;

    //LABELS
    public static final String ENDING_PROCESS_WITH_ERROR = "Ending '{}' Process with error = '{}' - '{}'";
    public static final String INVOKING_METHOD = "Invoking operation '{}' with args: '{}'";
    public static final String INVOKING_METHOD_WITHOUT_ARGS = "Invoking operation '{}'";
    public static final String SUCCESSFUL_OPERATION_LABEL = "Successful operation: '{}' = '{}'";
    public static final String SUCCESSFUL_OPERATION_LABEL_NO_ARGS = "Successful operation: '{}'";
    public static final String EXCEPTION_IN_PROCESS = "Exception in '{}'";
    public static final String INVOKING_INTERNAL_SERVICE = "Invoking internal service '{}' '{}'. Waiting Sync response.";
    public static final String CLIENT_METHOD_INVOCATION = "Client method '{}' - args: '{}'";
    public static final String CLIENT_METHOD_INVOCATION_WITH_ARGS = "Client method '{}' - args: '{}'";
    public static final String CLIENT_METHOD_RETURN = "Return client method: {} = {}";
    public static final String CLIENT_METHOD_RETURN_WITH_ERROR = "Return client method '{}' with error: {} - {}";
    public static final String INITIALIZING = "Initializing '{}'";
    public static final String EXCEPTION_DURING_INITIALIZATION = "Exception during '{}' initialization";
    public static final String ARG = " - '{}'";

    //DOWNLOAD
    public static final String DOWNLOAD_FILE = "DownloadCall.downloadFile()";

    //URI BUILDER
    public static final String CREATE_URI_FOR_DOWNLOAD_FILE = "UriBuilderService.createUriForDownloadFile()";
    public static final String CREATE_URI_FOR_UPLOAD_FILE = "UriBuilderService.createUriForUploadFile()";
    public static final String BUILDS_UPLOAD_URL = "UriBuilderService.buildsUploadUrl()";
    public static final String SIGN_BUCKET = "UriBuilderService.signBucket()";
    public static final String GET_FILE_DOWNLOAD_RESPONSE = "UriBuilderService.getFileDownloadResponse()";
    public static final String RECOVER_DOCUMENT_FROM_BUCKET = "UriBuilderService.recoverDocumentFromBucket()";
    public static final String GET_PRESIGNED_URL = "UriBuilderService.getPresignedUrl()";
    public static final String GET_FILE = "getFile()";
    public static final String CREATE_FILE = "createFile()";

    //UPDATE METADATA
    public static final String UPDATE_FILE_METADATA = "updateFileMetadata()";
    public static final String UPDATE_METADATA = "FileMetadataUpdateService.updateMetadata()";
    public static final String IGNORED_UPDATE_METADATA_CONFIG = "IgnoredUpdateMetadataConfig";
    public static final String PARSE_IGNORED_UPDATE_METADATA_LIST = "IgnoredUpdateMetadataConfig.parseIgnoredUpdateMetadataList()";
    public static final String REFRESH_IGNORED_UPDATE_METADATA_LIST = "IgnoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList()";
    public static final String REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED = "refreshIgnoredUpdateMetadataListScheduled()";
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
    public static final String INSERT_OR_UPDATE_SCADENZA_DOCUMENTI = "insertOrUpdateScadenzaDocumenti()";

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
    public static final String GET_OBJECT_TAGGING = "getObjectTagging()";
    public static final String LIST_OBJECT_VERSION = "getListObjectVersions()";
    public static final String DELETE_OBJECT_TAGGING = "deleteObjectTagging()";
    public static final String GET_OBJECT = "getObject()";
    public static final String HEAD_OBJECT = "headObject()";
    public static final String GET_BUCKET_LIFECYCLE_CONFIGURATION = "getBucketLifecycleConfiguration()";
    public static final String PRESIGN_GET_OBJECT = "presignGetObject()";
    public static final String RESTORE_OBJECT = "restoreObject()";
    public static final String DELETE_OBJECT = "deleteObject()";
    public static final String DELETE_VERSIONS_OBJECT = "deleteVersionsObject()";

    //CLOUDWATCH
    public static final String PUBLISH_RESPONSE_TIME = "CloudWatchMetricsService.publishResponseTime()";
    public static final String ERROR_RETRIEVING_METRIC_NAMESPACE = "Error retrieving metric namespace. The given provider is not valid.";
    public static final String GET_DIMENSION = "MetrcisDimensionConfiguration.getDimension()";

    //VALIDATION
    public static final String X_TRACE_ID_VALUE = "X_TRACE_ID_VALUE";
    public static final String USER_CONFIGURATION = "USER_CONFIGURATION";

    //TRANSFORMATION
    public static final String NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER = "newStagingBucketObjectCreatedListener()";
    public static final String NEW_STAGING_BUCKET_OBJECT_CREATED = "TransformationService.newStagingBucketObjectCreated()";
    public static final String OBJECT_TRANSFORMATION = "TransformationService.objectTransformation()";
    public static final String SIGN_AND_TIMEMARK_TRANSFORMATION = "TransformationService.signAndTimemarkTransformation()";
    public static final String RASTER_TRANSFORMATION = "TransformationService.rasterTransformation()";
    public static final String CHANGE_FROM_STAGING_BUCKET_TO_HOT_BUCKET = "TransformationService.changeFromStagingBucketToHotBucket()";
    public static final String TRANSFORMATION_CONFIG = "TransformationConfig";

    //RETENTION SERVICE
    public static final String GET_RETENTION_UNTIL = "RetentionService.getRetentionUntil()";
    public static final String SET_RETENTION_PERIOD_IN_BUCKET_OBJECT_METADATA = "RetentionService.setRetentionPeriodInBucketObjectMetadata()";

    //PN SIGN SERVICE

    public static final String PN_SIGN_PDF_DOCUMENT = "PnSignProviderService.signPdfDocument()";
    public static final String PN_SIGN_XML_DOCUMENT = "PnSignProviderService.signXmlDocument()";
    public static final String PN_PKCS_7_SIGNATURE = "PnSignProviderService.pkcs7Signature()";
    //SQS
    public static final String INSERTING_DATA_IN_SQS = "Inserting data {} in SQS '{}'";
    public static final String INSERTED_DATA_IN_SQS = "Inserted data in SQS '{}'";

    //RETRY
    public static final String RETRY_ATTEMPT = "Retry attempt number '{}' caused by : {}";
    public static final String SHORT_RETRY_ATTEMPT = "Short retry attempt number '{}' caused by : {} - {}";


    //INDEXING
    public static final String INDEXING_CONFIGURATION = "IndexingConfiguration";
    public static final String GET_TAGS_RELATIONS_OP = "getTagsRelations";
    public static final String GET_TAGS_DOCUMENT = "getTagsDocument";
    public static final String GET_TAGS_RELATIONS = "TagsService.getTagsRelations()";
    public static final String PUT_TAGS = "TagsService.putTags()";
    public static final String UPDATE_TAGS = "TagsService.updateTags()";
    public static final String SET_TAG = "TagsService.setTag()";
    public static final String DELETE_TAG = "TagsService.deleteTag()";
    public static final String UPDATE_RELATIONS = "TagsService.updateRelations()";
    public static final String PUT_TAGS_OP = "putTags";
    public static final String POST_TAG_DOCUMENT = "postTagsDocument";
    public static final String ADDITIONAL_FILE_TAGS_SEARCH = "additionalFileTagsSearch";
    public static final String SEARCH_TAGS = "AdditionalFileTagsService.searchTags()";
    public static final String VALIDATE_QUERY_PARAMS = "AdditionalFileTagsService.validateQueryParams()";
    public static final String MASSIVE_POST_TAG_DOCUMENT = "massivePostTagDocument";


    //EVENTBRIDGE
    public static final String EVENT_BRIDGE_PUT_SINGLE_EVENT = "EventBridge - PutEvents. putSingleEvent()";

}
