const { mockClient } = require("aws-sdk-client-mock");
const { S3Client, CopyObjectCommand, GetObjectTaggingCommand, DeleteObjectCommand } = require("@aws-sdk/client-s3");
const proxyquire = require("proxyquire").noPreserveCache();
const { expect } = require("chai");

const s3MockClient = mockClient(S3Client);

describe("pn-ss-transformation-antivirus-lambda basic test", function () {

    this.beforeEach(() => {
        s3MockClient.reset();
    });

    it("should return status 200 for a basic empty event", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        const event = {};  // Evento vuoto, non genera azioni

        const res = await lambda.handleEvent(event);
        expect(res.statusCode).to.equal(200);
    });

    it("should return 200 for JSON TransformationMessage", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        const event = {
            fileKey: "document.pdf",
            transformationType: "ANTIVIRUS",
            bucketName: "bucket-staging"
        };

        // Mocka la risposta di CopyObjectCommand
        s3MockClient.on(CopyObjectCommand).resolves({});

        const res = await lambda.handleEvent(event);
        expect(res.statusCode).to.equal(200);
    });

    it("should rename object with 'ANTIVIRUS_' prefix for JSON TransformationMessage", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        const event = {
            fileKey: "document.pdf",
            transformationType: "ANTIVIRUS",
            bucketName: "bucket-staging"
        };

        // Mock S3 CopyObjectCommand
        s3MockClient.on(CopyObjectCommand).resolves({});

        const res = await lambda.handleEvent(event);

        // Verify response status code
        expect(res.statusCode).to.equal(200);

        // Debug: print s3 command calls
        console.log("S3 calls:", JSON.stringify(s3MockClient.calls(), null, 2));

        // Verify CopyObjectCommand call parameters
        expect(s3MockClient.commandCalls(CopyObjectCommand)[0].args[0].input).to.deep.equal({
            Bucket: "bucket-staging",
            CopySource: "bucket-staging/document.pdf",
            Key: "ANTIVIRUS_document.pdf"
        });
    });

    it("should restore the original filename and apply Transformation_ANTIVIRUS tag after antivirus scan", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        const event = {
            Records: [{
                eventName: "ObjectTagging:Put",
                s3: { object: { key: "ANTIVIRUS_document.pdf" } }
            }]
        };

        // Simulate AVStatus tag retrieval (file is clean)
        s3MockClient.on(GetObjectTaggingCommand).resolves({
            TagSet: [{ Key: "AVStatus", Value: "CLEAN" }]
        });

        // Mock for copying file without "ANTIVIRUS_" prefix
        s3MockClient.on(CopyObjectCommand).resolves({});

        // Mock for deleting file with "ANTIVIRUS_" prefix
        s3MockClient.on(DeleteObjectCommand).resolves({});

        const res = await lambda.handleEvent(event);

        // Verify response status code (200)
        expect(res.statusCode).to.equal(200);

        // Verify that the file has been copied with the original name and the correct tag
        expect(s3MockClient.commandCalls(CopyObjectCommand)[0].args[0].input).to.deep.equal({
            Bucket: process.env.PnSsStagingBucketName,
            CopySource: `${process.env.PnSsStagingBucketName}/ANTIVIRUS_document.pdf`,
            Key: "document.pdf",
            TaggingDirective: "REPLACE",
            Tagging: "Transformation_ANTIVIRUS=OK"
        });

        // Verify that the file with the "ANTIVIRUS_" prefix has been deleted
        expect(s3MockClient.commandCalls(DeleteObjectCommand)[0].args[0].input).to.deep.equal({
            Bucket: process.env.PnSsStagingBucketName,
            Key: "ANTIVIRUS_document.pdf"
        });
    });

    it("should set Transformation_ANTIVIRUS to ERROR if file is INFECTED", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});
        const event = {
            Records: [{
                eventName: "ObjectTagging:Put",
                s3: { object: { key: "ANTIVIRUS_document.pdf" } }
            }]
        };

        // Mocking the retrieval of the AVStatus tag (file is INFECTED)
        s3MockClient.on(GetObjectTaggingCommand).resolves({
            TagSet: [{ Key: "AVStatus", Value: "INFECTED" }]
        });

        // Mocking the CopyObjectCommand to set the tag during the copy
        s3MockClient.on(CopyObjectCommand).resolves({});

        // Mocking the DeleteObjectCommand to remove the prefixed file
        s3MockClient.on(DeleteObjectCommand).resolves({});

        const res = await lambda.handleEvent(event);

        // Ensure the Lambda responds with 200 (successfully processed)
        expect(res.statusCode).to.equal(200);

        // Verify that the file was copied with the correct tag
        expect(s3MockClient.commandCalls(CopyObjectCommand)[0].args[0].input).to.deep.equal({
            Bucket: process.env.PnSsStagingBucketName,
            CopySource: `${process.env.PnSsStagingBucketName}/ANTIVIRUS_document.pdf`,
            Key: "document.pdf",
            TaggingDirective: "REPLACE",
            Tagging: "Transformation_ANTIVIRUS=ERROR"
        });

        // Verify that the original file was deleted
        expect(s3MockClient.commandCalls(DeleteObjectCommand)[0].args[0].input).to.deep.equal({
            Bucket: process.env.PnSsStagingBucketName,
            Key: "ANTIVIRUS_document.pdf"
        });
    });
});
