package tests;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtils {

	private static String baseURL = null;
	private static String [] asMimeType = {"application/pdf","application/xml", "application/zip", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "message/rfc822"};

	protected static String getBaseURL() {
		if( baseURL == null ) {
			baseURL = Config.getInstance().getBaseUrl();
			if( baseURL == null ) {
				baseURL="";
			}
		}
		return baseURL;
	}
	
	protected static int checkDump(Response oResp, boolean boDumpBody) {
		int iRc = oResp.getStatusCode();
		
		if(boDumpBody) {
			if( log.isDebugEnabled() ) oResp.then().log().all();
		}
		return iRc;
	}

	public static Response uploadFile(String sURL, File oFile, String sSHA256, String sMD5, String sContentType, String sSecret, Checksum eCS) throws MalformedURLException, UnsupportedEncodingException {
		log.info("In upload file");

		log.debug("uploadFile(\"{}\", \"{}\", \"{}\", \"{}\", \"{}\", "+eCS.name()+")", sURL, sSHA256, sMD5, sContentType, sSecret);
		EncoderConfig encoderConfig = new EncoderConfig();
		RequestSpecification oReq = given()
			.config(RestAssured.config()
                    .encoderConfig(encoderConfig.appendDefaultContentCharsetToContentTypeIfUndefined(false)))
			.header("content-type", sContentType);
		switch (eCS) {
			case MD5:
				log.info("In upload file CASE 1");

				oReq.header("Content-MD5", sMD5);
				break;
			case SHA256:
				log.info("In upload file CASE 2");

				oReq.header("x-amz-checksum-sha256", sSHA256);
				break;
			default:
				log.info("In upload file DEFAULT");

				break;
		}
		if( sSecret != null ) {
			log.info("In upload file IF");

			oReq.header("x-amz-meta-secret", sSecret);
		}
		oReq.body(oFile);
		log.info("In upload file");

		if (log.isDebugEnabled() ) {
			log.info("In upload file IF 2");

			oReq.log().all();
		}
		String sMyURL = URLDecoder.decode(sURL, "utf-8");
		Response oResp = oReq
			.put(sMyURL);
		log.debug("In upload file oResp --> " + oResp.getBody().asString());

		return oResp;
	}

	protected static Response myGet(RequestSpecification oReqSpec, String sURI) {
		oReqSpec.given().baseUri(getBaseURL()).basePath(sURI);
		QueryableRequestSpecification queryRequest = SpecificationQuerier.query(oReqSpec);
		log.debug("GET {}", queryRequest.getURI());
		Response oResp = oReqSpec.get();
		log.debug("RC: {}", oResp.getStatusCode());
		return oResp;
	}
	
	protected static Response myPost(RequestSpecification oReqSpec, String sURI) {
		oReqSpec.given().baseUri(getBaseURL()).basePath(sURI);
		QueryableRequestSpecification queryRequest = SpecificationQuerier.query(oReqSpec);
		log.debug("POST ", queryRequest.getURI());
		log.debug(queryRequest.getBody().toString());
		Response oResp = oReqSpec.post();
		return oResp;
	}
	
}
