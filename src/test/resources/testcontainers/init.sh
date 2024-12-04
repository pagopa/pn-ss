#!/bin/bash

set -e

start_time=$(date +%s)
log() { echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

log "### Initializing LocalStack Services ###"

## Configuration ##

AWS_REGION="eu-central-1"
LOCALSTACK_ENDPOINT="http://localhost:4566"
SQS_ENDPOINT=$LOCALSTACK_ENDPOINT
DYNAMODB_ENDPOINT=$LOCALSTACK_ENDPOINT
S3_ENDPOINT=$LOCALSTACK_ENDPOINT
SSM_ENDPOINT=$LOCALSTACK_ENDPOINT

DYNAMODB_TABLES=(
  "pn-SsAnagraficaClient:name"
  "pn-SsTipologieDocumenti:tipoDocumento"
  "pn-SsDocumenti:documentKey"
  "pn-SsScadenzaDocumenti:documentKey"
  "pn-SsTags:tagKeyValue"
)
S3_BUCKETS=("pn-ss-storage-safestorage" "pn-ss-storage-safestorage-staging")
SQS_QUEUES=("dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs" "Pn-Ss-Availability-Queue")

LIFECYCLE_RULE='{"Rules":[{"ID":"MoveToGlacier","Filter":{"Prefix":""},"Status":"Enabled","Transitions":[{"Days":1,"StorageClass":"GLACIER"}]}]}'
OBJECT_LOCK_CONFIG='{"ObjectLockEnabled":"Enabled","Rule":{"DefaultRetention":{"Mode":"GOVERNANCE","Days":1}}}'
INDEXING_CONFIG_PATH="src/test/resources/indexing/json/indexing-configuration-default.json"
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

## Functions ##
create_table() {
  local table_name=$1
  local pk=$2

  log "Creating table:  Name: $table_name , PK: $pk"

  if ! aws dynamodb describe-table --table-name "$table_name" --endpoint-url "$DYNAMODB_ENDPOINT" ; then
    if ! aws dynamodb create-table \
      --table-name "$table_name" \
      --attribute-definitions AttributeName="$pk",AttributeType=S \
      --key-schema AttributeName="$pk",KeyType=HASH \
      --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
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
  aws s3api create-bucket --bucket "$bucket" --region "$AWS_REGION"  --endpoint-url "$S3_ENDPOINT" --create-bucket-configuration LocationConstraint="$AWS_REGION" --object-lock-enabled-for-bucket && \
  aws s3api put-object-lock-configuration --bucket "$bucket" --object-lock-configuration "$OBJECT_LOCK_CONFIG"  --endpoint-url "$S3_ENDPOINT" && \
  aws s3api put-bucket-lifecycle-configuration --bucket "$bucket" --lifecycle-configuration "$LIFECYCLE_RULE"  --endpoint-url "$S3_ENDPOINT" && \
  log "Created and configured bucket: $bucket" || \
  { log "Failed to create bucket: $bucket" ; return 1; }
}

create_queue(){
  local queue=$1
  log "Creating queue: $queue"

  aws sqs create-queue --queue-name "$queue" --endpoint-url "$SQS_ENDPOINT" && \
  log "Created queue: $queue" || \
  { log "Failed to create queue: $queue"; return 1; }
}

create_buckets(){
  log "### Initializing S3 Buckets ###"
  local pids=()

  for bucket in "${S3_BUCKETS[@]}"; do
    if ! aws s3api head-bucket --bucket "$bucket" ; then
        create_bucket $bucket &
        pids+=($!)
    else
      log "Bucket already exists: $bucket"
    fi
  done

  for pid in "${pids[@]}"; do
    wait $pid || { log "Failed to create bucket"; return 1; }
  done

  log "S3 Initialization Succeeded"
}

create_queues(){
  log "### Initializing SQS Queues ###"
  local pids=()

  for queue in "${SQS_QUEUES[@]}"; do
    create_queue $queue &
    pids+=($!)
  done

  for pid in "${pids[@]}"; do
    wait $pid || { log "Failed to create queue"; return 1; }
  done

  log "SQS Initialization Succeeded"
}

initialize_dynamo() {
  log "Initializing DynamoDB tables"
  local pids=()

  for entry in "${DYNAMODB_TABLES[@]}"; do
    IFS=: read -r table_name pk <<< "$entry"
    create_table "$table_name" "$pk" &
    pids+=($!)
  done

  for pid in "${pids[@]}"; do
    wait $pid || { log "Failed to create DynamoDB table"; return 1; }
  done

  log "DynamoDB Initialization Succeeded"
}

initialize_sm(){
  log "### Initializing SecretsManager ###"
  aws secretsmanager create-secret --name "Pn-SS-SignAndTimemark" --endpoint-url $DYNAMODB_ENDPOINT --region "$AWS_REGION"  --secret-string '{
      "aruba.sign.delegated.domain": "demoprod",
      "aruba.sign.delegated.password": "password11",
      "aruba.sign.delegated.user": "delegato",
      "aruba.sign.otp.pwd": "dsign",
      "aruba.sign.type.otp.auth": "demoprod",
      "aruba.sign.user": "titolare_aut",
      "aruba.timemark.user": "user1",
      "aruba.timemark.password": "password1"
    }' && \
  log "SecretsManager: Created secret Pn-SS-SignAndTimemark" || \
  { log "Failed to create secret: Pn-SS-SignAndTimemark"; return 1; }
}

initialize_ssm(){
  log "### Initializing SSM Parameters ###"
    indexingConfig=$SSM_CONFIG
    aws ssm put-parameter --name "Pn-SS-IndexingConfiguration" --type "String" --value "$indexingConfig" --endpoint-url $SSM_ENDPOINT && \
    log "Created SSM parameter: Pn-SS-IndexingConfiguration" || \
    { log "Failed to create SSM parameter: Pn-SS-IndexingConfiguration"; return 1; }
}

## Main ##

main(){
  create_buckets && \
  create_queues && \
  initialize_dynamo && \
  initialize_sm && \
  initialize_ssm && \
  log "LocalStack Initialization Succeeded" || \
  log "LocalStack Initialization Failed"
}

## Execution ##
main
echo "Initialization complete"

end_time=$(date +%s)
log "### LocalStack Initialization Time: $((end_time - start_time)) seconds ###"
