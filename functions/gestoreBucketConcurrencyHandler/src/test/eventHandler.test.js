const AWS = require("aws-sdk-mock");
const proxyquire = require("proxyquire").noPreserveCache();
const { expect } = require("chai");
var originalEnv = process.env;

describe("gestoreBucketConcurrencyHandler tests", function () {

    this.beforeEach(() => {
        process.env.PnSsGestoreBucketTriggerId = "uuid";
        process.env.PnSsGestoreBucketLambdaArn = "function/gestoreBucketConcurrencyHandler";
        process.env.PnSsGestoreBucketInstancesPerTaskInstance = "10"
    });

    it("test ok", async () => {

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        var event = createEvent(2, 1);
        AWS.mock('Lambda', 'putFunctionConcurrency', function (params, callback) {
            callback(null, {});
        });
        AWS.mock('Lambda', 'updateEventSourceMapping', function (params, callback) {
            callback(null, {});
        });
        const res = await lambda.handleEvent(event);
        expect(res).deep.equals({
            statusCode: 200,
            body: 'Concurrency and EventSourceMapping successfully modified! '
        });
    })

    it("test putFunctionConcurrency ko", async () => {

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        var event = createEvent(2, 1);
        AWS.mock('Lambda', 'putFunctionConcurrency', function (params, callback) {
            callback(new Error("Errore!"), null);
        });
        AWS.mock('Lambda', 'updateEventSourceMapping', function (params, callback) {
            callback(null, {});
        });
        const res = await lambda.handleEvent(event);
        expect(res.statusCode).deep.equals(500);
    })

    it("test putFunctionConcurrency ko", async () => {

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        var event = createEvent(2, 1);
        AWS.mock('Lambda', 'putFunctionConcurrency', function (params, callback) {
            callback(null, {});
        });
        AWS.mock('Lambda', 'updateEventSourceMapping', function (params, callback) {
            callback(new Error("Errore!"), null);
        });
        const res = await lambda.handleEvent(event);
        expect(res.statusCode).deep.equals(500);
    })

    this.afterEach(() => {
        process.env = originalEnv;
        AWS.restore();
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