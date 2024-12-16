#!/bin/bash


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

verify_localstack() {
  if ! curl -s $LOCALSTACK_ENDPOINT > /dev/null; then
    echo "### Localstack is not running ###"
    exit 1
  fi
}

run_init() {
  if ! $INIT_SCRIPT ; then
    echo "### Failed to run init.sh ###"
    return 1
  fi
}

deploy_lambdas() {
  if ! $LAMBDAS_DEPLOY_SCRIPT $FUNCTIONS_DIR $REGION; then
    echo "### Failed to deploy lambdas ###"
    return 1
  fi
}


build_run(){
  local curr_dir=$(pwd)
  cd ..
  if ! ( ./mvnw -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=$PROFILE -Daws.accessKeyId=$ACCESS_KEY -Daws.secretAccessKey=$SECRET_KEY -Daws.region=$REGION" spring-boot:run ); then
  echo "### Initialization failed ###"
  return 1
  fi
  cd "$curr_dir" || return 1
}

init_localstack_env(){
  local exit_code=0

  run_init & \
  local infra_pid=$!

  deploy_lambdas & \
  local lambdas_pid=$!

  wait $infra_pid || exit_code=1
  wait $lambdas_pid || exit_code=1

  return "$exit_code"
}

main(){
  echo "### Starting pn-ss ###"
  aws configure set cli_pager ""
  local start_time=$(date +%s)
  verify_localstack && \
  init_localstack_env && \
  build_run || \
  { echo "### Failed to start pn-ss ###"; exit 1; }
  echo "### pn-ss started ###"
  local end_time=$(date +%s)
  echo "### Time taken: $((end_time - start_time)) seconds ###"
  aws configure set cli_pager "$cli_pager"
}


main