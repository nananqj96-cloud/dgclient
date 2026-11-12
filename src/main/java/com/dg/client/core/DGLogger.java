package com.dg.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Centralized DGClient logger with runtime debug toggle and bounded ring-buffer
 * for in-game diagnostic inspection.
 */
public final class DGLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("DGClient");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_RECENT_LOGS = 250;
    private static final Deque<String> RECENT_LOGS = new ArrayDeque<>(MAX_RECENT_LOGS);

    private static volatile boolean debugEnabled = false;

    private DGLogger() {
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        info("Debug logging {}", enabled ? "ENABLED" : "DISABLED");
    }

    public static void info(String message, Object... args) {
        LOGGER.info("[DGClient] " + message, args);
        record("INFO", format(message, args));
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn("[DGClient] " + message, args);
        record("WARN", format(message, args));
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error("[DGClient] " + message, throwable);
        record("ERROR", message + (throwable != null ? " (" + throwable.getClass().getSimpleName() + ": " + throwable.getMessage() + ")" : ""));
    }

    public static void debug(String message, Object... args) {
        if (!debugEnabled) {
            return;
        }
        LOGGER.info("[DGClient/DEBUG] " + message, args);
        record("DEBUG", format(message, args));
    }

    public static List<String> getRecentLogs() {
        synchronized (RECENT_LOGS) {
            return new ArrayList<>(RECENT_LOGS);
        }
    }

    private static void record(String level, String msg) {
        String entry = "[" + LocalTime.now().format(TIME_FMT) + "] [" + level + "] " + msg;
        synchronized (RECENT_LOGS) {
            if (RECENT_LOGS.size() >= MAX_RECENT_LOGS) {
                RECENT_LOGS.pollFirst();
            }
            RECENT_LOGS.addLast(entry);
        }
    }

    private static String format(String template, Object... args) {
        if (args == null || args.length == 0 || template == null) {
            return String.valueOf(template);
        }
        StringBuilder sb = new StringBuilder(template.length() + 32);
        int argIdx = 0;
        int cursor = 0;
        while (cursor < template.length()) {
            int brace = template.indexOf("{}", cursor);
            if (brace < 0 || argIdx >= args.length) {
                sb.append(template, cursor, template.length());
                break;
            }
            sb.append(template, cursor, brace);
            sb.append(args[argIdx++]);
            cursor = brace + 2;
        }
        return sb.toString();
    }
}
