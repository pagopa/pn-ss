const { AWS } = require("aws-sdk-mock");
const proxyquire = require("proxyquire").noPreserveCache();
const { expect } = require("chai");
const fs = require("fs");
const HttpRequestMock = require('http-request-mock');
const mocker = HttpRequestMock.setup();
var originalEnv = process.env;

describe("gestoreBucketEventHandler tests", function () {

  this.beforeEach(() => {
    process.env.PnSsGestoreRepositoryProtocol = "http";
    process.env.PnSsHostname = "localhost";
    process.env.PnSsGestoreRepositoryPort = "8080";
    process.env.PnSsGestoreRepositoryPathGetDocument = "/getDocument";
    process.env.PnSsGestoreRepositoryPathPatchDocument = "/patchDocument";
    process.env.PnSsStagingBucketName = "StagingBucket";
    process.env.GestoreBucketGetObjectChunkSize = "1048576"
  });


  it("test staging bucket ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const STAGINGBUCKET = process.env.PnSsStagingBucketName;

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const eventJSON = fs.readFileSync("./src/test/json/event.json");
    const event = JSON.parse(eventJSON);
    const docKey = event.Records[0].s3.object.key;

    event.Records[0].s3.bucket.name = STAGINGBUCKET;
    let jsonDocument = {
      documentKey: docKey,
      documentState: "staged",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent({
      Records: [{
        body: JSON.stringify(event),
        messageId: "messageId"
      }]
    });

    expect(JSON.parse(originalRequest.body)).to.deep.equal(jsonDocument);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test staging bucket patch ko", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const STAGINGBUCKET = process.env.PnSsStagingBucketName;

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const eventJSON = fs.readFileSync("./src/test/json/event.json");
    const event = JSON.parse(eventJSON);
    const docKey = event.Records[0].s3.object.key;

    event.Records[0].s3.bucket.name = STAGINGBUCKET;
    let jsonDocument = {
      documentKey: docKey,
      documentState: "staged",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      status: 500,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return;
      }
    });

    const res = await lambda.handleEvent({
      Records: [{
        body: JSON.stringify(event),
        messageId: "messageId"
      }]
    });

    expect(JSON.parse(originalRequest.body)).to.deep.equal(jsonDocument);
    expect(res).deep.equals({
      batchItemFailures: [{ itemIdentifier: "messageId" }],
    });

  });

  this.afterEach(() => {
    process.env = originalEnv;
  });

});