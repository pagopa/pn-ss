package it.pagopa.pnss.uribuilder.service;

import static it.pagopa.pnss.common.Constant.EU_CENTRAL_1;
import static it.pagopa.pnss.common.Constant.MAX_RECOVER_COLD;
import static it.pagopa.pnss.common.Constant.PN_AAR;
import static it.pagopa.pnss.common.Constant.PN_DOWNTIME_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_EXTERNAL_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_NOTIFICATION_ATTACHMENTS;
import static it.pagopa.pnss.common.Constant.listaStatus;
import static it.pagopa.pnss.common.Constant.listaTipoDocumenti;
import static it.pagopa.pnss.common.Constant.listaTipologieDoc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import it.pagopa.pn.template.internal.rest.v1.dto.CurrentStatus;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.configurationproperties.BucketName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.security.SecureRandom;
import java.util.*;

@Service
@Slf4j
public class UriBuilderService {


    UserConfigurationClientCall userConfigurationClientCall;
    DocumentClientCall documentClientCall;

    DocTypesClientCall docTypesClientCall;

    @Autowired
    private BucketName bucketName;

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall, DocTypesClientCall docTypesClientCall) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
        this.docTypesClientCall = docTypesClientCall;
    }

    Map<String, String> mapDocumentTypeToBucket;

    @PostConstruct
    public void createMap() {
        mapDocumentTypeToBucket = new HashMap<>();
        mapDocumentTypeToBucket.put(PN_NOTIFICATION_ATTACHMENTS, bucketName.ssHotName());
        mapDocumentTypeToBucket.put(PN_AAR, bucketName.ssHotName());
        mapDocumentTypeToBucket.put(PN_LEGAL_FACTS, bucketName.ssStageName());
        mapDocumentTypeToBucket.put(PN_EXTERNAL_LEGAL_FACTS, bucketName.ssHotName());
        mapDocumentTypeToBucket.put(PN_DOWNTIME_LEGAL_FACTS, bucketName.ssStageName());

    }

    public Mono<FileCreationResponse> createUriForUploadFile(String xPagopaSafestorageCxId, FileCreationRequest request) {
        String contentType = request.getContentType();
        String documentType = request.getDocumentType();
        String status = request.getStatus();
        List<String> secret = new ArrayList<>();
        secret.add(generateSecret());
        Map<String, String> metadata = new HashMap<>();
        metadata.put("secret", secret.toString());

        return Mono.fromCallable(() -> validationField(contentType, documentType, status))
                .flatMap(voidMono -> userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                .handle((userConfiguration, synchronousSink) -> {
                    if (!userConfiguration.getUserConfiguration().getCanCreate().contains(documentType)) {
                        throw (new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Client : " + xPagopaSafestorageCxId +
                                        " not has privilege for upload document type " +
                                        documentType));
                    }
                    synchronousSink.next(userConfiguration);
                })
                .doOnSuccess(o -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                .flatMap(o -> {

                       GenerateRandoKeyFile g = GenerateRandoKeyFile.getInstance();
                       String keyName = g.createKeyName(documentType);

                       return documentClientCall.getdocument(keyName).doOnNext(document -> {
                           throw new DocumentkeyPresentException(keyName);
                       }).onErrorResume(DocumentKeyNotPresentException.class, e -> {

                    	   DocumentType documentTypeDto = new DocumentType();
                    	   documentTypeDto.setTipoDocumento(documentType);

                           Document documentRepositoryDto = new Document();
                           documentRepositoryDto.setContentType(contentType);
                           documentRepositoryDto.setDocumentKey(keyName);
                           documentRepositoryDto.setDocumentState(Document.DocumentStateEnum.BOOKED);
                           documentRepositoryDto.setDocumentType(documentTypeDto);
                           return documentClientCall.postdocument(documentRepositoryDto);
                       });
                   })
                   .retryWhen(Retry.max(10)
                                   .filter(DocumentkeyPresentException.class::isInstance)
                                   .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                                       throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                         "Non e' stato possibile produrre una chiave per user " +
                                                                         xPagopaSafestorageCxId);
                                   }))
                   .map(document -> {
                       PresignedPutObjectRequest presignedRequest =
                               builsUploadUrl(documentType, document.getDocument().getDocumentState(),document.getDocument().getDocumentKey(), contentType,metadata);
                       String myURL = presignedRequest.url().toString();
                       FileCreationResponse response = new FileCreationResponse();
                       response.setKey(document.getDocument().getDocumentKey());
                       response.setSecret(secret.toString());

                    response.setUploadUrl(myURL);
                    response.setUploadMethod(extractUploadMethod(presignedRequest.httpRequest().method()));


                       return response;
                   })
                   .doOnNext(o -> log.info("--- RECUPERO PRESIGNE URL OK "));
    }

    private Mono<Boolean> validationField(String contentType, String documentType, String status) {

        if (!listaTipoDocumenti.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ContentType :" + contentType + " - Not valid");
        }
        if (!listaTipologieDoc.contains(documentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentType :" + documentType + " - Not valid");
        }
        if (!status.equals("")) {
            if (!listaStatus.contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status :" + status + " - Not valid ");
            } else {
                if (!(documentType.equals("PN_NOTIFICATION_ATTACHMENTS") && status.equals("PRELOADED"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status :" + status + " - Not valid for documentType");
                }
            }
        }
        return Mono.just(true);
    }

//    private Document.DocumentTypeEnum retrieveDocType(String documentType) {
//        if (documentType.equals(PN_DOWNTIME_LEGAL_FACTS)) {
//            //return Document.DocumentTypeEnum.PN_DOWNTIME_LEGAL_FACTS
//        }
//        if (documentType.equals(PN_NOTIFICATION_ATTACHMENTS)) {
//            return Document.DocumentTypeEnum.NOTIFICATION_ATTACHMENTS;
//        }
//        if (documentType.equals(PN_AAR)) {
//            return Document.DocumentTypeEnum.AAR;
//        }
//        if (documentType.equals(PN_LEGAL_FACTS)) {
//            return Document.DocumentTypeEnum.LEGAL_FACTS;
//        }
//        if (documentType.equals(PN_EXTERNAL_LEGAL_FACTS)) {
//            return Document.DocumentTypeEnum.EXTERNAL_LEGAL_FACTS;
//        }
//        return null;
//
//    }


    private FileCreationResponse.UploadMethodEnum extractUploadMethod(SdkHttpMethod method) {
        if (method.equals(SdkHttpMethod.POST)) {
            return FileCreationResponse.UploadMethodEnum.POST;
        }
        return FileCreationResponse.UploadMethodEnum.PUT;
    }

    private PresignedPutObjectRequest builsUploadUrl(String documentType, Document.DocumentStateEnum documentState, String keyName, String contentType, Map<String, String> secret) {

        String bucketName = mapDocumentTypeToBucket.get(documentType);
        PresignedPutObjectRequest response = null;
        String storageType = "";
        try {
            S3Presigner presigner = getS3Presigner();
            if(docTypesClientCall.getdocTypes(documentType).block().getDocType().getStatuses() != null) {
               Map<String, CurrentStatus> statuses = docTypesClientCall.getdocTypes(documentType).block().getDocType().getStatuses();
                if (statuses.containsKey(documentType)) {
                    storageType = statuses.get(documentState).getStorage();
                } else {
                    log.error(" Errore Lo stato non corrisponde");
                }
            }
            response = signBucket(presigner, bucketName, keyName, contentType,secret, storageType);
        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");

        }

        return response;

    }

    public static S3Presigner getS3Presigner() {
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
        Region region = EU_CENTRAL_1;
        return S3Presigner.builder().region(region)
                //.credentialsProvider(credentialsProvider)
                .build();
    }

    private PresignedPutObjectRequest signBucket(S3Presigner presigner, String bucketName, String keyName, String contenType, Map<String, String> secret, String storageType) {

        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName)
                .key(keyName).contentType(contenType)
                .metadata(secret)
                .tagging(storageType)
                .build();
        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        return presignedRequest;

    }


    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId) {
        // chiamare l'api di  GestoreRepositori per recupero dati
        //todo

        return Mono.fromCallable(() -> validationFieldCreateUri(fileKey, xPagopaSafestorageCxId))
                .flatMap(voidMono -> userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                .doOnSuccess(o -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                .flatMap(userConfigurationResponse -> {
                    List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();

                    return documentClientCall.getdocument(fileKey)
                            .onErrorResume(DocumentKeyNotPresentException.class,
                                    throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document key Not Found : " + fileKey)))

                             .map((documentResponse) -> {
                                if (!canRead.contains(documentResponse.getDocument().getDocumentType().getTipoDocumento())) {
                                    throw (new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Client : " + xPagopaSafestorageCxId +
                                                    " not has privilege for read document type " +
                                                    documentResponse.getDocument().getDocumentType()));
                                }

                                 return documentResponse.getDocument();
                             }).doOnSuccess(o -> log.info("---  FINE  CHECK PERMESSI LETTURA"));

                }).map(doc -> {
                    return  getFileDownloadResponse(fileKey, doc);
                }).doOnNext(o -> log.info("--- RECUPERO PRESIGNE URL OK "));







    }

    @NotNull
    private FileDownloadResponse getFileDownloadResponse(String fileKey, Document doc) {
        FileDownloadResponse downloadResponse = new FileDownloadResponse();

        BigDecimal contentLength = doc.getContentLenght();

        downloadResponse.setChecksum(doc.getCheckSum() !=null ? doc.getCheckSum().getValue():null);
        downloadResponse.setContentLength(contentLength);
        downloadResponse.setContentType(doc.getContentType());
        downloadResponse.setDocumentStatus(doc.getDocumentState().getValue());
        downloadResponse.setDocumentType(doc.getDocumentType().getTipoDocumento());

        downloadResponse.setKey(fileKey);
        downloadResponse.setRetentionUntil(new Date());
        downloadResponse.setVersionId(null);

        downloadResponse.setDownload(createFileDownloadInfo(fileKey,
        downloadResponse.getDocumentStatus(),
        downloadResponse.getDocumentType()));
        return downloadResponse;
    }

    private Mono<Boolean> validationFieldCreateUri(String fileKey, String xPagopaSafestorageCxId) {
        return Mono.just(true);
    }

    private FileDownloadInfo createFileDownloadInfo(String fileKey, String status, String documentType) {
        FileDownloadInfo fileDOwnloadInfo = null;
        try {
            S3Presigner presigner = getS3Presigner();
            String bucketName = mapDocumentTypeToBucket.get(documentType);
            log.info("INIZIO RECUPERO URL DOWLOAND ");
            if (!Document.DocumentStateEnum.FREEZED.getValue().equals(status)) {
                fileDOwnloadInfo = getPresignedUrl(presigner, bucketName, fileKey);
            } else {
                fileDOwnloadInfo = recoverDocumentFromBucket(presigner, bucketName, fileKey);
            }
        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");

        }

        return fileDOwnloadInfo;

    }

    private FileDownloadInfo recoverDocumentFromBucket(S3Presigner presigner, String bucketName, String fileKey) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        // mettere codice per far partire il recupero del file
        //todo
        fdinfo.setRetryAfter(MAX_RECOVER_COLD);
        return fdinfo;
    }

    private FileDownloadInfo getPresignedUrl(S3Presigner presigner, String bucketName, String keyName) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        log.info("INIZIO CREAZIONE OGGETTO  GetObjectRequest");
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(keyName).build();
        log.info("FINE  CREAZIONE OGGETTO  GetObjectRequest");
        log.info("INIZIO  CREAZIONE OGGETTO  GetObjectPresignRequest");
        GetObjectPresignRequest getObjectPresignRequest =
                GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(60)).getObjectRequest(getObjectRequest).build();
        log.info("FINE  CREAZIONE OGGETTO  GetObjectPresignRequest");

        log.info("INIZIO  RECUPERO URL ");
        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
        log.info("FINE   RECUPERO URL ");

        String theUrl = presignedGetObjectRequest.url().toString();
        fdinfo.setUrl(theUrl);
        return fdinfo;

    }

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[256];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }
}
