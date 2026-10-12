package com.spreedly.composer.messages;

/**
 * Represents a request to call a checkpoint
 */
public class CheckpointRequest {
    
    /**
     * Event data
     */
    public Event event;
    
    /**
     * Name of the checkpoint to call
     */
    public String checkpointName;
    
    /**
     * Session ID
     */
    public String sessionId;
    
    /**
     * User ID
     */
    public String userId;
    
    /**
     * Source token from the client SDK
     */
    public String sourceToken;
    
    /**
     * Verification ID to reuse from a previous verification
     */
    public String useVerificationId;
    
    /**
     * Response options
     */
    public CheckpointResponseOptions options;
    
    /**
     * Constructor
     */
    public CheckpointRequest() {
    }
    
    /**
     * Constructor with required fields
     *
     * @param event Event data
     * @param checkpointName Checkpoint name
     * @param sessionId Session ID
     * @param userId User ID
     */
    public CheckpointRequest(Event event, String checkpointName, String sessionId, String userId) {
        this.event = event;
        this.checkpointName = checkpointName;
        this.sessionId = sessionId;
        this.userId = userId;
    }
    
    /**
     * Constructor with all fields
     *
     * @param event Event data
     * @param checkpointName Checkpoint name
     * @param sessionId Session ID
     * @param userId User ID
     * @param sourceToken Source token
     * @param useVerificationId Verification ID to reuse
     */
    public CheckpointRequest(Event event, String checkpointName, String sessionId, String userId, 
                            String sourceToken, String useVerificationId) {
        this.event = event;
        this.checkpointName = checkpointName;
        this.sessionId = sessionId;
        this.userId = userId;
        this.sourceToken = sourceToken;
        this.useVerificationId = useVerificationId;
    }
}
