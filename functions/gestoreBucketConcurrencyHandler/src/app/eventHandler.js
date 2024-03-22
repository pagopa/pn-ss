"use strict";
const AWS = require('aws-sdk');
const uuid = process.env.PnSsGestoreBucketTriggerId;
const lambdaArn = process.env.PnSsGestoreBucketLambdaArn;
const lambdaArr = lambdaArn.split('function/')
var lambdaName = lambdaArr[1]
console.log('Lambda function named "', lambdaName, '" with arn "', lambdaArn, '" and triggerID = "', uuid, '" will be updated')

exports.handleEvent = async function (event) {

  try {
    const lambda = new AWS.Lambda();
    console.log(JSON.stringify(event));

    const newCapacity = event.detail.responseElements.service.desiredCount;
    const oldCapacity = event.detail.responseElements.service.runningCount;
    console.log("Safe Storage ECS Task capacity changed from ", oldCapacity, " to ", newCapacity);

    const instancesNumberPerTask = process.env.PnSsGestoreBucketInstancesPerTaskInstance;
    const concurrency = newCapacity * instancesNumberPerTask;
    console.log("New concurrency: ", concurrency);

    // Setting lambda concurrency params
    const lambda_params = {
      FunctionName: lambdaArn,
      ReservedConcurrentExecutions: concurrency
    };

    // Setting lambda params
    const event_source_mapping_params = {
      FunctionName: lambdaArn,
      UUID: uuid,
      ScalingConfig: {
        MaximumConcurrency: concurrency
      }
    };

    // Modifica la concurrency della funzione Lambda
    await new Promise(function (resolve, reject) {
      lambda.putFunctionConcurrency(lambda_params, (err, res) => {
        if (err) {
          console.log("* FATAL * An error occurred while calling putFunctionConcurrency of function ", lambdaName);
          reject(err);
        }
        else {
          console.log(JSON.stringify(res));
          console.log("Changed lambda concurrency to ", concurrency);
          resolve();
        }
      });
    });

    // Modifica l'EventSourceMapping della funzione Lambda
    await new Promise(function (resolve, reject) {
      lambda.updateEventSourceMapping(event_source_mapping_params, (err, res) => {
        if (err) {
          console.log("* FATAL * An error occurred while calling updateEventSourceMapping for trigger ", uuid);
          reject(err);
        }
        else {
          console.log(JSON.stringify(res));
          console.log("Updated Lambda Event Source Mapping with the following params: ", JSON.stringify(event_source_mapping_params));
          resolve();
        }
      });
    });
  }

  catch (error) {
    return { statusCode: 500, body: 'Error during Concurrency and EventSourceMapping updating : ' + error }
  }

  return {
    statusCode: 200,
    body: 'Concurrency and EventSourceMapping successfully modified! '
  };
}