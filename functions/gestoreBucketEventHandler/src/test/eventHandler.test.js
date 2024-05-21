const { mockClient } = require("aws-sdk-client-mock");
const { S3Client, GetObjectCommand } = require("@aws-sdk/client-s3")
const proxyquire = require("proxyquire").noPreserveCache();
const { expect } = require("chai");
const fs = require("fs");
const crypto = require("crypto");
const HttpRequestMock = require('http-request-mock');
const mocker = HttpRequestMock.setup();
var originalEnv = process.env;

const OBJECT_CREATED = "ObjectCreated:*";
const OBJECT_CREATED_PUT = "ObjectCreated:Put";
const OBJECT_CREATED_COPY = "ObjectCreated:Copy";
const OBJECT_RESTORE_COMPLETED = "ObjectRestore:Completed";
const OBJECT_RESTORE_DELETE = "ObjectRestore:Delete";
const OBJECT_REMOVED = "ObjectRemoved:DeleteMarkerCreated";
const LIFECYCLE_TRANSITION = "LifecycleTransition";
const LIFECYCLE_EXPIRATION = "LifecycleExpiration:DeleteMarkerCreated";

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

  s3MockClient = mockClient(S3Client);

  describe("Event : ObjectCreated:Put", function () {

    it("test staging bucket ok", async () => {

      const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
      const STAGINGBUCKET = process.env.PnSsStagingBucketName;
      const NOW = new Date(Date.now()).toISOString();

      const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

      const docKey = "fileKey";
      var event = createEvent(OBJECT_CREATED_PUT, docKey, "", STAGINGBUCKET, NOW);

      var expectedRequest = {
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

      const res = await lambda.handleEvent(event);

      expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
      expect(res).deep.equals({
        batchItemFailures: [],
      });

    });

    it("test staging bucket patch ko", async () => {

      const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
      const STAGINGBUCKET = process.env.PnSsStagingBucketName;
      const NOW = new Date(Date.now()).toISOString();

      const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

      const docKey = "fileKey";
      var event = createEvent(OBJECT_CREATED_PUT, docKey, "", STAGINGBUCKET, NOW);

      var expectedRequest = {
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

      const res = await lambda.handleEvent(event);

      expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
      expect(res).deep.equals({
        batchItemFailures: [{ itemIdentifier: "messageId" }],
      });

    });


    const checksumTypes = ["SHA256", "MD5", "NONE"];

    checksumTypes.forEach(checksumType => {
      it("test hot bucket ok", async () => {

        const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
        const PATHGET = process.env.PnSsGestoreRepositoryPathGetDocument;
        const NOW = new Date(Date.now()).toISOString();

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

        var expectedHash = "";
        const s3Object = Buffer.from(fs.readFileSync("./src/test/pdf/test.pdf"));
        if (checksumType != "NONE") {
          expectedHash = crypto.createHash(checksumType).update(s3Object).digest("base64");
        }
        const docKey = "fileKey";
        var event = createEvent(OBJECT_CREATED_PUT, docKey, s3Object.length, "pn-ss-bucket", NOW);
        var documentResponse = createDocument(docKey, checksumType);

        var expectedRequest = {
          checkSum: expectedHash,
          contentLenght: s3Object.length,
          documentKey: docKey,
          documentState: "available",
          lastStatusChangeTimestamp: NOW
        };

        mocker.get(PATHGET, documentResponse);

        var originalRequest;
        mocker.mock({
          url: PATHPATCH,
          response: function (requestInfo) {
            originalRequest = requestInfo;
            return {};
          }
        });

        s3MockClient.on(GetObjectCommand).resolves({ Body: s3Object });

        const res = await lambda.handleEvent(event);

        expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
        expect(res).deep.equals({
          batchItemFailures: [],
        });

      });
    })
  });

  it("test event ObjectCreated:* ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(OBJECT_CREATED, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test event ObjectCreated:Copy ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(OBJECT_CREATED_COPY, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "available",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test event ObjectCreated:Completed ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(OBJECT_RESTORE_COMPLETED, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "available",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test event LifecycleTransition ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(LIFECYCLE_TRANSITION, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "freezed",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test event ObjectRestore:Delete ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(OBJECT_RESTORE_DELETE, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "freezed",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test event LifecycleExpiration:DeleteMarkerCreated ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(LIFECYCLE_EXPIRATION, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "deleted",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });

  it("test event ObjectRemoved:DeleteMarkerCreated ok", async () => {

    const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
    const NOW = new Date(Date.now()).toISOString();

    const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

    const docKey = "fileKey";
    var event = createEvent(OBJECT_REMOVED, docKey, "", "bucket", NOW);

    var expectedRequest = {
      documentKey: docKey,
      documentState: "deleted",
    };

    var originalRequest;
    mocker.mock({
      url: PATHPATCH,
      response: function (requestInfo) {
        originalRequest = requestInfo;
        return {};
      }
    });

    const res = await lambda.handleEvent(event);

    expect(JSON.parse(originalRequest.body)).to.deep.equal(expectedRequest);
    expect(res).deep.equals({
      batchItemFailures: [],
    });

  });


  this.afterEach(() => {
    process.env = originalEnv;
    s3MockClient.reset();
  });

});

function createEvent(eventName, fileKey, fileSize, bucketName, eventTime) {
  var eventBody = {
    "Records": [
      {
        "eventTime": eventTime,
        "eventName": eventName,
        "s3": {
          "bucket": {
            "name": bucketName
          },
          "object": {
            "key": fileKey,
            "size": fileSize
          }
        }
      }
    ]
  }

  var event = {
    Records: [{
      body: JSON.stringify(eventBody),
      messageId: "messageId"
    }]
  }
  return event;
}

function createDocument(fileKey, checksumType) {
  var documentResponse = {
    "document": {
      "documentKey": fileKey,
      "documentType": {
        "checksum": checksumType
      }
    }
  }
  return documentResponse;
}