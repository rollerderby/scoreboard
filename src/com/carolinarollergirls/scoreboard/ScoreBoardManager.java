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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.json.JSONStateSnapshotter;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONListener;
import com.carolinarollergirls.scoreboard.viewer.ScoreBoardMetricsCollector;
import com.carolinarollergirls.scoreboard.viewer.TwitterViewer;

public class ScoreBoardManager {
    public interface Logger {
        void log(String msg);
    }

    public static void start() {
        setSystemProperties();
        loadVersion();
        loadProperties();

        scoreBoard = new ScoreBoardImpl();

        // JSON updates.
        JSONStateManager jsm = new JSONStateManager();
        new ScoreBoardJSONListener(scoreBoard, jsm);

        // Controllers.
        registerScoreBoardController(new JettyServletScoreBoardController(scoreBoard, jsm));

        // Viewers.
        registerScoreBoardViewer(new TwitterViewer((ScoreBoard)scoreBoard));
        registerScoreBoardViewer(new ScoreBoardMetricsCollector((ScoreBoard)scoreBoard).register());
        registerScoreBoardViewer(new JSONStateSnapshotter(jsm, ScoreBoardManager.getDefaultPath()));

        //FIXME - not the best way to load autosave doc.
        scoreBoard.getXmlScoreBoard().load();
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


    public static Properties getProperties() { return new Properties(properties); }

    public static String getProperty(String key) { return properties.getProperty(key); }
    public static String getProperty(String key, String dflt) { return properties.getProperty(key, dflt); }


    public static Object getScoreBoardController(String key) { return controllers.get(key); }

    public static Object getScoreBoardViewer(String key) { return viewers.get(key); }

    public static void registerScoreBoardController(Object sbC) {
        controllers.put(sbC.getClass().getName(), sbC);
    }

    public static void registerScoreBoardViewer(Object sbV) {
        viewers.put(sbV.getClass().getName(), sbV);
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

    private static void loadProperties() {
        InputStream is = ScoreBoardManager.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);

        if (null == is) {
            try {
                is = new FileInputStream(new File(new File(defaultPath, PROPERTIES_DIR_NAME), PROPERTIES_FILE_NAME));
            } catch ( FileNotFoundException fnfE ) {
                doExit("Could not find properties file " + PROPERTIES_FILE_NAME);
            }
        }

        try {
            properties.load(is);
        } catch ( Exception e ) {
            doExit("Could not load " + PROPERTIES_FILE_NAME + " file : " + e.getMessage(), e);
        }

        for (String key : properties_overrides.keySet()) {
            properties.put(key, properties_overrides.get(key));
        }

        try { is.close(); }
        catch ( IOException ioE ) { }
    }

    public static void setLogger(Logger l) { logger = l; }
    public static File getDefaultPath() { return defaultPath; }
    public static void setDefaultPath(File f) { defaultPath = f; }
    public static void setPropertyOverride(String key, String value) {
        properties_overrides.put(key, value);
        properties.put(key, value);
    }

    private static Properties properties = new Properties();
    private static Map<String,String> properties_overrides = new HashMap<String,String>();
    private static Map<String,Object> controllers = new ConcurrentHashMap<String,Object>();
    private static Map<String,Object> viewers = new ConcurrentHashMap<String,Object>();

    private static ScoreBoard scoreBoard;
    private static Logger logger = null;

    private static String versionRelease = "";

    private static File defaultPath = new File(".");

    public static final String VERSION_PATH = "com/carolinarollergirls/scoreboard/version";
    public static final String VERSION_RELEASE_PROPERTIES_NAME = VERSION_PATH+"/release.properties";
    public static final String VERSION_RELEASE_KEY = "release";

    public static final String PROPERTIES_DIR_NAME = "config";
    public static final String PROPERTIES_FILE_NAME = "crg.scoreboard.properties";
}
