package com.spreedly.composer;

import com.google.gson.GsonBuilder;
import com.spreedly.composer.messages.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for the safe logging default: an integrator who passes no
 * log level must never see request bodies, response bodies or event IPs in their logs.
 */
public class LoggingDefaultsTest {

    private static final String EVENT_IP = "203.0.113.7";
    private static final String EVENT_EMAIL = "shopper@example.com";

    private MockWebServer mockServer;
    private Logger.Severity originalFilterLevel;
    private PrintStream originalOut;
    private ByteArrayOutputStream captured;

    @BeforeEach
    public void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();

        originalFilterLevel = Logger.filterLevel;
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.setOut(originalOut);
        Logger.filterLevel = originalFilterLevel;

        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    @Test
    public void defaultLogLevelEmitsNothingForACheckpoint() throws Exception {
        String output = runCheckpoint(null);

        assertThat(Logger.filterLevel).isEqualTo(Logger.Severity.INFO);
        assertThat(output).isEmpty();
    }

    @Test
    public void traceLogLevelLogsRedactedRequestAndResponsePayloads() throws Exception {
        String output = runCheckpoint(Logger.ComposerLogLevel.TRACE);

        assertThat(output).contains("Request body: ");
        assertThat(output).contains(Constants.VerificationStatus.COMPLETE);
        assertThat(output).contains("[REDACTED]");
        assertThat(output).doesNotContain(EVENT_IP);
        assertThat(output).doesNotContain(EVENT_EMAIL);
    }

    @Test
    public void isTraceEnabledTracksTheFilterLevel() {
        Logger.filterLevel = Logger.Severity.INFO;
        assertThat(Logger.isTraceEnabled()).isFalse();

        Logger.filterLevel = Logger.Severity.TRACE;
        assertThat(Logger.isTraceEnabled()).isTrue();
    }

    /**
     * Runs a checkpoint against a stubbed API and returns everything the SDK logged.
     */
    private String runCheckpoint(Logger.ComposerLogLevel logLevel) throws Exception {
        Map<String, Object> endUserData = new HashMap<>();
        endUserData.put("email", EVENT_EMAIL);

        CheckpointResponse stubbedResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "verification123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                endUserData
            ),
            false
        );

        mockServer.enqueue(new MockResponse()
            .setBody(new GsonBuilder().create().toJson(stubbedResponse))
            .addHeader("Content-Type", "application/json"));

        Composer.Builder builder = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString());
        if (logLevel != null) {
            builder.setLogLevel(logLevel);
        }

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event(EVENT_IP, endUserData);
        request.checkpointName = "CHECKOUT";
        request.sessionId = "session123";

        builder.build().checkpoint(request).get(5, TimeUnit.SECONDS);

        return captured.toString();
    }
}
