package com.jdm;

import com.jdm.download.ChunkManager;
import com.jdm.model.DownloadChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkManagerTest {

    @Test
    void testChunkDivisionExact() {
        long totalSize = 1000;
        int numChunks = 4;
        List<DownloadChunk> chunks = ChunkManager.createChunks(totalSize, numChunks);

        assertEquals(4, chunks.size());
        assertEquals(0, chunks.get(0).getStartByte());
        assertEquals(249, chunks.get(0).getEndByte());

        assertEquals(250, chunks.get(1).getStartByte());
        assertEquals(499, chunks.get(1).getEndByte());

        assertEquals(500, chunks.get(2).getStartByte());
        assertEquals(749, chunks.get(2).getEndByte());

        assertEquals(750, chunks.get(3).getStartByte());
        assertEquals(999, chunks.get(3).getEndByte());
    }

    @Test
    void testChunkDivisionUneven() {
        long totalSize = 100;
        int numChunks = 3;
        List<DownloadChunk> chunks = ChunkManager.createChunks(totalSize, numChunks);

        assertEquals(3, chunks.size());
        assertEquals(0, chunks.get(0).getStartByte());
        assertEquals(32, chunks.get(0).getEndByte());

        assertEquals(33, chunks.get(1).getStartByte());
        assertEquals(65, chunks.get(1).getEndByte());

        assertEquals(66, chunks.get(2).getStartByte());
        assertEquals(99, chunks.get(2).getEndByte()); // last chunk absorbs remainder
    }

    @Test
    void testSingleChunkFallback() {
        List<DownloadChunk> chunks = ChunkManager.createChunks(500, 1);
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).getStartByte());
        assertEquals(499, chunks.get(0).getEndByte());
    }

    @Test
    void testIsAllCompleted() {
        List<DownloadChunk> chunks = ChunkManager.createChunks(100, 2);
        assertFalse(ChunkManager.isAllCompleted(chunks));

        chunks.get(0).setCompleted(true);
        assertFalse(ChunkManager.isAllCompleted(chunks));

        chunks.get(1).setCompleted(true);
        assertTrue(ChunkManager.isAllCompleted(chunks));
    }
}
