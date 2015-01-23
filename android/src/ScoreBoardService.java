package com.carolinarollergirls.scoreboard.android;

import android.app.*;
import android.content.*;
import android.os.*;

import java.io.*;
import java.util.zip.*;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class ScoreBoardService extends Service implements ScoreBoardManager.Logger {
	public void log(String msg) {
		sendLog(msg);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		ScoreBoardManager.setLogger(this);
		ScoreBoardManager.setPropertyOverride("com.carolinarollergirls.scoreboard.defaults.DefaultClockModel.interval", "250");
		initBaseFiles("BaseFiles.zip");
	}

	private void initBaseFiles(String BaseFileName) {
		File defaultPath = Environment.getExternalStoragePublicDirectory("CRG");
		ScoreBoardManager.setDefaultPath(defaultPath);
		ScoreBoardManager.printMessage("Default Path: " + defaultPath);
		defaultPath.mkdirs();

		byte[] buf = new byte[1024];

		InputStream basefiles = null;
		ZipInputStream zis = null;
		try {
			basefiles = getAssets().open(BaseFileName);
			zis = new ZipInputStream(basefiles);
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File of = new File(defaultPath, entry.getName());
				if (entry.isDirectory()) {
					of.mkdirs();
				} else {
					of.getParentFile().mkdirs();
					FileOutputStream o = null;
					try {
						o = new FileOutputStream(of);
						int read = 0;
						while ((read = zis.read(buf, 0, buf.length)) > 0) {
							o.write(buf, 0, read);
						}
					} catch (Exception ex) {
						ScoreBoardManager.printMessage("Unable to copy file " + entry.getName() + ": " + ex.toString());
						ex.printStackTrace();
					} finally {
						if (o != null) {
							try { o.close(); } catch (Exception ex) {}
						}
					}
				}
			}
		} catch (Exception ex) {
			ScoreBoardManager.printMessage("Unable to copy base files " + BaseFileName + ": " + ex.toString());
		} finally {
			if (zis != null) {
				try { zis.close(); } catch (Exception ex) {}
			}
			if (basefiles != null) {
				try { basefiles.close(); } catch (Exception ex) {}
			}
		}
	}

	class ScoreBoardThread extends Thread {
		public void run() {
			ScoreBoardManager.start();
		}
	}

	// RECV MESSAGE
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();        	
			int command = data.getInt("command");
			android.util.Log.i("CRG ScoreBoard (service)", "command: " + command);
			switch (command) {
				case CMD_START:
					android.util.Log.i("CRG ScoreBoard (service)", "Starting CRG Server");
					ScoreBoardThread sbt = new ScoreBoardThread();
					sbt.start();
					break;
				case CMD_STOP:
					android.util.Log.i("CRG ScoreBoard (service)", "Stopping CRG Server");
					ScoreBoardManager.stop();
					break;
			}
		}
	}

	final Messenger myMessenger = new Messenger(new IncomingHandler());
	@Override
	public IBinder onBind(Intent intent) {
		return myMessenger.getBinder();
	}

	// SEND MESSAGE
	public void sendMessage(int command) {
		Intent intent = new Intent();
		intent.setAction(ActivityPath);
		intent.putExtra("command", command);
		sendBroadcast(intent); 
	}

	public void sendLog(String logmsg) {
		android.util.Log.i("CRG ScoreBoard (service)", "sendLog: logmsg: " + logmsg);
		Intent intent = new Intent();
		intent.setAction(ActivityPath);
		intent.putExtra("command", CMD_LOG);
		intent.putExtra("msg", logmsg);
		sendBroadcast(intent); 
	}

	public final static String ServicePath = "com.carolinarollergirls.scoreboard.android.service";
	public final static String ActivityPath = "com.carolinarollergirls.scoreboard.android.activity";
	public final static int CMD_LOG = 1;
	public final static int CMD_START = 2;
	public final static int CMD_STOP = 3;
}
