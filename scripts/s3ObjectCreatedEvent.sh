#!/bin/bash

if [ $# -ne 5 ] ; then
  echo at least 5 parameters
  exit 1
fi

account=${1}
region=${2}
bucket=${3}
key=${4}
size=${5}

echo -e "{\
  \"version\": \"0\",\
  \"id\": \"4f28d4a3-0009-f677-1642-b929a6582c09\",\
  \"detailType\": \"Object Created\",\
  \"source\": \"aws.s3\",\
  \"account\": \"${account}\",\
  \"time\": \"$(date -Iseconds)\",\
  \"region\": \"${region}\",\
  \"resources\": [\
    \"arn:aws:s3:::${bucket}\"\
  ],\
  \"detail\": {\
    \"version\": \"0\",\
    \"bucket\": {\
      \"name\": \"${bucket}\"\
    },\
    \"object\": {\
      \"key\": \"${key}\",\
      \"size\": ${size},\
      \"etag\": \"8a3cb9dcb6053970b11dc7e839b7dc40\",\
      \"versionId\": null,\
      \"sequencer\": \"00653A5CF2327EFAAE\"\
    },\
    \"requestId\": \"76HSG9196FSRFJ41\",\
    \"requester\": \"089813480515\",\
    \"sourceIpAddress\": \"88.41.249.137\",\
    \"reason\": \"PutObject\"\
  }\
}"
  
