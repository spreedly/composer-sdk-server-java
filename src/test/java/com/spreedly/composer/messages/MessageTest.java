package com.spreedly.composer.messages;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for message classes
 */
public class MessageTest {
    
    // Event tests
    
    @Test
    public void testEvent_emptyConstructor() {
        Event event = new Event();
        assertThat(event).isNotNull();
    }
    
    @Test
    public void testEvent_constructorWithParams() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        
        Event event = new Event("192.168.1.1", data);
        
        assertThat(event.ip).isEqualTo("192.168.1.1");
        assertThat(event.data).isEqualTo(data);
    }
    
    @Test
    public void testEvent_setFields() {
        Event event = new Event();
        Map<String, Object> data = new HashMap<>();
        data.put("test", "data");
        
        event.ip = "10.0.0.1";
        event.data = data;
        
        assertThat(event.ip).isEqualTo("10.0.0.1");
        assertThat(event.data).containsEntry("test", "data");
    }
    
    // CheckpointRequest tests
    
    @Test
    public void testCheckpointRequest_emptyConstructor() {
        CheckpointRequest request = new CheckpointRequest();
        assertThat(request).isNotNull();
    }
    
    @Test
    public void testCheckpointRequest_constructorWithRequiredFields() {
        Event event = new Event("127.0.0.1", null);
        
        CheckpointRequest request = new CheckpointRequest(event, "LOGIN", "session-123", "user-456");
        
        assertThat(request.event).isEqualTo(event);
        assertThat(request.checkpointName).isEqualTo("LOGIN");
        assertThat(request.sessionId).isEqualTo("session-123");
        assertThat(request.userId).isEqualTo("user-456");
    }
    
    @Test
    public void testCheckpointRequest_constructorWithAllFields() {
        Event event = new Event("127.0.0.1", null);
        
        CheckpointRequest request = new CheckpointRequest(
            event, 
            "LOGIN", 
            "session-123", 
            "user-456",
            "source-token-789",
            "verification-abc"
        );
        
        assertThat(request.event).isEqualTo(event);
        assertThat(request.checkpointName).isEqualTo("LOGIN");
        assertThat(request.sessionId).isEqualTo("session-123");
        assertThat(request.userId).isEqualTo("user-456");
        assertThat(request.sourceToken).isEqualTo("source-token-789");
        assertThat(request.useVerificationId).isEqualTo("verification-abc");
    }
    
    // CheckpointResponse tests
    
    @Test
    public void testCheckpointResponse_emptyConstructor() {
        CheckpointResponse response = new CheckpointResponse();
        assertThat(response).isNotNull();
    }
    
    @Test
    public void testCheckpointResponse_constructorWithAllFields() {
        ComposerApiError error = new ComposerApiError("Error");
        ComposerVerification verification = new ComposerVerification(
            "ver-123",
            Constants.VerificationStatus.COMPLETE,
            Constants.VerificationOutcome.APPROVED,
            new java.util.HashMap<>()
        );
        
        CheckpointResponse response = new CheckpointResponse(
            true,
            new ComposerApiError[]{error},
            Constants.ApiVersion.V1,
            verification,
            false
        );
        
        assertThat(response.success).isTrue();
        assertThat(response.errors).hasSize(1);
        assertThat(response.version).isEqualTo(Constants.ApiVersion.V1);
        assertThat(response.verification).isEqualTo(verification);
        assertThat(response.isTimeout).isFalse();
    }
    
    // CheckpointResponseOptions tests
    
    @Test
    public void testCheckpointResponseOptions_emptyConstructor() {
        CheckpointResponseOptions options = new CheckpointResponseOptions();
        assertThat(options).isNotNull();
    }
    
    @Test
    public void testCheckpointResponseOptions_constructorWithParams() {
        CheckpointResponseOptions options = new CheckpointResponseOptions(true, 5000);

        assertThat(options.sync).isTrue();
        assertThat(options.timeout).isEqualTo(5000);
    }

    @Test
    public void testCheckpointResponseOptions_setFields() {
        CheckpointResponseOptions options = new CheckpointResponseOptions();

        options.sync = false;
        options.timeout = 10000;

        assertThat(options.sync).isFalse();
        assertThat(options.timeout).isEqualTo(10000);
    }
    
    // ComposerApiError tests
    
    @Test
    public void testComposerApiError_emptyConstructor() {
        ComposerApiError error = new ComposerApiError();
        assertThat(error).isNotNull();
    }
    
    @Test
    public void testComposerApiError_constructorWithMessage() {
        ComposerApiError error = new ComposerApiError("Error message");

        assertThat(error.message).isEqualTo("Error message");
    }
    
    // ComposerVerification tests
    
    @Test
    public void testComposerVerification_emptyConstructor() {
        ComposerVerification verification = new ComposerVerification();
        assertThat(verification).isNotNull();
    }
    
    @Test
    public void testComposerVerification_constructorWithParams() {
        Map<String, Object> stepData = Map.of("customMessage", "approved");

        ComposerVerification verification = new ComposerVerification(
            "ver-123",
            Constants.VerificationStatus.COMPLETE,
            Constants.VerificationOutcome.APPROVED,
            stepData
        );

        assertThat(verification.id).isEqualTo("ver-123");
        assertThat(verification.status).isEqualTo(Constants.VerificationStatus.COMPLETE);
        assertThat(verification.outcome).isEqualTo(Constants.VerificationOutcome.APPROVED);
        assertThat(verification.stepData).containsEntry("customMessage", "approved");
    }
    
    @Test
    public void testComposerVerification_setFields() {
        ComposerVerification verification = new ComposerVerification();
        
        verification.id = "ver-456";
        verification.status = Constants.VerificationStatus.PENDING;
        verification.outcome = Constants.VerificationOutcome.PENDING;
        verification.stepData = new java.util.HashMap<>();
        
        assertThat(verification.id).isEqualTo("ver-456");
        assertThat(verification.status).isEqualTo(Constants.VerificationStatus.PENDING);
        assertThat(verification.outcome).isEqualTo(Constants.VerificationOutcome.PENDING);
        assertThat(verification.stepData).isEmpty();
    }
    
    // NotificationRequest tests
    
    @Test
    public void testNotificationRequest_emptyConstructor() {
        NotificationRequest request = new NotificationRequest();
        assertThat(request).isNotNull();
    }
    
    @Test
    public void testNotificationRequest_constructorWithParams() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("type", "test");
        
        com.spreedly.composer.api.ApiEvent event = new com.spreedly.composer.api.ApiEvent("TEST", eventData);
        
        NotificationRequest request = new NotificationRequest(event, "session-123", "user-456", "token-789");
        
        assertThat(request.event).isEqualTo(event);
        assertThat(request.sessionId).isEqualTo("session-123");
        assertThat(request.userId).isEqualTo("user-456");
        assertThat(request.sourceToken).isEqualTo("token-789");
    }
    
    @Test
    public void testNotificationRequest_setFields() {
        NotificationRequest request = new NotificationRequest();
        com.spreedly.composer.api.ApiEvent event = new com.spreedly.composer.api.ApiEvent();
        
        request.event = event;
        request.sessionId = "session-abc";
        request.userId = "user-xyz";
        request.sourceToken = "token-123";
        
        assertThat(request.event).isEqualTo(event);
        assertThat(request.sessionId).isEqualTo("session-abc");
        assertThat(request.userId).isEqualTo("user-xyz");
        assertThat(request.sourceToken).isEqualTo("token-123");
    }
    
    // NotificationResponse tests
    
    @Test
    public void testNotificationResponse_emptyConstructor() {
        NotificationResponse response = new NotificationResponse();
        assertThat(response).isNotNull();
    }
    
    @Test
    public void testNotificationResponse_constructorWithParams() {
        ComposerApiError error = new ComposerApiError("Error");
        
        NotificationResponse response = new NotificationResponse(false, new ComposerApiError[]{error});
        
        assertThat(response.success).isFalse();
        assertThat(response.errors).hasSize(1);
        assertThat(response.version).isEqualTo(Constants.ApiVersion.V1);
    }
    
    @Test
    public void testNotificationResponse_setFields() {
        NotificationResponse response = new NotificationResponse();
        ComposerApiError error = new ComposerApiError("Test");
        
        response.success = true;
        response.errors = new ComposerApiError[]{error};
        response.version = Constants.ApiVersion.V1;
        
        assertThat(response.success).isTrue();
        assertThat(response.errors).hasSize(1);
        assertThat(response.version).isEqualTo(Constants.ApiVersion.V1);
    }
    
    // Constants tests
    
    @Test
    public void testConstants_constructors() {
        // Instantiate Constants to cover class declaration
        Constants constants = new Constants();
        assertThat(constants).isNotNull();
        
        // Instantiate inner classes to cover their declarations
        Constants.ApiVersion apiVersion = new Constants.ApiVersion();
        assertThat(apiVersion).isNotNull();
        
        Constants.VerificationStatus verificationStatus = new Constants.VerificationStatus();
        assertThat(verificationStatus).isNotNull();
        
        Constants.VerificationOutcome verificationOutcome = new Constants.VerificationOutcome();
        assertThat(verificationOutcome).isNotNull();
    }
    
    @Test
    public void testConstants_apiVersion() {
        assertThat(Constants.ApiVersion.V1).isEqualTo("v1");
    }
    
    @Test
    public void testConstants_verificationStatus() {
        assertThat(Constants.VerificationStatus.PENDING).isEqualTo("PENDING");
        assertThat(Constants.VerificationStatus.BLOCKED).isEqualTo("BLOCKED");
        assertThat(Constants.VerificationStatus.COMPLETE).isEqualTo("COMPLETE");
        assertThat(Constants.VerificationStatus.FAILED).isEqualTo("FAILED");
    }
    
    @Test
    public void testConstants_verificationOutcome() {
        assertThat(Constants.VerificationOutcome.APPROVED).isEqualTo("APPROVED");
        assertThat(Constants.VerificationOutcome.DENIED).isEqualTo("DENIED");
        assertThat(Constants.VerificationOutcome.PENDING).isEqualTo("PENDING");
        assertThat(Constants.VerificationOutcome.ERROR).isEqualTo("ERROR");
        assertThat(Constants.VerificationOutcome.USAGE_LIMIT).isEqualTo("USAGE_LIMIT");
        assertThat(Constants.VerificationOutcome.API_LIMIT).isEqualTo("API_LIMIT");
    }
    
    @Test
    public void testConstants_urls() {
        assertThat(Constants.DEFAULT_API_URL).isEqualTo("https://api.dodgeballhq.com");
    }
    
    @Test
    public void testConstants_polling() {
        assertThat(Constants.BASE_CHECKPOINT_POLLING_WAIT_MS).isEqualTo(100);
        assertThat(Constants.MAX_POLLING_WAIT_MS).isEqualTo(2000);
        assertThat(Constants.BASE_CHECKPOINT_MAX_TIMEOUT_MS).isEqualTo(10 * 60 * 1000);
        assertThat(Constants.MAX_FAILURE_COUNT).isEqualTo(3);
    }
}
