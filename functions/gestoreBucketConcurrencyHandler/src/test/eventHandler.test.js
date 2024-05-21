const { mockClient } = require("aws-sdk-client-mock");
const { LambdaClient, PutFunctionConcurrencyCommand,UpdateEventSourceMappingCommand } = require('@aws-sdk/client-lambda');
const proxyquire = require("proxyquire").noPreserveCache();
const { expect } = require("chai");
var originalEnv = process.env;

describe("gestoreBucketConcurrencyHandler tests", function () {

    lambdaMockClient=mockClient(LambdaClient)

    this.beforeEach(() => {
        process.env.PnSsGestoreBucketTriggerId = "uuid";
        process.env.PnSsGestoreBucketLambdaArn = "arn:aws:lambda:region:123456789:function:pn-safe-storage-gestore-bucket-lambda";
        process.env.PnSsGestoreBucketInstancesPerTaskInstance = "10"
    });

    it("test ok", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        var event = createEvent(2, 1);

        lambdaMockClient.on(PutFunctionConcurrencyCommand).resolves({});
        lambdaMockClient.on(UpdateEventSourceMappingCommand).resolves({});

        const res = await lambda.handleEvent(event);
        expect(res).deep.equals({
            statusCode: 200,
            body: 'Concurrency and EventSourceMapping successfully modified! '
        });    })

    it("test putFunctionConcurrency ko", async () => {

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        var event = createEvent(2, 1);
        lambdaMockClient.on(PutFunctionConcurrencyCommand).rejects("Errore!");
        lambdaMockClient.on(UpdateEventSourceMappingCommand).resolves({});

        const res = await lambda.handleEvent(event);
        expect(res.statusCode).deep.equals(500);
    })

    it("test putFunctionConcurrency ko", async () => {

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        var event = createEvent(2, 1);
        lambdaMockClient.on(PutFunctionConcurrencyCommand).resolves({});
        lambdaMockClient.on(UpdateEventSourceMappingCommand).rejects("Errore!");

        const res = await lambda.handleEvent(event);
        expect(res.statusCode).deep.equals(500);
    })

    this.afterEach(() => {
        process.env = originalEnv;
        lambdaMockClient.reset();
    });

})

function createEvent(desiredCount, runningCount) {
    return {
        detail: {
            responseElements: {
                service: {
                    "desiredCount": desiredCount,
                    "runningCount": runningCount
                }
            }
        }
    }
}