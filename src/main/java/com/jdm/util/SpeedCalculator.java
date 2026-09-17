package com.jdm.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Calculates current download speed and estimated time remaining using a sliding window algorithm.
 */
public class SpeedCalculator {
    private static final long WINDOW_MS = 5000; // 5 second sliding window

    private static class Sample {
        final long timeMs;
        final long downloadedBytes;

        Sample(long timeMs, long downloadedBytes) {
            this.timeMs = timeMs;
            this.downloadedBytes = downloadedBytes;
        }
    }

    private final Deque<Sample> samples = new ArrayDeque<>();

    public synchronized void recordSample(long downloadedBytes) {
        long now = System.currentTimeMillis();
        samples.addLast(new Sample(now, downloadedBytes));

        // Purge samples older than sliding window
        while (!samples.isEmpty() && (now - samples.peekFirst().timeMs) > WINDOW_MS) {
            samples.removeFirst();
        }
    }

    public synchronized double getCurrentSpeedBps() {
        if (samples.size() < 2) {
            return 0.0;
        }
        Sample oldest = samples.peekFirst();
        Sample newest = samples.peekLast();

        long timeDiffMs = newest.timeMs - oldest.timeMs;
        if (timeDiffMs <= 0) {
            return 0.0;
        }

        long bytesDiff = newest.downloadedBytes - oldest.downloadedBytes;
        if (bytesDiff < 0) {
            return 0.0;
        }

        return (bytesDiff * 1000.0) / timeDiffMs;
    }

    public synchronized long calculateEtaSeconds(long remainingBytes) {
        if (remainingBytes <= 0) {
            return 0;
        }
        double speed = getCurrentSpeedBps();
        if (speed <= 0.0) {
            return -1; // Unknown / stopped
        }
        return (long) Math.ceil(remainingBytes / speed);
    }

    public synchronized void reset() {
        samples.clear();
    }
}
