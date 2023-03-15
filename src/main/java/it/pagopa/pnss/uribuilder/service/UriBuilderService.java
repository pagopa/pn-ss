package it.pagopa.pnss.uribuilder.service;

import static it.pagopa.pnss.common.Constant.MAX_RECOVER_COLD;
import static it.pagopa.pnss.common.Constant.PN_AAR;
import static it.pagopa.pnss.common.Constant.PN_DOWNTIME_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_EXTERNAL_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_NOTIFICATION_ATTACHMENTS;
import static it.pagopa.pnss.common.Constant.listaStatus;
import static it.pagopa.pnss.common.Constant.listaTipoDocumenti;
import static it.pagopa.pnss.common.Constant.listaTipologieDoc;
import static it.pagopa.pnss.common.Constant.technicalStatus_attached;
import static it.pagopa.pnss.common.Constant.technicalStatus_available;
import static it.pagopa.pnss.common.Constant.technicalStatus_freezed;
import static it.pagopa.pnss.common.Constant.APPLICATION_PDF;
import static it.pagopa.pnss.common.Constant.APPLICATION_ZIP;
import static it.pagopa.pnss.common.Constant.IMAGE_TIFF;
import static it.pagopa.pnss.common.Constant.FILE_EXTENSION_PDF;
import static it.pagopa.pnss.common.Constant.FILE_EXTENSION_ZIP;
import static it.pagopa.pnss.common.Constant.FILE_EXTENSION_TIFF;

import static java.util.Map.entry;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.RestoreObjectRequest;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.ChecksumException;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.QueryParamException;
import it.pagopa.pnss.transformation.service.CommonS3ObjectService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Slf4j
public class UriBuilderService extends CommonS3ObjectService {
	
    @Value("${uri.builder.presigned.url.duration.minutes}")
    String duration;

    @Value("${uri.builder.stay.Hot.Bucket.tyme.days}")
    Integer stayHotTime;
    
    @Value("${header.presignUrl.checksum-sha256:#{null}}")
    String headerChecksumSha256;
    
    @Value("${presignedUrl.initial.newDocument.state}")
    String initialNewDocumentState;

    @Value("${test.aws.s3.endpoint:#{null}}")
    private String testAwsS3Endpoint;
    
    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    String queryParamPresignedUrlTraceId;

    private final UserConfigurationClientCall userConfigurationClientCall;
    private final DocumentClientCall documentClientCall;
    private final AwsConfigurationProperties awsConfigurationProperties;
    private final BucketName bucketName;

    public UriBuilderService(UserConfigurationClientCall userConfigurationClientCall, DocumentClientCall documentClientCall,
                             AwsConfigurationProperties awsConfigurationProperties, BucketName bucketName
                             ) {
        this.userConfigurationClientCall = userConfigurationClientCall;
        this.documentClientCall = documentClientCall;
        this.awsConfigurationProperties = awsConfigurationProperties;
        this.bucketName = bucketName;
    }

    private Map<String, String> mapDocumentTypeToBucket;

    @PostConstruct
    public void createMap() {
        mapDocumentTypeToBucket = Map.ofEntries(entry(PN_NOTIFICATION_ATTACHMENTS, bucketName.ssHotName()),
                                                entry(PN_AAR, bucketName.ssHotName()),
                                                entry(PN_LEGAL_FACTS, bucketName.ssStageName()),
                                                entry(PN_EXTERNAL_LEGAL_FACTS, bucketName.ssHotName()),
                                                entry(PN_DOWNTIME_LEGAL_FACTS, bucketName.ssStageName()));
    }

    public Mono<FileCreationResponse> createUriForUploadFile(
    		String xPagopaSafestorageCxId, FileCreationRequest request,
    		String checksumValue) {

        var contentType = request.getContentType();
        var documentType = request.getDocumentType();
        var status = request.getStatus();

        // NOTA : in questo modo, sono immutabili
//        var secret = List.of(generateSecret());
//        var metadata = Map.of("secret", secret.toString());
        var secret = new ArrayList<String>();
        secret.add(generateSecret());
        var metadata = new HashMap<String,String>();
        metadata.put("secret", secret.toString());

        return Mono.fromCallable(() -> validationField(contentType, documentType, status))
                   .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                   .handle((userConfiguration, synchronousSink) -> {
                       if (!userConfiguration.getUserConfiguration().getCanCreate().contains(documentType)) {
                           synchronousSink.error((new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                                              String.format(
                                                                                      "Client '%s' not has privilege for upload document " +
                                                                                      "type '%s'",
                                                                                      xPagopaSafestorageCxId,
                                                                                      documentType))));
                       } else {
                           synchronousSink.next(userConfiguration);
                       }
                   })
                   .doOnSuccess(object -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                   .flatMap(unused -> {
	            	   	String documenKeyTmp = GenerateRandoKeyFile.getInstance().createKeyName(documentType);
	            	   	switch(contentType) {
						case APPLICATION_PDF:
							documenKeyTmp += FILE_EXTENSION_PDF;
							break;
						case APPLICATION_ZIP:
							documenKeyTmp += FILE_EXTENSION_ZIP;
							break;
						case IMAGE_TIFF:
							documenKeyTmp += FILE_EXTENSION_TIFF;
							break;
						default:
							return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unrecognized Content Type"));
						}
	            	   	log.info("createUriForUploadFile(): documenKeyTmp = {} : ", documenKeyTmp);
	            	   	return documentClientCall.postDocument(new DocumentInput()
	            			   										.contentType(request.getContentType())
	                                                                .documentKey(documenKeyTmp)
	                                                                .documentState(initialNewDocumentState)
	                                                                .clientShortCode(xPagopaSafestorageCxId)
	                                                                .documentType(request.getDocumentType()))
	                                       .retryWhen(Retry.max(10)
	                                                       .filter(DocumentkeyPresentException.class::isInstance)
	                                                       .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
	                                                           throw new ResponseStatusException(HttpStatus.NOT_FOUND,
	                                                                                             "Non e' stato possibile " +
	                                                                                             "produrre una chiave per " + "user " +
	                                                                                             xPagopaSafestorageCxId);
	                                                       }));
                   	})
                   .flatMap(insertedDocument -> 
//                       PresignedPutObjectRequest presignedPutObjectRequest =
                               buildsUploadUrl(documentType, 
                            		   		  insertedDocument.getDocument().getDocumentState(), 
                            		   		  insertedDocument.getDocument().getDocumentKey(), 
                            		   		  contentType, 
                            		   		  metadata,
                            		   		  insertedDocument.getDocument().getDocumentType().getChecksum(),
                            		   		  checksumValue)
                               .map(presignedPutObjectRequest -> {
                                   FileCreationResponse response = new FileCreationResponse();
                                   response.setKey(insertedDocument.getDocument().getDocumentKey());
                                   response.setSecret(secret.toString());
                                   response.setUploadUrl(presignedPutObjectRequest.url().toString());
                                   response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest().method())); 
                                   return response;
                               })

                       
//                       FileCreationResponse response = new FileCreationResponse();
//                       response.setKey(insertedDocument.getDocument().getDocumentKey());
//                       response.setSecret(secret.toString());
//                       response.setUploadUrl(presignedPutObjectRequest.url().toString());
//                       response.setUploadMethod(extractUploadMethod(presignedPutObjectRequest.httpRequest().method()));
//
//                       return response;
                   )
                   .doOnNext(o -> log.info("--- RECUPERO PRESIGNED URL OK "));
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

    private FileCreationResponse.UploadMethodEnum extractUploadMethod(SdkHttpMethod method) {
        if (method.equals(SdkHttpMethod.POST)) {
            return FileCreationResponse.UploadMethodEnum.POST;
        }
        return FileCreationResponse.UploadMethodEnum.PUT;
    }

    private Mono<PresignedPutObjectRequest> buildsUploadUrl(String documentType, String documentState, String documentKey, 
    		String contentType, Map<String, String> secret, ChecksumEnum checksumType, String checksumValue) {
    	log.info("buildsUploadUrl() : START : "
    			+ "documentType {} : documentState {} : documentKey {} : "
    			+ "contentType {} : secret {} : checksumType {} : checksumValue {}",
    			documentType, documentState, documentKey, contentType, secret, checksumType, checksumValue);

        S3Presigner presigner = getS3Presigner();
        
        return signBucket(presigner, 
        				  mapDocumentTypeToBucket.get(documentType), 
        				  documentKey, 
        				  documentState, 
        				  documentType, 
        				  contentType, 
        				  secret,
        				  checksumType,
        				  checksumValue)
        		.onErrorResume(ChecksumException.class, throvable -> {
        			log.error("buildsUploadUrl() : Errore impostazione ChecksumValue = {}", throvable.getMessage(), throvable);
        			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, throvable.getMessage()));
        		})
        		.onErrorResume(AmazonServiceException.class, throvable -> {
        			log.error("buildsUploadUrl() : Errore AMAZON AmazonServiceException = {}", throvable.getMessage(), throvable);
        			return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException"));
        		})
				.onErrorResume(ResponseStatusException.class, throvable -> {
        			log.error("buildsUploadUrl() : Errore AMAZON SdkClientException = {}", throvable.getMessage(), throvable);
        			return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException "));
        		})
				.onErrorResume(Exception.class, throvable -> {
        			log.error("buildsUploadUrl() : Errore generico: {}", throvable.getMessage(), throvable);
        			return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico"));
        		});
        
//        try {
//            response = signBucket(presigner, bucketName, keyName, documentState, documentType, contentType, secret);
//        } catch (AmazonServiceException ase) {
//            log.error(" Errore AMAZON AmazonServiceException", ase);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
//        } catch (SdkClientException sce) {
//            log.error(" Errore AMAZON SdkClientException", sce);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException ");
//        } catch (Exception e) {
//            log.error(" Errore Generico", e);
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");
//        }
//
//        return response;
    }

    private Mono<PresignedPutObjectRequest> signBucket(S3Presigner s3Presigner, String bucketName, 
    		String documentKey, String documentState, String documentType,
    		String contenType, Map<String,String> secret, ChecksumEnum checksumType, String checksumValue) {

    	log.debug("signBucket() : START : s3Presigner IN : "
    			+ "bucketName {} : keyName {} : "
    			+ "documentState {} : documentType {} : contenType {} : "
    			+ "secret {} : checksumType{} : checksumValue {}",
    			bucketName, documentKey, documentState, 
    			documentType, contenType, secret, checksumType, checksumValue);
    	log.info("signBucket() : sign bucket {}", duration);
    	
    	if (checksumType == null || checksumValue == null || checksumValue.isBlank()) {
    		return Mono.error(new ChecksumException("Non e' stato possibile impostare il ChecksumValue nella PutObjectRequest"));
    	}
    	if (queryParamPresignedUrlTraceId == null || queryParamPresignedUrlTraceId.isBlank()) {
    		return Mono.error(new QueryParamException("Property \"queryParam.presignedUrl.traceId\" non impostata"));
    	}

    	return  Mono.just(checksumType)
		    		.flatMap(checksumTypeToEvaluate -> {
		    			if (ChecksumEnum.MD5.name().equals(checksumTypeToEvaluate.name())) {
		        			return Mono.just(PutObjectRequest.builder()
									 .bucket(bucketName)
									 .key(documentKey)
									 .contentType(contenType)
									 .metadata(secret)
									 .contentMD5(checksumValue)
					                //.tagging(storageType)
									 // Aggiungere queryParam custom alle presigned URL di upload e download
									 .overrideConfiguration(awsRequestOverrideConfiguration -> 
		 								awsRequestOverrideConfiguration.putRawQueryParameter(queryParamPresignedUrlTraceId, documentKey))
									 .build());
		    			}
		    			else if (headerChecksumSha256 != null && !headerChecksumSha256.isBlank()
		    					&& secret != null
		    					&& ChecksumEnum.SHA256.name().equals(checksumTypeToEvaluate.name())) {
		    				// secret.put(headerChecksumSha256, checksumValue);
			    			return Mono.just(PutObjectRequest.builder()
									 .bucket(bucketName)
									 .key(documentKey)
									 .contentType(contenType)
									 .metadata(secret)
									 .checksumSHA256(checksumValue)
					                //.tagging(storageType)
									 // Aggiungere queryParam custom alle presigned URL di upload e download
									 .overrideConfiguration(awsRequestOverrideConfiguration -> 
		 								awsRequestOverrideConfiguration.putRawQueryParameter(queryParamPresignedUrlTraceId, documentKey))
									 .build());
		    			}
		    			else {
		    				return Mono.error(new ChecksumException("Non e' stato possibile impostare il ChecksumValue nella PutObjectRequest"));
		    			}
		    		})
					.map(putObjectRequest -> PutObjectPresignRequest.builder()
					        .signatureDuration(Duration.ofMinutes(Long.parseLong(duration)))
					        .putObjectRequest(putObjectRequest)
					        .build()
					)
					.flatMap(putObjectPresignRequest -> Mono.just(s3Presigner.presignPutObject(putObjectPresignRequest)));
    }

    public Mono<FileDownloadResponse> createUriForDownloadFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly) {
        return Mono.fromCallable(() -> validationFieldCreateUri(fileKey, xPagopaSafestorageCxId))
                   .then(userConfigurationClientCall.getUser(xPagopaSafestorageCxId))
                   .doOnSuccess(o -> log.info("--- REST FINE  CHIAMATA USER CONFIGURATION"))
                   .flatMap(userConfigurationResponse -> {
                       List<String> canRead = userConfigurationResponse.getUserConfiguration().getCanRead();

                       return documentClientCall.getdocument(fileKey)
                                                .onErrorResume(DocumentKeyNotPresentException.class,
                                                               throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                                                   "Document key Not " +
                                                                                                                   "Found : " + fileKey)))
                                                .map(documentResponse -> {
                                                    if (!canRead.contains(documentResponse.getDocument()
                                                            .getDocumentType()
                                                            .getTipoDocumento())) {
                                                        throw (new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                                "Client : " + xPagopaSafestorageCxId +
                                                                        " not has privilege for read document type " +
                                                                        documentResponse.getDocument()
                                                                                .getDocumentType()));
                                                    }

                                                    return documentResponse.getDocument();
                                                })
                                                .doOnSuccess(o -> log.info("---  FINE  CHECK PERMESSI LETTURA"));
                   })
                   .map(doc -> getFileDownloadResponse(fileKey, doc, metadataOnly))
                   .doOnNext(o -> log.info("--- RECUPERO PRESIGNE URL OK "))
                   .onErrorResume(RuntimeException.class, throwable -> {
                	   log.error("createUriForDownloadFile() : erroe generico = {}", throwable.getMessage(), throwable);
                	   return Mono.error(throwable);
                   });
    }

    @NotNull
    private FileDownloadResponse getFileDownloadResponse(String fileKey, Document doc, Boolean metadataOnly) {
        FileDownloadResponse downloadResponse = new FileDownloadResponse();

        BigDecimal contentLength = doc.getContentLenght();

        downloadResponse.setChecksum(doc.getCheckSum() != null ? doc.getCheckSum() : null);
        downloadResponse.setContentLength(contentLength);
        downloadResponse.setContentType(doc.getContentType());
        // NOTA: deve essere restituito lo stato logico, piuttosto che lo stato tecnico
        //downloadResponse.setDocumentStatus(doc.getDocumentState().getValue());
        if (doc.getDocumentLogicalState() != null) {
            downloadResponse.setDocumentStatus(doc.getDocumentLogicalState());
        } else {
            downloadResponse.setDocumentStatus("");
        }
        log.info("getFileDownloadResponse() : documentState {} : documentLogicalState {}",
                 doc.getDocumentState(),
                 doc.getDocumentLogicalState());
        downloadResponse.setDocumentType(doc.getDocumentType().getTipoDocumento());

        downloadResponse.setKey(fileKey);
        
        if (doc.getRetentionUntil() != null && !doc.getRetentionUntil().isBlank()) {
        	try {
	        	final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
	        	downloadResponse.setRetentionUntil(new SimpleDateFormat(PATTERN_FORMAT).parse(doc.getRetentionUntil()));
	        }
        	catch (Exception e) {
        		log.error("getFileDownloadResponse() : errore = {}", e.getMessage(), e);
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
			}
        }
        log.info("getFileDownloadResponse() : doc.getRetentionUntil() = {} : downloadResponse.getRetentionUntil() = {}",
        		doc.getRetentionUntil() == null ? "puntatore null" : doc.getRetentionUntil() ,
        		downloadResponse.getRetentionUntil());

        downloadResponse.setVersionId(null);

        if (Boolean.FALSE.equals(metadataOnly) || metadataOnly == null) {
            if (doc.getDocumentState() == null ||
                    !( doc.getDocumentState().equalsIgnoreCase(technicalStatus_available)
                    ||doc.getDocumentState().equalsIgnoreCase(technicalStatus_attached)
                    ||doc.getDocumentState().equalsIgnoreCase(technicalStatus_freezed))){
                throw (new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Document : " + doc.getDocumentKey() +
                                " not has a valid state " ));
            }
            downloadResponse.setDownload(createFileDownloadInfo(fileKey,
            		doc.getDocumentState(),
                    downloadResponse.getDocumentType()));
        }
        return downloadResponse;
    }

    private Mono<Boolean> validationFieldCreateUri(String fileKey, String xPagopaSafestorageCxId) {
        return Mono.just(true);
    }

    private FileDownloadInfo createFileDownloadInfo(String fileKey, String status, String documentType) {
        FileDownloadInfo fileDOwnloadInfo = null;


            String bucketName = mapDocumentTypeToBucket.get(documentType);
            log.info("INIZIO RECUPERO URL DOWLOAND ");
            if (!status.equalsIgnoreCase(technicalStatus_freezed)) {
                fileDOwnloadInfo = getPresignedUrl( bucketName, fileKey);
            } else {
                fileDOwnloadInfo = recoverDocumentFromBucket( bucketName, fileKey);
            }


        return fileDOwnloadInfo;

    }

    private FileDownloadInfo recoverDocumentFromBucket( String bucketName, String keyName) {
        FileDownloadInfo fdinfo = new FileDownloadInfo();
        // mettere codice per far partire il recupero del file
        log.info("--- RESTORE DOCUMENT : " + keyName);

        try {
            log.info("--- CREATION S3 CLIENT DOCUMENT : " + keyName);
            AmazonS3 s3Client = getAmazonS3();

            // Create and submit a request to restore an object from Glacier for two days.
            log.info("--- REQUIRE RESTORE OBJECT DOCUMENT : " + keyName);
            RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, keyName, stayHotTime);
            log.info("--- RESTORE OBJECT DOCUMENT : " + keyName);
            s3Client.restoreObjectV2(requestRestore);

            // Check the restoration status of the object.
            ObjectMetadata response = s3Client.getObjectMetadata(bucketName, keyName);
            Boolean restoreFlag = response.getOngoingRestore();
            log.info("--- RENTION DATE "+response.getHttpExpiresDate() +" DOCUMENT "+keyName);
            log.info("Restoration status: %s.\n",restoreFlag ? "in progress" : "not in progress (finished or failed)");
        } catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            if (!ase.getErrorCode().equalsIgnoreCase("RestoreAlreadyInProgress")){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException - "+ase.getErrorMessage());
            }
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON SdkClientException - "+sce.getMessage());
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico "+e.getMessage());

        }

        fdinfo.setRetryAfter(MAX_RECOVER_COLD);
        return fdinfo;
    }



    private FileDownloadInfo getPresignedUrl( String bucketName, String keyName) {

        try {
            S3Presigner presigner = getS3Presigner();
            FileDownloadInfo fdinfo = new FileDownloadInfo();
            log.info("INIZIO CREAZIONE OGGETTO  GetObjectRequest");
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            		                                            .bucket(bucketName)
            		                                            .key(keyName)
            		                                            .overrideConfiguration(awsRequestOverrideConfiguration -> 
            		    		 										awsRequestOverrideConfiguration.putRawQueryParameter(queryParamPresignedUrlTraceId, keyName))
            		                                            .build();
            log.info("FINE  CREAZIONE OGGETTO  GetObjectRequest");
            log.info("INIZIO  CREAZIONE OGGETTO  GetObjectPresignRequest");
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(Long.parseLong(
                            duration)))
                    .getObjectRequest(getObjectRequest)
                    .build();
            log.info("FINE  CREAZIONE OGGETTO  GetObjectPresignRequest");

            log.info("INIZIO  RECUPERO URL ");
            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
            log.info("FINE   RECUPERO URL ");

            String theUrl = presignedGetObjectRequest.url().toString();
            fdinfo.setUrl(theUrl);
            return fdinfo;

        }
        catch (AmazonServiceException ase) {
            log.error(" Errore AMAZON AmazonServiceException", ase);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException - "+ase.getMessage());
        } catch (SdkClientException sce) {
            log.error(" Errore AMAZON SdkClientException", sce);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore AMAZON AmazonServiceException - "+ sce.getMessage());
        } catch (Exception e) {
            log.error(" Errore Generico", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Generico ");

        }

    }

    private String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[256];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }
}
