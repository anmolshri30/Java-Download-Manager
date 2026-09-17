package com.jdm.cli;

import com.jdm.manager.DownloadManager;
import com.jdm.model.DownloadMetadata;
import com.jdm.model.DownloadPriority;
import com.jdm.model.DownloadStatus;
import com.jdm.model.DownloadTask;
import com.jdm.util.FormatUtils;
import com.jdm.util.InputValidator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive Command Line Interface for JDM.
 */
public class CommandLineInterface {
    private final DownloadManager downloadManager;
    private final Scanner scanner;
    private volatile boolean running = true;

    public CommandLineInterface(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printBanner();
        checkInterruptedDownloadsOnStartup();

        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleAddDownload();
                    break;
                case "2":
                    handleListActiveDownloads();
                    break;
                case "3":
                    handlePauseDownload();
                    break;
                case "4":
                    handleResumeDownload();
                    break;
                case "5":
                    handleCancelDownload();
                    break;
                case "6":
                    handleViewQueue();
                    break;
                case "7":
                    handleViewHistory();
                    break;
                case "8":
                    handleSettings();
                    break;
                case "9":
                    printHelp();
                    break;
                case "0":
                case "exit":
                    running = false;
                    System.out.println("\nExiting JDM. Goodbye!");
                    downloadManager.shutdown();
                    break;
                default:
                    System.out.println("[!] Invalid option. Please try again.");
            }
        }
    }

    private void printBanner() {
        System.out.println("=========================================================");
        System.out.println("   _  ____  __  ___  (Java Download Manager v1.0)");
        System.out.println("  | |/ /  |/  |/ _ \\ ");
        System.out.println("  |   /| /| / / // / Academic-Grade Multithreaded Engine");
        System.out.println(" /_/|_|_/ |_/ /____/  Java 21 • NIO • Range Chunks");
        System.out.println("=========================================================");
    }

    private void checkInterruptedDownloadsOnStartup() {
        System.out.print("Scanning current directory for interrupted downloads (.jdm)... ");
        List<DownloadMetadata> interrupted = downloadManager.scanAndRecoverInterruptedDownloads(".");
        if (interrupted.isEmpty()) {
            System.out.println("None found.");
        } else {
            System.out.println("\n[!] Found " + interrupted.size() + " interrupted download(s):");
            for (DownloadMetadata meta : interrupted) {
                System.out.println("   - ID: " + meta.getId() + " | File: " + meta.getTargetFilePath() + " | Progress: " + FormatUtils.formatBytes(calculateMetaDownloaded(meta)) + " / " + FormatUtils.formatBytes(meta.getTotalSize()));
            }
            System.out.print("Would you like to resume all interrupted downloads? (y/n): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) {
                for (DownloadMetadata meta : interrupted) {
                    downloadManager.resumeDownload(meta.getId());
                }
                System.out.println("[+] Resumed " + interrupted.size() + " task(s).");
            }
        }
    }

    private long calculateMetaDownloaded(DownloadMetadata meta) {
        if (meta.getChunks() == null) return 0;
        long sum = 0;
        for (DownloadMetadata.ChunkMetadata cm : meta.getChunks()) {
            sum += cm.getDownloadedBytes();
        }
        return sum;
    }

    private void printMainMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Add New Download");
        System.out.println("2. Active Downloads Monitor");
        System.out.println("3. Pause Download");
        System.out.println("4. Resume Download");
        System.out.println("5. Cancel Download");
        System.out.println("6. View Queue");
        System.out.println("7. Download History");
        System.out.println("8. Queue Settings");
        System.out.println("9. Help & System Info");
        System.out.println("0. Exit");
        System.out.print("Enter choice [0-9]: ");
    }

    private void handleAddDownload() {
        System.out.println("\n--- ADD NEW DOWNLOAD ---");
        System.out.print("Enter Download URL: ");
        String url = scanner.nextLine().trim();

        if (!InputValidator.isValidUrl(url)) {
            System.out.println("[X] Invalid URL. Must start with http:// or https://");
            return;
        }

        String defaultFileName = InputValidator.extractFileNameFromUrl(url);
        System.out.print("Enter Destination Path [Default: ./" + defaultFileName + "]: ");
        String pathInput = scanner.nextLine().trim();
        String targetPathStr = pathInput.isEmpty() ? defaultFileName : pathInput;

        Path targetPath = Paths.get(targetPathStr);

        // Check file collision handling
        if (Files.exists(targetPath)) {
            System.out.println("[!] Target file already exists: " + targetPathStr);
            System.out.println("    1. Overwrite / Replace");
            System.out.println("    2. Auto-rename (e.g. filename_1.bin)");
            System.out.println("    3. Cancel operation");
            System.out.print("Select action [1-3]: ");
            String action = scanner.nextLine().trim();
            if (action.equals("2")) {
                targetPathStr = getAutoRenamedPath(targetPathStr);
                System.out.println("[+] Renamed target path to: " + targetPathStr);
            } else if (!action.equals("1")) {
                System.out.println("[*] Download cancelled.");
                return;
            }
        }

        System.out.print("Enter Thread Count (1-32) [Default: 4]: ");
        String threadStr = scanner.nextLine().trim();
        int threads = 4;
        if (!threadStr.isEmpty()) {
            try {
                int parsed = Integer.parseInt(threadStr);
                if (InputValidator.isValidThreadCount(parsed)) {
                    threads = parsed;
                } else {
                    System.out.println("[!] Invalid thread count. Using default 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Using default 4 threads.");
            }
        }

        System.out.print("Enter Priority (HIGH, MEDIUM, LOW) [Default: MEDIUM]: ");
        String priorityStr = scanner.nextLine().trim();
        DownloadPriority priority = InputValidator.parsePriority(priorityStr);

        DownloadTask task = downloadManager.createDownloadTask(url, targetPathStr, threads, priority);
        System.out.println("[+] Task created! Task ID: " + task.getId() + " | Status: " + task.getStatus());
    }

    private String getAutoRenamedPath(String targetPathStr) {
        File f = new File(targetPathStr);
        String name = f.getName();
        String parent = f.getParent();
        int dotIdx = name.lastIndexOf('.');
        String baseName = dotIdx != -1 ? name.substring(0, dotIdx) : name;
        String ext = dotIdx != -1 ? name.substring(dotIdx) : "";

        int count = 1;
        while (true) {
            String newName = baseName + "_" + count + ext;
            File newFile = parent != null ? new File(parent, newName) : new File(newName);
            if (!newFile.exists()) {
                return newFile.getPath();
            }
            count++;
        }
    }

    private void handleListActiveDownloads() {
        System.out.println("\n--- ACTIVE DOWNLOADS MONITOR ---");
        List<DownloadTask> all = downloadManager.getAllTasks();
        if (all.isEmpty()) {
            System.out.println("No tasks currently registered.");
            return;
        }

        for (DownloadTask t : all) {
            double pct = t.getProgressPercentage();
            String bar = FormatUtils.buildProgressBar(pct, 20);
            String downloaded = FormatUtils.formatBytes(t.getDownloadedBytes());
            String total = t.getTotalSize() > 0 ? FormatUtils.formatBytes(t.getTotalSize()) : "Unknown";
            String speed = FormatUtils.formatSpeed(t.getSpeedBytesPerSec());
            String eta = FormatUtils.formatTime(t.getEtaSeconds());

            System.out.printf("ID: %s | Status: %-11s | Prio: %-6s | Threads: %d%n",
                    t.getId(), t.getStatus(), t.getPriority(), t.getThreadCount());
            System.out.printf("File: %s%n", t.getFileName());
            System.out.printf("%s %.1f%%  [%s / %s]  Speed: %s  ETA: %s%n",
                    bar, pct, downloaded, total, speed, eta);
            if (t.getErrorMessage() != null) {
                System.out.println("   [Error]: " + t.getErrorMessage());
            }
            System.out.println("---------------------------------------------------------");
        }
    }

    private void handlePauseDownload() {
        System.out.print("\nEnter Task ID to Pause: ");
        String id = scanner.nextLine().trim();
        DownloadTask task = downloadManager.getTask(id);
        if (task == null) {
            System.out.println("[!] Task ID not found.");
            return;
        }
        downloadManager.pauseDownload(id);
        System.out.println("[+] Pause signal sent to Task ID: " + id);
    }

    private void handleResumeDownload() {
        System.out.print("\nEnter Task ID to Resume: ");
        String id = scanner.nextLine().trim();
        DownloadTask task = downloadManager.getTask(id);
        if (task == null) {
            System.out.println("[!] Task ID not found.");
            return;
        }
        downloadManager.resumeDownload(id);
        System.out.println("[+] Resume signal sent to Task ID: " + id);
    }

    private void handleCancelDownload() {
        System.out.print("\nEnter Task ID to Cancel: ");
        String id = scanner.nextLine().trim();
        DownloadTask task = downloadManager.getTask(id);
        if (task == null) {
            System.out.println("[!] Task ID not found.");
            return;
        }
        downloadManager.cancelDownload(id);
        System.out.println("[+] Task ID " + id + " cancelled.");
    }

    private void handleViewQueue() {
        System.out.println("\n--- QUEUED TASKS ---");
        List<DownloadTask> queued = downloadManager.getQueueManager().getQueuedTasks();
        if (queued.isEmpty()) {
            System.out.println("No queued tasks waiting.");
            return;
        }

        for (DownloadTask t : queued) {
            System.out.printf("ID: %s | Priority: %-6s | Threads: %d | File: %s | URL: %s%n",
                    t.getId(), t.getPriority(), t.getThreadCount(), t.getFileName(), t.getUrl());
        }
    }

    private void handleViewHistory() {
        System.out.println("\n--- DOWNLOAD HISTORY LOG ---");
        List<String> history = downloadManager.getHistoryManager().getHistory();
        if (history.isEmpty()) {
            System.out.println("No historical records found.");
        } else {
            for (String entry : history) {
                System.out.print(entry);
            }
        }
    }

    private void handleSettings() {
        System.out.println("\n--- QUEUE SETTINGS ---");
        System.out.println("Current Max Concurrent Downloads: " + downloadManager.getQueueManager().getMaxConcurrentDownloads());
        System.out.print("Enter New Max Concurrent Limit (1-10) [or Enter to keep]: ");
        String val = scanner.nextLine().trim();
        if (!val.isEmpty()) {
            try {
                int limit = Integer.parseInt(val);
                if (limit >= 1 && limit <= 10) {
                    downloadManager.getQueueManager().setMaxConcurrentDownloads(limit);
                    System.out.println("[+] Max concurrent downloads set to " + limit);
                } else {
                    System.out.println("[!] Limit must be between 1 and 10.");
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid integer.");
            }
        }
    }

    private void printHelp() {
        System.out.println("\n--- HELP & SYSTEM INFORMATION ---");
        System.out.println("JDM (Java Download Manager)");
        System.out.println("Built with Java 21 LTS, java.net.http.HttpClient, and NIO FileChannel.");
        System.out.println();
        System.out.println("Key Features:");
        System.out.println(" - HTTP Range Header Detection (RFC 7233)");
        System.out.println(" - Chunked Multithreaded Downloads via ExecutorService");
        System.out.println(" - Atomic .jdm Metadata Persistence & Automatic Crash Recovery");
        System.out.println(" - Priority Queue Management (HIGH, MEDIUM, LOW)");
        System.out.println(" - Dynamic Moving Window Speed & ETA Estimation");
        System.out.println(" - Retries with Exponential Backoff on 5xx / Transients");
    }
}
