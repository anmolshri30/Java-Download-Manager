package com.jdm.model;

/**
 * Represents the current status of a download task.
 */
public enum DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    INTERRUPTED
}
