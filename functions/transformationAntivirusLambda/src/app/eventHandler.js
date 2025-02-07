"use strict";
const { S3Client, CopyObjectCommand, GetObjectTaggingCommand, DeleteObjectCommand } = require("@aws-sdk/client-s3");

const s3 = new S3Client();
const BUCKET_NAME = process.env.PnSsStagingBucketName;

exports.handleEvent = async function (event) {
    try {
        console.log("Received event:", JSON.stringify(event));

        // Case 1: Check if event is a JSON TransformationMessage
        if (event.fileKey && event.transformationType && event.bucketName) {
            const s3Object = event.fileKey;
            const newKey = `ANTIVIRUS_${s3Object}`;

            console.log(`TransformationMessage received. Renaming object: ${s3Object} -> ${newKey}`);

            await s3.send(new CopyObjectCommand({
                Bucket: event.bucketName,
                CopySource: `${event.bucketName}/${s3Object}`,
                Key: newKey
            }));

            // Delete the original file to avoid duplication (renaming)
            await s3.send(new DeleteObjectCommand({
                Bucket: BUCKET_NAME,
                Key: s3Object
            }));

            return {statusCode: 200, body: "File renamed successfully for antivirus scan."};
        }

        // Case 2: Request for SafeStorage transformation → Rename with "ANTIVIRUS_" prefix
        if (event.Records) {
            for (const record of event.Records) {
                if (record.s3 && record.eventName.startsWith("ObjectTagging:Put")) {
                    const s3Object = record.s3.object.key;

                    // Retrieve antivirus scan result from tags
                    const tagResponse = await s3.send(new GetObjectTaggingCommand({
                        Bucket: BUCKET_NAME,
                        Key: s3Object
                    }));

                    const antivirusTag = tagResponse.TagSet.find(tag => tag.Key === "AVStatus");
                    if (!antivirusTag) {
                        console.log("No AVStatus tag found.");
                        continue;
                    }

                    // Determine a transformation result based on antivirus scan
                    const transformationTag = {
                        Key: "Transformation_ANTIVIRUS",
                        Value: antivirusTag.Value === "INFECTED" ? "ERROR" : "OK"
                    };
                    console.log(`Setting tag Transformation_ANTIVIRUS: ${transformationTag.Value}`);

                    // Restore original filename if prefixed with "ANTIVIRUS_"
                    if (s3Object.startsWith("ANTIVIRUS_")) {
                        const originalKey = s3Object.replace("ANTIVIRUS_", "");
                        console.log(`Restoring original filename: ${s3Object} -> ${originalKey}`);

                        // Copy file and set Transformation_ANTIVIRUS tag in one step
                        await s3.send(new CopyObjectCommand({
                            Bucket: BUCKET_NAME,
                            CopySource: `${BUCKET_NAME}/${s3Object}`,
                            Key: originalKey,
                            TaggingDirective: "REPLACE",
                            Tagging: `Transformation_ANTIVIRUS=${transformationTag.Value}`
                        }));

                        // Delete the prefixed file to avoid duplication
                        await s3.send(new DeleteObjectCommand({
                            Bucket: BUCKET_NAME,
                            Key: s3Object
                        }));
                    }
                }
            }
        }

        return {statusCode: 200, body: "Process completed successfully!"};
    } catch (error) {
        console.error("Error:", error);
        return {statusCode: 500, body: "Error during antivirus processing."};
    }
};
