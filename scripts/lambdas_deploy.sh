#!/bin/bash
COMMIT_ID=${1:-develop}

VERBOSE=false
LAMBDA_FUNCTIONS_FOLDER=functions
AWS_REGION=us-east-1
LOCALSTACK_ENDPOINT="http://localhost:4566"
TMP_PATH=$(mktemp -d)
NODE_RUNTIME="nodejs20.x"
lambdas=()

QUEUE_LAMBDA_SOURCES=( "pn-ss-main-bucket-events-queue:gestoreBucketEventHandler" )



## LOGGING FUNCTIONS ##
log() { echo "[pn-ss-lambda][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

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

## LAMBDA FUNCTIONS ##

verify_fun_directory(){
  local fun=$1
  local expected=("index.js" "package.json")
  local fun_path="$LAMBDA_FUNCTIONS_FOLDER/$fun"
  for file in "${expected[@]}"; do
    if [ ! -f "$fun_path/$file" ]; then
      log "Error: $fun_path/$file not found, not a valid Lambda"
      lambdas=("${lambdas[@]/$fun}")
      return 1
    fi
  done
}

# Copia le lambdas in una cartella temporanea, clonando parte del repository di pn-ss.
copy_lambdas_to_tmp() {
  log "Copying lambdas"
  chmod -R 755 "$TMP_PATH"

  local repo_url="https://github.com/pagopa/pn-ss.git"
  log "Cloning repo into $TMP_PATH"
  git clone "$repo_url" "$TMP_PATH" || { log "Clone failed"; exit 1; }
  cd "$TMP_PATH" || { log "Cannot enter $TMP_PATH"; exit 1; }

  log "Enabling sparse checkout for $LAMBDA_FUNCTIONS_FOLDER"
  git sparse-checkout init --cone && \
  git sparse-checkout set "$LAMBDA_FUNCTIONS_FOLDER" && \
  git checkout "$COMMIT_ID" || { log "Checkout failed for $COMMIT_ID"; exit 1; }
}


get_functions(){
  log "Getting Lambda Functions from $LAMBDA_FUNCTIONS_FOLDER"
  for fun in "$LAMBDA_FUNCTIONS_FOLDER"/*; do
    if [ -d "$fun" ]; then
      fun_name=$(basename "$fun")
      if verify_fun_directory "$fun_name"; then
        lambdas+=("$fun_name")
      fi
      log "Found Lambda $fun_name, added to deploy list"
    fi
  done
}

install_dependencies() {
  local fun=$1
  log "Installing npm dependencies for $fun at $LAMBDA_FUNCTIONS_FOLDER/$fun"
  local fun_path="$LAMBDA_FUNCTIONS_FOLDER/$fun"
  curr_dir=$(pwd)

  if [ ! -d "$fun_path/node_modules" ]; then
    cd "$fun_path" || { log "Error accessing directory $fun_path"; return 1; }
    log "Installing dependencies for $fun in $(pwd)"
    if ! ( silent npm install ); then
      log "Error installing dependencies"
      return 1
    fi
  else
    log "Dependencies already installed"
  fi
  cd $curr_dir || { log "Error accessing directory $fun_path"; return 1; }

}

zip_lambda() {
  local fun=$1
  local fun_path="$TMP_PATH/$LAMBDA_FUNCTIONS_FOLDER/$fun"
  local zip_path="$TMP_PATH/$LAMBDA_FUNCTIONS_FOLDER/$fun.zip"

  log "Changing permissions for $fun_path"

  if ! (silent chmod -R 755 "$fun_path"); then
    log "Error changing permissions for $fun_path"
    return 1
  fi

  log "Zipping Lambda $fun in $fun_path"

  curr_dir=$(pwd)
  cd "$fun_path" || { log "Error accessing directory $fun_path"; return 1; }

  if ! (silent zip -r ../"$fun.zip" ./*); then
    log "Error zipping Lambda $fun"
    cd "$curr_dir" || { log "Error accessing directory $fun_path"; return 1; }
    return 1
  fi

  cd "$curr_dir" ||  { log "Error accessing directory $fun_path"; return 1; }
  zips["$fun"]="$zip_path"
  log "Lambda $fun zipped at $zip_path"
}

deploy_lambda(){
  local fun=$1
  local zip=${zips[$fun]}
  local fun_name=$(basename "$zip" .zip)
  log "Deploying  Lambda $fun_name"

  if (aws lambda get-function --function-name "$fun_name" --endpoint-url "$LOCALSTACK_ENDPOINT" --region "$AWS_REGION" > /dev/null 2>&1); then
    log "Lambda $fun_name already exists"
    return 0
  fi
  silent aws lambda create-function \
        --function-name "$fun_name" \
        --runtime $NODE_RUNTIME \
        --handler index.handler \
        --role "arn:aws:iam::111122223333:role/service-role/$fun_name" \
        --timeout 660 \
        --zip-file fileb://"$zip" \
        --endpoint-url "$LOCALSTACK_ENDPOINT" \
        --region "$AWS_REGION" || \
        { log "Error creating Lambda $fun"; return 1; }
}

full_lambda_deploy(){
  local fun=$1
  install_dependencies $fun && \
  zip_lambda $fun && \
  deploy_lambda $fun || \
  { log "Error deploying Lambda $fun"; return 1;}
}

deploy_lambdas() {
  get_functions
  log "### Deploying Lambdas ###"
  local pids=()
  for fun in "${lambdas[@]}"; do
    log "Deploying Lambda $fun"
    full_lambda_deploy $fun & \
    pids+=($!)
  done

  wait_for_pids pids "Failed to deploy Lambdas"
  return "$?"
}

configure_lambdas() {
  log "Configuring Lambdas"

  for entry in "${QUEUE_LAMBDA_SOURCES[@]}"; do
    IFS=':' read -r queue lambda <<< "$entry"
    log "Configuring Lambda $lambda to listen to queue $queue"

    log "Waiting for Lambda $lambda to be ready"

    silent aws lambda wait function-active --function-name "$lambda" \
                                       --region "$AWS_REGION" \
                                       --endpoint-url "$LOCALSTACK_ENDPOINT" || {
      log "Error waiting for Lambda $lambda to be ready";
      return 1;
    }


    silent aws lambda create-event-source-mapping \
      --function-name "$lambda" \
      --event-source-arn "arn:aws:sqs:$AWS_REGION:000000000000:$queue" \
      --batch-size 10 \
      --endpoint-url "$LOCALSTACK_ENDPOINT" \
      --region "$AWS_REGION" || {
        log "Error configuring Lambda $lambda";
        return 1;
      }
  done

  for lambda in "${lambdas[@]}"; do
     local env_string=""
     env_file="$TMP_PATH/$LAMBDA_FUNCTIONS_FOLDER/$lambda/localdev.env"
      if [ -f $env_file ]; then
        log "#### Found $env_file for Lambda $lambda"
        declare -A env_vars
        while IFS='=' read -r key value || [[ -n "$key" ]]; do
          if [[ -n "$key" && -n "$value" ]]; then
            env_vars["$key"]="$value"
          fi
        done < "$env_file"

        env_string=$(for key in "${!env_vars[@]}"; do printf '%s=%s,' "$key" "${env_vars[$key]}"; done | sed 's/,$//')
      else
        log "#### No $env_file found for Lambda $lambda"
        exit 1
      fi

    log "Configuring environment variables for Lambda $lambda"


    aws lambda update-function-configuration \
      --function-name "$lambda" \
      --environment "Variables={$env_string}" \
      --endpoint-url "$LOCALSTACK_ENDPOINT" \
      --region "$AWS_REGION" || {
        log "Error configuring environment variables for Lambda $lambda";
        return 1;
      }
  done

  log "Lambdas configured successfully"
}

cleanup() {
  log "Cleaning up"
  rm -rf "$TMP_PATH"
  log "Cleanup complete"
}

main() {
  local start_time=$(date +%s)
  exit_code=0
  copy_lambdas_to_tmp && \
  deploy_lambdas && \
  configure_lambdas && \
  log "Lambdas deployed successfully" || \
  { log "Error deploying Lambdas"; exit_code=1; }
  cleanup &
  local end_time=$(date +%s)
  log "Lambdas deploy Execution time: $((end_time - start_time)) seconds"
  return "$exit_code"
}

main