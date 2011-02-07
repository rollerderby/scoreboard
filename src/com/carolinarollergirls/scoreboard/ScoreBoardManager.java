package com.carolinarollergirls.scoreboard;

import java.util.*;
import java.io.*;

import com.carolinarollergirls.scoreboard.model.*;

public class ScoreBoardManager
{
	public static void main(String argv[]) {
		loadProperties();

		loadModel();

		loadControllers();

		loadViewers();
	}

	public static Properties getProperties() { return new Properties(properties); }

	public static void registerScoreBoardController(ScoreBoardController sbC) {
		sbC.setScoreBoardModel(scoreBoardModel);
	}

	public static void registerScoreBoardViewer(ScoreBoardViewer sbV) {
		sbV.setScoreBoard(scoreBoardModel.getScoreBoard());
	}

	private static void loadProperties() {
		InputStream is = ScoreBoardManager.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);

		if (null == is) {
			System.err.println("Could not find properties file " + PROPERTIES_FILE_NAME);
			System.exit(1);
		}

		try {
			properties.load(is);
		} catch ( Exception e ) {
			System.err.println("Could not load " + PROPERTIES_FILE_NAME + " file : " + e.getMessage());
			System.exit(1);
		}

		try { is.close(); }
		catch ( IOException ioE ) { }
	}

	private static void loadModel() {
		String s = properties.getProperty(PROPERTY_MODEL_KEY);

		if (null == s) {
			System.err.println("No model defined.");
			System.exit(1);
		}

		try {
			scoreBoardModel = (ScoreBoardModel)Class.forName(s).newInstance();
			System.out.println("Loaded ScoreBoardModel : "+s);
		} catch ( Exception e ) {
			System.err.println("Could not create model : " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void loadControllers() {
		Iterator i = properties.keySet().iterator();
		int count = 0;

		while (i.hasNext()) {
			String key = i.next().toString();
			if (!key.startsWith(PROPERTY_CONTROLLER_KEY))
				continue;

			String value = properties.getProperty(key);
			try {
				registerScoreBoardController((ScoreBoardController)Class.forName(value).newInstance());
				System.out.println("Started ScoreBoardController : "+value);
				count++;
			} catch ( Exception e ) {
				System.err.println("Could not create controller " + value + " : " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (0 == count) {
			System.err.println("No controllers created.");
			System.exit(1);
		}
	}

	private static void loadViewers() {
		Iterator i = properties.keySet().iterator();

		while (i.hasNext()) {
			String key = i.next().toString();
			if (!key.startsWith(PROPERTY_VIEWER_KEY))
				continue;

			String value = properties.getProperty(key);
			try {
				registerScoreBoardViewer((ScoreBoardViewer)Class.forName(value).newInstance());
				System.out.println("Started ScoreBoardViewer : "+value);
			} catch ( Exception e ) {
				System.err.println("Could not create controller " + value + " : " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static Properties properties = new Properties();

	private static ScoreBoardModel scoreBoardModel;

	public static final String PROPERTIES_FILE_NAME = "crg.scoreboard.properties";

	public static final String PROPERTY_MODEL_KEY = ScoreBoardManager.class.getName()+".model";
	public static final String PROPERTY_CONTROLLER_KEY = ScoreBoardManager.class.getName()+".controller";
	public static final String PROPERTY_VIEWER_KEY = ScoreBoardManager.class.getName()+".viewer";
}
