package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.file.*;

public class LoadScoreBoard extends SegmentedXmlDocumentManager
{
	public LoadScoreBoard() { super("SaveLoad", "Load"); }

	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		super.setXmlScoreBoard(xsB);

		Element e = createXPathElement();
		editor.addElement(e, "LoadFile");
		editor.addElement(e, "MergeFile");
		update(e);
	}

	public void reset() {
		/* Don't reset anything, as this controls loading. */
	}

	protected void processChildElement(Element e) throws Exception {
		super.processChildElement(e);
		if (e.getName().equals("LoadFile")) {
			loadFromFile.setFile(e.getText()); 
			loadFromFile.load(xmlScoreBoard);
		} else if (e.getName().equals("MergeFile")) {
			loadFromFile.setFile(e.getText());
			loadFromFile.merge(xmlScoreBoard);
		}
	}

	protected ScoreBoardFromXmlFile loadFromFile = new ScoreBoardFromXmlFile(DIRECTORY_NAME);

	public static final String DIRECTORY_NAME = "html/save";
}
