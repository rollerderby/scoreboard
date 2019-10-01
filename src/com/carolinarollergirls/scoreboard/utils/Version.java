package com.carolinarollergirls.scoreboard.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {
    public static boolean load() throws IOException {
        Properties versionProperties = new Properties();
        ClassLoader cL = Version.class.getClassLoader();
        InputStream releaseIs = cL.getResourceAsStream(VERSION_RELEASE_PROPERTIES_NAME);
        try {
            versionProperties.load(releaseIs);
        } catch ( NullPointerException npE ) {
            Logger.printMessage("Could not find version release properties file '"+VERSION_RELEASE_PROPERTIES_NAME+"'");
            return false;
        } catch ( IOException ioE ) {
            Logger.printMessage("Could not load version release properties file '"+VERSION_RELEASE_PROPERTIES_NAME+"'");
            throw ioE;
        }
        try { releaseIs.close(); } catch ( Exception e ) { }
        versionRelease = versionProperties.getProperty(VERSION_RELEASE_KEY);
        Logger.printMessage("Carolina Rollergirls Scoreboard version "+Version.get());
        return true;
    }

    public static String get() { return versionRelease; }

    private static String versionRelease = "unset";

    public static final String VERSION_PATH = "com/carolinarollergirls/scoreboard/version";
    public static final String VERSION_RELEASE_PROPERTIES_NAME = VERSION_PATH+"/release.properties";
    public static final String VERSION_RELEASE_KEY = "release";
}
