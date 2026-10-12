package com.spreedly.composer.messages;

import java.util.List;
import java.util.Map;

/**
 * Represents a verification result from Composer
 */
public class ComposerVerification {
    
    /**
     * Verification ID
     */
    public String id;
    
    /**
     * Verification status (PENDING, BLOCKED, COMPLETE, FAILED)
     */
    public String status;
    
    /**
     * Verification outcome (APPROVED, DENIED, PENDING, ERROR)
     */
    public String outcome;
    
    /**
     * Step data for the verification (object-shaped on the API)
     */
    public Map<String, Object> stepData;

    /**
     * Steps the Client SDK runs next, such as MFA
     */
    public List<Map<String, Object>> nextSteps;

    /**
     * Constructor
     */
    public ComposerVerification() {
    }
    
    /**
     * Constructor
     *
     * @param id Verification ID
     * @param status Verification status
     * @param outcome Verification outcome
     * @param stepData Step data
     */
    public ComposerVerification(String id, String status, String outcome, Map<String, Object> stepData) {
        this.id = id;
        this.status = status;
        this.outcome = outcome;
        this.stepData = stepData;
    }
}
