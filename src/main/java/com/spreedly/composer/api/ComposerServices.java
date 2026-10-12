package com.spreedly.composer.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.spreedly.composer.Logger;
import com.spreedly.composer.messages.*;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Internal service classes for making API calls to Composer
 */
public class ComposerServices {
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson gson = new GsonBuilder().create();
    
    /**
     * Helper to construct the full API URL
     */
    private static String constructApiUrl(String baseUrl, String apiVersion) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + apiVersion + "/";
    }
    
    /**
     * Fields that carry end-user information and are masked before anything is logged.
     * nextSteps is included because a blocked checkpoint returns integration config
     * (including provider client secrets) alongside the full client integration bundle.
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of("ip", "data", "stepData", "nextSteps");
    
    private static final String REDACTED = "[REDACTED]";
    
    /**
     * Renders a payload as JSON with end-user fields masked, so verbose logs show the
     * shape of a request or response without exposing PII
     */
    static String redactSensitiveFields(Object payload) {
        return maskSensitiveFields(gson.toJsonTree(payload)).toString();
    }
    
    /**
     * Walks a JSON tree and replaces every sensitive field
     */
    private static JsonElement maskSensitiveFields(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject masked = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (SENSITIVE_FIELDS.contains(entry.getKey())) {
                    masked.add(entry.getKey(), new JsonPrimitive(REDACTED));
                } else {
                    masked.add(entry.getKey(), maskSensitiveFields(entry.getValue()));
                }
            }
            return masked;
        }
        
        if (element.isJsonArray()) {
            JsonArray masked = new JsonArray();
            for (JsonElement entry : element.getAsJsonArray()) {
                masked.add(maskSensitiveFields(entry));
            }
            return masked;
        }
        
        return element;
    }
    
    private static final String REQUEST_FAILED_BODY =
        "{\"success\":false,\"errors\":[{\"code\":500,\"message\":\"Internal Server Error\"}]}";

    /**
     * Builds a client whose timeout caps the whole call, not just each connect, read or write.
     * A non-positive timeout falls back to the default, since OkHttp treats 0 as no timeout.
     */
    static OkHttpClient buildClient(int requestTimeoutMs) {
        if (requestTimeoutMs <= 0) {
            requestTimeoutMs = Constants.DEFAULT_REQUEST_TIMEOUT_MS;
        }
        return new OkHttpClient.Builder()
            .callTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
            .connectTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    /**
     * Sends a request and returns its body. A network error or timeout returns the standard
     * failure body instead of throwing, so it is handled like any other failed response.
     */
    private static String send(OkHttpClient client, Request request) {
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) {
            Logger.error("There was an error making a request.", e).log();
            return REQUEST_FAILED_BODY;
        }
    }

    /**
     * Parses a checkpoint response body, returning null when it is not valid JSON or is empty
     * (e.g. an HTML error page from a proxy). The body is never logged.
     */
    private static CheckpointResponse parseCheckpointResponse(String body) {
        CheckpointResponse parsed = null;
        try {
            parsed = gson.fromJson(body, CheckpointResponse.class);
        } catch (JsonSyntaxException e) {
            // fall through to the log below; the exception message can echo the body
        }
        if (parsed == null) {
            Logger.error("The Composer API returned a response that is not valid JSON.").log();
        }
        return parsed;
    }

    /**
     * Helper to build request headers
     */
    private static Request.Builder buildHeaders(String apiKey, String verificationId, String sourceToken, String customerId, String sessionId) {
        try {
            return addHeaders(apiKey, verificationId, sourceToken, customerId, sessionId);
        } catch (IllegalArgumentException e) {
            // OkHttp's message includes the header value, which can be the secret key
            throw new IllegalArgumentException("A request header has a character that can't be sent, such as a newline.");
        }
    }

    private static Request.Builder addHeaders(String apiKey, String verificationId, String sourceToken, String customerId, String sessionId) {
        Request.Builder builder = new Request.Builder()
            .addHeader("Dodgeball-Secret-Key", apiKey)
            .addHeader("Composer-SDK-Version", Constants.SDK_VERSION)
            .addHeader("Composer-SDK-Source", "java-sdk")
            .addHeader("Content-Type", "application/json");
        
        if (verificationId != null && !verificationId.isEmpty()) {
            builder.addHeader("Dodgeball-Verification-Id", verificationId);
        }
        if (sourceToken != null && !sourceToken.isEmpty()) {
            builder.addHeader("Dodgeball-Source-Token", sourceToken);
        }
        if (customerId != null && !customerId.isEmpty()) {
            builder.addHeader("Dodgeball-Customer-Id", customerId);
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            builder.addHeader("Dodgeball-Session-Id", sessionId);
        }
        
        return builder;
    }
    
    /**
     * Callable class for checkpoint requests
     */
    public static class CheckpointCall {
        private final String baseUrl;
        private final String apiKey;
        private final CheckpointRequest request;
        private final int requestTimeoutMs;

        public CheckpointCall(String baseUrl, String apiKey, CheckpointRequest request, int requestTimeoutMs) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.request = request;
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public CheckpointResponse call() {
            OkHttpClient client = buildClient(requestTimeoutMs);

            try {
                Map<String, Object> eventBody = new HashMap<>();
                eventBody.put("type", request.checkpointName);
                if (request.event.ip != null && !request.event.ip.isEmpty()) {
                    eventBody.put("ip", request.event.ip);
                }
                if (request.event.data != null) {
                    eventBody.put("data", request.event.data);
                }

                Map<String, Object> apiOptions = new HashMap<>();
                boolean sync = request.options != null && Boolean.TRUE.equals(request.options.sync);
                apiOptions.put("sync", sync);

                Map<String, Object> apiBody = new HashMap<>();
                apiBody.put("event", eventBody);
                apiBody.put("options", apiOptions);

                String jsonBody = gson.toJson(apiBody);
                RequestBody body = RequestBody.create(jsonBody, JSON);
                
                if (Logger.isTraceEnabled()) {
                    Logger.trace("Request body: " + redactSensitiveFields(apiBody)).log();
                }
                
                // Build initial checkpoint request
                String url = constructApiUrl(baseUrl, Constants.ApiVersion.V1) + "checkpoint";
                Request httpRequest = buildHeaders(
                    apiKey,
                    request.useVerificationId,
                    request.sourceToken,
                    request.userId,
                    request.sessionId
                )
                    .url(url)
                    .post(body)
                    .build();
                
                // The polling budget includes the initial request
                long checkpointStartTime = System.currentTimeMillis();

                // Make initial checkpoint request
                CheckpointResponse response = parseCheckpointResponse(send(client, httpRequest));
                if (response == null) {
                    return gson.fromJson(REQUEST_FAILED_BODY, CheckpointResponse.class);
                }

                if (Logger.isTraceEnabled()) {
                    Logger.trace("Initial checkpoint response: " + redactSensitiveFields(response)).log();
                }
                
                if (!response.success || response.verification == null) {
                    return response;
                }
                
                // Only poll PENDING; BLOCKED waits on a client action such as MFA.
                boolean needsPolling = Constants.VerificationStatus.PENDING.equals(response.verification.status);

                if (!needsPolling) {
                    return response;
                }
                
                // Poll for completion
                String verificationId = response.verification.id;
                long checkpointTimeout = (request.options != null && request.options.timeout != null && request.options.timeout > 0) 
                    ? request.options.timeout 
                    : Constants.BASE_CHECKPOINT_MAX_TIMEOUT_MS;
                
                int currentPollingWait = Constants.BASE_CHECKPOINT_POLLING_WAIT_MS;
                int numFailures = 0;
                boolean hasNeverPolled = true;
                boolean isResolved = false;
                
                while (!isResolved && 
                       (System.currentTimeMillis() - checkpointStartTime < checkpointTimeout || hasNeverPolled) &&
                       numFailures < Constants.MAX_FAILURE_COUNT) {
                    
                    // Wait before polling
                    Thread.sleep(currentPollingWait);
                    currentPollingWait = Math.min(currentPollingWait * 2, Constants.MAX_POLLING_WAIT_MS);
                    hasNeverPolled = false;
                    
                    // Poll for verification status
                    String pollUrl = constructApiUrl(baseUrl, Constants.ApiVersion.V1) + "verification/" + verificationId;
                    Request pollRequest = buildHeaders(
                        apiKey,
                        request.useVerificationId,
                        request.sourceToken,
                        request.userId,
                        request.sessionId
                    )
                        .url(pollUrl)
                        .get()
                        .build();
                    
                    response = parseCheckpointResponse(send(client, pollRequest));
                    
                    if (response != null && response.success && response.verification != null) {
                        String status = response.verification.status;
                        if (status == null) {
                            numFailures++;
                        } else {
                            isResolved = !Constants.VerificationStatus.PENDING.equals(status);
                            numFailures = 0; // reset on successful response
                        }
                    } else {
                        numFailures++;
                    }
                }
                
                if (numFailures >= Constants.MAX_FAILURE_COUNT) {
                    Logger.error("Service Unavailable: Maximum retry count exceeded").log();
                    ComposerApiError error = new ComposerApiError("Service Unavailable: Maximum retry count exceeded");
                    error.code = 503;
                    return new CheckpointResponse(false, new ComposerApiError[]{error}, Constants.ApiVersion.V1, null, true);
                }
                
                // The polling budget can run out right after a non-JSON poll.
                if (response == null) {
                    return gson.fromJson(REQUEST_FAILED_BODY, CheckpointResponse.class);
                }

                if (Logger.isTraceEnabled()) {
                    Logger.trace("Returning response: " + redactSensitiveFields(response)).log();
                }

                return response;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return gson.fromJson(REQUEST_FAILED_BODY, CheckpointResponse.class);
            } catch (Exception e) {
                Logger.error("There was an error making a request.", e).log();
                return gson.fromJson(REQUEST_FAILED_BODY, CheckpointResponse.class);
            }
        }
    }
    
    /**
     * Callable class for notification/event requests
     */
    public static class NotificationCall {
        private final String baseUrl;
        private final String apiKey;
        private final NotificationRequest request;
        private final int requestTimeoutMs;

        public NotificationCall(String baseUrl, String apiKey, NotificationRequest request, int requestTimeoutMs) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.request = request;
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public NotificationResponse call() {
            OkHttpClient client = buildClient(requestTimeoutMs);

            try {
                // Ensure event has timestamp
                if (request.event != null && request.event.eventTime == null) {
                    request.event.eventTime = System.currentTimeMillis();
                }
                
                // Prepare request body
                String jsonBody = gson.toJson(request.event);
                RequestBody body = RequestBody.create(jsonBody, JSON);
                
                // Build request
                String url = constructApiUrl(baseUrl, Constants.ApiVersion.V1) + "track";
                Request httpRequest = buildHeaders(
                    apiKey,
                    "",
                    request.sourceToken,
                    request.userId,
                    request.sessionId
                )
                    .url(url)
                    .post(body)
                    .build();
                
                // Make request. send() returns only the body, so the HTTP status isn't available here.
                String responseBody = send(client, httpRequest);
                NotificationResponse response = gson.fromJson(responseBody, NotificationResponse.class);
                // send() returns this exact constant (same reference) after it has already logged a
                // request that never got a response, so skip the rejection log in that case.
                boolean requestFailed = responseBody == REQUEST_FAILED_BODY;
                if (!requestFailed && (response == null || !response.success)) {
                    Logger.error("Composer event was not accepted by the API.").log();
                }
                return response;

            } catch (Exception e) {
                // Only an unparseable response body means the API answered; anything else failed
                // before a request was sent (e.g. an invalid API URL).
                String message = e instanceof JsonSyntaxException
                    ? "Composer event was not accepted by the API."
                    : "Composer event was not sent: the request could not be made.";
                Logger.error(message, e).log();
                return gson.fromJson(REQUEST_FAILED_BODY, NotificationResponse.class);
            }
        }
    }
}
