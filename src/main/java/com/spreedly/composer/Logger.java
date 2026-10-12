package com.spreedly.composer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Logger for Composer SDK
 * Provides structured logging with configurable log levels
 */
public class Logger {
    
    /**
     * Log level enumeration
     */
    public enum ComposerLogLevel {
        TRACE,
        INFO,
        ERROR,
        NONE
    }
    
    /**
     * Backward compatibility alias
     */
    public static class DodgeballLogLevel {
        public static final ComposerLogLevel TRACE = ComposerLogLevel.TRACE;
        public static final ComposerLogLevel INFO = ComposerLogLevel.INFO;
        public static final ComposerLogLevel ERROR = ComposerLogLevel.ERROR;
        public static final ComposerLogLevel NONE = ComposerLogLevel.NONE;
    }
    
    /**
     * Numeric severity levels for filtering
     */
    public enum Severity {
        TRACE(0),
        INFO(1),
        ERROR(2),
        NONE(3);
        
        private final int value;
        
        Severity(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Current filter level - only logs at or above this level will be output
     */
    public static Severity filterLevel = Severity.INFO;
    
    /**
     * Reports whether a trace entry would be written. Callers check this before
     * assembling an expensive trace message, because the message is built before
     * log() gets a chance to discard it.
     */
    public static boolean isTraceEnabled() {
        return Severity.TRACE.getValue() >= filterLevel.getValue();
    }
    
    /**
     * Log entry builder class
     */
    public static class LogEntry {
        private Severity severity = Severity.INFO;
        private String message = "";
        private Throwable error = null;
        private Map<String, Object> parameters = new HashMap<>();
        private Instant date = Instant.now();
        
        public LogEntry(String message, Severity severity) {
            this.message = message;
            this.severity = severity;
        }
        
        public LogEntry(String message, Severity severity, Throwable error) {
            this.message = message;
            this.severity = severity;
            this.error = error;
        }
        
        public LogEntry setParameters(Map<String, Object> parameters) {
            if (parameters != null) {
                this.parameters = parameters;
            }
            return this;
        }
        
        public LogEntry setParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public LogEntry setSeverity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public LogEntry setError(Throwable error) {
            this.error = error;
            return this;
        }
        
        public void log() {
            Logger.log(this);
        }
        
        Severity getSeverity() {
            return severity;
        }
        
        String getMessage() {
            return message;
        }
        
        Throwable getError() {
            return error;
        }
        
        Map<String, Object> getParameters() {
            return parameters;
        }
        
        Instant getDate() {
            return date;
        }
    }
    
    /**
     * Create an info level log entry
     */
    public static LogEntry info(String message) {
        return new LogEntry(message, Severity.INFO);
    }
    
    /**
     * Create an info level log entry with parameters
     */
    public static LogEntry info(String message, Map<String, Object> parameters) {
        return new LogEntry(message, Severity.INFO).setParameters(parameters);
    }
    
    /**
     * Create a trace level log entry
     */
    public static LogEntry trace(String message) {
        return new LogEntry(message, Severity.TRACE);
    }
    
    /**
     * Create a trace level log entry with parameters
     */
    public static LogEntry trace(String message, Map<String, Object> parameters) {
        return new LogEntry(message, Severity.TRACE).setParameters(parameters);
    }
    
    /**
     * Create an error level log entry
     */
    public static LogEntry error(String message) {
        return new LogEntry(message, Severity.ERROR);
    }
    
    /**
     * Create an error level log entry with exception
     */
    public static LogEntry error(String message, Throwable error) {
        return new LogEntry(message, Severity.ERROR, error);
    }
    
    /**
     * Format an exception for logging
     */
    public static String formatError(Throwable error) {
        if (error == null) {
            return "";
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println();
        pw.println("    message: " + error.getMessage());
        pw.println("    name: " + error.getClass().getName());
        error.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Log a log entry if it meets the filter level
     */
    public static void log(LogEntry logEntry) {
        if (logEntry.getSeverity().getValue() >= filterLevel.getValue()) {
            StringBuilder logResults = new StringBuilder();
            logResults.append("\n");
            logResults.append("    Severity: ").append(logEntry.getSeverity()).append("\n");
            logResults.append("    Date: ").append(logEntry.getDate()).append("\n");
            logResults.append("    Message: ").append(logEntry.getMessage()).append("\n");
            
            if (logEntry.getError() != null) {
                logResults.append(formatError(logEntry.getError()));
            }
            
            if (!logEntry.getParameters().isEmpty()) {
                logResults.append("    Parameters: ").append(logEntry.getParameters()).append("\n");
            }
            
            System.out.println(logResults.toString());
        }
    }
}
