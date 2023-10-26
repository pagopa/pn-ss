#!/bin/bash

region=eu-south-1
account=089813480515

while getopts 'a:b:r:p:d:' opt ; do
  case "$opt" in 
    r)
      region=${OPTARG}
    ;;
    a)
      account=${OPTARG}
    ;;
    b)
      bucketName=${OPTARG}
    ;;
    p)
      profile=${OPTARG}
    ;;
    d)
      olderThan=${OPTARG}
    ;;
    :)
      >&2 echo -e "option requires an argument.\nUsage: $(basename $0) -b <bucket_name> -p <aws_profile> -d <older than n hours> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
    ?|h)
      >&2 echo "Usage: $(basename $0) -b <bucket_name> -p <aws_profile> -d <older than n hours> [-a <aws_account>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $bucketName ]] ; then
  >&2 echo "-b parameter is mandatory"
  exit 1
fi

if [[ ! $profile ]] ; then
  >&2 echo "-p parameter is mandatory"
  exit 1
fi

if [[ ! $olderThan ]] ; then
  >&2 echo "-d parameter is mandatory"
  exit 1
fi

currentTS=$(date +%s)
aws s3 ls ${bucketName} |
while read -r objDate objTime objSize objKey ; do
  objTimestamp=$(date -d "${objDate} ${objTime}" +"%s")
  delta=$((currentTS - objTimestamp))
  deltaDays=$((delta/3600))
  if [ $deltaDays -gt $olderThan ] ; then
    echo ${objKey}
  fi
done
