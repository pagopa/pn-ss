package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.exception.FileNotModifiedException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.utils.IgnoredUpdateMetadataConfigTestSetup;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTestWebEnv
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@CustomLog
class IgnoredUpdateMetadataConfigTest extends IgnoredUpdateMetadataConfigTestSetup {

    @Autowired
    private IgnoredUpdateMetadataConfig ignoredUpdateMetadataConfig;
    @Autowired
    private S3Client s3TestClient;
    @Autowired
    private BucketName bucketName;
    private String defaultBucketName;
    private String defaultFileName;
    private static final String FILE_KEY = "ignored-update-metadata.csv";

    @BeforeEach
    void beforeEach()
    {
        this.defaultBucketName = (String) ReflectionTestUtils.getField(ignoredUpdateMetadataConfig, "bucketName");
        this.defaultFileName = (String) ReflectionTestUtils.getField(ignoredUpdateMetadataConfig, "ignoredUpdateMetadataFileName");
    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(ignoredUpdateMetadataConfig, "bucketName", defaultBucketName);
        ReflectionTestUtils.setField(ignoredUpdateMetadataConfig, "ignoredUpdateMetadataFileName", defaultFileName);
    }

    @Test
    @Order(1)
    void testRefreshIgnoredUpdateMetadataListOk() {
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextMatches(size -> size.equals(5)).verifyComplete();
    }

    @Test
    @Order(2)
    void testRefreshIgnoredUpdateMetadataList_NonUpdatedFile() {
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectError(FileNotModifiedException.class).verify();
    }

    @Test
    @Order(3)
    void testRefreshIgnoredUpdateMetadataList_NonExistentFileKey() {
        ReflectionTestUtils.setField(ignoredUpdateMetadataConfig, "ignoredUpdateMetadataFileName", "nonExistentFileKey");
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextMatches(size -> size.equals(0)).verifyComplete();
    }

    @Test
    @Order(4)
    void testRefreshIgnoredUpdateMetadataList_NonExistentBucket() {
        ReflectionTestUtils.setField(ignoredUpdateMetadataConfig, "bucketName", "nonExistentBucket");
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextMatches(size -> size.equals(0)).verifyComplete();
    }

    @Test
    @Order(5)
    void testRefreshIgnoredUpdateMetadataList_EmptyFile() throws InterruptedException {
        Thread.sleep(1000);
        byte[] fileBytes = new byte[0];
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName.ssHotName()).key(FILE_KEY).contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
        s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
        Mono<Integer> fluxToTest = ignoredUpdateMetadataConfig.refreshIgnoredUpdateMetadataList();
        StepVerifier.create(fluxToTest).expectNextMatches(size -> size.equals(0)).verifyComplete();
    }

}
