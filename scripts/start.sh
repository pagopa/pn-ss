#!/bin/bash


VERBOSE=false
LOCALSTACK_ENDPOINT=http://localhost:4566
FUNCTIONS_DIR=../functions
REGION=eu-south-1
PROFILE=local
ACCESS_KEY=TEST
SECRET_KEY=TEST
curr_dir=$(pwd)
cli_pager=$(aws configure get cli_pager)

INIT_SCRIPT=../src/test/resources/testcontainers/init.sh
LAMBDAS_DEPLOY_SCRIPT=./lambdas_deploy.sh

## LOGGING FUNCTIONS ##
log() { echo "[pn-ss-start][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }

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

run_init() {
  if ! $INIT_SCRIPT ; then
    log "### Failed to run init.sh ###"
    return 1
  fi
}

deploy_lambdas() {
  if ! $LAMBDAS_DEPLOY_SCRIPT $FUNCTIONS_DIR $REGION; then
    log "### Failed to deploy lambdas ###"
    return 1
  fi
}


build_run(){
  local curr_dir=$(pwd)
  cd ..
  if ! ( ./mvnw -Dspring-boot.run.jvmArguments=" -Djava.net.preferIPv4Stack=true -Dspring.profiles.active=$PROFILE -Daws.accessKeyId=$ACCESS_KEY -Daws.secretAccessKey=$SECRET_KEY -Daws.region=$REGION" spring-boot:run ); then
  log "### Initialization failed ###"
  return 1
  fi
  cd "$curr_dir" || return 1
}

load_dynamodb(){
  log "### Populating DynamoDB ###"
  log "### Populating pn-SsAnagraficaClient ###" && \
  ./dynamoDBLoad.sh -t "pn-SsAnagraficaClient" -i "./AnagraficaClient.json" -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SsAnagraficaClient ###" ; return 1; }

  log "### Populating pn-SsTipologieDocumenti ###" && \
  ./dynamoDBLoad.sh -t "pn-SsTipologieDocumenti" -i "./TipoDocumenti.json"  -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SsTipologieDocumenti ###" ; return 1; }

  log "### Populating pn-SmStates ###" && \
  ./dynamoDBLoad.sh -t "pn-SmStates" -i "./StateMachine.json"  -r "$REGION" -e "$LOCALSTACK_ENDPOINT" || \
  { log "### Failed to populate pn-SmStates ###" ; return 1; }
}

init_localstack_env(){


  ( run_init && load_dynamodb ) && \
  echo "### Localstack environment initialized ###" || \
  { log "### Failed to initialize Localstack environment ###"; return 1; }

  deploy_lambdas && \
  echo "### Lambdas deployed ###" || \
  { log "### Failed to deploy lambdas ###"; return 1; }

}

main(){
  log "### Starting pn-ss ###"
  aws configure set cli_pager ""
  local start_time=$(date +%s)
  verify_localstack && \
  init_localstack_env && \
  build_run || \
  { log "### Failed to start pn-ss ###"; exit 1; }
  log "### pn-ss started ###"
  local end_time=$(date +%s)
  log "### Time taken: $((end_time - start_time)) seconds ###"
  aws configure set cli_pager "$cli_pager"
}


main