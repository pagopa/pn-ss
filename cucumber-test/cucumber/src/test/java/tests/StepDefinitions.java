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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.uribuilder.rest.FileMetadataUpdateApiController;
import it.pagopa.pnss.uribuilder.rest.FileUploadApiController;
import it.pagopa.pnss.uribuilder.service.FileMetadataUpdateService;
import lombok.CustomLog;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
	private String sPNClientUp = null;
	private String sPNClient_AKUp = null;
	private String status = null;
	private String retentionUntil = "";

	private Date retentionDate;
	UpdateFileMetadataRequest requestBody = new UpdateFileMetadataRequest();

	
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

	@Given("{string} authenticated by {string} try to update the document using {string} and {string} but has invalid or null {string}")
	public void no_file_to_update (String sPNClientUp, String sPNClient_AKUp, String status, String retentionUntil, String fileKey) {

		this.status = status;
		this.retentionUntil = retentionUntil;
		this.sPNClientUp = sPNClientUp;
		this.sPNClient_AKUp = sPNClient_AKUp;
		if (fileKey != null && !fileKey.isEmpty()) {
			this.sKey = fileKey;
		} else {
			this.sKey = "";
		}

		System.out.println("Client: "+sPNClientUp);

		Response oResp;

		if (retentionUntil != null && !retentionUntil.isEmpty()) {
			requestBody.setRetentionUntil(Date.from(Instant.parse(retentionUntil)));
		}
		requestBody.setStatus(status);

		CommonUtils.checkDump(oResp=SafeStorageUtils.updateObjectMetadata(sPNClientUp, sPNClient_AKUp, fileKey, requestBody), true);
		iRC = oResp.getStatusCode();
		System.out.println("FILE KEY: " + fileKey);
		System.out.println("NEW STATUS: "+status);
		System.out.println("NEW RETENTION UNTIL: "+retentionUntil);
	}

	@When ("{string} authenticated by {string} try to update the document just uploaded using {string} and {string}")
	public void a_file_to_update (String sPNClientUp, String sPNClient_AKUp, String status, String retentionUntil) {

		this.status = status;
		this.retentionUntil = retentionUntil;
		this.sPNClientUp = sPNClientUp;
		this.sPNClient_AKUp = sPNClient_AKUp;

		System.out.println("Client: "+sPNClientUp);

		Response oResp;

		if (retentionUntil != null && !retentionUntil.isEmpty()) {
			requestBody.setRetentionUntil(Date.from(Instant.parse(retentionUntil)));
		}
		requestBody.setStatus(status);

		CommonUtils.checkDump(oResp= SafeStorageUtils.updateObjectMetadata(sPNClientUp, sPNClient_AKUp, sKey, requestBody), true);
		iRC = oResp.getStatusCode();
		System.out.println("NEW STATUS: "+status);
		System.out.println("NEW RETENTION UNTIL: "+retentionUntil);

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

	@Then("i check that the document got updated")
	public void metadata_changed() throws JsonMappingException, JsonProcessingException, InterruptedException {

		Response oResp;
		iRC = 0;
		while ( iRC != 200 ) {
			oResp = SafeStorageUtils.getObjectMetadata(sPNClientUp, sPNClient_AKUp, sKey);
			iRC = oResp.getStatusCode();
			if( iRC == 200 ) {
				ObjectMapper objectMapper = new ObjectMapper();
				System.out.println(oResp.getBody().asString());
				FileDownloadResponse oFDR = objectMapper.readValue(oResp.getBody().asString(), FileDownloadResponse.class);
				System.out.println(oFDR);
				if (retentionUntil != null && !retentionUntil.isEmpty()) {
					retentionDate= Date.from(Instant.parse(retentionUntil));
				}

				System.out.println("RetentionDate: "+retentionDate);
				System.out.println("Status: "+status);
				boolean condition = false;

				if (oFDR.getDocumentStatus().equalsIgnoreCase(status)) {
					condition = true;
				} else if (oFDR.getRetentionUntil().toInstant().truncatedTo(ChronoUnit.SECONDS).equals(retentionDate.toInstant().truncatedTo(ChronoUnit.SECONDS))) {
					condition = true;
				}
				assertTrue(condition);

				}
			Thread.sleep(3000);
		}
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
