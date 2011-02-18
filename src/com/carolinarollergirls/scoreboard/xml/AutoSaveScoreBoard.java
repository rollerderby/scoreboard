package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.file.*;

public class AutoSaveScoreBoard extends SegmentedXmlDocumentManager
{
	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		super.setXmlScoreBoard(xsB);
		savers.add(new SaveToXmlFile(xmlScoreBoard, 1, 10));
		savers.add(new SaveToXmlFile(xmlScoreBoard, 10, 6));
		savers.add(new SaveToXmlFile(xmlScoreBoard, 60, 10));

		reset();
	}

	public void reset() {
		super.reset();
		Element e = createXPathElement();
		e.addContent(new Element("Enabled").setText("false"));
		update(e);
	}

	protected void start() throws Exception {
		synchronized (runLock) {
			if ("true".equals(getXPathElement().getChild("Enabled").getText()))
				return;
			executor = new ScheduledThreadPoolExecutor(10);
			Iterator<SaveToXmlFile> s = savers.iterator();
			while (s.hasNext()) {
				SaveToXmlFile r = s.next();
				executor.scheduleWithFixedDelay(r, 0, r.secondsInterval, TimeUnit.SECONDS);
			}
			update(createXPathElement().addContent(new Element("Enabled").setText("true")));
		}
	}

	protected void stop() throws Exception {
		synchronized (runLock) {
			if ("false".equals(getXPathElement().getChild("Enabled").getText()))
				return;
			executor.shutdownNow();
			update(createXPathElement().addContent(new Element("Enabled").setText("false")));
		}
	}

	protected void processChildElement(Element e) throws Exception {
		super.processChildElement(e);
		try {
			if (e.getName().equals("Enabled")) {
				if (Boolean.parseBoolean(e.getText()))
					AutoSaveScoreBoard.this.start();
				else if ("false".equalsIgnoreCase(e.getText()))
					AutoSaveScoreBoard.this.stop();
			}
		} catch ( Exception ex ) {
			// FIXME - maybe add subelement to indicate error msgs
		}
	}

	protected String getManagedElementName() { return "SaveLoad"; }
	protected String getManagedSubElementName() { return "AutoSave"; }

	protected ScheduledThreadPoolExecutor executor = null;

	protected Object runLock = new Object();

	protected List<SaveToXmlFile> savers = new LinkedList<SaveToXmlFile>();

	public static final String DIRECTORY_NAME = "html/save/autosave";

	protected class SaveToXmlFile implements Runnable
	{
		public SaveToXmlFile(XmlScoreBoard xsB, long s, int k) {
			xmlScoreBoard = xsB;
			secondsInterval = s;
			keepFiles = k;
			setUnits();
		}
		public void run() {
			try {
				int n = keepFiles;
				new File(toXmlFile.getDirectory(), getName(n)).delete();
				while (n > 0) {
					File to = new File(toXmlFile.getDirectory(), getName(n));
					File from = new File(toXmlFile.getDirectory(), getName(--n));
					from.renameTo(to);
				}
				toXmlFile.setFile(getName(0));
				toXmlFile.save(xmlScoreBoard);
			} catch ( Exception e ) {
				//FIXME - use something that can handle exceptions, or set something inthe xml?  and/or stop?
			}
		}
		public String getName(int n) {
			String when = (n*value)+units+"Ago";
			if (n == 0)
				when = "Now";
			return "every"+value+units+"-"+when+".xml";
		}
		protected void setUnits() {
			units = "seconds";
			value = secondsInterval;
			if (secondsInterval == 1) {
				units = "second";
			} else if (secondsInterval == 60) {
				units = "minute";
				value = 1;
			} else if ((secondsInterval % 60) == 0) {
				units = "minutes";
				value = (secondsInterval / 60);
			}
		}
		public ScoreBoardToXmlFile toXmlFile = new ScoreBoardToXmlFile(AutoSaveScoreBoard.DIRECTORY_NAME);
		public XmlScoreBoard xmlScoreBoard;
		public String units;
		public long value;
		public long secondsInterval;
		public int keepFiles;
	}
}
