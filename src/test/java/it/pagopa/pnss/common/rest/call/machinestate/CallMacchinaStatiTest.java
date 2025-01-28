package it.pagopa.pnss.common.rest.call.machinestate;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.exception.StateMachineServiceException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTestWebEnv
class CallMacchinaStatiTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CallMacchinaStati callMacchinaStati;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry r) {
        // Overriding of internal base url property to point to mock server
        r.add("internal-endpoint.state-machine.container-base-url", () -> "http://localhost:" + mockBackEnd.getPort());
    }

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @AfterEach
    public void afterEach() {
        // Setting default dispatcher after every test
        mockBackEnd.setDispatcher(new QueueDispatcher());
    }

    @Test
    void statusValidationOk() {
        //GIVEN
        MacchinaStatiValidateStatoResponseDto response = new MacchinaStatiValidateStatoResponseDto();
        response.setAllowed(true);

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(response));

        //THEN
        Mono<MacchinaStatiValidateStatoResponseDto> statusValidation = callMacchinaStati.statusValidation(new DocumentStatusChange());
        StepVerifier.create(statusValidation).expectNextMatches(MacchinaStatiValidateStatoResponseDto::isAllowed).verifyComplete();
    }

    @Test
    void statusValidationKo() {
        //GIVEN
        MacchinaStatiValidateStatoResponseDto response = new MacchinaStatiValidateStatoResponseDto();
        response.setAllowed(true);

        //WHEN
        Dispatcher mDispatcher = new Dispatcher() {
            @Override
            public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
                return buildMockResponse().setResponseCode(500);
            }
        };
        mockBackEnd.setDispatcher(mDispatcher);

        //THEN
        Mono<MacchinaStatiValidateStatoResponseDto> statusValidation = callMacchinaStati.statusValidation(new DocumentStatusChange());
        StepVerifier.create(statusValidation).expectError(StateMachineServiceException.class).verify();
    }

    @SneakyThrows
    private <T> MockResponse buildMockResponse(T body) {
        return new MockResponse().setBody(objectMapper.writeValueAsString(body)).addHeader("Content-Type", "application/json");
    }

    @SneakyThrows
    private MockResponse buildMockResponse() {
        return buildMockResponse("{}");
    }

}
