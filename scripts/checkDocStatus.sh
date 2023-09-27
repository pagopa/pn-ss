#!/bin/bash

region=eu-south-1
account=089813480515

while getopts 'a:i:r:' opt ; do
  case "$opt" in 
    r)
      region=${OPTARG}
    ;;
    a)
      account=${OPTARG}
    ;;
    i)
      inputFileName=${OPTARG}
    ;;
    :)
      >&2 echo -e "option requires an argument.\nUsage: $(basename $0) -i <input_file_name> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
    ?|h)
      >&2 echo "Usage: $(basename $0) -i <input_file_name> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $inputFileName ]] ; then
  >&2 echo "-i parameter is mandatory"
  exit 1
fi

if [[ ! -f ${inputFileName} ]] ; then
  <&2 echo "invalid input file name ${inputFileName}"
  exit 1
fi

case "${region}-${account}" in
  "eu-south-1-089813480515")
    balancer=internal-EcsA-20230406081307135800000004-20484703.eu-south-1.elb.amazonaws.com
  ;;
  *)
    >&2 echo "balancer not defined"
  ;;
esac

>&2 echo -e "Questa versione esegue gli step seguenti:\n\
  1. per i soli eventi di creazione 'ObjectCreated:Put' di oggetti nel bucket hot\n\
     reperisce e stampa lo stato dell'oggetto\n\
Se non sono specificati i parametri opzionali vengono usati i parametri dell'ambiente Dev"
  

jq -r '.[] | .eventName + " " + .s3.bucket.name + " " + .s3.object.key' ${inputFileName} \
| while read -r eventName bucketName objectKey ; do
  if [ ${eventName} != "ObjectCreated:Put" ] ; then
    echo "${objectKey} not verified; event ${eventName}"
    continue
  fi
  if [ ${bucketName} != "pn-safestorage-${region}-${account}" ] ; then
    echo "${objectKey} not verified; bucket ${bucketName}"
    continue
  fi
  
  response=$(curl -s -H "x-pagopa-safestorage-cx-id: internal" http://${balancer}:8080/safe-storage/v1/files/${objectKey}?metadataOnly=true)
  rc=$?
  if [ $rc -eq 0 ] ; then
    httpStatus=$(echo ${response} | jq '.status')
    if [ "x${httpStatus}" == "xnull" ] ; then
      echo ${objectKey} documentStatus: $(echo ${response} | jq '.documentStatus')
    else
      echo ${objectKey} HTTP status: ${httpStatus}
    fi
  else
    echo ${objectKey} response error: curl returns ${rc}
  fi
done
