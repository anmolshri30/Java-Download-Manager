package com.jdm;

import com.jdm.manager.DownloadManager;
import com.jdm.manager.HistoryManager;
import com.jdm.manager.QueueManager;
import com.jdm.model.DownloadPriority;
import com.jdm.model.DownloadStatus;
import com.jdm.model.DownloadTask;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DownloadManagerIntegrationTest {

    private HttpServer server;
    private String baseUrl;
    private byte[] testPayload;

    @BeforeEach
    void setUp() throws IOException {
        // Create 2 MB dummy test payload
        testPayload = new byte[2 * 1024 * 1024];
        for (int i = 0; i < testPayload.length; i++) {
            testPayload[i] = (byte) (i % 256);
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;

        // Serve range-enabled endpoint
        server.createContext("/range-file.bin", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                Headers headers = exchange.getResponseHeaders();
                headers.set("Accept-Ranges", "bytes");

                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    headers.set("Content-Length", String.valueOf(testPayload.length));
                    exchange.sendResponseHeaders(200, -1);
                    exchange.close();
                    return;
                }

                String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    String[] parts = rangeHeader.substring(6).split("-");
                    int start = Integer.parseInt(parts[0]);
                    int end = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : testPayload.length - 1;
                    int length = end - start + 1;

                    headers.set("Content-Range", "bytes " + start + "-" + end + "/" + testPayload.length);
                    headers.set("Content-Length", String.valueOf(length));
                    exchange.sendResponseHeaders(206, length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(testPayload, start, length);
                    }
                } else {
                    headers.set("Content-Length", String.valueOf(testPayload.length));
                    exchange.sendResponseHeaders(200, testPayload.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(testPayload);
                    }
                }
            }
        });

        // Serve single-thread (no range) endpoint
        server.createContext("/single-file.bin", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Length", String.valueOf(testPayload.length));
                exchange.sendResponseHeaders(200, testPayload.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(testPayload);
                }
            }
        });

        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testMultiThreadedRangeDownload(@TempDir Path tempDir) throws Exception {
        Path targetFile = tempDir.resolve("downloaded_range.bin");
        DownloadManager manager = new DownloadManager(new QueueManager(3), new HistoryManager(tempDir.resolve("history.log")));

        DownloadTask task = manager.createDownloadTask(
                baseUrl + "/range-file.bin",
                targetFile.toString(),
                4,
                DownloadPriority.HIGH
        );

        // Wait for task completion
        int waitMs = 0;
        while (task.getStatus() != DownloadStatus.COMPLETED && waitMs < 10000) {
            Thread.sleep(100);
            waitMs += 100;
        }

        assertEquals(DownloadStatus.COMPLETED, task.getStatus());
        assertTrue(Files.exists(targetFile));
        assertEquals(testPayload.length, Files.size(targetFile));

        byte[] downloaded = Files.readAllBytes(targetFile);
        assertArrayEquals(testPayload, downloaded);

        manager.shutdown();
    }

    @Test
    void testSingleThreadedFallbackDownload(@TempDir Path tempDir) throws Exception {
        Path targetFile = tempDir.resolve("downloaded_single.bin");
        DownloadManager manager = new DownloadManager(new QueueManager(3), new HistoryManager(tempDir.resolve("history.log")));

        DownloadTask task = manager.createDownloadTask(
                baseUrl + "/single-file.bin",
                targetFile.toString(),
                1,
                DownloadPriority.MEDIUM
        );

        int waitMs = 0;
        while (task.getStatus() != DownloadStatus.COMPLETED && waitMs < 10000) {
            Thread.sleep(100);
            waitMs += 100;
        }

        assertEquals(DownloadStatus.COMPLETED, task.getStatus());
        assertTrue(Files.exists(targetFile));
        assertEquals(testPayload.length, Files.size(targetFile));

        byte[] downloaded = Files.readAllBytes(targetFile);
        assertArrayEquals(testPayload, downloaded);

        manager.shutdown();
    }
}
