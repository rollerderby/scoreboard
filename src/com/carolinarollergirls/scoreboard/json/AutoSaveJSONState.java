package com.carolinarollergirls.scoreboard.json;

import io.prometheus.client.Histogram;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.fasterxml.jackson.jr.ob.JSON;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;

public class AutoSaveJSONState implements Runnable {

    public AutoSaveJSONState(JSONStateManager jsm, File dir) {
        this.dir = dir;
        this.jsm = jsm;

        try {
            FileUtils.forceMkdir(dir);
        } catch ( IOException ioE ) {
            ScoreBoardManager.printMessage("WARNING: Unable to create auto-save directory '"+dir+"' : "+ioE.getMessage());
            return;
        }
        backupAutoSavedFiles();
        executor.scheduleAtFixedRate(AutoSaveJSONState.this, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        Histogram.Timer timer = autosaveDuration.startTimer();
        try {
            int n = AUTOSAVE_FILES;
            getFile(n).delete();
            while (n > 0) {
                File to = getFile(n);
                File from = getFile(--n);
                if (from.exists()) {
                    from.renameTo(to);
                }
            }
            writeAutoSave(getFile(0));
        } catch ( Exception e ) {
            ScoreBoardManager.printMessage("WARNING: Unable to auto-save scoreboard : "+e.getMessage());
        }
        timer.observeDuration();
    }

    private void writeAutoSave(File file) {
        File tmp = null;
        FileWriter out = null;
        try {
            String json = JSON.std
                          .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                          .composeString()
                          .startObject()
                          .putObject("state", new TreeMap<>(jsm.getState()))
                          .end()
                          .finish();
            tmp = File.createTempFile(file.getName(), ".tmp", dir);
            out = new FileWriter(tmp);
            out.write(json);
            out.close();
            tmp.renameTo(file); // This is atomic.
        } catch (Exception e) {
            ScoreBoardManager.printMessage("Error writing JSON autosave: " + e.getMessage());
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception e) { }
            }
            if (tmp != null ) {
                try { tmp.delete(); } catch (Exception e) { }
            }
        }
    }

    private void backupAutoSavedFiles() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File mainBackupDir = new File(dir, "backup");
        File backupDir = new File(mainBackupDir, dateFormat.format(new Date()));
        if (backupDir.exists()) {
            ScoreBoardManager.printMessage("Could not back up auto-save files, backup directory already exists");
        } else {
            int n = 0;
            do {
                File from = getFile(n);
                if (from.exists()) {
                    try {
                        FileUtils.copyFileToDirectory(from, backupDir, true);
                    } catch ( Exception e ) {
                        ScoreBoardManager.printMessage("Could not back up auto-save file '"+from.getName()+"' : "+e.getMessage());
                    }
                }
            } while (n++ < AUTOSAVE_FILES);
        }
    }

    public File getFile(int n) {
        return getFile(n, dir);
    }
    public static File getFile(int n, File dir) {
        return new File(dir, ("scoreboard-" + (n * INTERVAL_SECONDS) + "-secs-ago.json"));
    }

    public static boolean loadAutoSave(ScoreBoard scoreBoard, File dir) {
        for (int i=0; i <= AUTOSAVE_FILES; i++) {
            File f = getFile(i, dir);
            if (!f.exists()) {
                continue;
            }
            try {
              loadFile(scoreBoard, f);
                ScoreBoardManager.printMessage("Loaded auto-saved scoreboard from "+f.getPath());
                return true;
              } catch ( Exception e ) {
                ScoreBoardManager.printMessage("Could not load auto-saved scoreboard XML file "+f.getPath()+" : "+e.getMessage());
                e.printStackTrace();
              }
            }

        return false;
    }
 
    public static void loadFile(ScoreBoard scoreBoard, File f) throws Exception{
      JSONObject json = new JSONObject(FileUtils.readFileToString(f, "utf-8"));

      List<ScoreBoardJSONSetter.JSONSet> jsl = new ArrayList<>();
      JSONObject state = json.getJSONObject("state");
      for (String key: state.keySet()) {
        Object value = state.get(key);
        String v;
        if (value == JSONObject.NULL) {
          v = null;
        } else {
          v = value.toString();
        }
        jsl.add(new ScoreBoardJSONSetter.JSONSet(key, v, Flag.FROM_AUTOSAVE));
      }

      ScoreBoardJSONSetter.set(scoreBoard, jsl);
    }

    private File dir;
    private JSONStateManager jsm;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final int AUTOSAVE_FILES = 6;
    private static final int INTERVAL_SECONDS = 10;


    private static final Histogram autosaveDuration = Histogram.build()
            .name("crg_json_autosave_write_duration_seconds").help("Time spent writing JSON autosaves to disk").register();
}
