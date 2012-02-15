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

public class LoadScoreBoardStream extends AbstractScoreBoardStream
{
  public LoadScoreBoardStream() { super("LoadStream"); }

  protected void doStart(File file) throws IOException,FileNotFoundException {
    try {
      firstDocument = true;
      inputStream = new ScoreBoardInputStream(file, new RealtimeXmlScoreBoardListenerFilter(this));
      inputStream.start();
    } catch ( FileNotFoundException fnfE ) {
      doStop();
      throw new FileNotFoundException("File '"+file.getName()+"' not found : "+fnfE.getMessage());
    } catch ( IOException ioE ) {
      doStop();
      throw new IOException("Could not start streaming from file '"+file.getName()+"' : "+ioE.getMessage());
    }
  }

  protected void doStop() {
    //FIXME - need to actually do a real load of the last SB state into real SB and xmldoc managers
    // this might need to be a feature of the XmlScoreBoard
    if (null != inputStream)
      inputStream.stop();
    inputStream = null;
  }

  protected void doXmlChange(Document d) {
    //FIXME - need to have some kind of locking/exclusion in XmlScoreBoard to prevent incoming commands from working
    // so only this exact stream is seen
    // but also to allow commands to this xmldoc manager to come in, to stop the load, or other future cmds
    if (null == d) {
      stop();
    } else if (firstDocument) {
      //FIXME - probably don't want to actually load, but just reset everything to defaults,
      //since the load will just "fake" events, sending only to listeners
      //then on stop (above) the real sb and xmldoc managers need to be updated with the last sb state
      getXmlScoreBoard().loadDocument(d);
      firstDocument = false;
    } else {
      getXmlScoreBoard().xmlChange(d);
    }
  }

  protected ScoreBoardInputStream inputStream = null;
  protected boolean firstDocument;
}
