package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.file.*;

public class AutoSaveScoreBoard implements Runnable
{
  public AutoSaveScoreBoard(XmlScoreBoard xsB) {
    xmlScoreBoard = xsB;
  }

  public synchronized void start() {
    backupAutoSavedFiles();
    running = executor.scheduleWithFixedDelay(this, 0, SAVE_DELAY, TimeUnit.SECONDS);
  }

  public synchronized void stop() {
    try {
      running.cancel(false);
      running = null;
    } catch ( NullPointerException npE ) { }
  }

  public void run() {
    try {
      int n = AUTOSAVE_FILES;
      File dir = toXmlFile.getDirectory();
      new File(dir, getName(n)).delete();
      while (n > 0) {
        File to = new File(dir, getName(n));
        File from = new File(dir, getName(--n));
        from.renameTo(to);
      }
      toXmlFile.setFile(getName(0));
      toXmlFile.save(xmlScoreBoard);
    } catch ( Exception e ) {
      ScoreBoardManager.printMessage("WARNING: Unable to auto-save scoreboard : "+e.getMessage());
    }
  }

  public static String getName(int n) {
    if (n == 0)
      return (FILE_NAME + "-0-now.xml");
    else
      return (FILE_NAME + "-" + (n * SAVE_DELAY) + "-secs-ago.xml");
  }

  protected void backupAutoSavedFiles() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    File mainBackupDir = new File(toXmlFile.getDirectory(), "backup");
    File backupDir = new File(mainBackupDir, dateFormat.format(new Date()));
    if (backupDir.exists()) {
      ScoreBoardManager.printMessage("Could not back up auto-save files, backup directory already exists");
    } else {
      int n = 0;
      File dir = toXmlFile.getDirectory();
      do {
        File to = new File(backupDir, getName(n));
        File from = new File(dir, getName(n));
        if (from.exists()) {
          if (!backupDir.exists() && !backupDir.mkdirs()) {
            ScoreBoardManager.printMessage("Could not back up auto-save files, failure creating backup directory");
            break;
          }
          from.renameTo(to);
          ScoreBoardManager.printMessage("Moved auto-save file "+from.getName()+" to "+backupDir.getPath());
        }
      } while (n++ < AUTOSAVE_FILES);
    }
  }

  protected XmlScoreBoard xmlScoreBoard;
  protected ScoreBoardToXmlFile toXmlFile = new ScoreBoardToXmlFile(AutoSaveScoreBoard.DIRECTORY_NAME);
  protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  protected ScheduledFuture running = null;

  public static final String DIRECTORY_NAME = "config/autosave";
  public static final String FILE_NAME = "scoreboard";
  public static final int AUTOSAVE_FILES = 6;
  public static final long SAVE_DELAY = 10; /* 10 secs */
}
