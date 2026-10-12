package com.spreedly.composer;

import org.apache.commons.lang3.StringUtils;
import com.spreedly.composer.messages.*;
import java.util.concurrent.CompletableFuture;

/**
 * Backward compatibility wrapper - delegates to Composer
 * 
 * @deprecated Use {@link Composer} instead. This class is maintained for backward compatibility.
 * 
 * Usage:
 *   Dodgeball dodgeball = Dodgeball.builder()
 *       .setApiKeys("your-secret-key")
 *       .build();
 */
@Deprecated
public class Dodgeball {
    
    private final Composer composer;
    
    /**
     * Constructor
     * 
     * @param composer The underlying Composer instance
     */
    protected Dodgeball(Composer composer) {
        this.composer = composer;
    }
    
    /**
     * Static constructor for a Builder instance
     * 
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Request to validate access to a protected resource (checkpoint)
     * 
     * @param request Access request properties
     * @return Async function monitoring a Composer workflow execution
     */
    public CompletableFuture<CheckpointResponse> checkpoint(CheckpointRequest request) {
        return composer.checkpoint(request);
    }
    
    /**
     * Send a tracking event to Composer
     * 
     * @param request Event request properties
     * @return Async function monitoring the event submission
     */
    public CompletableFuture<NotificationResponse> event(NotificationRequest request) {
        return composer.event(request);
    }
    
    /**
     * Deprecated alias for event()
     * 
     * @deprecated Use {@link #event(NotificationRequest)} instead
     */
    @Deprecated
    public CompletableFuture<NotificationResponse> notify(NotificationRequest request) {
        return composer.notify(request);
    }
    
    /**
     * Deprecated alias for event()
     * 
     * @deprecated Use {@link #event(NotificationRequest)} instead
     */
    @Deprecated
    public CompletableFuture<NotificationResponse> track(NotificationRequest request) {
        return composer.track(request);
    }
    
    /**
     * Builder for Dodgeball (backward compatibility)
     */
    public static class Builder {
        private String apiKey;
        private String dbUrl;
        private boolean isEnabled = true;
        private Logger.ComposerLogLevel logLevel = Logger.ComposerLogLevel.INFO;
        
        /**
         * Set the API key
         * 
         * @param apiKey Secret API key
         * @return This builder
         */
        public Builder setApiKeys(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        /**
         * Set a custom API URL (optional)
         * 
         * @param url Custom API URL
         * @return This builder
         */
        public Builder setApiUrl(String url) {
            this.dbUrl = url;
            return this;
        }
        
        /**
         * Enable or disable the SDK (optional, default: true)
         * 
         * @param enabled Whether to enable the SDK
         * @return This builder
         */
        public Builder setEnabled(boolean enabled) {
            this.isEnabled = enabled;
            return this;
        }
        
        /**
         * Set the log level (optional, default: INFO)
         * 
         * @param logLevel Log level for SDK operations
         * @return This builder
         */
        public Builder setLogLevel(Logger.ComposerLogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }
        
        /**
         * Build the Dodgeball instance
         * 
         * @composerBuilder.setLogLevel(this.logLevel);
            return Configured Dodgeball instance
         */
        public Dodgeball build() {
            Composer.Builder composerBuilder = Composer.builder()
                .setApiKeys(this.apiKey);
            
            if (!StringUtils.isEmpty(this.dbUrl)) {
                composerBuilder.setApiUrl(this.dbUrl);
            }
            
            composerBuilder.setEnabled(this.isEnabled);
            composerBuilder.setLogLevel(this.logLevel);
            
            return new Dodgeball(composerBuilder.build());
        }
    }
}
