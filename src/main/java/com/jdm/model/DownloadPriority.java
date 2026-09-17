package com.jdm.model;

/**
 * Priority levels for queued download tasks.
 */
public enum DownloadPriority {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int level;

    DownloadPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
