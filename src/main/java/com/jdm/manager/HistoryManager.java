package com.jdm.manager;

import com.jdm.model.DownloadTask;
import com.jdm.util.FormatUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists log of finished, cancelled, or failed download tasks.
 */
public class HistoryManager {
    private final Path historyFilePath;

    public HistoryManager() {
        this(Paths.get("history.log"));
    }

    public HistoryManager(Path historyFilePath) {
        this.historyFilePath = historyFilePath;
    }

    public synchronized void recordTask(DownloadTask task) {
        try {
            String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());

            String entry = String.format("[%s] ID: %s | Status: %s | Size: %s | File: %s | URL: %s%s%n",
                    timeStr,
                    task.getId(),
                    task.getStatus(),
                    FormatUtils.formatBytes(task.getTotalSize()),
                    task.getTargetFilePath().getFileName(),
                    task.getUrl(),
                    task.getErrorMessage() != null ? " | Error: " + task.getErrorMessage() : ""
            );

            Files.writeString(historyFilePath, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    public synchronized List<String> getHistory() {
        if (!Files.exists(historyFilePath)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(historyFilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public synchronized void clearHistory() {
        try {
            Files.deleteIfExists(historyFilePath);
        } catch (IOException ignored) {}
    }
}
