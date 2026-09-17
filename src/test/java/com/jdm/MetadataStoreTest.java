package com.jdm;

import com.jdm.model.DownloadChunk;
import com.jdm.model.DownloadMetadata;
import com.jdm.model.DownloadPriority;
import com.jdm.model.DownloadTask;
import com.jdm.storage.MetadataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetadataStoreTest {

    @Test
    void testSaveAndLoadMetadata(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("sample.iso");
        DownloadTask task = new DownloadTask("task-123", "https://example.com/sample.iso", targetFile.toString(), 4, DownloadPriority.HIGH);
        task.setTotalSize(10_000_000);

        List<DownloadChunk> chunks = new ArrayList<>();
        DownloadChunk c0 = new DownloadChunk(0, 0, 4_999_999, 2_500_000, false);
        DownloadChunk c1 = new DownloadChunk(1, 5_000_000, 9_999_999, 5_000_000, true);
        chunks.add(c0);
        chunks.add(c1);
        task.setChunks(chunks);

        Path metaPath = task.getMetadataFilePath();
        MetadataStore.saveMetadata(metaPath, task.toMetadata());

        assertTrue(metaPath.toFile().exists());

        DownloadMetadata loadedMeta = MetadataStore.loadMetadata(metaPath);
        assertNotNull(loadedMeta);
        assertEquals("task-123", loadedMeta.getId());
        assertEquals("https://example.com/sample.iso", loadedMeta.getUrl());
        assertEquals(targetFile.toString(), loadedMeta.getTargetFilePath());
        assertEquals(10_000_000, loadedMeta.getTotalSize());
        assertEquals(4, loadedMeta.getThreadCount());
        assertEquals("HIGH", loadedMeta.getPriority());

        assertEquals(2, loadedMeta.getChunks().size());
        assertEquals(0, loadedMeta.getChunks().get(0).getId());
        assertEquals(2_500_000, loadedMeta.getChunks().get(0).getDownloadedBytes());
        assertFalse(loadedMeta.getChunks().get(0).isCompleted());

        assertEquals(1, loadedMeta.getChunks().get(1).getId());
        assertEquals(5_000_000, loadedMeta.getChunks().get(1).getDownloadedBytes());
        assertTrue(loadedMeta.getChunks().get(1).isCompleted());

        MetadataStore.deleteMetadata(metaPath);
        assertFalse(metaPath.toFile().exists());
    }
}
