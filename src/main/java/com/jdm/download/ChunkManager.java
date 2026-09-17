package com.jdm.download;

import com.jdm.model.DownloadChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles byte range division and completion checks for multi-threaded downloads.
 */
public class ChunkManager {

    public static List<DownloadChunk> createChunks(long totalSize, int numChunks) {
        List<DownloadChunk> chunks = new ArrayList<>();
        if (totalSize <= 0 || numChunks <= 1) {
            chunks.add(new DownloadChunk(0, 0, Math.max(0, totalSize - 1)));
            return chunks;
        }

        long chunkSize = totalSize / numChunks;
        long start = 0;
        for (int i = 0; i < numChunks; i++) {
            long end = (i == numChunks - 1) ? totalSize - 1 : start + chunkSize - 1;
            chunks.add(new DownloadChunk(i, start, end));
            start = end + 1;
        }
        return chunks;
    }

    public static boolean isAllCompleted(List<DownloadChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return false;
        for (DownloadChunk chunk : chunks) {
            if (!chunk.isCompleted()) {
                return false;
            }
        }
        return true;
    }
}
