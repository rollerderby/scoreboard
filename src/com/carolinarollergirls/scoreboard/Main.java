package com.carolinarollergirls.scoreboard;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

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
        importFromOldVersion();
        start();
        if (guiFrameText != null) {
            guiFrameText.setText("ScoreBoard status: running (close this window to exit scoreboard)");
        }
    }

    public void start() {
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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Save any changes since last regular autosave before we shutdown.
                autosaver.run();
            }
        });
    }

    private void stop(Exception ex) {
        if (ex != null) { Logger.printStackTrace(ex); }
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
                port = Integer.parseInt(arg.split("=", 2)[1]);
            } else if (arg.startsWith("--host=") || arg.startsWith("-h=")) {
                host = arg.split("=", 2)[1];
            } else if (arg.startsWith("--import=") || arg.startsWith("-i=")) {
                importPath = arg.split("=", 2)[1];
            }
        }

        if (gui) { createGui(); }
    }

    private static void copyDir(Path src, Path dst, Path subdirectory, CopyOption... options) throws IOException {

        Files.walkFileTree(src.resolve(subdirectory), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(dst.resolve(src.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.copy(file, dst.resolve(src.relativize(file)), options);
                    return FileVisitResult.CONTINUE;
                } catch (FileAlreadyExistsException e) { return FileVisitResult.CONTINUE; }
            }
        });
    }

    private static void copyFiles(Path src, Path dst, Path subdirectory, String suffix, CopyOption... options)
        throws IOException {
        Files.createDirectories(dst.resolve(subdirectory));
        List<Path> paths = Files.list(src.resolve(subdirectory))
                               .map(Path::normalize)
                               .filter(path -> path.getFileName().toString().endsWith(suffix))
                               .collect(Collectors.toList());
        for (Path path : paths) { Files.copy(path, dst.resolve(src.relativize(path)), options); }
    }

    private void importFromOldVersion() {
        Path sourcePath = null;
        if (importPath == null) {
            // no import path given on command line
            if (Files.exists(Paths.get("config", "autosave"))) {
                Logger.printMessage("Found existing autosave dir - skipping import");
                return;
            } // if not first start don't import

            long newestAutosave = 0;
            try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(Paths.get(".").toAbsolutePath().normalize().getParent())) {
                for (Path dir : stream) {
                    if (Files.isDirectory(dir)) {
                        Path autosave = dir.resolve(Paths.get("config", "autosave", "scoreboard-0-secs-ago.json"));
                        if (Files.exists(autosave) && Files.getLastModifiedTime(autosave).toMillis() > newestAutosave) {
                            newestAutosave = Files.getLastModifiedTime(autosave).toMillis();
                            sourcePath = dir;
                        }
                    }
                }
            } catch (IOException e) {
                Logger.printMessage("Error looking for instance to import from:");
                Logger.printStackTrace(e);
                Logger.printMessage("Skipping import");
                return;
            }
        } else if (importPath.equals("")) {
            Logger.printMessage("Skipping import as per user request");
            return; // user explicitly requested no import
        } else {
            sourcePath = Paths.get(importPath);
        }

        if (sourcePath == null) {
            Logger.printMessage("No valid import path found - skipping import");
            return;
        }

        Logger.printMessage("importing data from " + sourcePath.toString());
        Path targetPath = Paths.get(".");
        try {
            copyFiles(sourcePath, targetPath, Paths.get("config", "autosave"), ".json",
                      StandardCopyOption.REPLACE_EXISTING);
            copyFiles(sourcePath, targetPath, Paths.get(""), ".xlsx");
            copyDir(sourcePath, targetPath, Paths.get("config", "penalties"));
            copyDir(sourcePath, targetPath, Paths.get("html", "game-data"));
            copyDir(sourcePath, targetPath, Paths.get("html", "custom"));
            copyDir(sourcePath, targetPath, Paths.get("html", "images"));
            copyDir(sourcePath, targetPath, Paths.get("html", "videos"));
        } catch (IOException e) {
            Logger.printMessage("Exception during importing: ");
            Logger.printStackTrace(e);
        }
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

    private String importPath = null;

    private static ScoreBoard scoreBoard;
}
