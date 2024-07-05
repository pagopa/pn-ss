package it.pagopa.pnss.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.pagopa.pnss.common.exception.MissingIndexingLimitsException;
import it.pagopa.pnss.common.exception.MissingTagException;
import it.pagopa.pnss.common.model.pojo.IndexingLimits;
import it.pagopa.pnss.common.model.pojo.IndexingTag;
import it.pagopa.pnss.common.utils.JsonUtils;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.ssm.SsmAsyncClient;

import java.io.FileNotFoundException;
import java.io.FileReader;

@SpringBootTestWebEnv
@CustomLog
class IndexingConfigurationTest {

    @Autowired
    SsmAsyncClient ssmAsyncClient;
    @Autowired
    JsonUtils jsonUtils;
    @Value("${pn.ss.indexing.configuration.name}")
    String indexingConfigurationName;
    private static final String IUN = "IUN";

    @BeforeAll
    static void setup() {
        System.setProperty("pn.ss.indexing.configuration.test", "true");
    }

    @Nested
    class TestDefaultConfiguration {

        @BeforeAll
        static void setup(@Autowired SsmAsyncClient ssmAsyncClient, @Value("${pn.ss.indexing.configuration.name}") String indexingConfigurationName) throws FileNotFoundException {
            log.info("Setting up default configuration for indexing...");
            String jsonString = parseJsonFile("src/test/resources/indexing/json/indexing-configuration-default.json");
            ssmAsyncClient.putParameter(builder -> builder.name(indexingConfigurationName).type("String").value(jsonString));
        }

        @Test
        void testDefaultConfigurationOk() {
            IndexingConfiguration indexingConfiguration = new IndexingConfiguration(ssmAsyncClient, jsonUtils, indexingConfigurationName);
            indexingConfiguration.init();

            // Checking correct parsing of json string.
            Assertions.assertEquals(3, indexingConfiguration.getGlobalTags().size());
            Assertions.assertEquals(2, indexingConfiguration.getLocalTags().size());
            assertAllLimitsNotNull(indexingConfiguration.getIndexingLimits());

            // Checking correct usage of isTagValid() method.
            Assertions.assertTrue(indexingConfiguration.isTagValid(IUN));
            Assertions.assertFalse(indexingConfiguration.isTagValid("NonExistingTag"));

            // Checking correct usage of getTagInfo() method.
            IndexingTag tagInfo = indexingConfiguration.getTagInfo(IUN);
            Assertions.assertNotNull(tagInfo);
            Assertions.assertEquals(IUN, tagInfo.getKey());
            Assertions.assertTrue(tagInfo.isIndexed());
            Assertions.assertTrue(tagInfo.isMultivalue());

        }
    }

    @Nested
    class TestEmptyTags {


        @BeforeAll
        static void setup(@Autowired SsmAsyncClient ssmAsyncClient, @Value("${pn.ss.indexing.configuration.name}") String indexingConfigurationName) throws FileNotFoundException {
            log.info("Setting up empty tags configuration for indexing...");
            String jsonString = parseJsonFile("src/test/resources/indexing/json/indexing-configuration-empty-tags.json");
            ssmAsyncClient.putParameter(builder -> builder.name(indexingConfigurationName).type("String").value(jsonString));
        }

        @Test
        void testEmptyTagsOk() {
            IndexingConfiguration indexingConfiguration = new IndexingConfiguration(ssmAsyncClient, jsonUtils, indexingConfigurationName);
            indexingConfiguration.init();

            // Checking correct parsing of json string.
            Assertions.assertEquals(0, indexingConfiguration.getGlobalTags().size());
            Assertions.assertEquals(0, indexingConfiguration.getLocalTags().size());
            assertAllLimitsNotNull(indexingConfiguration.getIndexingLimits());

            // Checking correct usage of isTagValid() method.
            Assertions.assertFalse(indexingConfiguration.isTagValid(IUN));

            // Checking correct usage of getTagInfo() method.
            Assertions.assertThrows(MissingTagException.class, () -> indexingConfiguration.getTagInfo(IUN));
        }
    }

    @Nested
    class TestMissingLimits {

        @BeforeAll
        static void setup(@Autowired SsmAsyncClient ssmAsyncClient, @Value("${pn.ss.indexing.configuration.name}") String indexingConfigurationName) throws FileNotFoundException {
            log.info("Setting up missing limits configuration for indexing...");
            String jsonString = parseJsonFile("src/test/resources/indexing/json/indexing-configuration-missing-limits.json");
            ssmAsyncClient.putParameter(builder -> builder.name(indexingConfigurationName).type("String").value(jsonString));
        }

        @Test
        void testMissingLimitsKo() {
            IndexingConfiguration indexingConfiguration = new IndexingConfiguration(ssmAsyncClient, jsonUtils, indexingConfigurationName);
            Assertions.assertThrows(MissingIndexingLimitsException.class, indexingConfiguration::init);
        }
    }

    private void assertAllLimitsNotNull(IndexingLimits indexingLimits) {
        Assertions.assertNotNull(indexingLimits);
        Assertions.assertNotNull(indexingLimits.getMaxFileKeysUpdateMassivePerRequest());
        Assertions.assertNotNull(indexingLimits.getMaxFileKeys());
        Assertions.assertNotNull(indexingLimits.getMaxTagsPerRequest());
        Assertions.assertNotNull(indexingLimits.getMaxMapValuesForSearch());
        Assertions.assertNotNull(indexingLimits.getMaxTagsPerDocument());
        Assertions.assertNotNull(indexingLimits.getMaxOperationsOnTagsPerRequest());
        Assertions.assertNotNull(indexingLimits.getMaxValuesPerTagDocument());
        Assertions.assertNotNull(indexingLimits.getMaxValuesPerTagPerRequest());
    }

    private static String parseJsonFile(String filePath) throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        var fileReader = new FileReader(filePath);
        Object json = gson.fromJson(fileReader, Object.class);
        return gson.toJson(json);
    }


}
