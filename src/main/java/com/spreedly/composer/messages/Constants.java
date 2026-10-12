package com.spreedly.composer.messages;

/**
 * Constants used throughout the Composer SDK
 */
public class Constants {

    /**
     * SDK version, sent in the Composer-SDK-Version header. Keep in sync with pom.xml.
     */
    public static final String SDK_VERSION = "1.0.0";

    /**
     * API Version constants
     */
    public static class ApiVersion {
        public static final String V1 = "v1";
    }
    
    /**
     * Verification Status constants
     */
    public static class VerificationStatus {
        public static final String PENDING = "PENDING";
        public static final String BLOCKED = "BLOCKED";
        public static final String COMPLETE = "COMPLETE";
        public static final String FAILED = "FAILED";
    }
    
    /**
     * Verification Outcome constants
     */
    public static class VerificationOutcome {
        public static final String APPROVED = "APPROVED";
        public static final String DENIED = "DENIED";
        public static final String PENDING = "PENDING";
        public static final String ERROR = "ERROR";
        public static final String USAGE_LIMIT = "USAGE_LIMIT";
        public static final String API_LIMIT = "API_LIMIT";
    }
    
    /**
     * Default API URL
     */
    public static final String DEFAULT_API_URL = "https://api.dodgeballhq.com";

    /**
     * Default per-request HTTP timeout
     */
    public static final int DEFAULT_REQUEST_TIMEOUT_MS = 30 * 1000; // 30 seconds

    /**
     * Polling constants
     */
    public static final int BASE_CHECKPOINT_POLLING_WAIT_MS = 100;
    public static final int MAX_POLLING_WAIT_MS = 2000;
    public static final int BASE_CHECKPOINT_MAX_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes
    public static final int MAX_FAILURE_COUNT = 3;
}
