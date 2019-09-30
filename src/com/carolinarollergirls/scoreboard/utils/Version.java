package com.carolinarollergirls.scoreboard.utils;

public class Version {
    public static void set(String version) {
        versionRelease = version;
    }

    public static String get() {
        return versionRelease;
    }

    private static String versionRelease = "";
}
