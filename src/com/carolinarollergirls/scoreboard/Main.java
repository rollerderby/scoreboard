package com.carolinarollergirls.scoreboard;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.json.AutoSaveJSONState;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONListener;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.carolinarollergirls.scoreboard.utils.Version;
import com.carolinarollergirls.scoreboard.viewer.ScoreBoardMetricsCollector;

public class Main extends Logger {
    public static void main(String argv[]) { new Main(argv); }

    public Main(String argv[]) {
        parseArgv(argv);
        setLogger(this);
        start();
        if (guiFrameText != null) {
            guiFrameText.setText("ScoreBoard status: running (close this window to exit scoreboard)");
        }
    }

    public void start() {
        setSystemProperties();
        try {
            if (!Version.load()) { stop(null); }
        } catch (IOException e) { stop(e); }

        scoreBoard = new ScoreBoardImpl();

        // JSON updates.
        final JSONStateManager jsm = scoreBoard.getJsm();
        new ScoreBoardJSONListener(scoreBoard, jsm);

        // Controllers.
        JettyServletScoreBoardController jetty = new JettyServletScoreBoardController(scoreBoard, jsm, host, port);

        // Viewers.
        new ScoreBoardMetricsCollector(scoreBoard).register();

        final File autoSaveDir = new File(BasePath.get(), "config/autosave");
        scoreBoard.runInBatch(new Runnable() {
            @Override
            public void run() {
                if (!AutoSaveJSONState.loadAutoSave(scoreBoard, autoSaveDir)) {
                    Logger.printMessage("No autosave to load from, using builtin defaults only");
                }
                scoreBoard.postAutosaveUpdate();
            }
        });

        // Only start auto-saves once everything is loaded in.
        final AutoSaveJSONState autosaver = new AutoSaveJSONState(jsm, autoSaveDir);
        jetty.start();

        String blankStatsbookPath = scoreBoard.getSettings().get(ScoreBoard.SETTING_STATSBOOK_INPUT);
        if (!"".equals(blankStatsbookPath)) {
            try {
                Workbook wb = WorkbookFactory.create(new FileInputStream(Paths.get(blankStatsbookPath).toFile()));
                wb.getCreationHelper().createFormulaEvaluator();
                wb.write(new FileOutputStream(Paths.get("config/tmp.xlsx").toFile()));
                wb.close();
                Paths.get("config/tmp.xlsx").toFile().delete();
            } catch (IOException e) { e.printStackTrace(); }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Save any changes since last regular autosave before we shutdown.
                autosaver.run();
            }
        });
    }

    private void setSystemProperties() {
        System.getProperties().setProperty("twitter4j.loggerFactory", "twitter4j.internal.logging.NullLoggerFactory");
    }

    private void stop(Exception ex) {
        if (ex != null) { ex.printStackTrace(); }
        Logger.printMessage("Fatal error.   Exiting in 15 seconds.");
        try {
            Thread.sleep(15000);
        } catch (Exception e) { /* Probably Ctrl-C or similar, ignore. */
        }
        System.exit(1);
    }

    @Override
    public void log(String msg) {
        if (guiMessages != null) {
            guiMessages.append(msg + "\n");
        } else {
            System.err.println(msg);
        }
    }

    private void parseArgv(String[] argv) {
        boolean gui = false;

        for (String arg : argv) {
            if (arg.equals("--gui") || arg.equals("-g")) {
                gui = true;
            } else if (arg.equals("--nogui") || arg.equals("-G")) {
                gui = false;
            } else if (arg.startsWith("--port=") || arg.startsWith("-p=")) {
                port = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--host") || arg.startsWith("-h=")) {
                host = arg.split("=")[1];
            }
        }

        if (gui) { createGui(); }
    }

    private void createGui() {
        if (guiFrame != null) { return; }

        guiFrame = new JFrame("CRG ScoreBoard");
        guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiMessages = new JTextArea();
        guiMessages.setEditable(false);
        guiMessages.setFont(new Font("monospaced", Font.PLAIN, 12));
        guiFrameText = new JLabel("ScoreBoard status: starting...");
        guiFrame.getContentPane().setLayout(new BoxLayout(guiFrame.getContentPane(), BoxLayout.Y_AXIS));
        guiFrame.getContentPane().add(guiFrameText);
        guiFrame.getContentPane().add(new JScrollPane(guiMessages));
        guiFrame.setSize(800, 600);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int w = guiFrame.getSize().width;
        int h = guiFrame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;
        guiFrame.setLocation(x, y);
        guiFrame.setVisible(true);
    }

    private JFrame guiFrame = null;
    private JTextArea guiMessages = null;
    private JLabel guiFrameText = null;

    private String host = null;
    private int port = 8000;

    private static ScoreBoard scoreBoard;
}
