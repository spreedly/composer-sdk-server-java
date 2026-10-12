package com.spreedly.composer.api;

import com.spreedly.composer.messages.Constants;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for API classes
 */
public class ApiTest {
    
    @Test
    public void testComposerServices_constructor() {
        // Instantiate to cover class declaration
        ComposerServices services = new ComposerServices();
        assertThat(services).isNotNull();
    }
    
    @Test
    public void testRedactSensitiveFields_masksNextSteps() {
        Map<String, Object> config = new HashMap<>();
        config.put("clientSecret", "vs_secret_value");
        
        Map<String, Object> step = new HashMap<>();
        step.put("name", "STRIPE_IDENTITY");
        step.put("config", config);
        
        Map<String, Object> verification = new HashMap<>();
        verification.put("id", "verification123");
        verification.put("status", "BLOCKED");
        verification.put("nextSteps", new Object[]{step});
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("verification", verification);
        
        String redacted = ComposerServices.redactSensitiveFields(payload);
        
        assertThat(redacted).contains("BLOCKED");
        assertThat(redacted).contains("[REDACTED]");
        assertThat(redacted).doesNotContain("vs_secret_value");
    }
    
    @Test
    public void testBuildClient_nonPositiveTimeoutUsesDefault() {
        // OkHttp treats 0 as no timeout, so a non-positive value must not reach it
        for (int timeout : new int[]{0, -1}) {
            OkHttpClient client = ComposerServices.buildClient(timeout);

            assertThat(client.callTimeoutMillis()).isEqualTo(Constants.DEFAULT_REQUEST_TIMEOUT_MS);
            assertThat(client.connectTimeoutMillis()).isEqualTo(Constants.DEFAULT_REQUEST_TIMEOUT_MS);
            assertThat(client.readTimeoutMillis()).isEqualTo(Constants.DEFAULT_REQUEST_TIMEOUT_MS);
            assertThat(client.writeTimeoutMillis()).isEqualTo(Constants.DEFAULT_REQUEST_TIMEOUT_MS);
        }
    }

    @Test
    public void testApiEvent_emptyConstructor() {
        ApiEvent event = new ApiEvent();
        assertThat(event).isNotNull();
    }
    
    @Test
    public void testApiEvent_constructorWithTypeAndData() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        
        ApiEvent event = new ApiEvent("TEST_EVENT", data);
        
        assertThat(event.type).isEqualTo("TEST_EVENT");
        assertThat(event.data).isEqualTo(data);
        assertThat(event.eventTime).isNotNull();
        assertThat(event.eventTime).isGreaterThan(0L);
    }
    
    @Test
    public void testApiEvent_constructorWithEventTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("test", "data");
        Long customTime = 1234567890L;
        
        ApiEvent event = new ApiEvent("CUSTOM_EVENT", data, customTime);
        
        assertThat(event.type).isEqualTo("CUSTOM_EVENT");
        assertThat(event.data).isEqualTo(data);
        assertThat(event.eventTime).isEqualTo(customTime);
    }
    
    @Test
    public void testApiEvent_setFields() {
        ApiEvent event = new ApiEvent();
        Map<String, Object> data = new HashMap<>();
        data.put("field", "value");
        
        event.type = "UPDATED_EVENT";
        event.data = data;
        event.eventTime = 9876543210L;
        
        assertThat(event.type).isEqualTo("UPDATED_EVENT");
        assertThat(event.data).containsEntry("field", "value");
        assertThat(event.eventTime).isEqualTo(9876543210L);
    }
}
