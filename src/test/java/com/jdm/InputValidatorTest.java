package com.jdm;

import com.jdm.model.DownloadPriority;
import com.jdm.util.InputValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void testValidUrls() {
        assertTrue(InputValidator.isValidUrl("http://example.com/file.zip"));
        assertTrue(InputValidator.isValidUrl("https://example.com/test.iso?query=1"));
        assertFalse(InputValidator.isValidUrl("ftp://example.com/file.txt"));
        assertFalse(InputValidator.isValidUrl("invalid-url"));
        assertFalse(InputValidator.isValidUrl(null));
        assertFalse(InputValidator.isValidUrl(""));
    }

    @Test
    void testThreadCountValidation() {
        assertTrue(InputValidator.isValidThreadCount(1));
        assertTrue(InputValidator.isValidThreadCount(4));
        assertTrue(InputValidator.isValidThreadCount(32));
        assertFalse(InputValidator.isValidThreadCount(0));
        assertFalse(InputValidator.isValidThreadCount(-5));
        assertFalse(InputValidator.isValidThreadCount(33));
    }

    @Test
    void testDirectoryValidation(@TempDir Path tempDir) {
        assertTrue(InputValidator.isValidDirectory(tempDir.toString()));
        assertFalse(InputValidator.isValidDirectory(tempDir.resolve("non_existent_folder").toString()));
        assertFalse(InputValidator.isValidDirectory(null));
    }

    @Test
    void testParsePriority() {
        assertEquals(DownloadPriority.HIGH, InputValidator.parsePriority("HIGH"));
        assertEquals(DownloadPriority.HIGH, InputValidator.parsePriority("h"));
        assertEquals(DownloadPriority.HIGH, InputValidator.parsePriority("3"));
        assertEquals(DownloadPriority.LOW, InputValidator.parsePriority("LOW"));
        assertEquals(DownloadPriority.LOW, InputValidator.parsePriority("1"));
        assertEquals(DownloadPriority.MEDIUM, InputValidator.parsePriority("MEDIUM"));
        assertEquals(DownloadPriority.MEDIUM, InputValidator.parsePriority("unknown"));
    }

    @Test
    void testExtractFileNameFromUrl() {
        assertEquals("archive.zip", InputValidator.extractFileNameFromUrl("https://test.org/path/to/archive.zip"));
        assertEquals("image.png", InputValidator.extractFileNameFromUrl("http://test.org/image.png?v=123"));
        assertEquals("download.bin", InputValidator.extractFileNameFromUrl("http://test.org/"));
    }
}
