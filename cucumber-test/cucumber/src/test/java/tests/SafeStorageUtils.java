package tests;

import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UpdateFileMetadataRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SafeStorageUtils {
	
	protected static RequestSpecification stdReq() {
		return given()
				.header("Accept", "application/json")
				.header("Content-type", "application/json")
				.header("x-amzn-trace-id", java.util.UUID.randomUUID().toString());
	}

	protected static RequestSpecification stdReqKo() {
		return given()
				.header("Accept", "application/json")
				.header("Content-type", "application/json");
				//.header("x-amzn-trace-id", java.util.UUID.randomUUID().toString());
	}

	public static Response getPresignedURLUpload(String sCxId, String sAPIKey, String sContentType, String sDocType, String sSHA256, String sMD5, String sStatus, boolean boHeader, Checksum eCS) {
		log.debug("getPresignedURLUpload(\"{}\",\"{}\",\"{}\", \"{}\", \"{}\", \"{}\", \"{}\", {}, {})", sCxId, sAPIKey, sContentType, sDocType, sSHA256, sMD5, sStatus, (boHeader?"header":"body"), eCS.name());		
		RequestSpecification oReq = stdReq() 
			.header("x-pagopa-safestorage-cx-id", sCxId)
			.header("x-api-key", sAPIKey);
		if( boHeader ) {
			switch (eCS) {
				case MD5:
					oReq.header("x-checksum-value", sMD5);
					break;	
				case SHA256:
					oReq.header("x-checksum-value", sSHA256);
					break;	
				default:
					break;
			}
		}
		String sBody = "{ \"contentType\": \"" + sContentType+ "\", \"documentType\": \"" + sDocType + "\", \"status\": \"" + sStatus + "\"";
		if( !boHeader) {
			switch (eCS) {
			case MD5:
				sBody += ", \"checksumValue\": \""+sMD5+"\"";
				break;	
			case SHA256:
				sBody += ", \"checksumValue\": \""+sSHA256+"\"";
				break;	
			default:
				break;
			}
		}

		sBody += "}";
		oReq.body(sBody);
		
		Response oResp = CommonUtils.myPost(oReq, "/safe-storage/v1/files"); 
		return oResp;
	}

	public static Response getPresignedURLUploadKo(String sCxId, String sAPIKey, String sContentType, String sDocType, String sSHA256, String sMD5, String sStatus, boolean boHeader, Checksum eCS) {
		log.debug("getPresignedURLUpload(\"{}\",\"{}\",\"{}\", \"{}\", \"{}\", \"{}\", \"{}\", {}, {})", sCxId, sAPIKey, sContentType, sDocType, sSHA256, sMD5, sStatus, (boHeader?"header":"body"), eCS.name());
		RequestSpecification oReq = stdReqKo()
				.header("x-pagopa-safestorage-cx-id", sCxId)
				.header("x-api-key", sAPIKey);
		if( boHeader ) {
			switch (eCS) {
				case MD5:
					oReq.header("x-checksum-value", sMD5);
					break;
				case SHA256:
					oReq.header("x-checksum-value", sSHA256);
					break;
				default:
					break;
			}
		}
		String sBody = "{ \"contentType\": \"" + sContentType+ "\", \"documentType\": \"" + sDocType + "\", \"status\": \"" + sStatus + "\"";
		if( !boHeader) {
			switch (eCS) {
				case MD5:
					sBody += ", \"checksumValue\": \""+sMD5+"\"";
					break;
				case SHA256:
					sBody += ", \"checksumValue\": \""+sSHA256+"\"";
					break;
				default:
					break;
			}
		}

		sBody += "}";
		oReq.body(sBody);

		Response oResp = CommonUtils.myPost(oReq, "/safe-storage/v1/files");
		return oResp;
	}


	public static Response getPresignedURLDownload(String sCxId, String sAPIKey, String sFileKey) {
		log.debug("getPresignedURLDownload(\"{}\",\"{}\",\"{}\")", sCxId, sAPIKey, sFileKey);		
		RequestSpecification oReq = stdReq() 
			.header("x-pagopa-safestorage-cx-id", sCxId)
			.header("x-api-key", sAPIKey);
		
		Response oResp = CommonUtils.myGet(oReq, "/safe-storage/v1/files/"+sFileKey); 
		return oResp;
	}

	public static Response getObjectMetadata(String sCxId, String sAPIKey, String sFileKey) {
		log.debug("getObjectMetadata(\"{}\",\"{}\",\"{}\")", sCxId, sAPIKey, sFileKey);		
		RequestSpecification oReq = stdReq() 
			.header("x-pagopa-safestorage-cx-id", sCxId)
			.header("x-api-key", sAPIKey)
			.pathParam("fileKey", sFileKey)
			.param("metadataOnly", true);
		
		Response oResp = CommonUtils.myGet(oReq, "/safe-storage/v1/files/{fileKey}"); 
		return oResp;
	}

	public static Response updateObjectMetadata (String sCxId, String sAPIKey, String sFileKey, UpdateFileMetadataRequest requestBody) {

		ObjectMapper objMapper = new ObjectMapper();
		String body = "";

		try {
			body = objMapper.writeValueAsString(requestBody);
		} catch (JsonProcessingException jpe) {
			// decidere come gestire eccezione
		}

		RequestSpecification oReq = stdReq()
				.header("x-pagopa-safestorage-cx-id", sCxId)
				.header("x-api-key", sAPIKey)
				.pathParam("fileKey", sFileKey)
				.body(body);

		Response oResp = CommonUtils.myPost(oReq, "/safe-storage/v1/files/{fileKey}");

		System.out.println("status: "+oResp.getStatusCode());

		return oResp;
	}


}
