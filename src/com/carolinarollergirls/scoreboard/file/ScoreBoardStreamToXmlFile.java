package com.carolinarollergirls.scoreboard.file;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.nio.charset.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.xml.*;

import org.jdom.*;
import org.jdom.output.*;

public class ScoreBoardStreamToXmlFile /*extends ScoreBoardToFile implements ScoreBoardStreamToFile,XmlScoreBoardListener*/
{
	public ScoreBoardStreamToXmlFile() { }
/*
	public void setScoreBoard(ScoreBoard sb) {
		scoreBoard = sb;
//FIXME - use XML structure to choose directory and filename, start and stop recording
		try {
			setDirectory("html/save");
			setFile(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()), "xml");
			start();
		} catch ( Exception e ) {
			System.err.println("Could not start saving XML ScoreBoard to file : "+e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean start() throws Exception {
		synchronized (this) {
			if (!running) {
				printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getFile()), Charset.forName("UTF-8"))));
				// I would rather use programmatic means to do this,
				// but I can't find anything that allows manually creating a toplevel element
				// and then streaming in subelements (and then eventually closing the element/doc)
				printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
				printWriter.println("<ScoreBoardStream version=\"0.1.4\">");
				running = true;
				scoreBoard.getXmlScoreBoard().addXmlScoreBoardListener(this);
			}
		}

		return running;
	}

	public boolean stop() throws Exception {
		synchronized (this) {
			if (running) {
				running = false;
				scoreBoard.getXmlScoreBoard().removeXmlScoreBoardListener(this);
				synchronized (outputter) {
					printWriter.println("</ScoreBoardStream>");
					printWriter.flush();
					printWriter.close();
				}
			}
		}

		return !running;
	}

	public boolean isRunning() { return running; }

	public void xmlChange(Document d) {
		synchronized (outputter) {
			if (running) {
				try {
					outputter.output(d.getRootElement(), printWriter);
					printWriter.println();
				} catch ( IOException ioE ) {
					System.err.println("Could not output ScoreBoard element to XML stream : " + ioE.getMessage());
					ioE.printStackTrace();
				}
			}
		}
	}

	protected ScoreBoard scoreBoard = null;
	protected PrintWriter printWriter = null;
	protected XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
*/
}
