package com.jdm;

import com.jdm.manager.QueueManager;
import com.jdm.model.DownloadPriority;
import com.jdm.model.DownloadTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueueManagerTest {

    @Test
    void testPriorityOrdering() {
        QueueManager qm = new QueueManager(2);

        DownloadTask low = new DownloadTask("t1", "http://ex.com/1", "1.bin", 2, DownloadPriority.LOW);
        DownloadTask high = new DownloadTask("t2", "http://ex.com/2", "2.bin", 2, DownloadPriority.HIGH);
        DownloadTask medium = new DownloadTask("t3", "http://ex.com/3", "3.bin", 2, DownloadPriority.MEDIUM);

        qm.enqueue(low);
        qm.enqueue(high);
        qm.enqueue(medium);

        // First polled task should be HIGH priority
        DownloadTask next1 = qm.pollNextTask(0);
        assertNotNull(next1);
        assertEquals("t2", next1.getId());

        // Next polled task should be MEDIUM priority
        qm.dequeue(next1);
        DownloadTask next2 = qm.pollNextTask(0);
        assertNotNull(next2);
        assertEquals("t3", next2.getId());

        // Next polled task should be LOW priority
        qm.dequeue(next2);
        DownloadTask next3 = qm.pollNextTask(0);
        assertNotNull(next3);
        assertEquals("t1", next3.getId());
    }

    @Test
    void testMaxConcurrentDownloadsLimit() {
        QueueManager qm = new QueueManager(2);

        DownloadTask t1 = new DownloadTask("t1", "http://ex.com/1", "1.bin", 2, DownloadPriority.MEDIUM);
        DownloadTask t2 = new DownloadTask("t2", "http://ex.com/2", "2.bin", 2, DownloadPriority.MEDIUM);

        qm.enqueue(t1);
        qm.enqueue(t2);

        assertNotNull(qm.pollNextTask(0)); // 0 active < 2 max -> allowed
        assertNotNull(qm.pollNextTask(1)); // 1 active < 2 max -> allowed
        assertNull(qm.pollNextTask(2));    // 2 active == 2 max -> blocked
    }
}
