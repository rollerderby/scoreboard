package com.carolinarollergirls.scoreboard.utils;

public abstract class Logger {
    protected static void setLogger(Logger log) {
        instance = log;
    }
    
    public static Logger getInstance() {
        return instance;
    }
    
    public static void printMessage(String msg) {
        if (instance != null) {
            instance.log(msg);
        } else {
            System.err.println(msg);
        }
    }

    public abstract void log(String msg);
    
    static Logger instance;
}
