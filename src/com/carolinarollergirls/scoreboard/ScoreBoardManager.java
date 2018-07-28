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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public class ScoreBoardManager {
	public interface Logger {
		void log(String msg);
	}

	public static void start() {
		setSystemProperties();
		loadVersion();
		loadProperties();
		loadModel();
		loadControllers();
		loadViewers();

		//FIXME - not the best way to load autosave doc.
		scoreBoardModel.getXmlScoreBoard().load();

		// Register Prometheus metrics about scoreboard state.
		new ScoreBoardMetricsCollector(scoreBoardModel).register();
	}

	public static void stop() {
	}

	public static String getVersion() {
		if ("".equals(versionBuild))
			return versionRelease;
		else
			return versionRelease+"-"+versionBuild;
	}

	public static void printMessage(String msg) {
		if (logger != null)
			logger.log(msg);
		else
			System.err.println(msg);
	}

	public static Properties getProperties() { return new Properties(properties); }

	public static String getProperty(String key) { return properties.getProperty(key); }
	public static String getProperty(String key, String dflt) { return properties.getProperty(key, dflt); }

	public static ScoreBoardController getScoreBoardController(String key) { return controllers.get(key); }

	public static ScoreBoardViewer getScoreBoardViewer(String key) { return viewers.get(key); }

	public static void registerScoreBoardController(ScoreBoardController sbC) {
		sbC.setScoreBoardModel(scoreBoardModel);
		controllers.put(sbC.getClass().getName(), sbC);
	}

	public static void registerScoreBoardViewer(ScoreBoardViewer sbV) {
		sbV.setScoreBoard(scoreBoardModel.getScoreBoard());
		viewers.put(sbV.getClass().getName(), sbV);
	}

	public static void doExit(String err) { doExit(err, null); }
	public static void doExit(String err, Exception ex) {
		printMessage(err);
		if (ex != null)
			ex.printStackTrace();
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
		InputStream buildIs = cL.getResourceAsStream(VERSION_BUILD_PROPERTIES_NAME);
		try {
			versionProperties.load(releaseIs);
		} catch ( NullPointerException npE ) {
			doExit("Could not find version release properties file '"+VERSION_RELEASE_PROPERTIES_NAME+"'");
		} catch ( IOException ioE ) {
			doExit("Could not load version release properties file '"+VERSION_RELEASE_PROPERTIES_NAME+"'", ioE);
		}
		try {
			versionProperties.load(buildIs);
		} catch ( Exception e ) {
			/* Ignore missing build properties */
			versionProperties.setProperty(VERSION_BUILD_KEY, "");
		}
		try { releaseIs.close(); } catch ( Exception e ) { }
		try { buildIs.close(); } catch ( Exception e ) { }
		versionRelease = versionProperties.getProperty(VERSION_RELEASE_KEY);
		versionBuild = versionProperties.getProperty(VERSION_BUILD_KEY);
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

	private static void loadModel() {
		String s = properties.getProperty(PROPERTY_MODEL_KEY);

		if (null == s)
			doExit("No model defined.");

		try {
			scoreBoardModel = (ScoreBoardModel)Class.forName(s).newInstance();
			printMessage("Loaded ScoreBoardModel : "+s);
		} catch ( Exception e ) {
			doExit("Could not create model : " + e.getMessage(), e);
		}
	}

	private static void loadControllers() {
		Iterator<Object> i = properties.keySet().iterator();
		int count = 0;

		while (i.hasNext()) {
			String key = i.next().toString();
			if (!key.startsWith(PROPERTY_CONTROLLER_KEY))
				continue;

			String value = properties.getProperty(key);
			try {
				registerScoreBoardController((ScoreBoardController)Class.forName(value).newInstance());
				printMessage("Started ScoreBoardController : "+value);
				count++;
			} catch ( Exception e ) {
				printMessage("Could not create controller " + value + " : " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (0 == count)
			doExit("No controllers created.");
	}

	private static void loadViewers() {
		Iterator<Object> i = properties.keySet().iterator();

		while (i.hasNext()) {
			String key = i.next().toString();
			if (!key.startsWith(PROPERTY_VIEWER_KEY))
				continue;

			String value = properties.getProperty(key);
			try {
				registerScoreBoardViewer((ScoreBoardViewer)Class.forName(value).newInstance());
				printMessage("Started ScoreBoardViewer : "+value);
			} catch ( Exception e ) {
				printMessage("Could not create controller " + value + " : " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public static void gameSnapshot() {
		synchronized (gameLock) {
			if (game != null)
				game.snapshot(false);
		}
	}
	public static void gameSnapshot(boolean jamEnd) {
		synchronized (gameLock) {
			if (game != null)
				game.snapshot(jamEnd);
		}
	}
	public static Game gameStart(String name) {
		synchronized (gameLock) {
			if (game != null)
				game.stop();
			game = new Game(scoreBoardModel);
			game.start(name);
			return game;
		}
	}
	public static Game getGame() {
		return game;
	}

	/* FIXME - replace with java 1.7 Objects.equals once we move to 1.7 */
	public static boolean ObjectsEquals(Object a, Object b) {
		if ((null == a) != (null == b))
			return false;
		if ((null != a) && !a.equals(b))
			return false;
		return true;
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
	private static Map<String,ScoreBoardController> controllers = new ConcurrentHashMap<String,ScoreBoardController>();
	private static Map<String,ScoreBoardViewer> viewers = new ConcurrentHashMap<String,ScoreBoardViewer>();

	private static ScoreBoardModel scoreBoardModel;
	private static Logger logger = null;

	private static String versionRelease;
	private static String versionBuild;

	private static File defaultPath = new File(".");
	private static Game game = null;
	private static Object gameLock = new Object();

	public static final String VERSION_PATH = "com/carolinarollergirls/scoreboard/version";
	public static final String VERSION_RELEASE_PROPERTIES_NAME = VERSION_PATH+"/release.properties";
	public static final String VERSION_BUILD_PROPERTIES_NAME = VERSION_PATH+"/build.properties";
	public static final String VERSION_RELEASE_KEY = "release";
	public static final String VERSION_BUILD_KEY = "build";

	public static final String PROPERTIES_DIR_NAME = "config";
	public static final String PROPERTIES_FILE_NAME = "crg.scoreboard.properties";

	public static final String PROPERTY_MODEL_KEY = ScoreBoardManager.class.getName()+".model";
	public static final String PROPERTY_CONTROLLER_KEY = ScoreBoardManager.class.getName()+".controller";
	public static final String PROPERTY_VIEWER_KEY = ScoreBoardManager.class.getName()+".viewer";
}
