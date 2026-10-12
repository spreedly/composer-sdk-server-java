package com.spreedly.composer;

import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.spreedly.composer.api.ComposerServices;
import com.spreedly.composer.messages.*;

/**
 * The primary Composer SDK class for Java applications.
 * 
 * Usage:
 *   Composer composer = Composer.builder()
 *       .setApiKeys("your-secret-key")
 *       .build();
 *   
 *   CompletableFuture<CheckpointResponse> response = composer.checkpoint(request);
 * 
 * @author Spreedly Team
 */
public class Composer {
    
    private final String apiKey;
    private final String baseUrl;
    private final boolean isEnabled;
    private final int requestTimeoutMs;

    /**
     * Private constructor - use builder() to create instances
     *
     * @param apiKey Client server-side API key
     * @param url API base URL
     * @param isEnabled Whether the SDK is enabled
     * @param logLevel Log level for SDK operations
     * @param requestTimeoutMs Per-request HTTP timeout in milliseconds, or null for the default
     */
    protected Composer(String apiKey, String url, boolean isEnabled, Logger.ComposerLogLevel logLevel, Integer requestTimeoutMs) {
        this.apiKey = apiKey;
        this.baseUrl = url;
        this.isEnabled = isEnabled;
        
        // Set logger filter level
        Logger.Severity severity;
        switch (logLevel) {
            case TRACE:
                severity = Logger.Severity.TRACE;
                break;
            case INFO:
                severity = Logger.Severity.INFO;
                break;
            case ERROR:
                severity = Logger.Severity.ERROR;
                break;
            case NONE:
                severity = Logger.Severity.NONE;
                break;
            default:
                severity = Logger.Severity.INFO;
        }
        Logger.filterLevel = severity;

        // Resolved after the logger is configured so the message below respects logLevel.
        // A non-positive timeout would fail every request the SDK makes.
        if (requestTimeoutMs != null && requestTimeoutMs > 0) {
            this.requestTimeoutMs = requestTimeoutMs;
        } else {
            if (requestTimeoutMs != null) {
                Logger.error("requestTimeoutMs must be a positive integer. Ignoring " + requestTimeoutMs
                    + " and using the " + Constants.DEFAULT_REQUEST_TIMEOUT_MS + "ms default.").log();
            }
            this.requestTimeoutMs = Constants.DEFAULT_REQUEST_TIMEOUT_MS;
        }
    }
    
    /**
     * Static constructor for a Builder instance
     * 
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Request to validate access to a protected resource (checkpoint)
     * 
     * @param request Access request properties
     * @return Async function monitoring a Composer workflow execution
     * @throws ComposerMissingParameterException If a required parameter is missing, even when the SDK is disabled
     */
    public CompletableFuture<CheckpointResponse> checkpoint(CheckpointRequest request) {
        if (request == null) {
            throw new ComposerMissingParameterException("request");
        }
        if (StringUtils.isBlank(request.checkpointName)) {
            throw new ComposerMissingParameterException("checkpointName");
        }
        if (request.event == null) {
            throw new ComposerMissingParameterException("event");
        }
        if (StringUtils.isBlank(request.sessionId) && StringUtils.isBlank(request.sourceToken)) {
            throw new ComposerMissingParameterException("Must provide either a sessionId or sourceToken");
        }

        if (!this.isEnabled) {
            // Return approved response when disabled
            CheckpointResponse disabledResponse = new CheckpointResponse(
                true,
                new ComposerApiError[]{},
                Constants.ApiVersion.V1,
                new ComposerVerification(
                    "COMPOSER_IS_DISABLED",
                    Constants.VerificationStatus.COMPLETE,
                    Constants.VerificationOutcome.APPROVED,
                    new HashMap<>()
                ),
                false
            );
            return CompletableFuture.completedFuture(disabledResponse);
        }
        
        ComposerServices.CheckpointCall caller = new ComposerServices.CheckpointCall(
            this.baseUrl,
            this.apiKey,
            request,
            this.requestTimeoutMs
        );
        return CompletableFuture.supplyAsync(caller::call);
    }
    
    /**
     * Send a tracking event to Composer. Missing required fields are logged and returned as
     * {@code success=false}; they do not throw, including when the SDK is disabled.
     *
     * @param request Event request properties
     * @return Async function monitoring the event submission
     */
    public CompletableFuture<NotificationResponse> event(NotificationRequest request) {
        if (request == null || request.event == null) {
            return unsentEvent("no event was provided");
        }
        if (StringUtils.isBlank(request.event.type)) {
            return unsentEvent("event.type is missing");
        }
        if (StringUtils.isBlank(request.sessionId) && StringUtils.isBlank(request.sourceToken)) {
            return unsentEvent("Must provide either a sessionId or sourceToken");
        }

        if (!this.isEnabled) {
            // Return success when disabled
            NotificationResponse disabledResponse = new NotificationResponse(true, new ComposerApiError[]{});
            return CompletableFuture.completedFuture(disabledResponse);
        }
        
        ComposerServices.NotificationCall caller = new ComposerServices.NotificationCall(
            this.baseUrl,
            this.apiKey,
            request,
            this.requestTimeoutMs
        );
        return CompletableFuture.supplyAsync(caller::call);
    }
    
    /**
     * Deprecated alias for event() - maintained for backward compatibility
     * 
     * @deprecated Use {@link #event(NotificationRequest)} instead
     * @param request Event request properties
     * @return Async function monitoring the event submission
     */
    @Deprecated
    public CompletableFuture<NotificationResponse> notify(NotificationRequest request) {
        return this.event(request);
    }
    
    /**
     * Deprecated alias for event() - maintained for backward compatibility
     * 
     * @deprecated Use {@link #event(NotificationRequest)} instead
     * @param request Event request properties
     * @return Async function monitoring the event submission
     */
    @Deprecated
    public CompletableFuture<NotificationResponse> track(NotificationRequest request) {
        return this.event(request);
    }
    
    /**
     * Accessor to determine whether access is allowed
     * 
     * @param checkpointResponse The checkpoint response
     * @return True if the workflow allows access
     */
    public static boolean isAllowed(CheckpointResponse checkpointResponse) {
        return (
            checkpointResponse.success &&
            checkpointResponse.verification != null &&
            stringsEqual(
                checkpointResponse.verification.status,
                Constants.VerificationStatus.COMPLETE) &&
            stringsEqual(
                checkpointResponse.verification.outcome,
                Constants.VerificationOutcome.APPROVED)
        );
    }
    
    /**
     * Accessor indicating whether the checkpoint workflow is still processing
     * 
     * @param checkpointResponse The checkpoint response
     * @return True if the workflow is still in process with no verdict yet
     */
    public static boolean isRunning(CheckpointResponse checkpointResponse) {
        if (checkpointResponse.success && checkpointResponse.verification != null) {
            String status = checkpointResponse.verification.status;
            return (Constants.VerificationStatus.PENDING.equals(status) ||
                    Constants.VerificationStatus.BLOCKED.equals(status)) &&
                   Constants.VerificationOutcome.PENDING.equals(checkpointResponse.verification.outcome);
        }
        return false;
    }
    
    /**
     * Accessor to determine whether the request was denied
     * 
     * @param checkpointResponse The checkpoint response
     * @return True if the workflow denied access
     */
    public static boolean isDenied(CheckpointResponse checkpointResponse) {
        if (checkpointResponse.success && checkpointResponse.verification != null) {
            return stringsEqual(
                checkpointResponse.verification.outcome,
                Constants.VerificationOutcome.DENIED
            );
        }
        return false;
    }
    
    /**
     * Accessor to determine whether no conclusion was reached
     * 
     * @param checkpointResponse The checkpoint response
     * @return True if the workflow completed successfully but no access decision was taken
     */
    public static boolean isUndecided(CheckpointResponse checkpointResponse) {
        return (
            checkpointResponse.success &&
            checkpointResponse.verification != null &&
            stringsEqual(
                checkpointResponse.verification.status,
                Constants.VerificationStatus.COMPLETE) &&
            stringsEqual(
                checkpointResponse.verification.outcome,
                Constants.VerificationOutcome.PENDING)
        );
    }
    
    /**
     * Indicates that a workflow execution error occurred
     * 
     * @param checkpointResponse The checkpoint response
     * @return True if the workflow received an error
     */
    public static boolean hasError(CheckpointResponse checkpointResponse) {
        return (
            !checkpointResponse.success ||
            (checkpointResponse.verification != null &&
             stringsEqual(
                checkpointResponse.verification.outcome,
                Constants.VerificationOutcome.ERROR))
        );
    }
    
    /**
     * Indicates whether the checkpoint timed out
     * 
     * @param checkpointResponse The checkpoint response
     * @return True if timed out
     */
    public static boolean isTimeout(CheckpointResponse checkpointResponse) {
        return checkpointResponse.isTimeout;
    }
    
    /**
     * Logs that an event was not sent and returns a failed response without calling the API.
     */
    private static CompletableFuture<NotificationResponse> unsentEvent(String reason) {
        Logger.error("Composer event was not sent: " + reason).log();
        return CompletableFuture.completedFuture(new NotificationResponse(false, new ComposerApiError[]{
            new ComposerApiError(reason)
        }));
    }

    /**
     * Utility method for case-insensitive string comparison
     */
    private static boolean stringsEqual(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }
        return s1.equalsIgnoreCase(s2);
    }
    
    /**
     * Builder class for constructing Composer instances
     */
    public static class Builder {
        private String apiKey;
        private String dbUrl;
        private boolean isEnabled = true;
        private Logger.ComposerLogLevel logLevel = Logger.ComposerLogLevel.INFO;
        private Integer requestTimeoutMs;

        /**
         * Set the API key
         * 
         * @param apiKey Secret API key
         * @return This builder
         */
        public Builder setApiKeys(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        /**
         * Set a custom API URL (optional)
         * 
         * @param url Custom API URL
         * @return This builder
         */
        public Builder setApiUrl(String url) {
            this.dbUrl = url;
            return this;
        }
        
        /**
         * Enable or disable the SDK (optional, default: true)
         * 
         * @param enabled Whether to enable the SDK
         * @return This builder
         */
        public Builder setEnabled(boolean enabled) {
            this.isEnabled = enabled;
            return this;
        }
        
        /**
         * Set the log level (optional, default: INFO)
         * 
         * @param logLevel Log level for SDK operations
         * @return This builder
         */
        public Builder setLogLevel(Logger.ComposerLogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Set how long a single HTTP request may take, in milliseconds (optional, default: 30000).
         * Applies to every call the SDK makes. This is not the checkpoint polling budget.
         *
         * @param requestTimeoutMs Per-request HTTP timeout in milliseconds
         * @return This builder
         */
        public Builder setRequestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        /**
         * Build the Composer instance
         * 
         * @return Configured Composer instance
         * @throws ComposerMissingConfigException If the API key is missing
         */
        public Composer build() {
            if (StringUtils.isEmpty(this.apiKey)) {
                throw new ComposerMissingConfigException("apiKey");
            }
            
            String url = StringUtils.isEmpty(this.dbUrl) ? 
                Constants.DEFAULT_API_URL : this.dbUrl;
            
            return new Composer(this.apiKey, url, this.isEnabled, this.logLevel, this.requestTimeoutMs);
        }
    }
}
