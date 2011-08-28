package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public abstract class XmlStats extends SegmentedXmlDocumentManager implements ScoreBoardListener
{
  public XmlStats(String n) { super("Stats", n); }

  public void setXmlScoreBoard(XmlScoreBoard xsB) {
    scoreBoard = xsB.getScoreBoardModel();
    super.setXmlScoreBoard(xsB);
    scoreBoard.addScoreBoardListener(this);
  }

  public void reset() {
    super.reset();
    statsStartTime = new Date().getTime();
    update(createXPathElement().addContent(new Element("Start").setText(Long.toString(statsStartTime))));
  }

  public abstract void scoreBoardChange(ScoreBoardEvent event);

  protected String getStatsTime() { return Long.toString(new Date().getTime() - statsStartTime); }

  protected ScoreBoard scoreBoard;
  protected long statsStartTime;
}
