package com.jdm.util;

import java.util.Locale;

/**
 * Utility functions for formatting byte sizes, speeds, durations, and progress bars.
 */
public class FormatUtils {

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }

    public static String formatSpeed(double bytesPerSec) {
        if (bytesPerSec <= 0) return "0.00 B/s";
        return formatBytes((long) bytesPerSec) + "/s";
    }

    public static String formatTime(long seconds) {
        if (seconds < 0) return "--:--";
        if (seconds > 86400 * 365) return "Unknown";
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hrs > 0) {
            return String.format("%02d:%02d:%02d", hrs, mins, secs);
        } else {
            return String.format("%02d:%02d", mins, secs);
        }
    }

    public static String buildProgressBar(double percentage, int width) {
        int filled = (int) Math.round((percentage / 100.0) * width);
        filled = Math.max(0, Math.min(width, filled));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                sb.append("=");
            } else if (i == filled && filled < width) {
                sb.append(">");
            } else {
                sb.append("-");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
