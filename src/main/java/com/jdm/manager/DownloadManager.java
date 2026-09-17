package com.jdm.manager;

import com.jdm.download.Downloader;
import com.jdm.download.MultiThreadDownloader;
import com.jdm.download.SingleThreadDownloader;
import com.jdm.model.DownloadMetadata;
import com.jdm.model.DownloadPriority;
import com.jdm.model.DownloadStatus;
import com.jdm.model.DownloadTask;
import com.jdm.storage.MetadataStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Core engine orchestrating tasks, probing server capabilities, handling queues,
 * pause/resume/cancel lifecycles, and crash recovery scans.
 */
public class DownloadManager {
    private final HttpClient httpClient;
    private final QueueManager queueManager;
    private final HistoryManager historyManager;
    private final Map<String, DownloadTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, Downloader> activeDownloaders = new ConcurrentHashMap<>();
    private final ExecutorService orchestratorPool = Executors.newCachedThreadPool();

    public DownloadManager() {
        this(new QueueManager(3), new HistoryManager());
    }

    public DownloadManager(QueueManager queueManager, HistoryManager historyManager) {
        this.queueManager = queueManager;
        this.historyManager = historyManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public DownloadTask createDownloadTask(String url, String targetFilePath, int threadCount, DownloadPriority priority) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        DownloadTask task = new DownloadTask(id, url, targetFilePath, threadCount, priority);
        tasks.put(id, task);
        queueManager.enqueue(task);
        checkAndStartNextQueuedTask();
        return task;
    }

    public void probeServerCapabilities(DownloadTask task) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(task.getUrl()))
                    .header("Accept-Encoding", "identity")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200 || response.statusCode() == 206) {
                response.headers().firstValueAsLong("Content-Length").ifPresent(task::setTotalSize);
                boolean acceptRanges = response.headers().firstValue("Accept-Ranges")
                        .map(val -> val.equalsIgnoreCase("bytes"))
                        .orElse(false);

                task.setSupportsRange(acceptRanges && task.getTotalSize() > 1024 * 1024);
            } else {
                // Fall back to GET test request if HEAD is not allowed by server
                probeServerCapabilitiesViaGet(task);
            }
        } catch (Exception e) {
            probeServerCapabilitiesViaGet(task);
        }
    }

    private void probeServerCapabilitiesViaGet(DownloadTask task) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(task.getUrl()))
                    .header("Accept-Encoding", "identity")
                    .header("Range", "bytes=0-0")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 206) {
                task.setSupportsRange(true);
                response.headers().firstValue("Content-Range").ifPresent(cr -> {
                    int lastSlash = cr.lastIndexOf('/');
                    if (lastSlash != -1) {
                        try {
                            task.setTotalSize(Long.parseLong(cr.substring(lastSlash + 1).trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                });
            } else {
                task.setSupportsRange(false);
            }
        } catch (Exception ignored) {
            task.setSupportsRange(false);
        }
    }

    public synchronized void startDownload(String taskId) {
        DownloadTask task = tasks.get(taskId);
        if (task == null) return;

        if (task.getStatus() == DownloadStatus.DOWNLOADING) {
            return;
        }

        probeServerCapabilities(task);

        Downloader downloader;
        if (task.isSupportsRange() && task.getThreadCount() > 1) {
            downloader = new MultiThreadDownloader(httpClient);
        } else {
            downloader = new SingleThreadDownloader(httpClient);
        }

        activeDownloaders.put(taskId, downloader);

        orchestratorPool.submit(() -> {
            try {
                downloader.download(task);
            } catch (Exception e) {
                task.setStatus(DownloadStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            } finally {
                activeDownloaders.remove(taskId);
                historyManager.recordTask(task);
                checkAndStartNextQueuedTask();
            }
        });
    }

    public void pauseDownload(String taskId) {
        DownloadTask task = tasks.get(taskId);
        if (task != null) {
            Downloader downloader = activeDownloaders.get(taskId);
            if (downloader != null) {
                downloader.pause();
            } else {
                task.setStatus(DownloadStatus.PAUSED);
            }
        }
    }

    public void resumeDownload(String taskId) {
        DownloadTask task = tasks.get(taskId);
        if (task != null && (task.getStatus() == DownloadStatus.PAUSED || task.getStatus() == DownloadStatus.INTERRUPTED)) {
            task.setStatus(DownloadStatus.QUEUED);
            queueManager.enqueue(task);
            checkAndStartNextQueuedTask();
        }
    }

    public void cancelDownload(String taskId) {
        DownloadTask task = tasks.get(taskId);
        if (task != null) {
            Downloader downloader = activeDownloaders.get(taskId);
            if (downloader != null) {
                downloader.cancel();
            }
            task.setStatus(DownloadStatus.CANCELLED);
            queueManager.dequeue(task);
            MetadataStore.deleteMetadata(task.getMetadataFilePath());
            historyManager.recordTask(task);
            checkAndStartNextQueuedTask();
        }
    }

    public synchronized void checkAndStartNextQueuedTask() {
        int activeCount = (int) tasks.values().stream()
                .filter(t -> t.getStatus() == DownloadStatus.DOWNLOADING)
                .count();

        DownloadTask nextTask = queueManager.pollNextTask(activeCount);
        if (nextTask != null) {
            startDownload(nextTask.getId());
        }
    }

    public List<DownloadMetadata> scanAndRecoverInterruptedDownloads(String directoryPath) {
        List<DownloadMetadata> recoveredList = new ArrayList<>();
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return recoveredList;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jdm")) {
            for (Path metaPath : stream) {
                DownloadMetadata meta = MetadataStore.loadMetadata(metaPath);
                if (meta != null) {
                    recoveredList.add(meta);
                    // Import into manager as INTERRUPTED task
                    DownloadTask task = new DownloadTask(
                            meta.getId(),
                            meta.getUrl(),
                            meta.getTargetFilePath(),
                            meta.getThreadCount(),
                            DownloadPriority.valueOf(meta.getPriority())
                    );
                    task.updateFromMetadata(meta);
                    task.setStatus(DownloadStatus.INTERRUPTED);
                    tasks.put(task.getId(), task);
                }
            }
        } catch (IOException ignored) {}
        return recoveredList;
    }

    public void shutdown() {
        for (Map.Entry<String, Downloader> entry : activeDownloaders.entrySet()) {
            entry.getValue().pause();
        }
        orchestratorPool.shutdownNow();
    }

    public List<DownloadTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public DownloadTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }
}
