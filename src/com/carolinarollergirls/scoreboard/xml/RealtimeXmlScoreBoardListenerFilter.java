package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public class RealtimeXmlScoreBoardListenerFilter implements XmlScoreBoardListener
{
  public RealtimeXmlScoreBoardListenerFilter(XmlScoreBoardListener l) { listener = l; }

  public void xmlChange(Document d) {
    if (null == d)
      listener.xmlChange(d);

    if (!startTimeSet)
      setStartTime(d);

    waitUntilTime(d);
    listener.xmlChange(d);
  }

  protected void setStartTime(Document d) {
    docStartTime = editor.getSystemTime(d);
    realStartTime = new Date().getTime();
    startTimeSet = true;
  }

  protected void waitUntilTime(Document d) {
    long docTime = editor.getSystemTime(d);
    long realTime = new Date().getTime();

    long elapsedDocTime = docTime - docStartTime;
    long elapsedRealTime = realTime - realStartTime;
    long sleepTime = elapsedDocTime - elapsedRealTime;

    try {
      if (sleepTime > 0)
        Thread.sleep(sleepTime);
    } catch ( InterruptedException iE ) {
      ScoreBoardManager.printMessage("Interrupted while waiting for document time to occur : "+iE.getMessage());
    }
  }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();
  protected XmlScoreBoardListener listener;
  protected long docStartTime;
  protected long realStartTime;
  protected boolean startTimeSet = false;
}
