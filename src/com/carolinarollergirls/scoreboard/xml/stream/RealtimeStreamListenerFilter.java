package com.carolinarollergirls.scoreboard.xml.stream;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class RealtimeStreamListenerFilter extends StreamListenerFilter implements StreamListener
{
  public RealtimeStreamListenerFilter(StreamListener l) { super(l); }

  public void xmlChange(Document d) {
    if (!startTimeSet)
      setStartTime(d);

    try {
      waitUntilTime(d);
    } catch ( InterruptedException iE ) {
      /* Indicate stop processing */
      return;
    }
    super.xmlChange(d);
  }

  protected void setStartTime(Document d) {
    docStartTime = editor.getSystemTime(d);
    realStartTime = new Date().getTime();
    startTimeSet = true;
  }

  protected void waitUntilTime(Document d) throws InterruptedException {
    long docTime = editor.getSystemTime(d);
    long realTime = new Date().getTime();

    long elapsedDocTime = docTime - docStartTime;
    long elapsedRealTime = realTime - realStartTime;
    long sleepTime = elapsedDocTime - elapsedRealTime;

    if (sleepTime > 0)
      Thread.sleep(sleepTime);
  }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();
  protected long docStartTime;
  protected long realStartTime;
  protected boolean startTimeSet = false;
}
