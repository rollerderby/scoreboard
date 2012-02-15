package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;

public class AutoSaveScoreBoard implements Runnable
{
  public AutoSaveScoreBoard(XmlScoreBoard xsB) {
    xmlScoreBoard = xsB;
  }

  public synchronized void start() {
    File dir = new File(DIRECTORY_NAME);
    if (!dir.exists() && !dir.mkdirs()) {
      ScoreBoardManager.printMessage("WARNING: Unable to create auto-save scoreboard directory");
      return;
    }
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
      getFile(n).delete();
      while (n > 0) {
        File to = getFile(n);
        File from = getFile(--n);
        from.renameTo(to);
      }
      FileOutputStream fos = new FileOutputStream(getFile(0));
      xmlOutputter.output(editor.filterNoSavePI(xmlScoreBoard.getDocument()), fos);
      fos.close();
    } catch ( Exception e ) {
      ScoreBoardManager.printMessage("WARNING: Unable to auto-save scoreboard : "+e.getMessage());
    }
  }

  public static File getFile(int n) { return getFile(DIRECTORY_NAME, n); }
  public static File getFile(String dir, int n) { return getFile(new File(dir), n); }
  public static File getFile(File dir, int n) {
    if (n == 0)
      return new File(dir, (FILE_NAME + "-0-now.xml"));
    else
      return new File(dir, (FILE_NAME + "-" + (n * SAVE_DELAY) + "-secs-ago.xml"));
  }

  protected void backupAutoSavedFiles() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    File mainBackupDir = new File(DIRECTORY_NAME, "backup");
    File backupDir = new File(mainBackupDir, dateFormat.format(new Date()));
    if (backupDir.exists()) {
      ScoreBoardManager.printMessage("Could not back up auto-save files, backup directory already exists");
    } else {
      int n = 0;
      do {
        File to = getFile(backupDir, n);
        File from = getFile(n);
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
  protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  protected ScheduledFuture running = null;
  protected XmlDocumentEditor editor = new XmlDocumentEditor();
  protected XMLOutputter xmlOutputter = XmlDocumentEditor.getPrettyXmlOutputter();

  public static final String DIRECTORY_NAME = "config/autosave";
  public static final String FILE_NAME = "scoreboard";
  public static final int AUTOSAVE_FILES = 6;
  public static final long SAVE_DELAY = 10; /* 10 secs */
}
