const s3 = require("@aws-sdk/client-s3");

const BUCKET_NAME = process.env.PnSsBucketName;

exports.handler = async (event) => {
    // Verifica che la lista dei Record sia popolata.
    if (event.Records && event.Records.length > 0) {
        const record = event.Records[0];

        // Estrai l'identificativo del file da DynamoDB
        const fileKey = record.dynamodb.Keys.documentKey.S;

        try {
            // Elimina il file da S3
            await s3.deleteObject({ Bucket: BUCKET_NAME, Key: fileKey }).promise();
            console.log(`File ${fileKey} eliminato con successo da S3.`);
            return { statusCode: 200, body: 'File eliminato con successo.' };
        } catch (error) {
            console.error("Errore durante l'eliminazione del file ${fileKey} da S3:", error);
            return { statusCode: 500, body: 'Errore durante l\'eliminazione del file.' };
        }
    } else {
        console.warn('Evento non valido per la cancellazione del file.');
        return { statusCode: 400, body: 'Evento non gestito.' };
    }
};
