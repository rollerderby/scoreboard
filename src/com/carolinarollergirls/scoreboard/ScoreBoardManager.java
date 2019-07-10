package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.json.AutoSaveJSONState;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.json.JSONStateSnapshotter;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONListener;
import com.carolinarollergirls.scoreboard.viewer.ScoreBoardMetricsCollector;

public class ScoreBoardManager {
    public interface Logger {
        void log(String msg);
    }

    public static void start(String host, int port) {
        setSystemProperties();
        loadVersion();

        scoreBoard = new ScoreBoardImpl();

        // JSON updates.
        JSONStateManager jsm = new JSONStateManager();
        new ScoreBoardJSONListener(scoreBoard, jsm);

        // Controllers.
        new JettyServletScoreBoardController(scoreBoard, jsm, host, port);

        // Viewers.
        new ScoreBoardMetricsCollector(scoreBoard).register();
        new JSONStateSnapshotter(jsm, ScoreBoardManager.getDefaultPath());

        File autoSaveDir = new File(getDefaultPath(), "config/autosave");
        if (!AutoSaveJSONState.loadAutoSave(scoreBoard, autoSaveDir)) {
            try {
                printMessage("No autosave to load from, using default.json");
                AutoSaveJSONState.loadFile(scoreBoard, new File(getDefaultPath(), "config/default.json"));
            } catch (Exception e) {
              doExit("Error loading default configuration", e);
            }
        }
        scoreBoard.postAutosaveUpdate();

        // Only start auto-saves once everything is loaded in.
        new AutoSaveJSONState(jsm, autoSaveDir);
    }

    public static void stop() {
    }

    public static String getVersion() {
        return versionRelease;
    }

    public static void printMessage(String msg) {
        if (logger != null) {
            logger.log(msg);
        } else {
            System.err.println(msg);
        }
    }


    public static void doExit(String err) { doExit(err, null); }
    public static void doExit(String err, Exception ex) {
        printMessage(err);
        if (ex != null) {
            ex.printStackTrace();
        }
        printMessage("Fatal error.	Exiting in 15 seconds.");
        try { Thread.sleep(15000); } catch ( Exception e ) { /* Probably Ctrl-C or similar, ignore. */ }
        System.exit(1);
    }

    private static void setSystemProperties() {
        System.getProperties().setProperty("twitter4j.loggerFactory", "twitter4j.internal.logging.NullLoggerFactory");
    }

    private static void loadVersion() {
        Properties versionProperties = new Properties();
        ClassLoader cL = ScoreBoardManager.class.getClassLoader();
        InputStream releaseIs = cL.getResourceAsStream(VERSION_RELEASE_PROPERTIES_NAME);
        try {
            versionProperties.load(releaseIs);
        } catch ( NullPointerException npE ) {
            doExit("Could not find version release properties file '"+VERSION_RELEASE_PROPERTIES_NAME+"'");
        } catch ( IOException ioE ) {
            doExit("Could not load version release properties file '"+VERSION_RELEASE_PROPERTIES_NAME+"'", ioE);
        }
        try { releaseIs.close(); } catch ( Exception e ) { }
        versionRelease = versionProperties.getProperty(VERSION_RELEASE_KEY);
        printMessage("Carolina Rollergirls Scoreboard version "+getVersion());
    }

    public static void setLogger(Logger l) { logger = l; }
    public static File getDefaultPath() { return defaultPath; }
    public static void setDefaultPath(File f) { defaultPath = f; }

    private static ScoreBoard scoreBoard;
    private static Logger logger = null;

    private static String versionRelease = "";

    private static File defaultPath = new File(".");

    public static final String VERSION_PATH = "com/carolinarollergirls/scoreboard/version";
    public static final String VERSION_RELEASE_PROPERTIES_NAME = VERSION_PATH+"/release.properties";
    public static final String VERSION_RELEASE_KEY = "release";
}
