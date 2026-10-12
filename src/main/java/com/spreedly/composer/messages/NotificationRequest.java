package com.spreedly.composer.messages;

import com.spreedly.composer.api.ApiEvent;

/**
 * Request to send a tracking event
 */
public class NotificationRequest {
    
    /**
     * Event to track
     */
    public ApiEvent event;
    
    /**
     * Session ID
     */
    public String sessionId;
    
    /**
     * User ID
     */
    public String userId;
    
    /**
     * Source token
     */
    public String sourceToken;
    
    /**
     * Constructor
     */
    public NotificationRequest() {
    }
    
    /**
     * Constructor
     *
     * @param event Event to track
     * @param sessionId Session ID
     * @param userId User ID
     * @param sourceToken Source token
     */
    public NotificationRequest(ApiEvent event, String sessionId, String userId, String sourceToken) {
        this.event = event;
        this.sessionId = sessionId;
        this.userId = userId;
        this.sourceToken = sourceToken;
    }
}
