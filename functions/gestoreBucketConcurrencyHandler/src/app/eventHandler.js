"use strict";
const uuid = process.env.PnSsGestoreBucketTriggerId;
const { LambdaClient, PutFunctionConcurrencyCommand, UpdateEventSourceMappingCommand} = require('@aws-sdk/client-lambda');
const lambdaArn = process.env.PnSsGestoreBucketLambdaArn;
const lambdaArr = lambdaArn.split('function:')
var lambdaName = lambdaArr[1]
console.log('Lambda function named "', lambdaName, '" with arn "', lambdaArn, '" and triggerID = "', uuid, '" will be updated')

exports.handleEvent = async function (event) {

  try {
    const lambda = new LambdaClient();
    console.log(JSON.stringify(event));

    const newCapacity = event.detail.responseElements.service.desiredCount;
    const oldCapacity = event.detail.responseElements.service.runningCount;
    console.log("Safe Storage ECS Task capacity changed from ", oldCapacity, " to ", newCapacity);

    const instancesNumberPerTask = process.env.PnSsGestoreBucketInstancesPerTaskInstance;
    const concurrency = newCapacity * instancesNumberPerTask;
    console.log("New concurrency: ", concurrency);

    // Setting lambda concurrency params
    const input = {
      FunctionName: lambdaArn,
      ReservedConcurrentExecutions: concurrency
    };

    const putCommand = new PutFunctionConcurrencyCommand(input);

    // Setting lambda params
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
          console.log("* FATAL * An error occurred while calling putFunctionConcurrency of function ", lambdaName);
          throw new Error('Error during Concurrency updating : ' + err)
        })
        .then((data) => {
          console.log(JSON.stringify(data));
          console.log("Changed lambda concurrency to ", concurrency);
        });
    await lambda.send(eventSourceMappingCommand)
        .catch((err) => {
          console.log("* FATAL * An error occurred while calling updateEventSourceMapping for trigger ", uuid);
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