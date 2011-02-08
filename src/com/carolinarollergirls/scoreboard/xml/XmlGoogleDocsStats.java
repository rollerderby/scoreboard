package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.gdata.*;

import com.google.gdata.util.*;
import com.google.gdata.data.*;
import com.google.gdata.client.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.client.docs.*;

public class XmlGoogleDocsStats extends XmlStats
{
	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		super.setXmlScoreBoard(xsB);

		reset();
	}

	public void reset() {
		Element s = createXPathElement();
		editor.addElement(s, "Running", null, "false");
		editor.addElement(s, "Authenticated", null, "false");
		editor.addElement(s, "Username", null, "");
		editor.addElement(s, "Password", null, "");
		update(s);
	}

	protected void initializeSpreadsheetEntry() throws IOException,ServiceException,MalformedURLException {
		spreadsheetEntry = googleDocs.createSpreadsheet();
		Element s = createXPathElement();
		Element e = editor.getElement(s, "Spreadsheet");
		editor.setElement(e, "Title", null, spreadsheetEntry.getTitle().getPlainText());
		editor.setElement(e, "Id", null, spreadsheetEntry.getId());
		//editor.setElement(e, "Link", null, spreadsheetEntry.getSpreadsheetLink().getHref());
		editor.setElement(e, "Link", null, spreadsheetEntry.getHtmlLink().getHref());
		editor.setElement(s, "Running", null, "true");
		update(s);
//FIXME - implement parsing out worksheets, rows, etc. to write stats into.
	}

	public void scoreBoardChange(ScoreBoardEvent event) {
//FIXME - implement.
	}

	protected void processElement(Element e) {
		setUsername(e.getChild("Username"));
		login(e.getChild("Password"));
	}

	protected void login(Element password) {
		if (password == null) return;

		Element s = createXPathElement();
		Element authError = editor.getElement(s, "AuthenticationError");
		try {
			googleDocs.login(getUsername(), password.getText());
			authError.setText("");
			setAuthenticated(true);
		} catch ( AuthenticationException aE ) {
			authError.setText(aE.getMessage());
			setAuthenticated(false);
			update(s);
			return;
		}
		try {
			initializeSpreadsheetEntry();
		} catch ( Exception e ) {
			editor.setElement(s, "CommunicationError", null, e.getMessage());
		}
		update(s);
	}

	protected void setUsername(Element e) {
		if (e == null) return;

		if (isAuthenticated())
			{ /* handle username update after already authenticated... */ }
		else
			update(editor.setElement(createXPathElement(), "Username", null, e.getText()));
	}

	protected String getUsername() {
		try { return getXPathElement().getChild("Username").getText(); }
		catch ( Exception e ) { return ""; }
	}

	protected void setAuthenticated(boolean auth) {
		update(editor.setElement(createXPathElement(), "Authenticated", null, String.valueOf(auth)));
	}

	protected boolean isAuthenticated() {
		try { return Boolean.parseBoolean(getXPathElement().getChild("Authenticated").getText()); }
		catch ( Exception e ) { return false; }
	}

	protected Element createXPathElement() {
		Element e = new Element("GoogleDocs");
		super.createXPathElement().addContent(e);
		return e;
	}

	protected String getXPathString() { return super.getXPathString() + "/GoogleDocs"; }

	protected GoogleDocs googleDocs = new GoogleDocs();
	protected SpreadsheetEntry spreadsheetEntry;
}
