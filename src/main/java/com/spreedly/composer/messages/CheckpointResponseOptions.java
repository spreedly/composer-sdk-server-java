package com.spreedly.composer.messages;

/**
 * Options for customizing checkpoint response behavior
 */
public class CheckpointResponseOptions {

    /**
     * Whether to wait for a synchronous response
     */
    public Boolean sync;

    /**
     * How long to keep polling for an unresolved verification, in milliseconds
     */
    public Integer timeout;

    /**
     * Constructor
     */
    public CheckpointResponseOptions() {
    }

    /**
     * Constructor
     *
     * @param sync Whether to wait synchronously
     * @param timeout Timeout in ms
     */
    public CheckpointResponseOptions(Boolean sync, Integer timeout) {
        this.sync = sync;
        this.timeout = timeout;
    }
}
