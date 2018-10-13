package com.carolinarollergirls.scoreboard.xml.stream;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.xml.XmlDocumentEditor;

public class ScoreBoardOutputStream {
    public ScoreBoardOutputStream(File f) throws FileNotFoundException {
        fileOutputStream = new FileOutputStream(f);
        xmlOutputter.getFormat().setEncoding(OUTPUT_ENCODING);
        xmlOutputter.getFormat().setOmitDeclaration(true);
    }

    public synchronized void start() throws IllegalStateException {
        if (null != printWriter) {
            throw new IllegalStateException("Already started");
        }
        if (finished) {
            throw new IllegalStateException("Finished; cannot restart");
        }

        printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fileOutputStream, Charset.forName(OUTPUT_ENCODING))));
        // Might be able to do this with a custom SAX writer/filter
        printWriter.print("<?xml version=\"1.0\" encoding=\""+OUTPUT_ENCODING+"\"?>");
        printWriter.print("<ScoreBoardStream version=\""+ScoreBoardManager.getVersion()+"\">");
    }

    public synchronized void stop() {
        if (null == printWriter) {
            return;
        }
        finished = true;

        printWriter.print("</ScoreBoardStream>");
        printWriter.flush();
        printWriter.close();
        printWriter = null;
    }

    public synchronized void write(Document d) throws IOException {
        xmlOutputter.output(d.getRootElement(), printWriter);
    }

    protected FileOutputStream fileOutputStream = null;
    protected PrintWriter printWriter = null;
    protected boolean finished = false;
    protected XMLOutputter xmlOutputter = XmlDocumentEditor.getRawXmlOutputter();

    public static final String OUTPUT_ENCODING = "UTF-8";

}
