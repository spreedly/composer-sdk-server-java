package com.spreedly.composer;

/**
 * Thrown when a required parameter is missing from a checkpoint or event request
 */
public class ComposerMissingParameterException extends IllegalArgumentException {

    public ComposerMissingParameterException(String parameter) {
        super("Composer SDK Error\nMissing parameter: " + parameter);
    }
}
