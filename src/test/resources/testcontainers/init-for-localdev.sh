#!/bin/bash
COMMIT_ID=${1:-develop}
set -euo pipefail

# Configurazione
VERBOSE=false
LOCALSTACK_ENDPOINT=http://localhost:4566
REGION=us-east-1
AWS_PROFILE="default"
PROFILE=local
ACCESS_KEY=TEST
SECRET_KEY=TEST

# Logging
log() { echo "[pn-ss-init][$(date +'%Y-%m-%d %H:%M:%S')] $*"; }
silent() { "$@" > /dev/null 2>&1 || true; }

# Verifica LocalStack
verify_localstack() {
  curl -fs "$LOCALSTACK_ENDPOINT" > /dev/null || {
    log "LocalStack non è attivo"; exit 1;
  }
}

# Deploy lambdas
deploy_lambdas() {
  log "Deploying lambdas"
  bash <(curl -s "https://raw.githubusercontent.com/pagopa/pn-ss/$COMMIT_ID/scripts/lambdas_deploy.sh") $COMMIT_ID || {
    log "Failed to deploy lambdas"
    exit 1
  }
}

# Popolamento DynamoDB
populate_table() {
  local table=$1 url=$2
  log "Populating $table"
  tmpfile=$(mktemp)
  curl -sL "$url" > "$tmpfile"
  curl -sL "https://raw.githubusercontent.com/pagopa/pn-ss/$COMMIT_ID/scripts/dynamoDBLoad.sh" | \
    bash -s -- -t "$table" -i "$tmpfile" -r "$REGION" -e "$LOCALSTACK_ENDPOINT" -j 20 || \
    log "Failed to populate $table"
}

load_dynamodb() {
  log "Populating DynamoDB"
  local base="https://raw.githubusercontent.com/pagopa/pn-ss/$COMMIT_ID/scripts"
  populate_table "pn-SsAnagraficaClient" "$base/localdev/AnagraficaClient.json"
  populate_table "pn-SsTipologieDocumenti" "$base/localdev/TipoDocumenti.json"
  populate_table "pn-SmStates" "$base/StateMachine.json"
}

# Inizializzazione
execute_init() {
  log "Executing init script"
  bash <(curl -s "https://raw.githubusercontent.com/pagopa/pn-ss/$COMMIT_ID/src/test/resources/testcontainers/init.sh")
}

# Main
main() {
  log "Starting pn-ss localdev configuration."
  local start=$(date +%s)

  verify_localstack
  execute_init
  load_dynamodb
  deploy_lambdas

  local duration=$(( $(date +%s) - start ))
  log "init-for-localdev.sh executed in ${duration}s"
}

main
