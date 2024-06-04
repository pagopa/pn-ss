package it.pagopa.pnss.utils;

import it.pagopa.pnss.configurationproperties.BucketName;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.FileInputStream;
import java.io.IOException;

@TestPropertySource(properties = {"pn.ss.ignored.update.metadata.list=s3://${s3.bucket.ss-hot-name}/ignored-update-metadata.csv"})
@SpringBootTest
public class IgnoredUpdateMetadataConfigTestSetup {
    private static final String fileKey = "ignored-update-metadata.csv";

    @BeforeAll
    public static void beforeAll(@Autowired S3Client s3TestClient, @Autowired BucketName bucketName) throws IOException {
        byte[] fileBytes;
        try (FileInputStream fileInputStream = new FileInputStream("src/test/resources/configuration/ignored-update-metadata.csv")) {
            fileBytes = fileInputStream.readAllBytes();
        }
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName.ssHotName()).key(fileKey).contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
        s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
    }

}
