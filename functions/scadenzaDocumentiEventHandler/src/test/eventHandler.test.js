const {  mockClient } = require("aws-sdk-client-mock");
const { expect } = require("chai");
const { eventHandler } = require("../app/eventHandler.js");
const {S3Client, DeleteObjectCommand, ListObjectVersionsCommand} = require("@aws-sdk/client-s3");
const proxyquire = require("proxyquire").noPreserveCache();
const fs = require("fs");

const s3Mock = mockClient(S3Client);

describe("scadenzaDocumentiEventHandler tests", function() {

    this.beforeEach(() => {
        s3Mock.reset();
    });

    it("test delete marker ok", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

        s3Mock.on(DeleteObjectCommand).resolves({});

        const eventJSON = fs.readFileSync("./src/test/eventHandler.event.json");
        const event = JSON.parse(eventJSON);

        const oldDeletionMode = process.env.PnSsDocumentDeletionMode;
        process.env.PnSsDocumentDeletionMode = "MARKER";
        const res = await lambda.handleEvent(event);
        process.env.PnSsDocumentDeletionMode = oldDeletionMode;

        expect(res).deep.equals({
            batchItemFailures: [],
        });
    });

   /* it("test delete complete ok", async () => {

        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

        const listObjectVersionResponseJSON = fs.readFileSync("./src/test/eventHandler.listObjectVersionResponse.json");
        const listObjectVersionResponse = JSON.parse(listObjectVersionResponseJSON);

        s3Mock.on(ListObjectVersionsCommand).resolves(listObjectVersionResponse);

        s3Mock.on(DeleteObjectCommand).resolves({});

        const eventJSON = fs.readFileSync("./src/test/eventHandler.event.json");
        const event = JSON.parse(eventJSON);

        const oldDeletionMode = process.env.PnSsDocumentDeletionMode;
        process.env.PnSsDocumentDeletionMode = "COMPLETE";
        const res = await lambda.handleEvent(event);
        process.env.PnSsDocumentDeletionMode = oldDeletionMode;


        expect(res).deep.equals({
            batchItemFailures: [],
        });
    });

    it("test delete complete without versions", async () => {
        const lambda = proxyquire.callThru().load("../app/eventHandler.js", {});

        const listObjectVersionResponseJSON = fs.readFileSync("./src/test/eventHandler.listObjectVersionResponse.json");
        const listObjectVersionResponse = JSON.parse(listObjectVersionResponseJSON);
        listObjectVersionResponse.Versions = undefined;

        s3Mock.on(ListObjectVersionsCommand).resolves(listObjectVersionResponse);
        s3Mock.on(DeleteObjectCommand).resolves({});

        const eventJSON = fs.readFileSync("./src/test/eventHandler.event.json");
        const event = JSON.parse(eventJSON);

        const oldDeletionMode = process.env.PnSsDocumentDeletionMode;
        process.env.PnSsDocumentDeletionMode = "COMPLETE";
        const res = await lambda.handleEvent(event);
        process.env.PnSsDocumentDeletionMode = oldDeletionMode;

        expect(res).deep.equals({
            batchItemFailures: [{
                "itemIdentifier": "fileKey"
            }],
        });
    }); */


});