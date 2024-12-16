#!/bin/bash

aws_profile=""
aws_region="eu-south-1"
endpoint_url="http://localhost:4566"

while getopts 't:i:p:r:e:' opt ; do
  case "$opt" in 
    p)
      aws_profile=${OPTARG}
    ;;
    r)
      aws_region=${OPTARG}
    ;;
    t)
      tableName=${OPTARG}
    ;;
    i)
      inputFileName=${OPTARG}
    ;;
    e)
      endpoint_url=${OPTARG}
    ;;
    :)
      echo -e "option requires an argument.\nUsage: $(basename $0) [-a] [-b] [-c arg]"
      exit 1
    ;;
    ?|h)
      echo "Usage: $(basename $0) -t <table_name> -i <input_file_name> [-p <aws_profile>] [-r <aws_region>]"
      exit 1
    ;;
  esac
done

if [[ ! $tableName || ! $inputFileName ]] ; then
  echo "both -t and -i parameters are mandatory"
  exit 1
fi

if [[ ! -f ${inputFileName} ]] ; then
  echo "invalid input file name ${inputFileName}"
  exit 1
fi

aws_command_base_args=""
if ( [ ! -z "${aws_profile}" ] ) then
  echo "using profile ${aws_profile}"
  aws_command_base_args="${aws_command_base_args} --profile $aws_profile"
fi
if ( [ ! -z "${aws_region}" ] ) then
  echo "using region ${aws_region}"
  aws_command_base_args="${aws_command_base_args} --region  $aws_region"
fi
if ( [ ! -z "${endpoint_url}" ] ) then
  aws_command_base_args="${aws_command_base_args} --endpoint-url $endpoint_url"
fi

numOfLines=$(($(cat ${inputFileName} | wc -l)))
lineNum=0
while read line ;
do
  lineNum=$((lineNum + 1))
  printf "\r%3d%%" $((lineNum * 100 / numOfLines))

  if ! aws ${aws_command_base_args} dynamodb put-item --table-name "${tableName}" --item "${line}" > /dev/null 2>&1; then
    echo -e "\nFailed to put item at line ${lineNum}: ${line}"
    exit 1
  fi
done < "${inputFileName}"

echo -e "\nAll items processed successfully."
