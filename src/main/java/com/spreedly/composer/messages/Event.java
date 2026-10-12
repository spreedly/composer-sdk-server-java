package com.spreedly.composer.messages;

import java.util.Map;

/**
 * Represents an event sent to a checkpoint
 */
public class Event {
    
    /**
     * IP address where the request originated
     */
    public String ip;
    
    /**
     * Arbitrary data to include with the event
     */
    public Map<String, Object> data;
    
    /**
     * Constructor
     */
    public Event() {
    }
    
    /**
     * Constructor
     *
     * @param ip IP address
     * @param data Event data
     */
    public Event(String ip, Map<String, Object> data) {
        this.ip = ip;
        this.data = data;
    }
}
