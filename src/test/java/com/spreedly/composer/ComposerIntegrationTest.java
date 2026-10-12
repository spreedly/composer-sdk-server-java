package com.spreedly.composer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spreedly.composer.api.ApiEvent;
import com.spreedly.composer.messages.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Composer SDK with MockWebServer
 */
public class ComposerIntegrationTest {
    
    private MockWebServer mockServer;
    private Composer composer;
    private Gson gson;
    private Logger.Severity originalFilterLevel;
    
    @BeforeEach
    public void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        
        gson = new GsonBuilder().create();
        
        // Save original filter level and suppress logging during tests
        originalFilterLevel = Logger.filterLevel;
        
        String baseUrl = mockServer.url("/").toString();
        
        composer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(baseUrl)
            .setLogLevel(Logger.ComposerLogLevel.NONE)
            .build();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        // Restore original filter level
        Logger.filterLevel = originalFilterLevel;
        
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }
    
    // ============================================================================
    // Basic Checkpoint Tests
    // ============================================================================
    
    @Test
    public void testCheckpoint_withImmediateApproval() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(Composer.isAllowed(response)).isTrue();
        assertThat(response.verification.id).isEqualTo("test-verification-id");
        
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getPath()).startsWith("/v1/checkpoint");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader("Dodgeball-Secret-Key")).isEqualTo("test-secret-key");
        assertThat(recordedRequest.getHeader("Content-Type")).startsWith("application/json");
    }

    @Test
    public void testCheckpoint_leavesOutNullOrEmptyIp() throws Exception {
        String approved = gson.toJson(new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        ));

        for (String ip : new String[]{null, ""}) {
            mockServer.enqueue(new MockResponse()
                .setBody(approved)
                .addHeader("Content-Type", "application/json"));

            Event event = new Event();
            event.ip = ip;

            CheckpointRequest request = new CheckpointRequest();
            request.event = event;
            request.checkpointName = "TEST_CHECKPOINT";
            request.sessionId = "test-session-id";

            CheckpointResponse response = composer.checkpoint(request).get(5, TimeUnit.SECONDS);

            assertThat(Composer.isAllowed(response)).isTrue();
            String body = mockServer.takeRequest().getBody().readUtf8();
            assertThat(gson.fromJson(body, com.google.gson.JsonObject.class).getAsJsonObject("event").has("ip")).isFalse();
        }
    }

    @Test
    public void testCheckpoint_withImmediateDenial() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.DENIED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(Composer.isDenied(response)).isTrue();
        assertThat(Composer.isAllowed(response)).isFalse();
    }
    
    @Test
    public void testCheckpoint_withError() throws Exception {
        CheckpointResponse errorResponse = new CheckpointResponse(
            false,
            new ComposerApiError[]{new ComposerApiError("Test error")},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.FAILED,
                Constants.VerificationOutcome.ERROR,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(errorResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isFalse();
        assertThat(Composer.hasError(response)).isTrue();
        assertThat(response.errors).hasSize(1);
        assertThat(response.errors[0].message).isEqualTo("Test error");
    }
    
    @Test
    public void testCheckpoint_failedResponseNoVerification() throws Exception {
        CheckpointResponse errorResponse = new CheckpointResponse();
        errorResponse.success = false;
        errorResponse.errors = new ComposerApiError[]{new ComposerApiError("API Error")};
        errorResponse.version = Constants.ApiVersion.V1;
        errorResponse.verification = null;
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(errorResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isFalse();
        assertThat(response.verification).isNull();
        assertThat(Composer.isRunning(response)).isFalse();
    }
    
    // ============================================================================
    // Polling Tests
    // ============================================================================
    
    @Test
    public void testCheckpoint_withPolling() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        CheckpointResponse approvedResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(approvedResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(Composer.isAllowed(response)).isTrue();
        
        RecordedRequest checkpointRequest = mockServer.takeRequest();
        assertThat(checkpointRequest.getPath()).startsWith("/v1/checkpoint");
        assertThat(checkpointRequest.getMethod()).isEqualTo("POST");
        
        RecordedRequest pollingRequest = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(pollingRequest).isNotNull();
        assertThat(pollingRequest.getPath()).startsWith("/v1/verification/test-verification-id");
        assertThat(pollingRequest.getMethod()).isEqualTo("GET");
    }
    
    @Test
    public void testCheckpoint_pollingLoopSucceeds() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        CheckpointResponse resolvedResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(resolvedResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
        assertThat(Composer.isAllowed(response)).isTrue();
    }
    
    @Test
    public void testCheckpoint_blockedStatusReturnsWithoutPolling() throws Exception {
        CheckpointResponse blockedResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.BLOCKED,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(blockedResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        // BLOCKED waits on a client action (such as MFA), so the SDK returns
        // immediately without polling rather than driving to a final outcome.
        assertThat(response.success).isTrue();
        assertThat(Composer.isRunning(response)).isTrue();
        assertThat(Composer.isDenied(response)).isFalse();
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    public void testCheckpoint_blockedResponseIncludesNextSteps() throws Exception {
        mockServer.enqueue(new MockResponse()
            .setBody("{\"success\":true,\"version\":\"v1\",\"verification\":{\"id\":\"ver-123\",\"status\":\"BLOCKED\",\"outcome\":\"PENDING\","
                + "\"nextSteps\":[{\"id\":\"step-1\",\"name\":\"MFA\",\"url\":\"https://example.com/mfa.js\",\"config\":{}}]}}")
            .addHeader("Content-Type", "application/json"));

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        CheckpointResponse response = composer.checkpoint(request).get(10, TimeUnit.SECONDS);

        assertThat(response.verification.nextSteps).hasSize(1);
        assertThat(response.verification.nextSteps.get(0).get("id")).isEqualTo("step-1");
    }

    @Test
    public void testCheckpoint_pollingWithNullStatus() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.errors = new ComposerApiError[]{};
        pendingResponse.version = Constants.ApiVersion.V1;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "ver-123";
        pendingResponse.verification.status = null;
        pendingResponse.verification.outcome = Constants.VerificationOutcome.PENDING;
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 500;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
    }
    
    @Test
    public void testCheckpoint_nullVerificationDuringPolling() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        CheckpointResponse nullVerResponse = new CheckpointResponse();
        nullVerResponse.success = true;
        nullVerResponse.errors = new ComposerApiError[]{};
        nullVerResponse.version = Constants.ApiVersion.V1;
        nullVerResponse.verification = null;
        
        for (int i = 0; i < 3; i++) {
            mockServer.enqueue(new MockResponse()
                .setBody(gson.toJson(nullVerResponse))
                .addHeader("Content-Type", "application/json"));
        }
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 1000;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isFalse();
        assertThat(response.errors).isNotEmpty();
    }
    
    @Test
    public void testCheckpoint_pollingRecoversFromNullVerification() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        CheckpointResponse nullVerResponse = new CheckpointResponse();
        nullVerResponse.success = true;
        nullVerResponse.verification = null;
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(nullVerResponse))
            .addHeader("Content-Type", "application/json"));
        
        CheckpointResponse successResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(successResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(Composer.isAllowed(response)).isTrue();
    }
    
    @Test
    public void testCheckpoint_maxFailuresExceeded() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "v123";
        pendingResponse.verification.status = Constants.VerificationStatus.PENDING;
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(pendingResponse))
            .setHeader("Content-Type", "application/json"));
        
        for (int i = 0; i < 5; i++) {
            CheckpointResponse failureResponse = new CheckpointResponse();
            failureResponse.success = true;
            failureResponse.verification = new ComposerVerification();
            failureResponse.verification.id = "v123";
            failureResponse.verification.status = null;
            
            mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(failureResponse))
                .setHeader("Content-Type", "application/json"));
        }
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event();
        request.event.ip = "127.0.0.1";
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 5000;

        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);

        assertThat(response.success).isFalse();
        assertThat(response.errors).isNotEmpty();
        assertThat(response.errors[0].message).contains("Maximum retry count exceeded");
        assertThat(response.errors[0].code).isEqualTo(503);
        assertThat(Composer.isTimeout(response)).isTrue();
    }
    
    @Test
    public void testCheckpoint_pollingWithUnsuccessfulResponse() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "v123";
        pendingResponse.verification.status = Constants.VerificationStatus.PENDING;
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(pendingResponse))
            .setHeader("Content-Type", "application/json"));
        
        for (int i = 0; i < 4; i++) {
            CheckpointResponse failureResponse = new CheckpointResponse();
            failureResponse.success = false;
            mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(failureResponse))
                .setHeader("Content-Type", "application/json"));
        }
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event();
        request.event.ip = "127.0.0.1";
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 5000;

        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);

        assertThat(response.success).isFalse();
    }
    
    @Test
    public void testCheckpoint_unsuccessfulDuringPolling() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .addHeader("Content-Type", "application/json"));
        
        CheckpointResponse failedResponse = new CheckpointResponse();
        failedResponse.success = false;
        failedResponse.errors = new ComposerApiError[]{new ComposerApiError("Polling error")};
        
        for (int i = 0; i < 3; i++) {
            mockServer.enqueue(new MockResponse()
                .setBody(gson.toJson(failedResponse))
                .addHeader("Content-Type", "application/json"));
        }
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 2000;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response.success).isFalse();
        assertThat(response.errors).isNotEmpty();
    }
    
    // ============================================================================
    // Timeout Tests
    // ============================================================================
    
    @Test
    public void testCheckpoint_withTimeout() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        for (int i = 0; i < 10; i++) {
            mockServer.enqueue(new MockResponse()
                .setBody(gson.toJson(pendingResponse))
                .addHeader("Content-Type", "application/json"));
        }
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 500;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(Composer.isTimeout(response)).isFalse();
        assertThat(Composer.isRunning(response)).isTrue();
    }
    
    @Test
    public void testCheckpoint_timeoutIncludesTheInitialRequest() throws Exception {
        String pending = gson.toJson(new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        ));

        // The initial request alone uses up the 300 ms budget
        mockServer.enqueue(new MockResponse()
            .setBody(pending)
            .addHeader("Content-Type", "application/json")
            .setHeadersDelay(500, TimeUnit.MILLISECONDS));
        for (int i = 0; i < 5; i++) {
            mockServer.enqueue(new MockResponse()
                .setBody(pending)
                .addHeader("Content-Type", "application/json"));
        }

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 300;

        CheckpointResponse response = composer.checkpoint(request).get(10, TimeUnit.SECONDS);

        assertThat(Composer.isRunning(response)).isTrue();
        // The initial request plus the single poll the SDK always makes
        assertThat(mockServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    public void testCheckpoint_timeoutOnBoundary() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        for (int i = 0; i < 5; i++) {
            mockServer.enqueue(new MockResponse()
                .setBody(gson.toJson(pendingResponse))
                .addHeader("Content-Type", "application/json"));
        }
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 100;

        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(10, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(Composer.isTimeout(response)).isFalse();
        assertThat(Composer.isRunning(response)).isTrue();
    }
    
    @Test
    public void testCheckpoint_withCustomTimeout() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 30000;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
    }
    
    @Test
    public void testCheckpoint_withZeroTimeout() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 0;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
    }
    
    @Test
    public void testCheckpoint_veryLongTimeout() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = Integer.MAX_VALUE;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
    }
    
    @Test
    public void testCheckpoint_withSyncOption() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.sync = true;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
    }
    
    // ============================================================================
    // Headers and URL Tests
    // ============================================================================
    
    @Test
    public void testCheckpoint_withCustomHeaders() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-verification-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        request.userId = "test-user-id";
        request.sourceToken = "test-source-token";
        request.useVerificationId = "previous-verification-id";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getHeader("Dodgeball-Session-Id")).isEqualTo("test-session-id");
        assertThat(recordedRequest.getHeader("Dodgeball-Customer-Id")).isEqualTo("test-user-id");
        assertThat(recordedRequest.getHeader("Dodgeball-Source-Token")).isEqualTo("test-source-token");
        assertThat(recordedRequest.getHeader("Dodgeball-Verification-Id")).isEqualTo("previous-verification-id");
    }
    
    @Test
    public void testCheckpoint_withEmptyOptionalHeaders() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "";
        request.userId = "";
        request.sourceToken = "source-token";
        request.useVerificationId = "";

        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);

        assertThat(response.success).isTrue();

        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getHeader("Dodgeball-Session-Id")).isNull();
        assertThat(recordedRequest.getHeader("Dodgeball-Customer-Id")).isNull();
        assertThat(recordedRequest.getHeader("Dodgeball-Source-Token")).isEqualTo("source-token");
        assertThat(recordedRequest.getHeader("Dodgeball-Verification-Id")).isNull();
    }
    
    @Test
    public void testCheckpoint_withNullOptionalHeaders() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = null;
        request.userId = null;
        request.sourceToken = "source-token";
        request.useVerificationId = null;
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
    }

    @Test
    public void testCheckpoint_omitsBlankSourceTokenWhenSessionIdPresent() throws Exception {
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );

        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));

        String[] blankSourceTokens = { "", null };
        for (String sourceToken : blankSourceTokens) {
            CheckpointRequest request = new CheckpointRequest();
            request.event = new Event("127.0.0.1", null);
            request.checkpointName = "TEST";
            request.sessionId = "sess-123";
            request.sourceToken = sourceToken;

            composer.checkpoint(request).get(5, TimeUnit.SECONDS);

            RecordedRequest recordedRequest = mockServer.takeRequest();
            assertThat(recordedRequest.getHeader("Dodgeball-Session-Id")).isEqualTo("sess-123");
            assertThat(recordedRequest.getHeader("Dodgeball-Source-Token")).isNull();
        }
    }
    
    @Test
    public void testCheckpoint_withUrlWithoutTrailingSlash() throws Exception {
        mockServer.shutdown();
        mockServer = new MockWebServer();
        mockServer.start();
        
        String baseUrlNoSlash = mockServer.url("/").toString().replaceAll("/$", "");
        
        composer = Composer.builder()
            .setApiKeys("test-key")
            .setApiUrl(baseUrlNoSlash)
            .setLogLevel(Logger.ComposerLogLevel.NONE)
            .build();
        
        CheckpointResponse mockResponse = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "ver-123",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
    }
    
    // ============================================================================
    // Event Tests
    // ============================================================================
    
    @Test
    public void testEvent_success() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse(true, new ComposerApiError[]{});
        mockResponse.version = Constants.ApiVersion.V1;
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("testKey", "testValue");
        
        ApiEvent event = new ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "test-session-id";
        
        CompletableFuture<NotificationResponse> future = composer.event(request);
        NotificationResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getPath()).startsWith("/v1/track");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader("Dodgeball-Secret-Key")).isEqualTo("test-secret-key");
        assertThat(recordedRequest.getHeader("Content-Type")).startsWith("application/json");
    }
    
    @Test
    public void testEvent_withError() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse(
            false, 
            new ComposerApiError[]{new ComposerApiError("Test event error")}
        );
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json")
            .setResponseCode(500));
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("testKey", "testValue");
        
        ApiEvent event = new ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "test-session-id";
        
        CompletableFuture<NotificationResponse> future = composer.event(request);
        NotificationResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isFalse();
        assertThat(response.errors).isNotEmpty();
    }
    
    @Test
    public void testEvent_networkError() throws Exception {
        mockServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"success\": false, \"errors\": [{\"message\": \"Server error\"}]}")
            .addHeader("Content-Type", "application/json"));
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        ApiEvent event = new ApiEvent("ERROR_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "sess-123";
        
        CompletableFuture<NotificationResponse> future = composer.event(request);
        NotificationResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
    }
    
    @Test
    public void testEvent_withNullTimestamp() throws Exception {
        NotificationResponse mockResponse = new NotificationResponse(true, new ComposerApiError[]{});
        
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(mockResponse))
            .addHeader("Content-Type", "application/json"));
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        ApiEvent event = new ApiEvent("TEST_EVENT", eventData);
        event.eventTime = null;
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "sess-123";
        
        CompletableFuture<NotificationResponse> future = composer.event(request);
        NotificationResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response.success).isTrue();
        
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getBody().readUtf8()).contains("eventTime");
    }
    
    @Test
    public void testEvent_nullEvent() throws Exception {
        Composer errorLoggingComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.ERROR)
            .build();
        NotificationRequest request = new NotificationRequest();
        request.event = null;
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        NotificationResponse response;
        try {
            response = errorLoggingComposer.event(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(response.success).isFalse();
        assertThat(response.errors[0].message).isEqualTo("no event was provided");
        assertThat(output.toString()).contains("Composer event was not sent: no event was provided");
        assertThat(mockServer.getRequestCount()).isZero();
    }
    
    @Test
    public void testEvent_missingType() throws Exception {
        Composer errorLoggingComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.ERROR)
            .build();
        NotificationRequest request = new NotificationRequest();
        request.event = new ApiEvent(null, new HashMap<>());
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        NotificationResponse missingType;
        NotificationResponse emptyType;
        NotificationResponse blankType;
        try {
            missingType = errorLoggingComposer.event(request).get(5, TimeUnit.SECONDS);
            request.event.type = "";
            emptyType = errorLoggingComposer.event(request).get(5, TimeUnit.SECONDS);
            request.event.type = "   ";
            blankType = errorLoggingComposer.event(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(missingType.success).isFalse();
        assertThat(missingType.errors[0].message).isEqualTo("event.type is missing");
        assertThat(emptyType.success).isFalse();
        assertThat(blankType.success).isFalse();
        assertThat(output.toString()).contains("Composer event was not sent: event.type is missing");
        assertThat(mockServer.getRequestCount()).isZero();
    }

    @Test
    public void testEvent_missingSessionIdentifierLogsAndSkipsTheRequest() throws Exception {
        Composer errorLoggingComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.ERROR)
            .build();
        NotificationRequest request = new NotificationRequest();
        request.event = new ApiEvent("TEST_EVENT", new HashMap<>());
        request.sessionId = "   ";
        request.sourceToken = null;

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        NotificationResponse response;
        try {
            response = errorLoggingComposer.event(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(response.success).isFalse();
        assertThat(response.errors).hasSize(1);
        assertThat(response.errors[0].message).isEqualTo("Must provide either a sessionId or sourceToken");
        assertThat(output.toString())
            .contains("Composer event was not sent: Must provide either a sessionId or sourceToken");
        assertThat(mockServer.getRequestCount()).isZero();
    }

    // ============================================================================
    // Exception Tests
    // ============================================================================
    
    @Test
    public void testCheckpoint_ioException() throws Exception {
        mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isFalse();
        assertThat(response.errors).isNotEmpty();
    }
    
    @Test
    public void testCheckpoint_requestTimeoutReturnsStandardFailure() throws Exception {
        Composer fastTimeoutComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.NONE)
            .setRequestTimeoutMs(200)
            .build();
        mockServer.enqueue(new MockResponse()
            .setBody("{\"success\": true}")
            .setHeadersDelay(2, TimeUnit.SECONDS));

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        long start = System.currentTimeMillis();
        CheckpointResponse response = fastTimeoutComposer.checkpoint(request).get(5, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(1500);
        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(500);
        assertThat(response.errors[0].message).isEqualTo("Internal Server Error");
    }

    @Test
    public void testCheckpoint_requestTimeoutCapsTheWholeCall() throws Exception {
        Composer fastTimeoutComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.NONE)
            .setRequestTimeoutMs(500)
            .build();
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "v123";
        pendingResponse.verification.status = Constants.VerificationStatus.PENDING;
        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .setHeader("Content-Type", "application/json"));

        // Polls are GETs, so only the response is throttled. One byte every 100 ms means no
        // single read trips a per-read timeout; only a whole-call deadline cuts these short.
        String approvedBody = "{\"success\":true,\"version\":\"v1\",\"errors\":[],"
            + "\"verification\":{\"id\":\"v123\",\"status\":\"COMPLETE\",\"outcome\":\"APPROVED\"}}";
        for (int i = 0; i < 3; i++) {
            mockServer.enqueue(new MockResponse()
                .setBody(approvedBody)
                .throttleBody(1, 100, TimeUnit.MILLISECONDS));
        }

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        CheckpointResponse response = fastTimeoutComposer.checkpoint(request).get(20, TimeUnit.SECONDS);

        // Each slow poll is abandoned at 500 ms and counts as a failure, rather than
        // completing several seconds later as an approval
        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(503);
    }

    @Test
    public void testCheckpoint_pollNetworkErrorsCountTowardMaxFailures() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "v123";
        pendingResponse.verification.status = Constants.VerificationStatus.PENDING;

        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .setHeader("Content-Type", "application/json"));
        for (int i = 0; i < 3; i++) {
            mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        }

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        CheckpointResponse response = composer.checkpoint(request).get(10, TimeUnit.SECONDS);

        assertThat(mockServer.getRequestCount()).isEqualTo(4);
        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(503);
        assertThat(Composer.isTimeout(response)).isTrue();
    }

    @Test
    public void testCheckpoint_nonJsonResponseReturnsStandardFailure() throws Exception {
        Composer loggingComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.ERROR)
            .build();
        mockServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .setBody("<html>Bad Gateway</html>"));

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        CheckpointResponse response;
        try {
            response = loggingComposer.checkpoint(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(500);
        assertThat(output.toString()).contains("not valid JSON").doesNotContain("Bad Gateway");
    }

    @Test
    public void testCheckpoint_pollNonJsonResponsesCountTowardMaxFailures() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "v123";
        pendingResponse.verification.status = Constants.VerificationStatus.PENDING;

        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .setHeader("Content-Type", "application/json"));
        for (int i = 0; i < 3; i++) {
            mockServer.enqueue(new MockResponse()
                .setResponseCode(502)
                .setBody("<html>Bad Gateway</html>"));
        }

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        CheckpointResponse response = composer.checkpoint(request).get(10, TimeUnit.SECONDS);

        assertThat(mockServer.getRequestCount()).isEqualTo(4);
        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(503);
        assertThat(Composer.isTimeout(response)).isTrue();
    }

    @Test
    public void testCheckpoint_timeoutAfterNonJsonPollReturnsStandardFailure() throws Exception {
        CheckpointResponse pendingResponse = new CheckpointResponse();
        pendingResponse.success = true;
        pendingResponse.verification = new ComposerVerification();
        pendingResponse.verification.id = "v123";
        pendingResponse.verification.status = Constants.VerificationStatus.PENDING;

        mockServer.enqueue(new MockResponse()
            .setBody(gson.toJson(pendingResponse))
            .setHeader("Content-Type", "application/json"));
        mockServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .setBody("<html>Bad Gateway</html>"));

        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        request.options = new CheckpointResponseOptions();
        request.options.timeout = 1;

        CheckpointResponse response = composer.checkpoint(request).get(10, TimeUnit.SECONDS);

        assertThat(mockServer.getRequestCount()).isEqualTo(2);
        assertThat(response).isNotNull();
        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(500);
    }

    @Test
    public void testBuilder_nonPositiveRequestTimeoutLogsAndFallsBack() {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        try {
            Composer.builder()
                .setApiKeys("test-secret-key")
                .setLogLevel(Logger.ComposerLogLevel.ERROR)
                .setRequestTimeoutMs(0)
                .build();
        } finally {
            System.setOut(originalOut);
        }

        assertThat(output.toString())
            .contains("requestTimeoutMs must be a positive integer. Ignoring 0 and using the 30000ms default.");
    }

    @Test
    public void testEvent_logsWhenTheApiDoesNotAcceptTheEvent() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"success\":false,\"errors\":[]}"));

        assertThat(sendEventCapturingOutput()).contains("Composer event was not accepted by the API.");
    }

    @Test
    public void testEvent_logsNothingWhenTheApiAcceptsTheEvent() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(201).setBody("{\"success\":true,\"errors\":[]}"));

        assertThat(sendEventCapturingOutput()).isEmpty();
    }

    @Test
    public void testEvent_doesNotReportAnUnsendableRequestAsAnApiRejection() throws Exception {
        Composer badUrlComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl("not a url")
            .setLogLevel(Logger.ComposerLogLevel.INFO)
            .build();
        NotificationRequest request = new NotificationRequest();
        request.event = new ApiEvent("TEST_EVENT", new HashMap<>());
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        try {
            badUrlComposer.event(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(output.toString())
            .contains("Composer event was not sent: the request could not be made.")
            .doesNotContain("not accepted by the API");
    }

    @Test
    public void testCheckpoint_keepsAnUnsendableHeaderValueOutOfTheResponseAndLogs() throws Exception {
        // A key read from a file often ends in a newline, which OkHttp rejects with the value in its message
        Composer newlineKeyComposer = Composer.builder()
            .setApiKeys("sk_test_FAKE123\n")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.INFO)
            .build();
        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event("127.0.0.1", null);
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        CheckpointResponse response;
        try {
            response = newlineKeyComposer.checkpoint(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(response.success).isFalse();
        assertThat(response.errors[0].code).isEqualTo(500);
        assertThat(response.errors[0].message).isEqualTo("Internal Server Error");
        assertThat(gson.toJson(response)).doesNotContain("sk_test_FAKE123");
        assertThat(output.toString())
            .contains("There was an error making a request.")
            .doesNotContain("sk_test_FAKE123");
    }

    @Test
    public void testEvent_keepsAnUnsendableHeaderValueOutOfTheResponseAndLogs() throws Exception {
        Composer newlineKeyComposer = Composer.builder()
            .setApiKeys("sk_test_FAKE123\n")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.INFO)
            .build();
        NotificationRequest request = new NotificationRequest();
        request.event = new ApiEvent("TEST_EVENT", new HashMap<>());
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        NotificationResponse response;
        try {
            response = newlineKeyComposer.event(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(gson.toJson(response)).doesNotContain("sk_test_FAKE123");
        assertThat(output.toString())
            .contains("Composer event was not sent: the request could not be made.")
            .doesNotContain("sk_test_FAKE123");
    }

    private String sendEventCapturingOutput() throws Exception {
        Composer infoComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl(mockServer.url("/").toString())
            .setLogLevel(Logger.ComposerLogLevel.INFO)
            .build();
        NotificationRequest request = new NotificationRequest();
        request.event = new ApiEvent("TEST_EVENT", new HashMap<>());
        request.sessionId = "sess-123";

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(output));
        try {
            infoComposer.event(request).get(5, TimeUnit.SECONDS);
        } finally {
            System.setOut(originalOut);
        }
        return output.toString();
    }

    @Test
    public void testEvent_doesNotReportANetworkFailureAsAnApiRejection() throws Exception {
        mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertThat(sendEventCapturingOutput())
            .containsOnlyOnce("There was an error making a request.")
            .doesNotContain("not accepted by the API");
    }

    @Test
    public void testEvent_ioException() throws Exception {
        mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        ApiEvent event = new ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "sess-123";
        
        CompletableFuture<NotificationResponse> future = composer.event(request);
        NotificationResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
        assertThat(response.success).isFalse();
        assertThat(response.errors).isNotEmpty();
    }
    
    @Test
    public void testCheckpoint_malformedJson() throws Exception {
        mockServer.enqueue(new MockResponse()
            .setBody("{invalid json")
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
    }
    
    @Test
    public void testEvent_malformedJson() throws Exception {
        mockServer.enqueue(new MockResponse()
            .setBody("{invalid json")
            .addHeader("Content-Type", "application/json"));
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        ApiEvent event = new ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "sess-123";
        
        CompletableFuture<NotificationResponse> future = composer.event(request);
        NotificationResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
    }
    
    @Test
    public void testCheckpoint_withNullResponseBody() throws Exception {
        mockServer.enqueue(new MockResponse()
            .setBody("null")
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
    }
    
    @Test
    public void testCheckpoint_serverError500() throws Exception {
        mockServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"success\":false,\"errors\":[{\"code\":500,\"message\":\"Server error\"}]}")
            .addHeader("Content-Type", "application/json"));
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST";
        request.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> future = composer.checkpoint(request);
        CheckpointResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertThat(response).isNotNull();
    }
    
    // ============================================================================
    // Dodgeball Compatibility Tests
    // ============================================================================
    
    @Test
    @SuppressWarnings("deprecation")
    public void testDodgeball_allMethods() throws Exception {
        Dodgeball dodgeball = Dodgeball.builder()
            .setApiKeys("test-key")
            .setApiUrl("https://test.example.com")
            .setEnabled(false)
            .build();
        
        Event event = new Event("127.0.0.1", null);
        CheckpointRequest checkpointReq = new CheckpointRequest();
        checkpointReq.event = event;
        checkpointReq.checkpointName = "TEST";
        checkpointReq.sessionId = "sess-123";
        
        CompletableFuture<CheckpointResponse> checkpointFuture = dodgeball.checkpoint(checkpointReq);
        CheckpointResponse checkpointResp = checkpointFuture.get();
        assertThat(checkpointResp.success).isTrue();
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        ApiEvent apiEvent = new ApiEvent("TEST", eventData);
        
        NotificationRequest notifReq = new NotificationRequest();
        notifReq.event = apiEvent;
        notifReq.sessionId = "sess-123";
        
        CompletableFuture<NotificationResponse> eventFuture = dodgeball.event(notifReq);
        NotificationResponse eventResp = eventFuture.get();
        assertThat(eventResp.success).isTrue();
        
        CompletableFuture<NotificationResponse> notifyFuture = dodgeball.notify(notifReq);
        NotificationResponse notifyResp = notifyFuture.get();
        assertThat(notifyResp.success).isTrue();
        
        CompletableFuture<NotificationResponse> trackFuture = dodgeball.track(notifReq);
        NotificationResponse trackResp = trackFuture.get();
        assertThat(trackResp.success).isTrue();
    }
}
