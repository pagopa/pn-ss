"use strict";
const uuid = process.env.PnSsGestoreBucketTriggerId;
const { LambdaClient, PutFunctionConcurrencyCommand, UpdateEventSourceMappingCommand} = require('@aws-sdk/client-lambda');
const lambdaArn = process.env.PnSsGestoreBucketLambdaArn;
var lambdaName = lambdaArn.split('function:')[1]
console.log('Lambda function named "', lambdaName, '" with arn "', lambdaArn, '" and triggerID = "', uuid, '" will be updated.')

const lambda = new LambdaClient();

exports.handleEvent = async function (event) {

  try {
    
    console.log(JSON.stringify(event));
    const newCount = event.detail.responseElements.service.desiredCount;
    const oldCount = event.detail.responseElements.service.runningCount;
    
    if (newCount == oldCount){
      console.log("The Safe Storage Fargate service is being updated but the number of tasks is not changed.")
      return {statusCode: 200,  body: 'Reserved concurrency and EventSourceMapping concurrency WERE NOT modified!'}
    }
    console.log("Safe Storage ECS Task desired count changed from ", oldCount, " to ", newCount);

    const instancesNumberPerTask = process.env.PnSsGestoreBucketInstancesPerTaskInstance;
    const concurrency = newCount * instancesNumberPerTask;
    
    console.log("New function reserved concurrency: ", concurrency);

    // Setting lambda reserved concurrency params
    const input = {
      FunctionName: lambdaArn,
      ReservedConcurrentExecutions: concurrency
    };

    const putCommand = new PutFunctionConcurrencyCommand(input);

    // Setting trigger concurrency params
    const event_source_mapping_params = {
      FunctionName: lambdaArn,
      UUID: uuid,
      ScalingConfig: {
        MaximumConcurrency: concurrency
      }
    };

    const eventSourceMappingCommand = new UpdateEventSourceMappingCommand(event_source_mapping_params);

    await lambda.send(putCommand)
        .catch((err) => {
          console.error("* FATAL * An error occurred while calling putFunctionConcurrency: ", err);
          throw new Error('Error during Concurrency updating : ' + err)
        })
        .then((data) => {
          console.log(JSON.stringify(data));
          console.log("Changed function reserved concurrency to ", concurrency);
        });
        
    await lambda.send(eventSourceMappingCommand)
        .catch((err) => {
          console.error("* FATAL * An error occurred while calling updateEventSourceMapping with uuid ", uuid, ": ", err);
          throw new Error('Error during Concurrency updating : ' + err)
        })
        .then((data) => {
          console.log(JSON.stringify(data));
          console.log("Updated Lambda Event Source Mapping with the following params: ", JSON.stringify(event_source_mapping_params));
        });


    return {statusCode: 200,  body: 'Concurrency and EventSourceMapping successfully modified! '}
  }
  catch(err)
    {
      return {statusCode: 500, body: 'Error during Concurrency updating : ' + err}
    }
}