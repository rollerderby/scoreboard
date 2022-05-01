package com.carolinarollergirls.scoreboard.utils;

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

    public static void printStackTrace(Throwable e) {
        instance.log(e.toString());
        for (StackTraceElement element : e.getStackTrace()) { instance.log("        at " + element.toString()); }
    }

    public abstract void log(String msg);

    static Logger instance;
}
