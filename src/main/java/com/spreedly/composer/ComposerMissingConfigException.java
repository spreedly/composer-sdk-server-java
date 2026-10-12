package com.spreedly.composer;

/**
 * Thrown when a required configuration value is missing from the builder
 */
public class ComposerMissingConfigException extends IllegalStateException {

    public ComposerMissingConfigException(String configName) {
        super("Composer SDK Error\nMissing configuration: " + configName);
    }
}
