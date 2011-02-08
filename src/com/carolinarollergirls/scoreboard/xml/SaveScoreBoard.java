package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.file.*;

public class SaveScoreBoard extends AbstractXmlDocumentManager
{
	public void reset() {
		super.reset();
		Element e = createXPathElement();
		e.addContent(new Element("Filename"));
		e.addContent(new Element("Save"));
		e.addContent(new Element("Error").setText("false"));
		e.addContent(new Element("Message"));
		update(e);
	}

	protected void processChildElement(Element e) {
		if (e.getName() == "Filename")
			update(editor.cloneDocumentToElement(e, true));
		else if (e.getName() == "Save")
			save();
	}

	protected void save() {
		Element msg = new Element("Message");
		Element error = new Element("Error").setText("false");
		Element updateE = createXPathElement().addContent(msg).addContent(error);
		String filename = "";
		try {
			filename = getXPathElement().getChild("Filename").getText();
			ScoreBoardToXmlFile toFile = new ScoreBoardToXmlFile(DIRECTORY_NAME, filename);
			toFile.save(getXmlScoreBoard());
			msg.setText("Saved ScoreBoard to file '"+toFile.getFile().getName()+"'");
		} catch ( Exception e ) {
			msg.setText("Could not save to file '"+filename+"' : "+e.getMessage());
			error.setText("true");
		} finally {
			update(updateE);
		}
	}

	protected Element createXPathElement() {
		Element e = new Element("Save");
		createDocument().getRootElement().addContent(new Element("SaveLoad").addContent(e));
		return e;
	}

	protected String getXPathString() { return "/*/SaveLoad/Save"; }

	public static final String DIRECTORY_NAME = "html/save";

}
