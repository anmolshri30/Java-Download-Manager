package com.jdm.download;

import com.jdm.model.DownloadChunk;
import com.jdm.model.DownloadStatus;
import com.jdm.model.DownloadTask;
import com.jdm.storage.MetadataStore;
import com.jdm.util.SpeedCalculator;

import java.net.http.HttpClient;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Multi-threaded range downloader strategy using ExecutorService, FileChannel offset writing,
 * and periodic metadata flushing.
 */
public class MultiThreadDownloader implements Downloader {
    private final HttpClient httpClient;
    private volatile boolean cancelled = false;
    private volatile boolean paused = false;
    private ExecutorService executor;

    public MultiThreadDownloader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void download(DownloadTask task) throws Exception {
        task.setStatus(DownloadStatus.DOWNLOADING);

        Path partFile = task.getPartFilePath();
        Path targetFile = task.getTargetFilePath();
        Path metadataPath = task.getMetadataFilePath();

        // Ensure parent directory exists
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }

        // Divide chunks if new download
        if (task.getChunks().isEmpty()) {
            task.setChunks(ChunkManager.createChunks(task.getTotalSize(), task.getThreadCount()));
        }

        // Open .part file using NIO FileChannel
        try (FileChannel fileChannel = FileChannel.open(partFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {

            // Pre-allocate file size if known
            if (task.getTotalSize() > 0 && fileChannel.size() < task.getTotalSize()) {
                fileChannel.truncate(task.getTotalSize());
            }

            int numThreads = Math.min(task.getThreadCount(), task.getChunks().size());
            executor = Executors.newFixedThreadPool(numThreads);

            List<Future<?>> futures = new ArrayList<>();
            for (DownloadChunk chunk : task.getChunks()) {
                if (!chunk.isCompleted()) {
                    DownloadWorker worker = new DownloadWorker(task, chunk, fileChannel, httpClient, this);
                    futures.add(executor.submit(worker));
                }
            }

            SpeedCalculator speedCalc = new SpeedCalculator();
            long lastMetaSave = System.currentTimeMillis();

            // Progress monitoring loop
            while (!futures.isEmpty()) {
                if (cancelled) {
                    shutdownExecutor();
                    task.setStatus(DownloadStatus.CANCELLED);
                    MetadataStore.saveMetadata(metadataPath, task.toMetadata());
                    return;
                }
                if (paused) {
                    shutdownExecutor();
                    task.setStatus(DownloadStatus.PAUSED);
                    MetadataStore.saveMetadata(metadataPath, task.toMetadata());
                    return;
                }

                // Check for worker failures
                for (Future<?> f : futures) {
                    if (f.isDone()) {
                        try {
                            f.get();
                        } catch (Exception e) {
                            shutdownExecutor();
                            task.setStatus(DownloadStatus.FAILED);
                            task.setErrorMessage(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                            MetadataStore.saveMetadata(metadataPath, task.toMetadata());
                            throw e;
                        }
                    }
                }

                // Remove completed futures
                futures.removeIf(Future::isDone);

                // Update speed & ETA
                long currentDownloaded = task.getDownloadedBytes();
                speedCalc.recordSample(currentDownloaded);
                task.setSpeedBytesPerSec(speedCalc.getCurrentSpeedBps());

                if (task.getTotalSize() > 0) {
                    long remaining = task.getTotalSize() - currentDownloaded;
                    task.setEtaSeconds(speedCalc.calculateEtaSeconds(remaining));
                }

                // Periodically save metadata (every 2 seconds)
                if (System.currentTimeMillis() - lastMetaSave > 2000) {
                    MetadataStore.saveMetadata(metadataPath, task.toMetadata());
                    lastMetaSave = System.currentTimeMillis();
                }

                Thread.sleep(200);
            }

            shutdownExecutor();
        }

        // Verify completion
        if (ChunkManager.isAllCompleted(task.getChunks())) {
            Files.move(partFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            MetadataStore.deleteMetadata(metadataPath);
            task.setStatus(DownloadStatus.COMPLETED);
            task.setSpeedBytesPerSec(0);
            task.setEtaSeconds(0);
        } else if (!paused && !cancelled) {
            task.setStatus(DownloadStatus.FAILED);
            task.setErrorMessage("Download ended prematurely before all chunks completed");
        }
    }

    private void shutdownExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void pause() {
        this.paused = true;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }
}
