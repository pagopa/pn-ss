package tests;

import io.cucumber.java.After;
import io.cucumber.java.en.*;
import io.restassured.response.Response;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileDownloadResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.*;

public class StepDefinitions {
	private String sPNClient = null;
	private String sPNClient_AK = null;
	private String sDocumentType = null;
	private String sSHA256 = null;
	private String sMD5 = null;
	private static boolean boHeader = true;
	private String sURL = null;
	private String sKey = null;
	private String sSecret = null;
	private String sMimeType = null;
	private int iRC = 0;
	File oFile = null;
	
	@Given("{string} authenticated by {string} try to upload a document of type {string} with content type {string} using {string}")
	public void a_file_to_upload(String sPNClient, String sPNClient_AK, String sDocumentType, String sMimeType, String sFileName) throws NoSuchAlgorithmException, FileNotFoundException, IOException {

		Path pathFile = Paths.get(sFileName).toAbsolutePath();
		System.out.println("filePath: " + pathFile);

		this.sPNClient = sPNClient;
		this.sPNClient_AK = sPNClient_AK;
		this.sDocumentType = sDocumentType;
		this.sMimeType = sMimeType;
		
		oFile = new File(sFileName);
		FileInputStream oFIS = new FileInputStream(oFile);
		byte[] baFile = oFIS.readAllBytes();
		oFIS.close();
		MessageDigest md = MessageDigest.getInstance("SHA256");
	    md.update(baFile);
	    byte[] digest = md.digest();
	    sSHA256=Base64.getEncoder().encodeToString(digest);
	    
		md = MessageDigest.getInstance("MD5");
	    md.update(baFile);
	    digest = md.digest();
	    sMD5=Base64.getEncoder().encodeToString(digest);
	}
	
	@When("request a presigned url to upload the file")
	public void getUploadPresignedURL() {
		Response oResp;

		CommonUtils.checkDump(oResp=SafeStorageUtils.getPresignedURLUpload(sPNClient, sPNClient_AK, sMimeType, sDocumentType, sSHA256, sMD5, "SAVED", boHeader, Checksum.SHA256), true);
		System.out.println("CLIENT: " + sPNClient);

		iRC = oResp.getStatusCode();
		System.out.println("oResp body: " + oResp.getBody().asString());
		System.out.println("oResp uploadUrl: " + oResp.then().extract().path("uploadUrl"));
		System.out.println("oResp key: " + oResp.then().extract().path("key"));
		System.out.println("oResp secret: " + oResp.then().extract().path("secret"));
		System.out.println("iRC: " + iRC);
		if(iRC == 200) {
			sURL = oResp.then().extract().path("uploadUrl");
			sKey = oResp.then().extract().path("key");
			sSecret = oResp.then().extract().path("secret");
		}
	}

	@When("request a presigned url to upload the file without traceId")
	public void getUploadPresignedURLWithoutTraceId() {
		Response oResp;

		CommonUtils.checkDump(oResp=SafeStorageUtils.getPresignedURLUploadKo(sPNClient, sPNClient_AK, sMimeType, sDocumentType, sSHA256, sMD5, "SAVED", boHeader, Checksum.SHA256), true);
		System.out.println("CLIENT: " + sPNClient);

		iRC = oResp.getStatusCode();
		System.out.println("oResp body: " + oResp.getBody().asString());
		System.out.println("oResp uploadUrl: " + oResp.then().extract().path("uploadUrl"));
		System.out.println("oResp key: " + oResp.then().extract().path("key"));
		System.out.println("oResp secret: " + oResp.then().extract().path("secret"));
		System.out.println("iRC: " + iRC);
		if(iRC == 200) {
			sURL = oResp.then().extract().path("uploadUrl");
			sKey = oResp.then().extract().path("key");
			sSecret = oResp.then().extract().path("secret");
		}
	}


	@When("upload that file")
	public void uploadFile() throws MalformedURLException, UnsupportedEncodingException {
		System.out.println("sURL: " + sURL);
		Assertions.assertNotNull( sURL );
		iRC =  CommonUtils.uploadFile(sURL, oFile, sSHA256, sMD5, sMimeType, sSecret, Checksum.SHA256).getStatusCode();
	}

	@When("it's available")
	public void it_s_available() throws JsonMappingException, JsonProcessingException, InterruptedException {
		Response oResp;
		iRC = 0;
		while ( iRC != 200 ) {
			oResp = SafeStorageUtils.getObjectMetadata(sPNClient, sPNClient_AK, sKey);
			iRC = oResp.getStatusCode();
			if( iRC == 200 ) {
				ObjectMapper objectMapper = new ObjectMapper();
				System.out.println(oResp.getBody().asString());
				FileDownloadResponse oFDR = objectMapper.readValue(oResp.getBody().asString(), FileDownloadResponse.class);
				if( oFDR.getDocumentStatus().equalsIgnoreCase("SAVED") || oFDR.getDocumentStatus().equalsIgnoreCase("PRELOADED")) {
					break;
				}
			}
			Thread.sleep(3000);
		}
	}
	
	@Then("i found in S3")
    public void i_found_in_s3() {
		Assertions.assertEquals( 200, CommonUtils.checkDump(SafeStorageUtils.getPresignedURLDownload(sPNClient, sPNClient_AK, sKey), true)); // Ok
    }
	
	@Then("i get an error {string}")
	public void i_get_an_error(String sRC) {
		Assertions.assertEquals( Integer.parseInt(sRC), iRC);
		
	}

	@Then("i get an error with client {string}")
	public void verifyErrorMessage(String sRC) {
		// Add code to check if the response contains the expected error message
		// You might use assertions or other validation mechanisms here
	}
	
	@After
	public void doFinally() throws IOException {
	}


}
