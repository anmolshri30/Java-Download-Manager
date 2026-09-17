package com.jdm.download;

import com.jdm.model.DownloadChunk;
import com.jdm.model.DownloadStatus;
import com.jdm.model.DownloadTask;
import com.jdm.util.SpeedCalculator;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

/**
 * Fallback single-stream downloader strategy for servers without HTTP range support.
 */
public class SingleThreadDownloader implements Downloader {
    private final HttpClient httpClient;
    private volatile boolean cancelled = false;
    private volatile boolean paused = false;

    public SingleThreadDownloader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void download(DownloadTask task) throws Exception {
        task.setStatus(DownloadStatus.DOWNLOADING);

        Path partFile = task.getPartFilePath();
        Path targetFile = task.getTargetFilePath();

        if (task.getChunks().isEmpty()) {
            DownloadChunk singleChunk = new DownloadChunk(0, 0, Math.max(0, task.getTotalSize() - 1));
            task.setChunks(Collections.singletonList(singleChunk));
        }
        DownloadChunk chunk = task.getChunks().get(0);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(task.getUrl()))
                .header("Accept-Encoding", "identity")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Server responded with HTTP " + response.statusCode());
        }

        if (task.getTotalSize() <= 0) {
            response.headers().firstValueAsLong("Content-Length").ifPresent(task::setTotalSize);
        }

        SpeedCalculator speedCalc = new SpeedCalculator();

        try (InputStream in = response.body();
             FileChannel outChannel = FileChannel.open(partFile,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[16384];
            int bytesRead;
            long written = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                if (cancelled) {
                    task.setStatus(DownloadStatus.CANCELLED);
                    return;
                }
                if (paused) {
                    task.setStatus(DownloadStatus.PAUSED);
                    return;
                }

                ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesRead);
                while (bb.hasRemaining()) {
                    outChannel.write(bb);
                }

                written += bytesRead;
                chunk.setDownloadedBytes(written);

                speedCalc.recordSample(written);
                task.setSpeedBytesPerSec(speedCalc.getCurrentSpeedBps());
                if (task.getTotalSize() > 0) {
                    task.setEtaSeconds(speedCalc.calculateEtaSeconds(task.getTotalSize() - written));
                }
            }

            chunk.setCompleted(true);
        }

        if (task.getStatus() == DownloadStatus.DOWNLOADING) {
            // Rename .part to target file
            Files.move(partFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            task.setStatus(DownloadStatus.COMPLETED);
            task.setSpeedBytesPerSec(0);
            task.setEtaSeconds(0);
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
