package jpm.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean debugEnabled = false;

    public static void setDebug(boolean debug) {
        debugEnabled = debug;
    }

    public static void info(String message) {
        System.out.println(message);
    }

    public static void warn(String message) {
        System.err.println("Warning: " + message);
    }

    public static void error(String message) {
        System.err.println("Error: " + message);
    }

    public static void debug(String message) {
        if (debugEnabled) {
            System.out.println("[DEBUG " + LocalDateTime.now().format(DATE_FORMAT) + "] " + message);
        }
    }
}
