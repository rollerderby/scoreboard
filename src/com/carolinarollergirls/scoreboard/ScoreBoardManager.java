package com.carolinarollergirls.scoreboard;

import java.util.*;
import java.io.*;

import java.awt.*;
import javax.swing.*;

import com.carolinarollergirls.scoreboard.model.*;

public class ScoreBoardManager
{
	public static void main(String argv[]) {
		for (int i=0; i<argv.length; i++) {
			if ("--gui".equals(argv[i]) || "-g".equals(argv[i]))
				createGui();
		}

		loadVersion();

		loadProperties();

		loadModel();

		loadControllers();

		loadViewers();

		if (guiFrameText != null)
			guiFrameText.setText("ScoreBoard status: running (close this window to exit scoreboard)");
		printMessage("");
		printMessage("Now double-click/open the 'start.html' file");
		printMessage("or open a web browser (Google Chrome or Mozilla Firefox) to http://localhost:8000");
	}

	public static String getVersion() {
		if ("".equals(versionBuild))
			return versionRelease;
		else
			return versionRelease+"-"+versionBuild;
	}

	public static void printMessage(String msg) {
		if (guiMessages != null)
			guiMessages.append(msg+"\n");
		else
			System.err.println(msg);
	}

	public static Properties getProperties() { return new Properties(properties); }

	public static void registerScoreBoardController(ScoreBoardController sbC) {
		sbC.setScoreBoardModel(scoreBoardModel);
	}

	public static void registerScoreBoardViewer(ScoreBoardViewer sbV) {
		sbV.setScoreBoard(scoreBoardModel.getScoreBoard());
	}

	private static void createGui() {
		if (guiFrame != null)
			return;

		guiFrame = new JFrame("Carolina Rollergirls ScoreBoard");
		guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		guiMessages = new JTextArea();
		guiMessages.setEditable(false);
		guiFrameText = new JLabel("ScoreBoard status: starting...");
		guiFrame.getContentPane().setLayout(new BoxLayout(guiFrame.getContentPane(), BoxLayout.Y_AXIS));
		guiFrame.getContentPane().add(guiFrameText);
		guiFrame.getContentPane().add(new JScrollPane(guiMessages));
		guiFrame.setSize(800, 600);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = guiFrame.getSize().width;
		int h = guiFrame.getSize().height;
		int x = (dim.width-w)/2;
		int y = (dim.height-h)/2;
		guiFrame.setLocation(x, y);
		guiFrame.setVisible(true);
	}

	private static void doExit(String err) { doExit(err, null); }
	private static void doExit(String err, Exception ex) {
		printMessage(err);
		if (ex != null)
			ex.printStackTrace();
		printMessage("Fatal error.  Exiting in 15 seconds.");
		try { Thread.sleep(15000); } catch ( Exception e ) { /* Probably Ctrl-C or similar, ignore. */ }
		System.exit(1);
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
				is = new FileInputStream(new File(PROPERTIES_DIR_NAME, PROPERTIES_FILE_NAME));
			} catch ( FileNotFoundException fnfE ) {
				doExit("Could not find properties file " + PROPERTIES_FILE_NAME);
			}
		}

		try {
			properties.load(is);
		} catch ( Exception e ) {
			doExit("Could not load " + PROPERTIES_FILE_NAME + " file : " + e.getMessage(), e);
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
		Iterator i = properties.keySet().iterator();
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
		Iterator i = properties.keySet().iterator();

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

	private static Properties properties = new Properties();

	private static ScoreBoardModel scoreBoardModel;

	private static JFrame guiFrame = null;
	private static JTextArea guiMessages = null;
	private static JLabel guiFrameText = null;

	private static String versionRelease;
	private static String versionBuild;

	public static final String VERSION_PATH = "com/carolinarollergirls/scoreboard/version";
	public static final String VERSION_RELEASE_PROPERTIES_NAME = VERSION_PATH+"/release.properties";
	public static final String VERSION_BUILD_PROPERTIES_NAME = VERSION_PATH+"/build.properties";
	public static final String VERSION_RELEASE_KEY = "release";
	public static final String VERSION_BUILD_KEY = "build";

	public static final String PROPERTIES_DIR_NAME = "lib";
	public static final String PROPERTIES_FILE_NAME = "crg.scoreboard.properties";

	public static final String PROPERTY_MODEL_KEY = ScoreBoardManager.class.getName()+".model";
	public static final String PROPERTY_CONTROLLER_KEY = ScoreBoardManager.class.getName()+".controller";
	public static final String PROPERTY_VIEWER_KEY = ScoreBoardManager.class.getName()+".viewer";
}
