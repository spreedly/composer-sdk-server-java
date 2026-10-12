package com.spreedly.composer.messages;

/**
 * Response from sending a tracking event
 */
public class NotificationResponse {
    
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
     * Constructor
     */
    public NotificationResponse() {
    }
    
    /**
     * Constructor
     *
     * @param success Whether successful
     * @param errors Array of errors
     */
    public NotificationResponse(boolean success, ComposerApiError[] errors) {
        this.success = success;
        this.errors = errors;
        this.version = Constants.ApiVersion.V1;
    }
}
