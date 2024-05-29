"use strict";

const { S3Client, DeleteObjectCommand, ListObjectVersionsCommand  } = require("@aws-sdk/client-s3");

exports.handleEvent = async (event) => {
    const BUCKET_NAME = process.env.PnSsBucketName;
    const DELETION_MODE = process.env.PnSsDocumentDeletionMode;


    console.log(JSON.stringify(event));
    const batchItemFailures = [];
    await Promise.allSettled(event.Records.map(async (record) => {

    const fileKey = record.dynamodb.Keys.documentKey.S;
    const client = new S3Client({});

    try{
            switch (DELETION_MODE) {
                case "MARKER":
                    console.log(`Applicazione del DeleteMarker sul file ${fileKey} in S3...`);

                    await client.send(new DeleteObjectCommand({
                      Bucket: BUCKET_NAME,
                      Key: fileKey
                    }));

                    console.log(`File ${fileKey} eliminato con successo da S3.`);
                    break;
/*                case "COMPLETE":
                    console.log(`Ottenendo tutte le versioni del file ${fileKey} da S3...`);
                    const command=new ListObjectVersionsCommand({
                      Bucket: BUCKET_NAME,
                      Prefix: fileKey
                    });
                    const versions=await client.send(command);
                    await Promise.all(versions.Versions.map(
                        async version => {
                                console.log(`Cancellazione della versione "${version.VersionId}"...`);
                                await client.send(new DeleteObjectCommand({
                                   Bucket: BUCKET_NAME,
                                   Key: version.Key,
                                   VersionId: version.VersionId
                                }));
                        }
                        ));
                    break; */
                default:
                    console.log(`Nessuna azione eseguita per il file ${fileKey} con DELETION_MODE=${DELETION_MODE}.`);
                    return;
            }
    }
    catch(error)
    {
        console.error("Errore durante l'eliminazione del file : " + error);
        batchItemFailures.push({itemIdentifier: fileKey});
    }

    }));
    console.log("FINE LAVORAZIONE EVENTO");
    return { batchItemFailures };
};
