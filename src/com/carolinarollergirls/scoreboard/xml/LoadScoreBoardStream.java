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

public class LoadScoreBoardStream extends AbstractScoreBoardStream implements StreamListener
{
  public LoadScoreBoardStream() { super("LoadStream"); }

  public void update(Document d) {
    if (running)
      d.setProperty("DocumentManager", this);
    super.update(d);
  }

  public void setXmlScoreBoard(XmlScoreBoard xsB) {
    super.setXmlScoreBoard(xsB);

    updateStartTime(0);
    updateCurrentTime(0);
    updateEndTime(0);

    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Pause"), "false"));
    updateE.addContent(editor.setText(new Element("Speed"), "1.0"));
    update(updateE);
  }

  protected void processChildElement(Element e) throws JDOMException {
    super.processChildElement(e);
    synchronized (processLock) {
      if (e.getName().equals("Pause"))
        pause(editor.isTrue(e));
      else if (e.getName().equals("Speed"))
        speed(editor.getText(e));
    }
  }

  protected void pause(boolean pause) {
    if (!running)
      return;
    realtimeFilter.setPaused(pause);
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Pause"), Boolean.toString(pause)));
    update(updateE);
  }

  protected void speed(String s) {
    if (!running)
      return;
    double speed;
    try { speed = Double.parseDouble(s); }
    catch ( NumberFormatException nfE ) { return; }
    realtimeFilter.setSpeed(speed);
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Speed"), Double.toString(speed)));
    update(updateE);
  }

  protected void doStart(File file) throws IOException,FileNotFoundException {
    try {
      if (!getXmlScoreBoard().startExclusive(this))
        throw new IOException("Could not get exclusive XmlScoreBoard access");
      realtimeFilter = new MyRealtimeFilter(this);
      bufferedFilter = new MyBufferedFilter(realtimeFilter);
      inputStream = new ScoreBoardInputStream(file, bufferedFilter);
      inputStream.start();
      bufferedFilter.start();
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
    if (null != bufferedFilter)
      bufferedFilter.stop();
    inputStream = null;
    bufferedFilter = null;
    realtimeFilter = null;
    getXmlScoreBoard().endExclusive(this);
    updateStartTime(0);
    updateCurrentTime(0);
    updateEndTime(0);
  }

  protected void updateStartTime(long time) {
    startTime = time;
  }
  protected void updateEndTime(long time) {
    if (!running)
      return;
    synchronized (timeLock) {
      if ((currentTime < endTime) && (time >= endTime) && ((time - endTime) < 1000))
        return;
      endTime = time;
    }
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("EndTime"), Long.toString(time-startTime)));
    update(updateE);
  }
  protected void updateCurrentTime(long time) {
    if (!running)
      return;
    synchronized (timeLock) {
      currentTime = time;
      if (currentTime >= endTime)
        updateEndTime(currentTime);
    }
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("CurrentTime"), Long.toString(time-startTime)));
    update(updateE);
  }

  protected void doXmlChange(Document d) { update(d); }

  public void end() { stop(); }

  protected ScoreBoardInputStream inputStream = null;
  protected RealtimeStreamListenerFilter realtimeFilter;
  protected BufferedStreamListenerFilter bufferedFilter;
  protected long startTime = 0, currentTime = 0, endTime = 0;
  protected Object timeLock = new Object();

  protected class MyBufferedFilter extends BufferedStreamListenerFilter
  {
    public MyBufferedFilter(StreamListener l) { super(l); }
    protected void addDocument(Document d) {
      super.addDocument(d);
      LoadScoreBoardStream.this.updateStartTime(getStartTime());
      LoadScoreBoardStream.this.updateEndTime(getEndTime());
    }
  }

  protected class MyRealtimeFilter extends RealtimeStreamListenerFilter
  {
    public MyRealtimeFilter(StreamListener l) { super(l); }
    public void xmlChange(Document d) {
      super.xmlChange(d);
      LoadScoreBoardStream.this.updateCurrentTime(LoadScoreBoardStream.this.editor.getSystemTime(d));
    }
  }
}
