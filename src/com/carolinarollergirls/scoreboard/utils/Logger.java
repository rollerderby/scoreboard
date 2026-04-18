package com.carolinarollergirls.scoreboard.utils;

import java.time.LocalDateTime;

public abstract class Logger {
    protected static void setLogger(Logger log) { instance = log; }

    public static Logger getInstance() { return instance; }

    public static void printMessage(String msg) {
        if (instance != null) {
            instance.log(msg);
        } else {
            System.err.println(msg);
        }
    }

    public static void printStackTrace(String context, Throwable e) {
        instance.log("At " + LocalDateTime.now().toString() + ": Exception in " + context + ": " + e.toString());
        for (StackTraceElement element : e.getStackTrace()) { instance.log("        at " + element.toString()); }
    }

    public abstract void log(String msg);

    static Logger instance;
}
