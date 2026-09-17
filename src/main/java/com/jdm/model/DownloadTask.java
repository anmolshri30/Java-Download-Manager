package com.jdm.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main domain model representing an active, queued, or completed download task.
 */
public class DownloadTask {
    private final String id;
    private final String url;
    private final Path targetFilePath;
    private long totalSize;
    private volatile DownloadStatus status;
    private DownloadPriority priority;
    private int threadCount;
    private boolean supportsRange;
    private final List<DownloadChunk> chunks = new ArrayList<>();
    
    private double speedBytesPerSec;
    private long etaSeconds;
    private final long createdAt;
    private String errorMessage;

    public DownloadTask(String id, String url, String targetFilePath, int threadCount, DownloadPriority priority) {
        this.id = id;
        this.url = url;
        this.targetFilePath = Paths.get(targetFilePath);
        this.threadCount = threadCount;
        this.priority = priority;
        this.status = DownloadStatus.QUEUED;
        this.totalSize = -1;
        this.supportsRange = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Path getTargetFilePath() {
        return targetFilePath;
    }

    public String getFileName() {
        return targetFilePath.getFileName().toString();
    }

    public Path getMetadataFilePath() {
        return Paths.get(targetFilePath.toString() + ".jdm");
    }

    public Path getPartFilePath() {
        return Paths.get(targetFilePath.toString() + ".part");
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public DownloadPriority getPriority() {
        return priority;
    }

    public void setPriority(DownloadPriority priority) {
        this.priority = priority;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public boolean isSupportsRange() {
        return supportsRange;
    }

    public void setSupportsRange(boolean supportsRange) {
        this.supportsRange = supportsRange;
    }

    public List<DownloadChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<DownloadChunk> newChunks) {
        this.chunks.clear();
        if (newChunks != null) {
            this.chunks.addAll(newChunks);
        }
    }

    public long getDownloadedBytes() {
        if (chunks.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (DownloadChunk chunk : chunks) {
            sum += chunk.getDownloadedBytes();
        }
        return sum;
    }

    public double getProgressPercentage() {
        if (totalSize <= 0) {
            return 0.0;
        }
        long downloaded = getDownloadedBytes();
        return Math.min(100.0, (downloaded * 100.0) / totalSize);
    }

    public double getSpeedBytesPerSec() {
        return speedBytesPerSec;
    }

    public void setSpeedBytesPerSec(double speedBytesPerSec) {
        this.speedBytesPerSec = speedBytesPerSec;
    }

    public long getEtaSeconds() {
        return etaSeconds;
    }

    public void setEtaSeconds(long etaSeconds) {
        this.etaSeconds = etaSeconds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public DownloadMetadata toMetadata() {
        DownloadMetadata metadata = new DownloadMetadata();
        metadata.setId(id);
        metadata.setUrl(url);
        metadata.setTargetFilePath(targetFilePath.toString());
        metadata.setTotalSize(totalSize);
        metadata.setThreadCount(threadCount);
        metadata.setPriority(priority.name());
        metadata.setCreatedTime(createdAt);
        metadata.setLastUpdatedTime(System.currentTimeMillis());

        List<DownloadMetadata.ChunkMetadata> chunkMetaList = new ArrayList<>();
        for (DownloadChunk chunk : chunks) {
            chunkMetaList.add(new DownloadMetadata.ChunkMetadata(
                    chunk.getId(),
                    chunk.getStartByte(),
                    chunk.getEndByte(),
                    chunk.getDownloadedBytes(),
                    chunk.isCompleted()
            ));
        }
        metadata.setChunks(chunkMetaList);
        return metadata;
    }

    public void updateFromMetadata(DownloadMetadata metadata) {
        this.totalSize = metadata.getTotalSize();
        this.threadCount = metadata.getThreadCount();
        try {
            this.priority = DownloadPriority.valueOf(metadata.getPriority());
        } catch (Exception ignored) {}

        this.chunks.clear();
        if (metadata.getChunks() != null) {
            for (DownloadMetadata.ChunkMetadata cm : metadata.getChunks()) {
                this.chunks.add(new DownloadChunk(
                        cm.getId(),
                        cm.getStartByte(),
                        cm.getEndByte(),
                        cm.getDownloadedBytes(),
                        cm.isCompleted()
                ));
            }
        }
    }
}
