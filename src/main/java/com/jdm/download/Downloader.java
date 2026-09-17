package com.jdm.download;

import com.jdm.model.DownloadTask;

/**
 * Interface defining the strategy for executing download tasks.
 */
public interface Downloader {
    void download(DownloadTask task) throws Exception;
    void pause();
    void cancel();
}
