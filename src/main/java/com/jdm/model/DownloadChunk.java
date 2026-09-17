package com.jdm.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Details and state tracking for an individual byte-range chunk.
 */
public class DownloadChunk {
    private final int id;
    private final long startByte;
    private final long endByte;
    private final AtomicLong downloadedBytes;
    private volatile boolean completed;

    public DownloadChunk(int id, long startByte, long endByte) {
        this(id, startByte, endByte, 0, false);
    }

    public DownloadChunk(int id, long startByte, long endByte, long downloadedBytes, boolean completed) {
        this.id = id;
        this.startByte = startByte;
        this.endByte = endByte;
        this.downloadedBytes = new AtomicLong(downloadedBytes);
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public long getStartByte() {
        return startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public long getChunkSize() {
        return endByte - startByte + 1;
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public void addDownloadedBytes(long bytes) {
        downloadedBytes.addAndGet(bytes);
    }

    public void setDownloadedBytes(long bytes) {
        downloadedBytes.set(bytes);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getNextByteToDownload() {
        return startByte + downloadedBytes.get();
    }

    public long getRemainingBytes() {
        long remaining = getChunkSize() - downloadedBytes.get();
        return Math.max(0, remaining);
    }
}
