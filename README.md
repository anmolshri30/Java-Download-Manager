# JDM (Java Download Manager)

Academic-grade, high-performance CLI download manager built with **Java 21 LTS**, standard `java.net.http.HttpClient`, Java NIO (`FileChannel`), ExecutorService multithreading, chunked range downloading, metadata persistence, crash recovery, and priority queueing.

---

## Key Features

1. **HTTP Byte-Range Auto-Detection (RFC 7233)**:
   - Probes remote servers via HTTP `HEAD` / `GET` range requests.
   - Automatically detects `Accept-Ranges: bytes` and `Content-Length`.
2. **Multithreaded Chunk Downloading**:
   - Divides large files (>1 MB) into $N$ contiguous byte-range chunks.
   - Concurrently downloads chunks into target position offsets using Java NIO `FileChannel`.
3. **Single-Threaded Fallback Strategy**:
   - Automatically falls back to single-stream download for servers without byte-range support or unknown file sizes.
4. **Metadata Persistence & Crash Recovery**:
   - Saves `.jdm` JSON state files periodically during downloads.
   - Automatically scans directory on startup to resume interrupted/paused downloads.
5. **Priority-Based Queue Engine**:
   - Enforces configurable maximum concurrent downloads limit.
   - Automatically schedules waiting downloads ordered by priority (`HIGH` > `MEDIUM` > `LOW`) and timestamp.
6. **Interactive Terminal CLI**:
   - Interactive menu, live progress bars `[==================------]`, percentage, transferred bytes, real-time speed (MB/s), and ETA calculations.
7. **Comprehensive Automated Unit & Integration Tests**:
   - Tests run against an embedded JDK `com.sun.net.httpserver.HttpServer` with zero external network dependencies.

---

## Project Structure

```
com.jdm
 ├── Main.java                        # Entry point, initializes CLI & handles shutdown hooks
 ├── cli/
 │    └── CommandLineInterface.java   # Interactive CLI menu system, user input validation, progress renderer
 ├── model/
 │    ├── DownloadTask.java           # Domain model representing active/queued download
 │    ├── DownloadMetadata.java       # DTO serialized to .jdm files for crash recovery
 │    ├── DownloadChunk.java          # Range chunk state tracking
 │    ├── DownloadStatus.java         # Enum: QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED, INTERRUPTED
 │    └── DownloadPriority.java       # Enum: HIGH, MEDIUM, LOW
 ├── download/
 │    ├── Downloader.java             # Interface for download strategies
 │    ├── SingleThreadDownloader.java # Single-stream download strategy
 │    ├── MultiThreadDownloader.java  # Multithreaded range downloader using ExecutorService & FileChannel
 │    ├── DownloadWorker.java         # Worker task fetching a byte-range chunk
 │    └── ChunkManager.java           # Chunk range division & completion verification
 ├── manager/
 │    ├── DownloadManager.java        # Core orchestrator managing tasks, pause/resume, cancel, retries
 │    ├── QueueManager.java           # Priority-based queue manager for active & waiting tasks
 │    └── HistoryManager.java         # Persistent log of completed/failed/cancelled tasks
 ├── storage/
 │    └── MetadataStore.java          # Serialization and persistence of .jdm metadata files
 └── util/
      ├── SpeedCalculator.java        # Dynamic sliding-window speed and ETA estimation
      ├── InputValidator.java         # Input validation (URLs, thread counts, directories)
      └── FormatUtils.java            # Byte size, speed, duration, and progress bar formatting
```

---

## Build & Test Instructions

### Prerequisites
- JDK 21+
- Apache Maven 3.9+

### Running Automated Tests
```powershell
$env:JAVA_HOME="C:\Users\MY-PC\.vscode\extensions\redhat.java-1.56.0-win32-x64\jre\21.0.12.1-win32-x86_64"
& "C:\Users\MY-PC\.vscode\extensions\oracle.oracle-java-26.0.2\nbcode\java\maven\bin\mvn.cmd" clean test
```

### Packaging Executable JAR
```powershell
& "C:\Users\MY-PC\.vscode\extensions\oracle.oracle-java-26.0.2\nbcode\java\maven\bin\mvn.cmd" clean package
```

### Running the CLI Application
```powershell
& "C:\Users\MY-PC\.vscode\extensions\redhat.java-1.56.0-win32-x64\jre\21.0.12.1-win32-x86_64\bin\java.exe" -jar target/jdm.jar
```

---

## Crash Recovery & `.jdm` Metadata Structure

During active downloads, JDM writes a JSON metadata file alongside the target download (e.g. `file.zip.jdm`).
Sample `.jdm` structure:

```json
{
  "id": "a1b2c3d4",
  "url": "https://example.com/file.zip",
  "targetFilePath": "file.zip",
  "totalSize": 10485760,
  "threadCount": 4,
  "priority": "HIGH",
  "createdTime": 1726618800000,
  "lastUpdatedTime": 1726618805000,
  "chunks": [
    {
      "id": 0,
      "startByte": 0,
      "endByte": 2621439,
      "downloadedBytes": 2621440,
      "completed": true
    },
    {
      "id": 1,
      "startByte": 2621440,
      "endByte": 5242879,
      "downloadedBytes": 1310720,
      "completed": false
    }
  ]
}
```

Upon successful completion, the `.part` file is atomically renamed to the destination file and the `.jdm` metadata file is deleted.
