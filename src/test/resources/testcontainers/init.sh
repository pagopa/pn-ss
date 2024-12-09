#!/bin/bash

set -e

start_time=$(date +%s)

## CONFIGURATION ##
VERBOSE=true
AWS_REGION="eu-central-1"
LOCALSTACK_ENDPOINT="http://localhost:4566"
SQS_ENDPOINT=$LOCALSTACK_ENDPOINT
DYNAMODB_ENDPOINT=$LOCALSTACK_ENDPOINT
S3_ENDPOINT=$LOCALSTACK_ENDPOINT
SSM_ENDPOINT=$LOCALSTACK_ENDPOINT
LAMBDA_FUNCTIONS_PATH="/tmp/pn-ss/lambda_import" # LAMBDA
TMP_PATH="/tmp/pn-ss/lambda_functions" # LAMBDA

## DEFINITIONS ##
S3_BUCKETS=("pn-ss-storage-safestorage" "pn-ss-storage-safestorage-staging")
SQS_QUEUES=("dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs" "Pn-Ss-Availability-Queue")

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

# VARIABLES #
declare -A zips
lambdas=()


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
        silent create_bucket $bucket &
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
  silent aws secretsmanager create-secret --name "Pn-SS-SignAndTimemark" --endpoint-url $DYNAMODB_ENDPOINT --region "$AWS_REGION"  --secret-string '{
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
    silent aws ssm put-parameter --name "Pn-SS-IndexingConfiguration" --type "String" --value "$indexingConfig" --endpoint-url $SSM_ENDPOINT && \
    log "Created SSM parameter: Pn-SS-IndexingConfiguration" || \
    { log "Failed to create SSM parameter: Pn-SS-IndexingConfiguration"; return 1; }
}

# lambdas-FUNCTIONS #

verify_fun_directory(){
  local fun=$1
  local expected=("index.js" "package.json")
  local fun_path="$LAMBDA_FUNCTIONS_PATH/$fun"
  for file in "${expected[@]}"; do
    if [ ! -f "$fun_path/$file" ]; then
      log "Error: $fun_path/$file not found, not a valid Lambda"
      return 1
    fi
  done
}

get_functions(){
  log "Getting Lambda Functions from $LAMBDA_FUNCTIONS_PATH"
  for fun in "$LAMBDA_FUNCTIONS_PATH"/*; do
    if [ -d "$fun" ]; then
      fun_name=$(basename "$fun")
      if ! verify_fun_directory "$fun_name"; then
        return 1
      fi
      lambdas+=("$fun_name")
      log "Found Lambda $fun_name, added to deploy list"
    fi
  done
}

copy_directory_to_tmp() {
  local fun=$1
  log "Copying Directory $LAMBDA_FUNCTIONS_PATH/$fun to $TMP_PATH"
  mkdir -p "$TMP_PATH"
  if ! ( silent cp -r "$LAMBDA_FUNCTIONS_PATH/$fun" "$TMP_PATH" ); then
    log "Error copying directory"
    return 1
  fi
}

install_dependencies() {
  local fun=$1
  log "Installing npm dependencies for $fun"
  local fun_path="$TMP_PATH/$fun"

  if [ ! -d "$fun_path/node_modules" ]; then
    if ! (silent npm install --prefix "$fun_path"); then
      log "Error installing dependencies"
      return 1
    fi
  else
    log "Dependencies already installed"
  fi
}

zip_lambda() {
  local fun=$1
  log "Zipping Lambda $fun"
  local fun_path="$TMP_PATH/$fun"
  local zip_path="$TMP_PATH/$fun.zip"

  if ! (silent zip -r "$zip_path" "$fun_path"); then
    log "Error zipping Lambda $fun"
    return 1
  fi

  zips["$fun"]="$zip_path"
  log "Lambda $fun zipped at $zip_path"
}

deploy_lambda(){
  local fun=$1
  local zip=${zips[$fun]}
  local fun_name=$(basename "$zip" .zip)
  log "Deploying  Lambda $fun_name"

  silent aws lambda create-function \
        --function-name "$fun_name" \
        --runtime nodejs14.x \
        --handler index.handler \
        --role "arn:aws:iam::111122223333:role/service-role/$fun_name" \
        --zip-file fileb://"$zip" \
        --endpoint-url "$LOCALSTACK_ENDPOINT" \
        --region "$AWS_REGION" || \
        { log "Error creating Lambda $fun"; return 1; }
}

full_lambda_deploy(){
  local fun=$1
  copy_directory_to_tmp $fun && \
  install_dependencies $fun && \
  zip_lambda $fun && \
  deploy_lambda $fun || \
  { log "Error deploying Lambda $fun"; return 1;}
}

deploy_lambdas() {
  get_functions
  log "Deploying Lambdas"
  local pids=()
  for fun in "${lambdas[@]}"; do
    log "Deploying Lambda $fun"
    full_lambda_deploy $fun & \
    pids+=($!)
  done

  wait_for_pids pids "Failed to deploy Lambdas"
  return "$?"
}

cleanup() {
  log "Cleaning up"
  silent rm -rf "$TMP_PATH"
  log "Cleanup complete"
}

## MAIN ##
main(){
  log "### Initializing LocalStack Services ###"

  deploy_lambdas & \
  local lambdas_pid=$!

  ( create_buckets && \
      create_queues && \
      initialize_dynamo && \
      initialize_sm && \
      initialize_ssm ) & \
  local infra_pid=$!

  local exit_code=0

  wait $lambdas_pid || \
  { log "Error deploying Lambdas"; exit_code=1; }

  wait $infra_pid || \
  { log "Error initializing infrastructure"; exit_code=1; }

  local end_time=$(date +%s)

  if [ "$exit_code" -eq 0 ]; then
    log "LocalStack Initialization Succeeded in $(($end_time - $start_time)) seconds"
  else
    log "LocalStack Initialization Failed in $(($end_time - $start_time)) seconds"
  fi
}

## EXECUTION ##
main
echo "Initialization complete"
#cleanup