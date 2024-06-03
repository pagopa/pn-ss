package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.S3Service;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.FileInputStream;
import java.io.IOException;

@SpringBootTestWebEnv
public class IgnoredUpdateMetadataConfigTestSetup {
    private static final String fileKey = "ignored-update-metadata.csv";
    public static IgnoredUpdateMetadataConfig ignoredUpdateMetadataConfig;

    @BeforeAll
    public static void beforeAll(@Autowired S3Client s3TestClient, @Autowired BucketName bucketName, @Autowired S3Service s3Service) throws IOException, InterruptedException {
        //Using constructor and explicit call to init() method to avoid bean initialization issues.
        ignoredUpdateMetadataConfig = new IgnoredUpdateMetadataConfig("s3://" + bucketName.ssHotName() + "/" + fileKey, s3Service);

        byte[] fileBytes;
        try (FileInputStream fileInputStream = new FileInputStream("src/test/resources/ignored-update-metadata.csv")) {
            fileBytes = fileInputStream.readAllBytes();
        }
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName.ssHotName()).key(fileKey).contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
        s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));

        ignoredUpdateMetadataConfig.init();
    }

}
