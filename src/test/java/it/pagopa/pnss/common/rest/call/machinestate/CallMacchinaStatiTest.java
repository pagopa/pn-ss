package it.pagopa.pnss.common.rest.call.machinestate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTestWebEnv
public class CallMacchinaStatiTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private CallMacchinaStati callMacchinaStati;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        System.setProperty("internal-endpoint.state-machine.container-base-url", String.format("http://localhost:%s", mockBackEnd.getPort()));
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void statusValidationOk() throws JsonProcessingException {
        MacchinaStatiValidateStatoResponseDto response = new MacchinaStatiValidateStatoResponseDto();
        response.setAllowed(true);
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(response)).addHeader("Content-Type", "application/json"));

        Mono<MacchinaStatiValidateStatoResponseDto> statusValidation = callMacchinaStati.statusValidation(new DocumentStatusChange());
        StepVerifier.create(statusValidation).expectNextCount(1).verifyComplete();
    }

    @Test
    void statusValidationKo() throws JsonProcessingException {
        MacchinaStatiValidateStatoResponseDto response = new MacchinaStatiValidateStatoResponseDto();
        response.setAllowed(false);
        mockBackEnd.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(response)).addHeader("Content-Type", "application/json"));

        Mono<MacchinaStatiValidateStatoResponseDto> statusValidation = callMacchinaStati.statusValidation(new DocumentStatusChange());
        StepVerifier.create(statusValidation).expectError(InvalidNextStatusException.class).verify();
    }

}
