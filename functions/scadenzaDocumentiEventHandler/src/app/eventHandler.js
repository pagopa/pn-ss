"use strict";

const s3 = require("@aws-sdk/client-s3");

exports.handler = async (event) => {
    const BUCKET_NAME = process.env.PnSsBucketName;
    const DELETION_MODE = process.env.PnSsDocumentDeletionMode

    const batchItemFailures = [];
    await Promise.allSettled(event.Records.map(async (record) => {

        try {
            const fileKey = record.dynamodb.Keys.documentKey.S;
            switch (DELETION_MODE) {
                case "MARKER":
                    await s3.deleteObject({ Bucket: BUCKET_NAME, Key: fileKey }).promise();
                    console.log(`File ${fileKey} eliminato con successo da S3.`);
                    break;
                case "DELETE":
                    console.log(`Ottenendo tutte le versioni del file ${fileKey} da S3...`);
                    const versions = await s3.listObjectVersions({ Bucket: BUCKET_NAME, Prefix: fileKey }).promise();
                    const deletePromises = versions.Versions.map(async version => {
                        await s3.deleteObject({
                            Bucket: BUCKET_NAME,
                            Key: version.Key,
                            VersionId: version.VersionId
                        }).promise();
                        console.log(`Versione ${version.VersionId} eliminata.`);
                    });
                    await Promise.all(deletePromises).catch(error => {
                        console.error(`Errore durante l'eliminazione delle versioni: `, error);
                        throw error;
                    });
                    console.log(`Eliminate tutte le versioni del file ${fileKey} da S3.`);
                    break;
            }
        }
        catch (s3Exception) {
            console.error(`Errore durante l'eliminazione del file ${fileKey} in S3:`, s3Exception);
            batchItemFailures.push({ itemIdentifier: record.messageId });
            return;
        }
    }));
    console.log("FINE LAVORAZIONE EVENTO");
    return { batchItemFailures };
};
