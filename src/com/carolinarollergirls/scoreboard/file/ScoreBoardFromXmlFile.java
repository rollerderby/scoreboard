package com.carolinarollergirls.scoreboard.file;

import org.jdom.*;
import org.jdom.input.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class ScoreBoardFromXmlFile extends ScoreBoardFromFile
{
	public ScoreBoardFromXmlFile() { super(); }
	public ScoreBoardFromXmlFile(String d) { super(d); }
	public ScoreBoardFromXmlFile(String d, String f) { super(d, f); }

	public void load(ScoreBoardModel sbM) throws Exception { load(sbM.getXmlScoreBoard()); }
	public void load(XmlScoreBoard xsB) throws Exception {
		synchronized (saxBuilder) {
			xsB.loadDocument(saxBuilder.build(getFile()));
		}
	}

	public void merge(ScoreBoardModel sbM) throws Exception { merge(sbM.getXmlScoreBoard()); }
	public void merge(XmlScoreBoard xsB) throws Exception {
		synchronized (saxBuilder) {
			xsB.mergeDocument(saxBuilder.build(getFile()));
		}
	}

	public static void load(ScoreBoardModel sbM, String f) throws Exception { load(sbM.getXmlScoreBoard(), f); }
	public static void load(XmlScoreBoard xsB, String f) throws Exception {
		xsB.loadDocument(new SAXBuilder().build(f));
	}

	public static void merge(ScoreBoardModel sbM, String f) throws Exception { merge(sbM.getXmlScoreBoard(), f); }
	public static void merge(XmlScoreBoard xsB, String f) throws Exception {
		xsB.mergeDocument(new SAXBuilder().build(f));
	}

	protected SAXBuilder saxBuilder = new SAXBuilder();
}
