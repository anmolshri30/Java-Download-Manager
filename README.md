# JDM - Java Download Manager

JDM (Java Download Manager) is a command-line download manager made using Java 21. It is a Programming in Java project that focuses mainly on file handling, networking, multithreading and basic download management.

The program can download files using HTTP/HTTPS links and can use multiple threads for larger files when the server supports byte-range requests. It also has options for pausing, resuming and cancelling downloads, along with a queue and download history.

## Features

* Download files using HTTP/HTTPS URLs
* Multi-threaded downloading for supported servers
* Automatic fallback to a normal single-threaded download
* Pause and resume downloads
* Cancel downloads
* Show download progress, speed and estimated time remaining
* Run multiple downloads at the same time
* Priority-based download queue
* Retry failed downloads
* Save download information for recovery after the program is restarted
* Keep a download history
* Command-line interface
* Automated tests using Java's built-in HTTP server

## How Multithreaded Downloads Work

When the server supports byte-range requests and the file size is known, JDM divides the file into smaller parts.

For example, a file can be divided into four parts:

```text
File
+----------------+----------------+----------------+----------------+
|    Chunk 1     |    Chunk 2     |    Chunk 3     |    Chunk 4     |
+----------------+----------------+----------------+----------------+
       |                |                |                |
    Thread 1         Thread 2         Thread 3         Thread 4
```

Each thread downloads its assigned part of the file. The parts are written to their respective positions in the file using `FileChannel`.

If the server does not support byte-range requests, JDM uses a normal single-threaded download instead.

## Project Structure

```text
src/
└── main/
    └── java/
        └── com/
            └── jdm/
                ├── Main.java
                │
                ├── cli/
                │   └── CommandLineInterface.java
                │
                ├── model/
                │   ├── DownloadTask.java
                │   ├── DownloadMetadata.java
                │   ├── DownloadChunk.java
                │   ├── DownloadStatus.java
                │   └── DownloadPriority.java
                │
                ├── download/
                │   ├── Downloader.java
                │   ├── SingleThreadDownloader.java
                │   ├── MultiThreadDownloader.java
                │   ├── DownloadWorker.java
                │   └── ChunkManager.java
                │
                ├── manager/
                │   ├── DownloadManager.java
                │   ├── QueueManager.java
                │   └── HistoryManager.java
                │
                ├── storage/
                │   └── MetadataStore.java
                │
                └── util/
                    ├── SpeedCalculator.java
                    ├── InputValidator.java
                    └── FormatUtils.java
```

### Main Parts

**`cli`**
Handles the terminal menu, user input and displaying download progress.

**`model`**
Contains the classes and enums used to represent downloads, chunks, status and priority.

**`download`**
Contains the actual download logic. It includes both single-threaded and multi-threaded download methods.

**`manager`**
Controls downloads, the waiting queue and download history.

**`storage`**
Handles saving and loading do
