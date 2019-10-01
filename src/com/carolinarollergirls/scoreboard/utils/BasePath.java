package com.carolinarollergirls.scoreboard.utils;

import java.io.File;

public class BasePath {
    public static File get() { return basePath; }
    public static void set(File f) { basePath = f; } // for unit tests

    private static File basePath = new File(".");
}
