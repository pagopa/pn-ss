package it.pagopa.pnss.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.common.client.impl.UserConfigurationClientCallImpl;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTestWebEnv
public class UserConfigurationClientCallTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UserConfigurationClientCall userConfigurationClientCall;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        System.setProperty("internal.base.url", String.format("http://localhost:%s", mockBackEnd.getPort()));
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getUserOk() throws JsonProcessingException {
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(new UserConfigurationResponse())).addHeader("Content-Type", "application/json"));

        Mono<UserConfigurationResponse> getUserConfiguration = userConfigurationClientCall.getUser("user");
        StepVerifier.create(getUserConfiguration).expectNextCount(1).verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 409})
    void getUser4xx(int responseCode) {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(responseCode));

        Mono<UserConfigurationResponse> getUserConfiguration = userConfigurationClientCall.getUser("user");
        StepVerifier.create(getUserConfiguration).expectError(IdClientNotFoundException.class).verify();
    }

    @Test
    void postUserOk() {
        Assertions.assertNull(userConfigurationClientCall.postUser(new UserConfiguration()));
    }

    @Test
    void updateUserOk() {
        Assertions.assertNull(userConfigurationClientCall.updateUser(new UserConfiguration()));
    }

    @Test
    void deleteUserOk() {
        Assertions.assertNull(userConfigurationClientCall.deleteUser("user"));
    }

}
