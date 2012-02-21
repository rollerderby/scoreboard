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

  public void update(Document d) {
    if (running)
      d.setProperty("DocumentManager", this);
    super.update(d);
  }

  protected void doStart(File file) throws IOException,FileNotFoundException {
    try {
      if (!getXmlScoreBoard().startExclusive(this))
        throw new IOException("Could not get exclusive XmlScoreBoard access");
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
    if (null != inputStream)
      inputStream.stop();
    inputStream = null;
    getXmlScoreBoard().endExclusive(this);
  }

  protected void doXmlChange(Document d) {
    if (null == d)
      stop();
    else
      update(d);
  }

  protected ScoreBoardInputStream inputStream = null;
}
