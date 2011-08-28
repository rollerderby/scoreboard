package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
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
      int n = BACKUP_FILES;
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
    return (FILE_NAME + n + ".xml");
  }

  protected XmlScoreBoard xmlScoreBoard;
  protected ScoreBoardToXmlFile toXmlFile = new ScoreBoardToXmlFile(AutoSaveScoreBoard.DIRECTORY_NAME);
  protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  protected ScheduledFuture running = null;

  public static final String DIRECTORY_NAME = "config/autosave";
  public static final String FILE_NAME = "scoreboard";
  public static final int BACKUP_FILES = 3;
  public static final long SAVE_DELAY = 1; /* 1 sec */
}
