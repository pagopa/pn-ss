#!/bin/bash


VERBOSE=false
LOCALSTACK_ENDPOINT=http://localhost:4566
REGION=us-east-1
AWS_PROFILE="default"
PROFILE=local
ACCESS_KEY=TEST
SECRET_KEY=TEST
curr_dir=$(pwd)
cli_pager=$(aws configure get cli_pager)


## LOGGING FUNCTIONS ##
log() { echo "[pn-ss-init-for-localdev][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

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

verify_localstack() {
  if ! curl -s $LOCALSTACK_ENDPOINT > /dev/null; then
    log "### Localstack is not running ###"
    exit 1
  fi
}

deploy_lambdas() {
  log "### Deploying lambdas ###"
  bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-ss/develop/scripts/lambdas_deploy.sh)

}

load_dynamodb(){
  log "### Populating DynamoDB ###"

  local BASE_REPO="https://raw.githubusercontent.com/pagopa/pn-ss/develop"
  local DYNAMO_SCRIPT_URL="$BASE_REPO/scripts/dynamoDBLoad.sh"
  local ANAGRAFICA_URL="$BASE_REPO/scripts/localdev/AnagraficaClient.json"
  local TIPO_DOC_URL="$BASE_REPO/scripts/localdev/TipoDocumenti.json"
  local STATE_MACHINE_URL="$BASE_REPO/scripts/StateMachine.json"

  log "### Populating pn-SsAnagraficaClient ###" && \
  curl -sL "$DYNAMO_SCRIPT_URL" | bash -s -- \
    -t "pn-SsAnagraficaClient" \
    -i <(curl -sL "$ANAGRAFICA_URL") \
    -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SsAnagraficaClient ###"; return 1; }

  log "### Populating pn-SsTipologieDocumenti ###" && \
  curl -sL "$DYNAMO_SCRIPT_URL" | bash -s -- \
    -t "pn-SsTipologieDocumenti" \
    -i <(curl -sL "$TIPO_DOC_URL") \
    -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SsTipologieDocumenti ###"; return 1; }

  log "### Populating pn-SmStates ###" && \
  curl -sL "$DYNAMO_SCRIPT_URL" | bash -s -- \
    -t "pn-SmStates" \
    -i <(curl -sL "$STATE_MACHINE_URL") \
    -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SmStates ###"; return 1; }
}

execute_init(){
  log "### Try to execute init.sh ###"
  bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-ss/develop/src/test/resources/testcontainers/init.sh)
}

build_run(){
  local curr_dir=$(pwd)
  cd ..
  if ! ( ./mvnw -Dspring-boot.run.jvmArguments=" -Djava.net.preferIPv4Stack=true -Dspring.profiles.active=$PROFILE -Daws.accessKeyId=$ACCESS_KEY -Daws.secretAccessKey=$SECRET_KEY -Daws.region=us-east-1 -Daws.profile=default" spring-boot:run ); then
  log "### Initialization failed ###"
  return 1
  fi
  cd "$curr_dir" || return 1
}

main(){
  log "### Starting pn-ss ###"
  aws configure set cli_pager ""
  local start_time=$(date +%s)
  verify_localstack && \
  execute_init && \
  load_dynamodb && \
  deploy_lambdas && \
  build_run || \
  { log "### Failed to start pn-ss ###"; exit 1; }
  log "### pn-ss started ###"
  local end_time=$(date +%s)
  log "### Time taken: $((end_time - start_time)) seconds ###"
  aws configure set cli_pager "$cli_pager"
}


main