package com.jdm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing task state serialized to .jdm files for crash recovery.
 */
public class DownloadMetadata {
    private String id;
    private String url;
    private String targetFilePath;
    private long totalSize;
    private int threadCount;
    private String priority;
    private long createdTime;
    private long lastUpdatedTime;
    private List<ChunkMetadata> chunks = new ArrayList<>();

    public DownloadMetadata() {
    }

    public static class ChunkMetadata {
        private int id;
        private long startByte;
        private long endByte;
        private long downloadedBytes;
        private boolean completed;

        public ChunkMetadata() {
        }

        public ChunkMetadata(int id, long startByte, long endByte, long downloadedBytes, boolean completed) {
            this.id = id;
            this.startByte = startByte;
            this.endByte = endByte;
            this.downloadedBytes = downloadedBytes;
            this.completed = completed;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public long getStartByte() { return startByte; }
        public void setStartByte(long startByte) { this.startByte = startByte; }

        public long getEndByte() { return endByte; }
        public void setEndByte(long endByte) { this.endByte = endByte; }

        public long getDownloadedBytes() { return downloadedBytes; }
        public void setDownloadedBytes(long downloadedBytes) { this.downloadedBytes = downloadedBytes; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTargetFilePath() { return targetFilePath; }
    public void setTargetFilePath(String targetFilePath) { this.targetFilePath = targetFilePath; }

    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

    public long getLastUpdatedTime() { return lastUpdatedTime; }
    public void setLastUpdatedTime(long lastUpdatedTime) { this.lastUpdatedTime = lastUpdatedTime; }

    public List<ChunkMetadata> getChunks() { return chunks; }
    public void setChunks(List<ChunkMetadata> chunks) { this.chunks = chunks; }
}
