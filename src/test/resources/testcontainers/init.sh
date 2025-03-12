#!/bin/bash

set -e

start_time=$(date +%s)

## CONFIGURATION ##
VERBOSE=false
AWS_REGION="eu-south-1"
LOCALSTACK_ENDPOINT="http://localhost:4566"

if [ "$RUNNING_IN_DOCKER" = "true" ]; then
    CONFIG_FILES_DIR="/config"
else
    CONFIG_FILES_DIR="../src/test/resources/testcontainers/config"
fi

## DEFINITIONS ##
S3_BUCKETS=(
            "pn-ss-storage-safestorage"
            "pn-ss-storage-safestorage-staging"
            "pn-runtime-environment-variables"
)

MAIN_BUCKET_QUEUE="pn-ss-main-bucket-events-queue"

FILES_TO_BUCKETS=(
  "UpdateFileMetadataIgnore.list:pn-runtime-environment-variables/pn-safe-storage"
)

SQS_QUEUES=(
  "Pn-Ss-Availability-Queue"
  "dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs"
  "pn-ss-staging-bucket-events-queue"
  "pn-ss-availability-events-queue"
  $MAIN_BUCKET_QUEUE
  "pn-ss-gestore-bucket-invocation-errors-queue"
  "pn-ss-external-notification-DEV-queue"
  "pn-ss-forward-events-pncoreeventbus-DLQueue"
  "pn-ss-scadenza-documenti-dynamoDBStreamDLQ-queue"
  "pn-ss-transformation-dummy-queue"
  "pn-ss-transformation-sign-and-timemark-queue"
  "pn-ss-transformation-sign-queue"
  "pn-ss-transformation-raster-queue"
)

DYNAMODB_TABLES=(
  "pn-SsAnagraficaClient:name"
  "pn-SsTipologieDocumenti:tipoDocumento"
  "pn-SsDocumenti:documentKey"
  "pn-SsScadenzaDocumenti:documentKey"
  "pn-SsTags:tagKeyValue"
              )

LIFECYCLE_RULE=$(cat ${CONFIG_FILES_DIR}/lifecycle-rules.json)
OBJECT_LOCK_CONFIG=$(cat ${CONFIG_FILES_DIR}/object-lock-configuration.json)
INDEXING_CONFIG=$(cat ${CONFIG_FILES_DIR}/indexing-config.json)
METRICS_SCHEMA_CONFIG=$(cat ${CONFIG_FILES_DIR}/metrics-schema-config.json)
TRANSFORMATION_CONFIG=$(cat ${CONFIG_FILES_DIR}/transformation-config.json)

## LOGGING FUNCTIONS ##
log() { echo "[pn-ss-init][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

silent() {
  if [ "$VERBOSE" = false ]; then
    "$@" > /dev/null 2>&1
  else
    "$@"
  fi
}

## HELPER FUNCTIONS ##
wait_for_pids() {
  local -n pid_array=$1
  local error_message=$2
  local exit_code=0

  for pid in "${pid_array[@]}"; do
    wait "$pid" || { log "$error_message"; exit_code=1; }
  done

  return "$exit_code"
}


## FUNCTIONS ##
create_table() {
  local table_name=$1
  local pk=$2

  log "Creating table:  Name: $table_name , PK: $pk"

  if ! silent aws dynamodb describe-table --table-name "$table_name" \
                                   --endpoint-url "$LOCALSTACK_ENDPOINT" \
                                   --region $AWS_REGION ; then
    if ! silent aws dynamodb create-table --table-name "$table_name" \
                                   --attribute-definitions AttributeName="$pk",AttributeType=S \
                                   --key-schema AttributeName="$pk",KeyType=HASH \
                                   --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
                                   --region $AWS_REGION \
                                   --endpoint-url "$LOCALSTACK_ENDPOINT" ; then
      log "Failed to create DynamoDB table: $table_name"
      return 1
    else
      log "Created DynamoDB table: $table_name"
    fi
  else
    log "Table already exists: $table_name"
  fi
}

create_bucket(){
  local bucket=$1

  if silent aws s3api head-bucket --bucket "$bucket" \
                           --region "$AWS_REGION" \
                           --endpoint-url "$LOCALSTACK_ENDPOINT"; then
    log "Bucket already exists: $bucket"
    return 0
  fi

  log "Creating bucket: $bucket"
  aws s3api create-bucket --bucket "$bucket" \
                          --region "$AWS_REGION"  \
                          --endpoint-url "$LOCALSTACK_ENDPOINT" \
                          --create-bucket-configuration LocationConstraint="$AWS_REGION" \
                          --object-lock-enabled-for-bucket && \
  aws s3api put-object-lock-configuration --bucket "$bucket" \
                                          --object-lock-configuration "$OBJECT_LOCK_CONFIG"  \
                                          --region "$AWS_REGION"  \
                                          --endpoint-url "$LOCALSTACK_ENDPOINT" && \
  echo "Created bucket: $bucket" || \
  { log "Failed to create bucket: $bucket" ; return 1; }
}



load_to_s3(){
  local file=$1
  local bucket_with_path=$2

  local bucket=$(echo "$bucket_with_path" | cut -d'/' -f1)
  local s3_path=$(echo "$bucket_with_path" | cut -d'/' -f2-)
  local s3_key="$s3_path/$(basename "$file")"

  if silent aws s3api head-object --bucket "$bucket" \
                           --key "$s3_key" \
                           --region "$AWS_REGION" \
                           --endpoint-url "$LOCALSTACK_ENDPOINT"; then
    log "File already exists: $file; Skipping upload"
    return 0
  fi

  if [ ! -f "$file" ]; then
    log "File does not exist, creating empty file: $file"
    touch "$file"
  fi

  log "Uploading file: $file"
  silent aws s3 cp "$file" "s3://$bucket/$s3_key" --region "$AWS_REGION" \
                                           --endpoint-url "$LOCALSTACK_ENDPOINT" && \
  log "Uploaded file: $file" || \
  { log "Failed to upload file: $file to s3://$bucket/$s3_key"; return 1; }

  # Verify file
  silent aws s3api head-object --bucket "$bucket" \
                        --key "$s3_key" \
                        --region "$AWS_REGION" \
                        --endpoint-url "$LOCALSTACK_ENDPOINT" || \
  { log "Failed to verify file: $file in s3://$bucket/$s3_key"; return 1; }

  # Delete file
  rm -f "$file"
}


load_files_to_buckets(){
  log "### Loading Files to S3 Buckets ###"
  local pids=()

  for entry in "${FILES_TO_BUCKETS[@]}"; do
    IFS=: read -r file bucket <<< "$entry"
    load_to_s3 "$file" "$bucket" &
    pids+=($!)
  done

  wait_for_pids pids "Failed to load files to buckets"
  return "$?"
}

create_queue(){
  local queue=$1
  log "Creating queue: $queue"

  if silent aws sqs get-queue-url --queue-name "$queue" \
                                  --region "$AWS_REGION"  \
                                  --endpoint-url "$LOCALSTACK_ENDPOINT"; then
    log "Queue already exists: $queue"
    return 0
  fi

  silent aws sqs create-queue --queue-name "$queue" \
                       --region "$AWS_REGION"  \
                       --endpoint-url "$LOCALSTACK_ENDPOINT" && \
  log "Created queue: $queue" || \
  { log "Failed to create queue: $queue"; return 1; }
}

create_event_bus()
{
  local event_bus_name=$1

  silent aws events describe-event-bus \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name $event_bus_name && \
    log "Event bus already exists: $event_bus_name" && \
    return 0

  aws events create-event-bus \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name $event_bus_name && \
    log "Event bus created: $event_bus_name" || \
  { log "Failed to create event bus: $event_bus_name"; return 1; }
}

create_eventbridge_rule() {
   local event_name=$1
   local event_pattern=$2

   if [ "x$event_pattern" = "x" ]; then
     log "##### Event pattern not provided, using schedule expression #####"
      aws events put-rule \
        --endpoint-url="$LOCALSTACK_ENDPOINT" \
        --region "$AWS_REGION" \
        --name $event_name \
        --schedule-expression "rate(1 minute)" \
        --state "ENABLED" && \
        log "Event rule created: $event_name" || \
      { log "Failed to create event rule: $event_name"; return 1; }
   else
     log "##### Event pattern provided #####"
     aws events put-rule \
      --endpoint-url="$LOCALSTACK_ENDPOINT" \
      --region "$AWS_REGION" \
      --name $event_name \
      --event-pattern "$event_pattern" \
      --state "ENABLED" && \
      log "Event rule created: $event_name" || \
      { log "Failed to create event rule: $event_name"; return 1; }
   fi
}

# Creazione dei parametri SSM
create_ssm_parameter() {
  local parameter_name=$1
  local parameter_value=$2
  echo "Creating parameter: $parameter_name"
  echo "Parameter value: $parameter_value"

  silent aws ssm get-parameter \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$parameter_name" && \
    log "Parameter already exists: $parameter_name" && \
    return 0

  aws ssm put-parameter \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --name "$parameter_name" \
    --type String \
    --value "$parameter_value" && \
    log "Parameter created: $parameter_name" || \
  { log "Failed to create parameter: $parameter_name"; return 1; }
}


put_sqs_as_rule_target() {
  local queue_name=$1
  local rule_name=$2
  echo "Putting queue $queue_name as target for rule $rule_name"

  local queue_arn=$(get_queue_arn $queue_name)

  aws events put-targets \
    --region "$AWS_REGION" \
    --endpoint-url "$LOCALSTACK_ENDPOINT" \
    --rule "$rule_name" \
    --targets "Id=${queue_name}-target,Arn=${queue_arn}"
}

create_buckets(){
  log "### Initializing S3 Buckets ###"
  local pids=()

  for bucket in "${S3_BUCKETS[@]}"; do
    log "Creating bucket: $bucket" && \
    create_bucket $bucket &
    pids+=($!)
  done

  wait_for_pids pids "Failed to create bucket"
  return "$?"
}

create_queues(){
  log "### Initializing SQS Queues ###"
  local pids=()

  for queue in "${SQS_QUEUES[@]}"; do
    create_queue $queue &
    pids+=($!)
  done

  wait_for_pids pids "Failed to create queue"
  return "$?"
}

initialize_dynamo() {
  log "### Initializing DynamoDB tables ###"
  local pids=()

  for entry in "${DYNAMODB_TABLES[@]}"; do
    IFS=: read -r table_name pk <<< "$entry"
    create_table "$table_name" "$pk" &
    pids+=($!)
  done

  wait_for_pids pids "Failed to create DynamoDB table"
  return "$?"
}

initialize_sm(){
  log "### Initializing SecretsManager ###"
    local secret_name=$1
    local secret_value=$2
    log "Creating secret: $secret_name"
    silent echo "Secret value: $secret_value"

    if silent aws secretsmanager get-secret-value \
      --region "$AWS_REGION" \
      --endpoint-url "$LOCALSTACK_ENDPOINT" \
      --secret-id "$secret_name"; then
      log "Secret already exists: $secret_name"
      return 0
    fi

   silent aws secretsmanager create-secret \
      --region "$AWS_REGION" \
      --endpoint-url "$LOCALSTACK_ENDPOINT" \
      --name "$secret_name" && \
   silent aws secretsmanager put-secret-value \
      --region "$AWS_REGION" \
      --endpoint-url "$LOCALSTACK_ENDPOINT" \
      --secret-id "$secret_name" \
      --secret-string "$secret_value" || \
  { log "Failed to create secret: Pn-SS-SignAndTimemark"; return 1; }
}

initialize_ssm(){
  log "### Initializing SSM Parameters ###"

  create_ssm_parameter "Pn-SS-IndexingConfiguration" "$INDEXING_CONFIG" && \
  create_ssm_parameter "Pn-SS-SignAndTimemark-MetricsSchema" "$METRICS_SCHEMA_CONFIG" && \
  create_ssm_parameter "Pn-SS-TransformationConfiguration" "$TRANSFORMATION_CONFIG" || \
  { log "Failed to initialize SSM parameters"; return 1; }
}

initialize_event_bridge() {
    log "Initializing EventBridge"
    local pids=()

    # Creating EventBridge Rules
    create_eventbridge_rule "PnSsEventRuleExternalNotifications" '{"source": ["GESTORE DISPONIBILITA"],"region": ["eu-south-1"],"account": ["000000000000"]}' &
    pids+=($!)

    create_eventbridge_rule "pn-ec-microsvc-dev-PnEcEventRuleAvailabilityManager" '{"source": ["GESTORE DISPONIBILITA"],"detail": {"documentType": ["PN_PAPER_ATTACHMENT"]}}' &
    pids+=($!)

    create_eventbridge_rule "PnSsEventRuleStagingBucket" '{"source": ["aws.s3"],"detail-type": ["Object Created", "Object Tags Added"],"detail": {"bucket": {"name": ["pn-ss-storage-safestorage-staging"]}}}' &
    pids+=($!)

    wait_for_pids pids "Failed to initialize EventBridge rules"
    [ "$?" -ne 0 ] && return 1

    pids=()
    # Attaching SQS queues as targets to rules
    put_sqs_as_rule_target "pn-ss-external-notification-DEV-queue" "PnSsEventRuleExternalNotifications" &
    pids+=($!)
    put_sqs_as_rule_target "pn-ss-staging-bucket-events-queue" "PnSsEventRuleStagingBucket" &
    pids+=($!)
    put_sqs_as_rule_target "pn-ss-availability-events-queue" "pn-ec-microsvc-dev-PnEcEventRuleAvailabilityManager" &
    pids+=($!)

    wait_for_pids pids "Failed to initialize EventBridge targets"
    return "$?"
}

cleanup() {
  log "Cleaning up"
  silent rm -rf "$TMP_PATH"
  log "Cleanup complete"
}

get_queue_arn()
    {
    local queue_name=$1
    queue_url=$(aws sqs get-queue-url --region $AWS_REGION --endpoint-url $LOCALSTACK_ENDPOINT --queue-name $queue_name --query "QueueUrl" --output text | tr -d '\r')

    if [[ $? -eq 0 ]]; then
      queue_arn=$(aws sqs get-queue-attributes --region "$AWS_REGION" --endpoint-url "$LOCALSTACK_ENDPOINT" --queue-url "$queue_url" --attribute-names "QueueArn" --query "Attributes.QueueArn" --output text | tr -d '\r')
      if [[ $? -eq 0 ]]; then
        echo "$queue_arn"
      else
        return 1
      fi
    fi
}

### QUEUE CONFIGURATIONS
pn_ss_storage_safestorage_staging_config(){
  local queue_arn=$(get_queue_arn "pn-ss-main-bucket-events-queue")

  if [ -z "$queue_arn" ]; then
    log "Failed to get queue ARN for queue: pn-ss-main-bucket-events-queue"
    return 1
  fi

  local notification_config='{
    "EventBridgeConfiguration": {},
    "QueueConfigurations": [
      {
        "QueueArn": "'"$queue_arn"'",
        "Events": ["s3:ObjectCreated:*"]
      }
    ]
  }'

  echo "$notification_config"
}

pn_ss_storage_safestorage_config(){
  local queue_arn=$(get_queue_arn "pn-ss-main-bucket-events-queue")

  if [ -z "$queue_arn" ]; then
    log "Failed to get queue ARN for queue: pn-ss-main-bucket-events-queue"
    return 1
  fi

  local notification_config='{
    "QueueConfigurations": [
      {
        "QueueArn": "'$queue_arn'",
        "Events": ["s3:ObjectCreated:*"]
      },
      {
        "QueueArn": "'$queue_arn'",
        "Events": ["s3:ObjectRemoved:*"]
      },
      {
        "QueueArn": "'$queue_arn'",
        "Events": ["s3:ObjectRestore:*"]
      },
      {
        "QueueArn": "'$queue_arn'",
        "Events": ["s3:LifecycleExpiration:DeleteMarkerCreated"]
      },
      {
        "QueueArn": "'$queue_arn'",
        "Events": ["s3:LifecycleTransition"]
      }
    ]
  }'

  echo "$notification_config"
}



buckets_configuration(){
  log "### Configuring Buckets ###"
  local pids=()

  local pn_ss_storage_safestorage_staging_config=$(pn_ss_storage_safestorage_staging_config)
  local pn_ss_storage_safestorage_config=$(pn_ss_storage_safestorage_config)

  for bucket in "${S3_BUCKETS[@]}"; do
    case $bucket in
      "pn-ss-storage-safestorage-staging")
        log "Configuring bucket: $bucket" && \
        silent aws s3api put-bucket-notification-configuration --bucket "$bucket" \
                                                      --notification-configuration "$pn_ss_storage_safestorage_staging_config" \
                                                      --region "$AWS_REGION" \
                                                      --endpoint-url "$LOCALSTACK_ENDPOINT" &
        pids+=($!)
        ;;
      "pn-ss-storage-safestorage")
        log "Configuring bucket: $bucket" && \
        ( silent aws s3api put-bucket-notification-configuration --bucket "$bucket" \
                                                      --notification-configuration "$pn_ss_storage_safestorage_config" \
                                                      --region "$AWS_REGION" \
                                                      --endpoint-url "$LOCALSTACK_ENDPOINT" && \
          silent aws s3api put-bucket-lifecycle-configuration --bucket "$bucket" \
                                                       --region "$AWS_REGION"  \
                                                       --lifecycle-configuration "$LIFECYCLE_RULE"  \
                                                       --endpoint-url "$LOCALSTACK_ENDPOINT" ) & \
        pids+=($!)
        ;;
    esac
  done


}

## MAIN ##
main(){
  log "### Initializing LocalStack Services ###"

  ( create_queues && \
      create_buckets && \
      load_files_to_buckets && \
      buckets_configuration && \
      initialize_event_bridge && \
      initialize_dynamo && \
      initialize_sm 'Pn-SS-SignAndTimemark' '{
                                              "aruba.sign.delegated.domain": "demoprod",
                                              "aruba.sign.delegated.password": "password11",
                                              "aruba.sign.delegated.user": "delegato",
                                              "aruba.sign.otp.pwd": "dsign",
                                              "aruba.sign.type.otp.auth": "demoprod",
                                              "aruba.sign.user": "titolare_aut",
                                              "aruba.timemark.user": "user1",
                                              "aruba.timemark.password": "password1"
                                            }' && \
      initialize_ssm ) & \
  local infra_pid=$!
  local exit_code=0

  wait $infra_pid || \
  { log "Error initializing infrastructure"; exit_code=1; }
  echo "Initialization complete"

  local end_time=$(date +%s)

  if [ "$exit_code" -eq 0 ]; then
    log "LocalStack Initialization Succeeded in $(($end_time - $start_time)) seconds"
  else
    log "LocalStack Initialization Failed in $(($end_time - $start_time)) seconds"
    return 1
  fi
}

## EXECUTION ##
main
#cleanup