"use strict";

const http = require(process.env.PnSsGestoreRepositoryProtocol);
const { S3Client, GetObjectCommand, ListObjectVersionsCommand } = require("@aws-sdk/client-s3")
const crypto = require("crypto");

const HOSTNAME = process.env.PnSsHostname;
const PORT = process.env.PnSsGestoreRepositoryPort;
const PATHGET = process.env.PnSsGestoreRepositoryPathGetDocument;
const PATHPATCH = process.env.PnSsGestoreRepositoryPathPatchDocument;
const STAGINGBUCKET = process.env.PnSsStagingBucketName;
const chunkSize = Number(process.env.GestoreBucketGetObjectChunkSize);

exports.handleEvent = async (event) => {
  const s3 = new S3Client({});
  const batchItemFailures = [];
  console.log(JSON.stringify(event));

  await Promise.allSettled(
    event.Records.map(async (record) => {
      const bodyData = JSON.parse(record.body);

      console.log("Processing " + record.messageId);

      let jsonDocument = {
        documentKey: "",
        documentState: "",
      };

      let bucketName;
      console.log("Bucket Name: " + bodyData.Records[0].s3.bucket.name);
      console.log("Object Key: " + bodyData.Records[0].s3.object.key);
      console.log("Object Size: " + bodyData.Records[0].s3.object.size);
      let params = {
        Bucket: "",
        Key: "",
      };
      bucketName = bodyData.Records[0].s3.bucket.name;
      jsonDocument.documentKey = bodyData.Records[0].s3.object.key;
      switch (bodyData.Records[0].eventName) {
        case "ObjectCreated:*":
          break;
        case "ObjectCreated:Put":
          if (bucketName === STAGINGBUCKET) {
            jsonDocument.documentState = "staged";
          } else {
            jsonDocument.contentLenght = bodyData.Records[0].s3.object.size;
            jsonDocument.documentState = "available";
            jsonDocument.checkSum = "";
            params.Bucket = bucketName;
            params.Key = jsonDocument.documentKey;
            try {
              const doc = await getDocumentFromDB(jsonDocument.documentKey);
              console.log(doc.document);
              console.log(doc.document.documentType.checksum);
              console.log(JSON.stringify(doc.document.documentType.checksum));

              if (doc.document.documentType.checksum != "NONE") {
                console.log("Starting file hashing...")

                let hash = crypto.createHash(doc.document.documentType.checksum);
                let offset = 0;

                while (offset < jsonDocument.contentLenght) {
                  const end = Math.min(offset + chunkSize, jsonDocument.contentLenght);
                  params.Range = `bytes=${offset}-${end - 1}`;
                  const chunk = await s3.send(new GetObjectCommand(params));

                  let data = [];

                  for await (let piece of chunk.Body) {
                    if (typeof piece === 'number') {
                      piece = String.fromCharCode(piece);
                    }
                    data.push(Buffer.from(piece));
                  }

                  let buffer = Buffer.concat(data);

                  hash.update(buffer);
                  offset += chunkSize;
                }
                jsonDocument.checkSum = hash.digest('base64');
                console.log("File hashing done!")
              }

            } catch (error) {
              console.log(error);
              const messageError = `* FATAL * Errore nella lavorazione dell'oggetto ${jsonDocument.documentKey} dal bucket ${bucketName}. Verificare che l'oggetto esiste nel bucket o sul database Dynamo.`;
              console.log(messageError);
              batchItemFailures.push({ itemIdentifier: record.messageId });
              return;
              //throw new Error(messageError);
            }
          }
          jsonDocument.lastStatusChangeTimestamp = bodyData.Records[0].eventTime;
          console.log(jsonDocument);
          break;
        case "ObjectCreated:Copy":
          jsonDocument.documentState = "available";
          break;
        case "ObjectRestore:Completed":
          jsonDocument.documentState = "available";
          break;
        case "LifecycleTransition":
          jsonDocument.documentState = "freezed";
          break;
        case "ObjectRestore:Delete":
          jsonDocument.documentState = "freezed";
          break;
        case "LifecycleExpiration:DeleteMarkerCreated":
          jsonDocument.documentState = "deleted";
          break;
        case "ObjectRemoved:DeleteMarkerCreated":
          jsonDocument.documentState = "deleted";
          break;
        case "ObjectRemoved:Delete":
          try {
            const response = await s3.send(new ListObjectVersionsCommand({
              Bucket: bucketName,
              Prefix: jsonDocument.documentKey
            }));
            if (response.Versions == null && response.DeleteMarkers == null) {
              console.log("All file versions have been removed. Setting document in 'deleted' status...")
              jsonDocument.documentState = "deleted";
            }
          }
          catch (error) {
            const messageError = `* FATAL * Errore nella lavorazione dell'oggetto ${jsonDocument.documentKey} dal bucket ${bucketName}: ${error}`;
            console.log(messageError);
            batchItemFailures.push({ itemIdentifier: record.messageId });
            return;
          }
          break;
        default:
          return;
      }
      await updateDynamo(jsonDocument).then(
        function (data) {
          console.log("############## EXIT  ####################");
          return;
        },
        function (error) {
          const retMessgeError = "* FATAL * Errore nella patch su DynamoDB - " + error;
          console.log(retMessgeError);
          batchItemFailures.push({ itemIdentifier: record.messageId });
          return;
        }
      );
    })
  );
  console.log("FINE LAVORAZIONE EVENTO");
  return { batchItemFailures };
};
function getDocumentFromDB(docKey) {
  const options = {
    method: "GET",
    hostname: HOSTNAME,
    port: PORT,
    path: PATHGET + "/" + docKey,
    headers: {
      "x-pagopa-pn-cx-id": docKey,
      "Content-Type": "application/json",
    },
  };
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseBody = "";

      res.on("data", (chunk) => {
        responseBody += chunk;
      });

      res.on("end", () => {
        resolve(JSON.parse(responseBody));
      });
    });

    req.on("error", (err) => {
      console.error(err);
      reject(err);
    });
    req.end();
  });
}
function updateDynamo(data) {
  const options = {
    method: "PATCH",
    hostname: HOSTNAME,
    port: PORT,
    path: PATHPATCH + "/" + data.documentKey,
    headers: {
      "x-pagopa-pn-cx-id": data.documentKey,
      "Content-Type": "application/json",
    },
  };
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseBody = "";
      res.on("data", (chunk) => {
        responseBody += chunk;
      });
      res.on("end", () => {
        console.log(res.statusCode + " - " + responseBody);
        switch (res.statusCode) {
          case 200:
            resolve(JSON.parse(responseBody));
            break;
          default:
            reject(res.statusCode + " - " + data.documentKey);
            break;
        }
      });
    });
    req.on("error", (err) => {
      console.error(err);
      reject(err);
    });
    req.write(JSON.stringify(data));
    req.end();
  });
}