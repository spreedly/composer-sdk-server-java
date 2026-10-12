package com.spreedly.composer.messages;

/**
 * Response from calling a checkpoint
 */
public class CheckpointResponse {
    
    /**
     * Whether the request was successful
     */
    public boolean success;
    
    /**
     * Array of errors (if any)
     */
    public ComposerApiError[] errors;
    
    /**
     * API version used
     */
    public String version;
    
    /**
     * Verification result
     */
    public ComposerVerification verification;
    
    /**
     * Whether the checkpoint timed out
     */
    public boolean isTimeout;
    
    /**
     * Constructor
     */
    public CheckpointResponse() {
    }
    
    /**
     * Constructor with all fields
     *
     * @param success Whether successful
     * @param errors Array of errors
     * @param version API version
     * @param verification Verification result
     * @param isTimeout Whether timed out
     */
    public CheckpointResponse(boolean success, ComposerApiError[] errors, String version,
                             ComposerVerification verification, boolean isTimeout) {
        this.success = success;
        this.errors = errors;
        this.version = version;
        this.verification = verification;
        this.isTimeout = isTimeout;
    }
}
