package com.spreedly.composer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Logger class
 */
public class LoggerTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private Logger.Severity originalFilterLevel;
    
    @BeforeEach
    public void setUp() {
        // Capture System.out
        System.setOut(new PrintStream(outContent));
        // Save original filter level
        originalFilterLevel = Logger.filterLevel;
    }
    
    @AfterEach
    public void tearDown() {
        // Restore System.out
        System.setOut(originalOut);
        // Restore original filter level
        Logger.filterLevel = originalFilterLevel;
    }
    
    // Test Severity enum values
    
    @Test
    public void testSeverityValues() {
        assertThat(Logger.Severity.TRACE.getValue()).isEqualTo(0);
        assertThat(Logger.Severity.INFO.getValue()).isEqualTo(1);
        assertThat(Logger.Severity.ERROR.getValue()).isEqualTo(2);
        assertThat(Logger.Severity.NONE.getValue()).isEqualTo(3);
    }
    
    @Test
    public void testSeverityOrdering() {
        assertThat(Logger.Severity.TRACE.getValue())
            .isLessThan(Logger.Severity.INFO.getValue());
        assertThat(Logger.Severity.INFO.getValue())
            .isLessThan(Logger.Severity.ERROR.getValue());
        assertThat(Logger.Severity.ERROR.getValue())
            .isLessThan(Logger.Severity.NONE.getValue());
    }
    
    // Test LogEntry creation
    
    @Test
    public void testLogEntryCreation() {
        Logger.LogEntry entry = new Logger.LogEntry("Test message", Logger.Severity.INFO);
        
        assertThat(entry.getMessage()).isEqualTo("Test message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.INFO);
        assertThat(entry.getError()).isNull();
        assertThat(entry.getParameters()).isEmpty();
        assertThat(entry.getDate()).isNotNull();
    }
    
    @Test
    public void testLogEntryCreationWithError() {
        RuntimeException error = new RuntimeException("Test error");
        Logger.LogEntry entry = new Logger.LogEntry("Error message", Logger.Severity.ERROR, error);
        
        assertThat(entry.getMessage()).isEqualTo("Error message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.ERROR);
        assertThat(entry.getError()).isEqualTo(error);
    }
    
    // Test LogEntry builder methods
    
    @Test
    public void testSetParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", 42);
        
        Logger.LogEntry entry = new Logger.LogEntry("Test", Logger.Severity.INFO);
        Logger.LogEntry result = entry.setParameters(params);
        
        assertThat(entry.getParameters()).containsEntry("key1", "value1");
        assertThat(entry.getParameters()).containsEntry("key2", 42);
        assertThat(result).isSameAs(entry); // Check method chaining
    }
    
    @Test
    public void testSetParametersWithNull() {
        Logger.LogEntry entry = new Logger.LogEntry("Test", Logger.Severity.INFO);
        Logger.LogEntry result = entry.setParameters(null);
        
        assertThat(entry.getParameters()).isEmpty();
        assertThat(result).isSameAs(entry);
    }
    
    @Test
    public void testSetParameter() {
        Logger.LogEntry entry = new Logger.LogEntry("Test", Logger.Severity.INFO);
        
        Logger.LogEntry result = entry.setParameter("key1", "value1");
        assertThat(entry.getParameters()).containsEntry("key1", "value1");
        assertThat(result).isSameAs(entry); // Check method chaining
        
        entry.setParameter("key2", 123);
        assertThat(entry.getParameters()).containsEntry("key2", 123);
        assertThat(entry.getParameters()).hasSize(2);
    }
    
    @Test
    public void testSetSeverity() {
        Logger.LogEntry entry = new Logger.LogEntry("Test", Logger.Severity.INFO);
        
        Logger.LogEntry result = entry.setSeverity(Logger.Severity.ERROR);
        
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.ERROR);
        assertThat(result).isSameAs(entry); // Check method chaining
    }
    
    @Test
    public void testSetError() {
        Logger.LogEntry entry = new Logger.LogEntry("Test", Logger.Severity.INFO);
        RuntimeException error = new RuntimeException("Test error");
        
        Logger.LogEntry result = entry.setError(error);
        
        assertThat(entry.getError()).isEqualTo(error);
        assertThat(result).isSameAs(entry); // Check method chaining
    }
    
    // Test Logger static methods
    
    @Test
    public void testLoggerInfo() {
        Logger.LogEntry entry = Logger.info("Info message");
        
        assertThat(entry.getMessage()).isEqualTo("Info message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.INFO);
        assertThat(entry.getError()).isNull();
    }
    
    @Test
    public void testLoggerInfoWithParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        
        Logger.LogEntry entry = Logger.info("Info message", params);
        
        assertThat(entry.getMessage()).isEqualTo("Info message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.INFO);
        assertThat(entry.getParameters()).containsEntry("key", "value");
    }
    
    @Test
    public void testLoggerTrace() {
        Logger.LogEntry entry = Logger.trace("Trace message");
        
        assertThat(entry.getMessage()).isEqualTo("Trace message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.TRACE);
        assertThat(entry.getError()).isNull();
    }
    
    @Test
    public void testLoggerTraceWithParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("debug", true);
        
        Logger.LogEntry entry = Logger.trace("Trace message", params);
        
        assertThat(entry.getMessage()).isEqualTo("Trace message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.TRACE);
        assertThat(entry.getParameters()).containsEntry("debug", true);
    }
    
    @Test
    public void testLoggerError() {
        Logger.LogEntry entry = Logger.error("Error message");
        
        assertThat(entry.getMessage()).isEqualTo("Error message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.ERROR);
        assertThat(entry.getError()).isNull();
    }
    
    @Test
    public void testLoggerErrorWithException() {
        RuntimeException error = new RuntimeException("Test exception");
        Logger.LogEntry entry = Logger.error("Error message", error);
        
        assertThat(entry.getMessage()).isEqualTo("Error message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.ERROR);
        assertThat(entry.getError()).isEqualTo(error);
    }
    
    // Test formatError
    
    @Test
    public void testFormatErrorWithNull() {
        String result = Logger.formatError(null);
        assertThat(result).isEmpty();
    }
    
    @Test
    public void testFormatErrorWithException() {
        RuntimeException error = new RuntimeException("Test exception");
        String result = Logger.formatError(error);
        
        assertThat(result).contains("Test exception");
        assertThat(result).contains("java.lang.RuntimeException");
        assertThat(result).contains("message:");
        assertThat(result).contains("name:");
    }
    
    @Test
    public void testFormatErrorWithStackTrace() {
        RuntimeException error = new RuntimeException("Stack trace test");
        String result = Logger.formatError(error);
        
        // Should contain stack trace elements
        assertThat(result).contains("at ");
        assertThat(result).contains("LoggerTest");
    }
    
    // Test log filtering
    
    @Test
    public void testLogFilteringWithTrace() {
        Logger.filterLevel = Logger.Severity.TRACE;
        
        Logger.LogEntry traceEntry = Logger.trace("Trace message");
        traceEntry.log();
        
        String output = outContent.toString();
        assertThat(output).contains("Trace message");
        assertThat(output).contains("Severity: TRACE");
    }
    
    @Test
    public void testLogFilteringWithInfo() {
        Logger.filterLevel = Logger.Severity.INFO;
        
        // Trace should be filtered
        outContent.reset();
        Logger.LogEntry traceEntry = Logger.trace("Should not appear");
        traceEntry.log();
        assertThat(outContent.toString()).isEmpty();
        
        // Info should appear
        outContent.reset();
        Logger.LogEntry infoEntry = Logger.info("Should appear");
        infoEntry.log();
        assertThat(outContent.toString()).contains("Should appear");
        assertThat(outContent.toString()).contains("Severity: INFO");
    }
    
    @Test
    public void testLogFilteringWithError() {
        Logger.filterLevel = Logger.Severity.ERROR;
        
        // Trace should be filtered
        outContent.reset();
        Logger.LogEntry traceEntry = Logger.trace("Should not appear");
        traceEntry.log();
        assertThat(outContent.toString()).isEmpty();
        
        // Info should be filtered
        outContent.reset();
        Logger.LogEntry infoEntry = Logger.info("Should not appear");
        infoEntry.log();
        assertThat(outContent.toString()).isEmpty();
        
        // Error should appear
        outContent.reset();
        Logger.LogEntry errorEntry = Logger.error("Should appear");
        errorEntry.log();
        assertThat(outContent.toString()).contains("Should appear");
        assertThat(outContent.toString()).contains("Severity: ERROR");
    }
    
    @Test
    public void testLogFilteringWithNone() {
        Logger.filterLevel = Logger.Severity.NONE;
        
        // Nothing should be logged
        Logger.trace("Trace").log();
        Logger.info("Info").log();
        Logger.error("Error").log();
        
        assertThat(outContent.toString()).isEmpty();
    }
    
    // Test log output format
    
    @Test
    public void testLogOutputFormat() {
        Logger.filterLevel = Logger.Severity.TRACE;
        
        Logger.LogEntry entry = Logger.info("Test message");
        entry.log();
        
        String output = outContent.toString();
        assertThat(output).contains("Severity: INFO");
        assertThat(output).contains("Date:");
        assertThat(output).contains("Message: Test message");
    }
    
    @Test
    public void testLogOutputWithParameters() {
        Logger.filterLevel = Logger.Severity.TRACE;
        
        Map<String, Object> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", 42);
        
        Logger.LogEntry entry = Logger.info("Test message", params);
        entry.log();
        
        String output = outContent.toString();
        assertThat(output).contains("Parameters:");
        assertThat(output).contains("key1");
        assertThat(output).contains("value1");
    }
    
    @Test
    public void testLogOutputWithError() {
        Logger.filterLevel = Logger.Severity.TRACE;
        
        RuntimeException error = new RuntimeException("Test error");
        Logger.LogEntry entry = Logger.error("Error occurred", error);
        entry.log();
        
        String output = outContent.toString();
        assertThat(output).contains("Test error");
        assertThat(output).contains("message:");
        assertThat(output).contains("name:");
    }
    
    // Test method chaining
    
    @Test
    public void testMethodChaining() {
        Map<String, Object> params = new HashMap<>();
        params.put("chain", "test");
        
        RuntimeException error = new RuntimeException("Chain error");
        
        Logger.LogEntry entry = Logger.info("Chained message")
            .setParameters(params)
            .setSeverity(Logger.Severity.ERROR)
            .setError(error)
            .setParameter("extra", "value");
        
        assertThat(entry.getMessage()).isEqualTo("Chained message");
        assertThat(entry.getSeverity()).isEqualTo(Logger.Severity.ERROR);
        assertThat(entry.getError()).isEqualTo(error);
        assertThat(entry.getParameters()).containsEntry("chain", "test");
        assertThat(entry.getParameters()).containsEntry("extra", "value");
    }
    
    // Test ComposerLogLevel enum
    
    @Test
    public void testComposerLogLevel() {
        assertThat(Logger.ComposerLogLevel.TRACE).isNotNull();
        assertThat(Logger.ComposerLogLevel.INFO).isNotNull();
        assertThat(Logger.ComposerLogLevel.ERROR).isNotNull();
        assertThat(Logger.ComposerLogLevel.NONE).isNotNull();
    }
    
    // Test DodgeballLogLevel backward compatibility
    
    @Test
    public void testDodgeballLogLevelBackwardCompatibility() {
        assertThat(Logger.DodgeballLogLevel.TRACE).isEqualTo(Logger.ComposerLogLevel.TRACE);
        assertThat(Logger.DodgeballLogLevel.INFO).isEqualTo(Logger.ComposerLogLevel.INFO);
        assertThat(Logger.DodgeballLogLevel.ERROR).isEqualTo(Logger.ComposerLogLevel.ERROR);
        assertThat(Logger.DodgeballLogLevel.NONE).isEqualTo(Logger.ComposerLogLevel.NONE);
    }
    
    // Test static log method
    
    @Test
    public void testStaticLogMethod() {
        Logger.filterLevel = Logger.Severity.INFO;
        
        Logger.LogEntry entry = new Logger.LogEntry("Static log test", Logger.Severity.INFO);
        Logger.log(entry);
        
        String output = outContent.toString();
        assertThat(output).contains("Static log test");
    }
    
    @Test
    public void testStaticLogMethodFiltersCorrectly() {
        Logger.filterLevel = Logger.Severity.ERROR;
        
        Logger.LogEntry infoEntry = new Logger.LogEntry("Should be filtered", Logger.Severity.INFO);
        Logger.log(infoEntry);
        
        assertThat(outContent.toString()).isEmpty();
    }

    @Test
    public void testLoggerConstructorInstantiation() {
        // Covers the implicit default constructor of the Logger utility class
        Logger logger = new Logger();
        assertThat(logger).isNotNull();
    }

    @Test
    public void testDodgeballLogLevelConstructorInstantiation() {
        // Covers the constructor of the DodgeballLogLevel inner class
        Logger.DodgeballLogLevel level = new Logger.DodgeballLogLevel();
        assertThat(level).isNotNull();
    }
}
