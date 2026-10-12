package com.spreedly.composer.api;

import java.util.Map;

/**
 * Represents a tracking event
 */
public class ApiEvent {
    
    /**
     * Event type/name
     */
    public String type;
    
    /**
     * Event data
     */
    public Map<String, Object> data;
    
    /**
     * Event timestamp in ms since epoch
     */
    public Long eventTime;
    
    /**
     * Constructor
     */
    public ApiEvent() {
    }
    
    /**
     * Constructor
     *
     * @param type Event type
     * @param data Event data
     */
    public ApiEvent(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
        this.eventTime = System.currentTimeMillis();
    }
    
    /**
     * Constructor with event time
     *
     * @param type Event type
     * @param data Event data
     * @param eventTime Event timestamp
     */
    public ApiEvent(String type, Map<String, Object> data, Long eventTime) {
        this.type = type;
        this.data = data;
        this.eventTime = eventTime;
    }
}
