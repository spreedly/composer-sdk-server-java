package com.spreedly.composer.messages;

/**
 * Represents an API error returned from the Composer service
 */
public class ComposerApiError {

    /**
     * Error code (if available)
     */
    public Integer code;

    /**
     * Error message
     */
    public String message;

    /**
     * Constructor
     */
    public ComposerApiError() {
    }

    /**
     * Constructor with message
     *
     * @param message Error message
     */
    public ComposerApiError(String message) {
        this.message = message;
    }
}
