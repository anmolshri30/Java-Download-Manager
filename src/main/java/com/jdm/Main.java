package com.jdm;

import com.jdm.cli.CommandLineInterface;
import com.jdm.manager.DownloadManager;

/**
 * Entry point for JDM application.
 */
public class Main {
    public static void main(String[] args) {
        DownloadManager downloadManager = new DownloadManager();

        // Register shutdown hook for Ctrl+C / SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[JDM] Gracefully shutting down downloads and saving metadata state...");
            downloadManager.shutdown();
        }));

        CommandLineInterface cli = new CommandLineInterface(downloadManager);
        cli.start();
    }
}
