package com.jdm.util;

import com.jdm.model.DownloadPriority;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates user input (URLs, target directories, thread counts, priority strings).
 */
public class InputValidator {

    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean isValidThreadCount(int threadCount) {
        return threadCount >= 1 && threadCount <= 32;
    }

    public static boolean isValidDirectory(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return false;
        }
        try {
            Path p = Paths.get(dirPath.trim());
            return Files.exists(p) && Files.isDirectory(p);
        } catch (Exception e) {
            return false;
        }
    }

    public static DownloadPriority parsePriority(String input) {
        if (input == null) return DownloadPriority.MEDIUM;
        String trimmed = input.trim().toUpperCase();
        switch (trimmed) {
            case "HIGH":
            case "H":
            case "3":
                return DownloadPriority.HIGH;
            case "LOW":
            case "L":
            case "1":
                return DownloadPriority.LOW;
            case "MEDIUM":
            case "M":
            case "2":
            default:
                return DownloadPriority.MEDIUM;
        }
    }

    public static String extractFileNameFromUrl(String urlStr) {
        if (!isValidUrl(urlStr)) {
            return "download.bin";
        }
        try {
            URI uri = new URI(urlStr);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    String name = path.substring(lastSlash + 1);
                    // sanitize query/hash components if any left
                    int paramIdx = name.indexOf('?');
                    if (paramIdx >= 0) name = name.substring(0, paramIdx);
                    if (!name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "download.bin";
    }
}
