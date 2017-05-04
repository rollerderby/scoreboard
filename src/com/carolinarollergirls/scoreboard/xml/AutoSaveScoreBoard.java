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

import org.apache.commons.io.*;

import com.carolinarollergirls.scoreboard.*;

public class AutoSaveScoreBoard implements Runnable
{
	public AutoSaveScoreBoard(XmlScoreBoard xsB) {
		xmlScoreBoard = xsB;
	}

	public synchronized void start() {
		try {
			FileUtils.forceMkdir(new File(ScoreBoardManager.getDefaultPath(), DIRECTORY_NAME));
		} catch ( IOException ioE ) {
			ScoreBoardManager.printMessage("WARNING: Unable to create auto-save directory '"+DIRECTORY_NAME+"' : "+ioE.getMessage());
			return;
		}
		Runnable r = new Runnable() {
				public void run() {
					backupAutoSavedFiles();
					ScoreBoardManager.printMessage("Starting auto-save");
					running = executor.scheduleWithFixedDelay(AutoSaveScoreBoard.this, 0, SAVE_DELAY, TimeUnit.SECONDS);
				}
			};
		running = executor.schedule(r, SAVE_DELAY, TimeUnit.SECONDS);
	}

	public synchronized void stop() {
		try {
			running.cancel(false);
			running = null;
		} catch ( NullPointerException npE ) { }
	}

	public void run() {
		FileOutputStream fos = null;
		try {
			int n = AUTOSAVE_FILES;
			getFile(n).delete();
			while (n > 0) {
				File to = getFile(n);
				File from = getFile(--n);
				if (from.exists())
					FileUtils.moveFile(from, to);
			}
			fos = FileUtils.openOutputStream(getFile(0));
			xmlOutputter.output(editor.filterNoSavePI(xmlScoreBoard.getDocument()), fos);
		} catch ( Exception e ) {
			ScoreBoardManager.printMessage("WARNING: Unable to auto-save scoreboard : "+e.getMessage());
		} finally {
			if (null != fos)
				try { fos.close(); } catch ( IOException ioE ) { }
		}
	}

	public static File getFile(int n) { return getFile(new File(ScoreBoardManager.getDefaultPath(), DIRECTORY_NAME), n); }
	public static File getFile(String dir, int n) { return getFile(new File(dir), n); }
	public static File getFile(File dir, int n) {
		if (n == 0)
			return new File(dir, (FILE_NAME + "-0-now.xml"));
		else
			return new File(dir, (FILE_NAME + "-" + (n * SAVE_DELAY) + "-secs-ago.xml"));
	}

	protected void backupAutoSavedFiles() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		File mainBackupDir = new File(new File(ScoreBoardManager.getDefaultPath(), DIRECTORY_NAME), "backup");
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
						ScoreBoardManager.printMessage("Copied auto-save file "+from.getName()+" to "+backupDir.getPath());
					} catch ( Exception e ) {
						ScoreBoardManager.printMessage("Could not back up auto-save file '"+from.getName()+"' : "+e.getMessage());
					}
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
