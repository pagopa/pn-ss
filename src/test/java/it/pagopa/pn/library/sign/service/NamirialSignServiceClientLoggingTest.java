package it.pagopa.pn.library.sign.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.namirial.sign.library.service.PnSignServiceImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NamirialSignServiceClientLoggingTest {

    private static final String SIGN_CLIENT_LOGGER = "com.namirial.sign.library.service.SignServiceClient";

    @TempDir
    Path tempDir;

    private MockWebServer mockWebServer;
    private PnSignServiceImpl pnSignServiceImpl;
    private Logger signClientLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        System.setProperty("namirial.server.address", "http://localhost:" + mockWebServer.getPort());
        System.setProperty("namirial.server.max-connections", "10");
        System.setProperty("namirial.server.pending-acquire-timeout", "30");
        pnSignServiceImpl = new PnSignServiceImpl();

        signClientLogger = (Logger) LoggerFactory.getLogger(SIGN_CLIENT_LOGGER);
        signClientLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        signClientLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() throws IOException {
        signClientLogger.detachAppender(listAppender);
        listAppender.stop();
        mockWebServer.shutdown();
    }

    @Test
    void pkcs7Signature_namirialResponse_logsXSignboxTransactionId() throws IOException {
        String transactionId = UUID.randomUUID().toString();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader("X-SIGNBOX-TRANSACTION-ID", transactionId)
                .setBody("{}"));

        StepVerifier.create(pnSignServiceImpl.pkcs7Signature("test-content".getBytes(), false))
                .expectError()
                .verify(Duration.ofSeconds(10));

        Path logFile = tempDir.resolve("namirial-sign-client.log");
        List<String> logLines = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        Files.write(logFile, logLines);

        String logContent = Files.readString(logFile);
        assertThat(logContent).contains(transactionId);
    }
}
