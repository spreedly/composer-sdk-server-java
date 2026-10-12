package com.spreedly.composer;

import com.spreedly.composer.messages.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Composer SDK
 */
public class ComposerTest {
    
    private Composer composer;
    
    @BeforeEach
    public void setUp() {
        composer = Composer.builder()
            .setApiKeys("test-secret-key")
            .build();
    }
    
    // Constructor tests
    
    @Test
    public void testBuilderRequiresApiKey() {
        assertThatThrownBy(() -> {
            Composer.builder().build();
        }).isInstanceOf(ComposerMissingConfigException.class)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Missing configuration: apiKey");
    }
    
    @Test
    public void testBuilderOnlyRequiresApiKey() {
        Composer c = Composer.builder()
            .setApiKeys("test-secret-key")
            .build();
        
        assertThat(c).isNotNull();
    }
    
    @Test
    public void testBuilderAcceptsCustomUrl() {
        Composer c = Composer.builder()
            .setApiKeys("test-secret-key")
            .setApiUrl("https://custom-api.example.com/")
            .build();
        
        assertThat(c).isNotNull();
    }
    
    @Test
    public void testBuilderAcceptsEnabledFlag() {
        Composer c = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();
        
        assertThat(c).isNotNull();
    }
    
    // Helper method tests
    
    @Test
    public void testIsAllowed_withApprovedComplete() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isAllowed(response)).isTrue();
    }
    
    @Test
    public void testIsAllowed_withDenied() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.DENIED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isAllowed(response)).isFalse();
    }
    
    @Test
    public void testIsAllowed_withPending() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isAllowed(response)).isFalse();
    }
    
    @Test
    public void testIsAllowed_withNullVerification() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            null,
            false
        );
        
        assertThat(Composer.isAllowed(response)).isFalse();
    }
    
    @Test
    public void testIsAllowed_withUnsuccessful() {
        CheckpointResponse response = new CheckpointResponse(
            false,
            new ComposerApiError[]{new ComposerApiError("Error")},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.FAILED,
                Constants.VerificationOutcome.ERROR,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isAllowed(response)).isFalse();
    }
    
    @Test
    public void testIsRunning_withPending() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isRunning(response)).isTrue();
    }
    
    @Test
    public void testIsRunning_withBlocked() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.BLOCKED,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isRunning(response)).isTrue();
    }

    @Test
    public void testIsRunning_withDeniedBlocked() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.BLOCKED,
                Constants.VerificationOutcome.DENIED,
                new java.util.HashMap<>()
            ),
            false
        );

        assertThat(Composer.isRunning(response)).isFalse();
    }

    @Test
    public void testIsRunning_withDeniedPending() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.DENIED,
                new java.util.HashMap<>()
            ),
            false
        );

        assertThat(Composer.isRunning(response)).isFalse();
    }

    @Test
    public void testIsRunning_withApprovedBlocked() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.BLOCKED,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );

        assertThat(Composer.isRunning(response)).isFalse();
    }

    @Test
    public void testIsRunning_withComplete() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isRunning(response)).isFalse();
    }
    
    @Test
    public void testIsRunning_withNullVerification() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            null,
            false
        );
        
        assertThat(Composer.isRunning(response)).isFalse();
    }
    
    @Test
    public void testIsDenied_withDeniedOutcome() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.DENIED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isDenied(response)).isTrue();
    }
    
    @Test
    public void testIsDenied_withApprovedOutcome() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isDenied(response)).isFalse();
    }
    
    @Test
    public void testIsDenied_withNullVerification() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            null,
            false
        );
        
        assertThat(Composer.isDenied(response)).isFalse();
    }
    
    @Test
    public void testIsUndecided_withCompletePending() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isUndecided(response)).isTrue();
    }
    
    @Test
    public void testIsUndecided_withCompleteApproved() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isUndecided(response)).isFalse();
    }
    
    @Test
    public void testIsUndecided_withRunning() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isUndecided(response)).isFalse();
    }
    
    @Test
    public void testHasError_withErrors() {
        CheckpointResponse response = new CheckpointResponse(
            false,
            new ComposerApiError[]{new ComposerApiError("Error message")},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.FAILED,
                Constants.VerificationOutcome.ERROR,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.hasError(response)).isTrue();
    }
    
    @Test
    public void testHasError_withFailedStatus() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.FAILED,
                Constants.VerificationOutcome.ERROR,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.hasError(response)).isTrue();
    }
    
    @Test
    public void testHasError_withSuccess() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.hasError(response)).isFalse();
    }
    
    @Test
    public void testHasError_withNullVerification() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            null,
            false
        );

        assertThat(Composer.hasError(response)).isFalse();
    }

    @Test
    public void testHasError_withUnsuccessfulAndNoVerificationOrErrors() {
        CheckpointResponse response = new CheckpointResponse(
            false,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            null,
            false
        );

        assertThat(Composer.hasError(response)).isTrue();
    }

    @Test
    public void testHasError_withErrorsWhileRunning() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{new ComposerApiError("Error message")},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            false
        );

        assertThat(Composer.hasError(response)).isFalse();
    }

    @Test
    public void testIsTimeout_withTimeout() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.PENDING,
                Constants.VerificationOutcome.PENDING,
                new java.util.HashMap<>()
            ),
            true
        );
        
        assertThat(Composer.isTimeout(response)).isTrue();
    }
    
    @Test
    public void testIsTimeout_withoutTimeout() {
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(
                "test-id",
                Constants.VerificationStatus.COMPLETE,
                Constants.VerificationOutcome.APPROVED,
                new java.util.HashMap<>()
            ),
            false
        );
        
        assertThat(Composer.isTimeout(response)).isFalse();
    }
    
    // Test checkpoint with SDK disabled
    
    @Test
    public void testCheckpoint_withDisabledSDK() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        
        CompletableFuture<CheckpointResponse> future = disabledComposer.checkpoint(request);
        CheckpointResponse response = future.get();
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(response.verification).isNotNull();
        assertThat(response.verification.id).isEqualTo("COMPOSER_IS_DISABLED");
        assertThat(response.verification.status).isEqualTo(Constants.VerificationStatus.COMPLETE);
        assertThat(response.verification.outcome).isEqualTo(Constants.VerificationOutcome.APPROVED);
    }
    
    // Test event with SDK disabled
    
    @Test
    public void testEvent_withDisabledSDK() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        com.spreedly.composer.api.ApiEvent event = new com.spreedly.composer.api.ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "test-session-id";
        
        CompletableFuture<NotificationResponse> future = disabledComposer.event(request);
        NotificationResponse response = future.get();

        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
    }

    // Test parameter validation, which runs even when the SDK is disabled

    private static CheckpointRequest validCheckpointRequest() {
        CheckpointRequest request = new CheckpointRequest();
        request.event = new Event();
        request.event.ip = "127.0.0.1";
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        return request;
    }

    @Test
    public void testCheckpoint_validatesParametersWhenDisabled() {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();

        CheckpointRequest missingName = validCheckpointRequest();
        missingName.checkpointName = "";
        assertThatThrownBy(() -> disabledComposer.checkpoint(missingName))
            .isInstanceOf(ComposerMissingParameterException.class)
            .hasMessageContaining("Missing parameter: checkpointName");

        CheckpointRequest blankName = validCheckpointRequest();
        blankName.checkpointName = "   ";
        assertThatThrownBy(() -> disabledComposer.checkpoint(blankName))
            .isInstanceOf(ComposerMissingParameterException.class)
            .hasMessageContaining("Missing parameter: checkpointName");

        CheckpointRequest missingEvent = validCheckpointRequest();
        missingEvent.event = null;
        assertThatThrownBy(() -> disabledComposer.checkpoint(missingEvent))
            .isInstanceOf(ComposerMissingParameterException.class)
            .hasMessageContaining("Missing parameter: event");

        CheckpointRequest missingIdentifiers = validCheckpointRequest();
        missingIdentifiers.sessionId = null;
        assertThatThrownBy(() -> disabledComposer.checkpoint(missingIdentifiers))
            .isInstanceOf(ComposerMissingParameterException.class)
            .hasMessageContaining("Must provide either a sessionId or sourceToken");

        assertThatThrownBy(() -> disabledComposer.checkpoint(null))
            .isInstanceOf(ComposerMissingParameterException.class);
    }

    @Test
    public void testCheckpoint_acceptsSourceTokenWithoutSessionId() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();

        CheckpointRequest request = validCheckpointRequest();
        request.sessionId = null;
        request.sourceToken = "source-token";

        assertThat(disabledComposer.checkpoint(request).get().success).isTrue();
    }

    @Test
    public void testCheckpoint_acceptsMissingIp() throws Exception {
        // The API treats event.ip as optional.
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();

        CheckpointRequest request = validCheckpointRequest();
        request.event.ip = null;

        assertThat(disabledComposer.checkpoint(request).get().success).isTrue();
    }

    @Test
    public void testEvent_validatesEventWhenDisabled() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();

        NotificationRequest request = new NotificationRequest();
        request.sessionId = "test-session-id";

        NotificationResponse missingEvent = disabledComposer.event(request).get();
        assertThat(missingEvent.success).isFalse();
        assertThat(missingEvent.errors[0].message).isEqualTo("no event was provided");

        NotificationResponse nullRequest = disabledComposer.event(null).get();
        assertThat(nullRequest.success).isFalse();
        assertThat(nullRequest.errors[0].message).isEqualTo("no event was provided");

        request.event = new com.spreedly.composer.api.ApiEvent(null, new HashMap<>());
        NotificationResponse missingType = disabledComposer.event(request).get();
        assertThat(missingType.success).isFalse();
        assertThat(missingType.errors[0].message).isEqualTo("event.type is missing");
        request.event.type = "";
        NotificationResponse emptyType = disabledComposer.event(request).get();
        assertThat(emptyType.success).isFalse();
        assertThat(emptyType.errors[0].message).isEqualTo("event.type is missing");
    }

    @Test
    public void testEvent_missingSessionIdentifierReturnsFailureWhenDisabled() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();

        NotificationRequest request = new NotificationRequest();
        request.event = new com.spreedly.composer.api.ApiEvent("TEST_EVENT", new HashMap<>());
        request.sessionId = null;
        request.sourceToken = "   ";

        NotificationResponse response = disabledComposer.event(request).get();
        assertThat(response.success).isFalse();
        assertThat(response.errors[0].message).isEqualTo("Must provide either a sessionId or sourceToken");
    }

    @Test
    public void testEvent_acceptsSourceTokenWithoutSessionId() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();

        NotificationRequest request = new NotificationRequest();
        request.event = new com.spreedly.composer.api.ApiEvent("TEST_EVENT", new HashMap<>());
        request.sourceToken = "source-token";

        assertThat(disabledComposer.event(request).get().success).isTrue();
    }

    // Test backward compatibility aliases
    
    @Test
    public void testNotify_isAliasForEvent() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        com.spreedly.composer.api.ApiEvent event = new com.spreedly.composer.api.ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "test-session-id";
        
        @SuppressWarnings("deprecation")
        CompletableFuture<NotificationResponse> future = disabledComposer.notify(request);
        NotificationResponse response = future.get();
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
    }
    
    @Test
    public void testTrack_isAliasForEvent() throws Exception {
        Composer disabledComposer = Composer.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("key", "value");
        
        com.spreedly.composer.api.ApiEvent event = new com.spreedly.composer.api.ApiEvent("TEST_EVENT", eventData);
        
        NotificationRequest request = new NotificationRequest();
        request.event = event;
        request.sessionId = "test-session-id";
        
        @SuppressWarnings("deprecation")
        CompletableFuture<NotificationResponse> future = disabledComposer.track(request);
        NotificationResponse response = future.get();
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
    }
    
    // Test Dodgeball backward compatibility wrapper
    
    @Test
    @SuppressWarnings("deprecation")
    public void testDodgeballWrapper_checkpoint() throws Exception {
        Dodgeball dodgeball = Dodgeball.builder()
            .setApiKeys("test-secret-key")
            .setEnabled(false)
            .build();
        
        Event event = new Event();
        event.ip = "127.0.0.1";
        
        CheckpointRequest request = new CheckpointRequest();
        request.event = event;
        request.checkpointName = "TEST_CHECKPOINT";
        request.sessionId = "test-session-id";
        
        CompletableFuture<CheckpointResponse> future = dodgeball.checkpoint(request);
        CheckpointResponse response = future.get();
        
        assertThat(response).isNotNull();
        assertThat(response.success).isTrue();
        assertThat(response.verification.id).isEqualTo("COMPOSER_IS_DISABLED");
    }

    // Builder log level coverage tests

    @Test
    public void testBuilderWithTraceLogLevel() {
        Composer c = Composer.builder()
            .setApiKeys("test-secret-key")
            .setLogLevel(Logger.ComposerLogLevel.TRACE)
            .build();
        assertThat(c).isNotNull();
        assertThat(Logger.filterLevel).isEqualTo(Logger.Severity.TRACE);
    }

    @Test
    public void testBuilderWithErrorLogLevel() {
        Composer c = Composer.builder()
            .setApiKeys("test-secret-key")
            .setLogLevel(Logger.ComposerLogLevel.ERROR)
            .build();
        assertThat(c).isNotNull();
        assertThat(Logger.filterLevel).isEqualTo(Logger.Severity.ERROR);
    }

    @Test
    public void testBuilderWithNoneLogLevel() {
        Composer c = Composer.builder()
            .setApiKeys("test-secret-key")
            .setLogLevel(Logger.ComposerLogLevel.NONE)
            .build();
        assertThat(c).isNotNull();
        assertThat(Logger.filterLevel).isEqualTo(Logger.Severity.NONE);
    }

    @Test
    public void testIsAllowed_withNullStatus() {
        // Exercises stringsEqual(null, "COMPLETE") → hits the null-branch return
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{},
            Constants.ApiVersion.V1,
            new ComposerVerification(null, null, null, new java.util.HashMap<>()),
            false
        );
        assertThat(Composer.isAllowed(response)).isFalse();
    }

    @Test
    public void testDodgeballBuilderSetLogLevel() {
        Dodgeball d = Dodgeball.builder()
            .setApiKeys("test-secret-key")
            .setLogLevel(Logger.ComposerLogLevel.TRACE)
            .build();
        assertThat(d).isNotNull();
    }
}
