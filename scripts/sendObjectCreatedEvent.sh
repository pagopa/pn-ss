#!/bin/bash

region=eu-south-1
account=089813480515

while getopts 'a:b:r:p:q:i:' opt ; do
  case "$opt" in 
    r)
      region=${OPTARG}
    ;;
    a)
      account=${OPTARG}
    ;;
    q)
      queueURL=${OPTARG}
    ;;
    p)
      profile=${OPTARG}
    ;;
    b)
      bucket=${OPTARG}
    ;;
    i)
      inputFilename=${OPTARG}
    ;;
    :)
      >&2 echo -e "option requires an argument.\nUsage: $(basename $0) -i <input_filename> -p <aws_profile> -q <SQS queue URL> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
    ?|h)
      >&2 echo "Usage: $(basename $0) -i <input_filename> -p <aws_profile> -q <SQS queue URL> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $inputFilename ]] ; then
  >&2 echo "-i parameter is mandatory"
  exit 1
fi

if [[ ! $profile ]] ; then
  >&2 echo "-p parameter is mandatory"
  exit 1
fi

if [[ ! $queueURL ]] ; then
  >&2 echo "-q parameter is mandatory"
  exit 1
fi

rowNum=$(wc -l ${inputFilename} | cut -f 1 -d ' ')

index=0
cat ${inputFilename} |
while read -r bucket key ; do
  payload="$(./s3ObjectCreatedEvent.sh ${account} ${region} ${bucket} ${key} 1)"
  aws sqs send-message --queue-url "${queueURL}" --message-body "${payload}" > /dev/null
  rc=$?
  if [ $rc -ne 0 ] ; then
    echo "[ERROR] error ${rc} while sending ObjectCreated event - key: ${key}"
    exit 1
  fi
  ((index++))
  echo -ne "$((index*100/rowNum))%\r"
done
echo -ne "\n"
