"use strict";
          const AWS = require('aws-sdk');
          const uuid = process.env.PnSsGestoreBucketTriggerId;
          const lambdaArn = process.env.PnSsGestoreBucketLambdaArn;
          const lambdaArr = lambdaArn.split('function/')
          var lambdaName = lambdaArr[1]
          console.log('Lambda function named "', lambdaName, '" with arn "', lambdaArn ,'" and triggerID = "', uuid, '" will be updated')
          const lambda = new AWS.Lambda();

          exports.handleEvent = async function (event) {
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
            await new Promise(function (resolve, reject){
              lambda.putFunctionConcurrency(lambda_params, (res) => {
                console.log("Changed lambda concurrency to ", concurrency);
                resolve();
              }).on("error", (e) => {
                console.log("* FATAL * An error occurred while calling putFunctionConcurrency of function ", lambdaName);
                reject(Error(e));
              });
            });

            // Modifica l'EventSourceMapping della funzione Lambda
            await new Promise(function (resolve, reject){
              lambda.updateEventSourceMapping(event_source_mapping_params, (res) => {
                console.log("Updated Lambda Event Source Mapping with the following params: ", JSON.stringify(event_source_mapping_params));
                resolve();
              }).on("error", (e) => {
                console.log("* FATAL * An error occurred while calling updateEventSourceMapping for trigger ", uuid);
                reject(Error(e));
              });
            });

            return {
              statusCode: 200,
              body: 'Concurrency and EventSourceMapping successfully modified! '
            };
          }