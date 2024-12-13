VERBOSE=false
LAMBDA_FUNCTIONS_PATH=$1 # LAMBDA
AWS_REGION=$2 # LAMBDA
LOCALSTACK_ENDPOINT="http://localhost:4566"
TMP_PATH="./tmp" # LAMBDA
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

make_tmp_dir() {
  mkdir -p "$TMP_PATH"
}

## LAMBDA FUNCTIONS ##

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
      if verify_fun_directory "$fun_name"; then
        lambdas+=("$fun_name")
      fi
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
    cd "$fun_path"
    log "Installing dependencies for $fun in $fun_path"
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
  local fun_path="$TMP_PATH/$fun"
  local zip_path="$TMP_PATH/$fun.zip"

  log "Zipping Lambda $fun in $fun_path"

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
  if [ -d "$TMP_PATH" ]; then
    log "Removing temporary directory $TMP_PATH"
    silent rm -rfv "$TMP_PATH"
  fi
  log "Cleanup complete"
}

main() {
  local start_time=$(date +%s)
  exit_code=0
  make_tmp_dir && \
  deploy_lambdas && \
  log "Lambdas deployed successfully" || \
  { log "Error deploying Lambdas"; exit_code=1; }
  cleanup
  local end_time=$(date +%s)
  log "Lambdas deploy Execution time: $((end_time - start_time)) seconds"
  return "$exit_code"
}

main