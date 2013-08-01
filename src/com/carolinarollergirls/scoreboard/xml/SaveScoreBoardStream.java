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
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.stream.*;

public class SaveScoreBoardStream extends AbstractScoreBoardStream
{
  public SaveScoreBoardStream() { super("SaveStream"); }

  protected void doStart(File file) throws IOException,FileNotFoundException {
    if (!file.getName().matches("^.*[.][xX][mM][lL]$")) {
      String xmlFilename = file.getName()+".xml";
      Element e = createXPathElement();
      e.addContent(editor.setText(new Element("Filename"), xmlFilename));
      update(e);
      file = new File(file.getParentFile(), xmlFilename);
    }
    try {
      outputStream = new ScoreBoardOutputStream(file);
      outputStream.start();
      outputStream.write(editor.filterNoSavePI(getXmlScoreBoard().getDocument()));
      getXmlScoreBoard().addXmlScoreBoardListener(this);
    } catch ( FileNotFoundException fnfE ) {
      doStop();
      throw new FileNotFoundException("File '"+file.getName()+"' could not be created : "+fnfE.getMessage());
    } catch ( IOException ioE ) {
      doStop();
      throw new IOException("Could not start streaming to file '"+file.getName()+"' : "+ioE.getMessage());
    }
  }

  protected void doStop() {
    getXmlScoreBoard().removeXmlScoreBoardListener(this);
    if (null != outputStream)
      outputStream.stop();
    outputStream = null;
  }

  protected void doXmlChange(Document d) throws IOException {
    try {
      outputStream.write(editor.filterNoSavePI(d));
    } catch ( IOException ioE ) {
      outputStream.stop();
      throw new IOException("Error while streaming to file : "+ioE.getMessage());
    }
  }

  protected ScoreBoardOutputStream outputStream = null;
}
