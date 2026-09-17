package com.jdm.manager;

import com.jdm.model.DownloadPriority;
import com.jdm.model.DownloadStatus;
import com.jdm.model.DownloadTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages priority-based queuing and maximum concurrent active download limits.
 */
public class QueueManager {
    private int maxConcurrentDownloads = 3;
    private final List<DownloadTask> queue = new CopyOnWriteArrayList<>();

    public QueueManager() {
    }

    public QueueManager(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public synchronized void enqueue(DownloadTask task) {
        if (!queue.contains(task)) {
            task.setStatus(DownloadStatus.QUEUED);
            queue.add(task);
            sortQueue();
        }
    }

    public synchronized void dequeue(DownloadTask task) {
        queue.remove(task);
    }

    public synchronized List<DownloadTask> getQueuedTasks() {
        List<DownloadTask> result = new ArrayList<>();
        for (DownloadTask t : queue) {
            if (t.getStatus() == DownloadStatus.QUEUED) {
                result.add(t);
            }
        }
        return result;
    }

    public synchronized DownloadTask pollNextTask(int activeCount) {
        if (activeCount >= maxConcurrentDownloads) {
            return null;
        }
        sortQueue();
        for (DownloadTask t : queue) {
            if (t.getStatus() == DownloadStatus.QUEUED) {
                return t;
            }
        }
        return null;
    }

    private void sortQueue() {
        queue.sort(Comparator
                .comparing((DownloadTask t) -> t.getPriority().getLevel(), Comparator.reverseOrder())
                .thenComparingLong(DownloadTask::getCreatedAt));
    }

    public synchronized boolean hasQueuedTasks() {
        return queue.stream().anyMatch(t -> t.getStatus() == DownloadStatus.QUEUED);
    }
}
