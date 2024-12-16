#!/bin/bash

set -e

start_time=$(date +%s)

## CONFIGURATION ##
VERBOSE=false
AWS_REGION="eu-south-1"
LOCALSTACK_ENDPOINT="http://localhost:4566"
SQS_ENDPOINT=$LOCALSTACK_ENDPOINT
DYNAMODB_ENDPOINT=$LOCALSTACK_ENDPOINT
S3_ENDPOINT=$LOCALSTACK_ENDPOINT
SSM_ENDPOINT=$LOCALSTACK_ENDPOINT


## DEFINITIONS ##
S3_BUCKETS=(
            "pn-ss-storage-safestorage"
            "pn-ss-storage-safestorage-staging"
            "pn-runtime-environment-variables"
            )

FILES_TO_BUCKETS=(
  "UpdateFileMetadataIgnore.list:pn-runtime-environment-variables/pn-safe-storage"
)

SQS_QUEUES=(
  "dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs"
  "Pn-Ss-Availability-Queue"
          )

DYNAMODB_TABLES=(
  "pn-SsAnagraficaClient:name"
  "pn-SsTipologieDocumenti:tipoDocumento"
  "pn-SsDocumenti:documentKey"
  "pn-SsScadenzaDocumenti:documentKey"
  "pn-SsTags:tagKeyValue"
              )

LIFECYCLE_RULE='{
                  "Rules": [
                    {
                      "ID": "MoveToGlacier",
                      "Filter": {
                        "Prefix": ""
                      },
                      "Status": "Enabled",
                      "Transitions": [
                        {
                          "Days": 1,
                          "StorageClass": "GLACIER"
                        }
                      ]
                    }
                  ]
                }'

OBJECT_LOCK_CONFIG='{
                      "ObjectLockEnabled": "Enabled",
                      "Rule": {
                        "DefaultRetention": {
                          "Mode": "GOVERNANCE",
                          "Days": 1
                        }
                      }
                    }'

SSM_CONFIG='{
      "globals": [
        {
          "key": "IUN",
          "indexed": true,
          "multivalue": true
        },
        {
          "key": "DataNotifica",
          "indexed": true,
          "multivalue": true
        },
        {
          "key": "Conservazione",
          "indexed": false,
          "multivalue": false
        },
        {
          "key": "TAG_MULTIVALUE_NOT_INDEXED",
          "indexed": false,
          "multivalue": true
        },
        {
          "key": "TAG_SINGLEVALUE_INDEXED",
          "indexed": true,
          "multivalue": false
        }
      ],
      "locals": [
        {
          "key": "pn-radd-fsu~DataCreazione",
          "indexed": true,
          "multivalue": true
        },
        {
          "key": "pn-downtime-logs~active",
          "indexed": false,
          "multivalue": false
        }
      ],
      "limits": {
        "MaxTagsPerRequest": 50,
        "MaxOperationsOnTagsPerRequest": 4,
        "MaxFileKeys": 5,
        "MaxMapValuesForSearch": 10,
        "MaxFileKeysUpdateMassivePerRequest": 100,
        "MaxTagsPerDocument": 2,
        "MaxValuesPerTagDocument": 5,
        "MaxValuesPerTagPerRequest": 5
      }
    }'


## LOGGING FUNCTIONS ##
log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

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

  if ! aws dynamodb describe-table --table-name "$table_name" \
                                   --endpoint-url "$DYNAMODB_ENDPOINT" \
                                   --region $AWS_REGION ; then
    if ! aws dynamodb create-table --table-name "$table_name" \
                                   --attribute-definitions AttributeName="$pk",AttributeType=S \
                                   --key-schema AttributeName="$pk",KeyType=HASH \
                                   --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
                                   --region $AWS_REGION \
                                   --endpoint-url "$DYNAMODB_ENDPOINT" ; then
      log "Failed to create DynamoDB table: $table_name"
      return 1
    else
      log "Created DynamoDB table: $table_name"
    fi
  else
    log "Table already exists: $table_name"
  fi
  log "DynamoDB table $table_name initialized"
}

create_bucket(){
  local bucket=$1
  log "Creating bucket: $bucket"
  aws s3api create-bucket --bucket "$bucket" \
                          --region "$AWS_REGION"  \
                          --endpoint-url "$S3_ENDPOINT" \
                          --create-bucket-configuration LocationConstraint="$AWS_REGION" \
                          --object-lock-enabled-for-bucket && \
  aws s3api put-object-lock-configuration --bucket "$bucket" \
                                          --object-lock-configuration "$OBJECT_LOCK_CONFIG"  \
                                          --region "$AWS_REGION"  \
                                          --endpoint-url "$S3_ENDPOINT" && \
  aws s3api put-bucket-lifecycle-configuration --bucket "$bucket" \
                                               --region "$AWS_REGION"  \
                                               --lifecycle-configuration "$LIFECYCLE_RULE"  \
                                               --endpoint-url "$S3_ENDPOINT" && \
  log "Created and configured bucket: $bucket" || \
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
                           --endpoint-url "$S3_ENDPOINT"; then
    log "File already exists: $file; Skipping upload"
    return 0
  fi

  if [ ! -f "$file" ]; then
    log "File does not exist, creating empty file: $file"
    touch "$file"
  fi

  log "Uploading file: $file"
  silent aws s3 cp "$file" "s3://$bucket/$s3_key" --region "$AWS_REGION" \
                                           --endpoint-url "$S3_ENDPOINT" && \
  log "Uploaded file: $file" || \
  { log "Failed to upload file: $file to s3://$bucket/$s3_key"; return 1; }

  # Verify file
  silent aws s3api head-object --bucket "$bucket" \
                        --key "$s3_key" \
                        --region "$AWS_REGION" \
                        --endpoint-url "$S3_ENDPOINT" || \
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

  aws sqs create-queue --queue-name "$queue" \
                       --region "$AWS_REGION"  \
                       --endpoint-url "$SQS_ENDPOINT" && \
  log "Created queue: $queue" || \
  { log "Failed to create queue: $queue"; return 1; }
}

create_buckets(){
  log "### Initializing S3 Buckets ###"
  local pids=()

  for bucket in "${S3_BUCKETS[@]}"; do
    if ! silent aws s3api head-bucket --bucket "$bucket" \
                               --endpoint-url $LOCALSTACK_ENDPOINT; then
        { silent create_bucket $bucket && log "Created bucket: $bucket"; } &
        pids+=($!)
    else
      log "Bucket already exists: $bucket"
    fi
  done

  wait_for_pids pids "Failed to create bucket"
  return "$?"
}

create_queues(){
  log "### Initializing SQS Queues ###"
  local pids=()

  for queue in "${SQS_QUEUES[@]}"; do
    silent create_queue $queue &
    pids+=($!)
  done

  wait_for_pids pids "Failed to create queue"
  return "$?"
}

initialize_dynamo() {
  log "Initializing DynamoDB tables"
  local pids=()

  for entry in "${DYNAMODB_TABLES[@]}"; do
    IFS=: read -r table_name pk <<< "$entry"
    silent create_table "$table_name" "$pk" &
    pids+=($!)
  done

  wait_for_pids pids "Failed to create DynamoDB table"
  return "$?"
}

initialize_sm(){
  log "### Initializing SecretsManager ###"
    local secret_name=$1
    local secret_value=$2
    echo "Creating secret: $secret_name"
    silent echo "Secret value: $secret_value"

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
    indexingConfig=$SSM_CONFIG
    silent aws ssm put-parameter --name "Pn-SS-IndexingConfiguration" \
                                 --type "String" \
                                 --value "$indexingConfig" \
                                 --region $AWS_REGION \
                                 --endpoint-url $SSM_ENDPOINT && \
    log "Created SSM parameter: Pn-SS-IndexingConfiguration" || \
    { log "Failed to create SSM parameter: Pn-SS-IndexingConfiguration"; return 1; }
}




cleanup() {
  log "Cleaning up"
  silent rm -rf "$TMP_PATH"
  log "Cleanup complete"
}

## MAIN ##
main(){
  log "### Initializing LocalStack Services ###"

  ( create_buckets && \
      load_files_to_buckets && \
      create_queues && \
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