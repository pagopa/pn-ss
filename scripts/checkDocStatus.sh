#!/bin/bash

region=eu-south-1
account=""

while getopts 'a:i:r:p:' opt ; do
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
    p)
      profile=${OPTARG}
    ;;
    :)
      >&2 echo -e "option requires an argument.\nUsage: $(basename $0) -i <input_file_name> -p <aws_profile> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
    ?|h)
      >&2 echo "Usage: $(basename $0) -i <input_file_name> -p <aws_profile> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $inputFileName ]] ; then
  >&2 echo "-i parameter is mandatory"
  exit 1
fi

if [[ ! $profile ]] ; then
  >&2 echo "-p parameter is mandatory"
  exit 1
fi

if [[ ! $account ]] ; then
  >&2 echo "-a parameter is mandatory"
  exit 1
fi

if [[ ! -f ${inputFileName} ]] ; then
  <&2 echo "invalid input file name ${inputFileName}"
  exit 1
fi

echo "{ \"#K\": \"documentKey\",\"#S\": \"documentState\"}" > projection.json
          
>&2 echo -e "Questa versione esegue gli step seguenti:\n\
  1. per i soli eventi di creazione 'ObjectCreated:Put' di oggetti nel bucket hot\n\
     reperisce e stampa lo stato dell'oggetto\n\
Se non sono specificati i parametri opzionali vengono usati i parametri dell'ambiente Dev"
  

jq -r '.[] | .eventName + " " + .s3.bucket.name + " " + .s3.object.key' ${inputFileName} \
| while read -r eventName bucketName objectKey ; do
  if [ ${eventName} != "ObjectCreated:Put" ] ; then
    echo "[ERROR] ${objectKey} - event ${eventName} not recognized"
    continue
  fi
  if [ ${bucketName} != "pn-safestorage-${region}-${account}" ] ; then
    echo "[ERROR] ${objectKey} - bucket ${bucketName} not recognized"
    continue
  fi
  
  response=$(aws --profile ${profile} --region ${region} dynamodb get-item --table-name pn-SsDocumenti --key "{\"documentKey\": {\"S\": \"${objectKey}\"}}" --projection-expression "#K, #S" --expression-attribute-names file://projection.json)
  documentKey=$(echo ${response} | jq -r '.Item.documentKey.S')
  if [ "x${documentKey}" == "x" ] ; then
    echo "[ERROR] ${objectKey} - documentKey not found"
    continue
  fi
  if [ "${documentKey}" != "${objectKey}" ] ; then
    echo "[ERROR] ${objectKey} - documentKey mismatch ${documentKey}"
    continue
  fi
  echo "[INFO] ${objectKey} documentStatus: $(echo ${response} | jq -r '.Item.documentState.S')"
done
