package com.ids.bot.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Per-navigation ground-truth log — equivalent of the Locust bot's
 * request_logger.py, for KS testing on inter-navigation gaps without a PCAP.
 *
 * Persona/session id are not passed explicitly: MixedTrafficRunner already
 * names each bot thread "bot-<persona>-<id>" before running a scenario, so
 * that thread name is reused here as the row's identity column. (App.java's
 * single-bot mode just logs under "main" — one thread, no grouping needed.)
 */
public class NavLog {

    private static final Path LOG_PATH = Paths.get("nav_log.csv");
    private static final Object LOCK = new Object();
    private static volatile boolean initialized = false;

    public static void logNav(String pageState) {
        ensureInitialized();
        String line = String.format("%d,%s,%s%n",
            System.currentTimeMillis(), Thread.currentThread().getName(), pageState);
        synchronized (LOCK) {
            try {
                Files.writeString(LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("[nav_log] failed to write: " + e.getMessage());
            }
        }
    }

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (LOCK) {
            if (initialized) return;
            try {
                Files.writeString(LOG_PATH, "epoch_millis,thread_label,page\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.err.println("[nav_log] failed to initialize: " + e.getMessage());
            }
            initialized = true;
        }
    }
}
