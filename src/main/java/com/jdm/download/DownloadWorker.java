package com.jdm.download;

import com.jdm.model.DownloadChunk;
import com.jdm.model.DownloadTask;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Worker thread fetching a single HTTP byte range chunk into FileChannel.
 */
public class DownloadWorker implements Runnable {
    private final DownloadTask task;
    private final DownloadChunk chunk;
    private final FileChannel fileChannel;
    private final HttpClient httpClient;
    private final Downloader parentDownloader;

    private static final int BUFFER_SIZE = 16384; // 16 KB buffer
    private static final int MAX_RETRIES = 3;

    public DownloadWorker(DownloadTask task, DownloadChunk chunk, FileChannel fileChannel,
                          HttpClient httpClient, Downloader parentDownloader) {
        this.task = task;
        this.chunk = chunk;
        this.fileChannel = fileChannel;
        this.httpClient = httpClient;
        this.parentDownloader = parentDownloader;
    }

    @Override
    public void run() {
        if (chunk.isCompleted()) {
            return;
        }

        int attempts = 0;
        Exception lastException = null;

        while (attempts <= MAX_RETRIES && !Thread.currentThread().isInterrupted()) {
            try {
                long startByte = chunk.getNextByteToDownload();
                long endByte = chunk.getEndByte();

                if (startByte > endByte) {
                    chunk.setCompleted(true);
                    return;
                }

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(task.getUrl()))
                        .header("Accept-Encoding", "identity");

                if (task.isSupportsRange()) {
                    reqBuilder.header("Range", "bytes=" + startByte + "-" + endByte);
                }

                HttpRequest request = reqBuilder.GET().build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                int statusCode = response.statusCode();
                if (statusCode != 200 && statusCode != 206) {
                    if (statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504) {
                        attempts++;
                        Thread.sleep(1000L * attempts);
                        continue;
                    } else {
                        throw new RuntimeException("HTTP Range request failed with status code: " + statusCode);
                    }
                }

                try (InputStream in = response.body()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long currentOffset = startByte;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }

                        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesRead);
                        while (bb.hasRemaining()) {
                            fileChannel.write(bb, currentOffset);
                        }

                        currentOffset += bytesRead;
                        chunk.addDownloadedBytes(bytesRead);
                    }
                }

                if (chunk.getDownloadedBytes() >= chunk.getChunkSize()) {
                    chunk.setCompleted(true);
                }
                return;

            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts <= MAX_RETRIES) {
                    try {
                        Thread.sleep(1000L * attempts);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        if (lastException != null && !chunk.isCompleted()) {
            throw new RuntimeException("Chunk " + chunk.getId() + " failed after " + MAX_RETRIES + " retries: " + lastException.getMessage(), lastException);
        }
    }
}
