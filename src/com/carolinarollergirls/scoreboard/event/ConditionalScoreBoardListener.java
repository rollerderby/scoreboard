package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

public class ConditionalScoreBoardListener implements ScoreBoardListener
{
  public ConditionalScoreBoardListener(Class c, String id, String prop, Object v, ScoreBoardListener l) {
    this(new ScoreBoardCondition(c, id, prop, v), l);
  }
  public ConditionalScoreBoardListener(Class c, String id, String prop, Object v) {
    this(new ScoreBoardCondition(c, id, prop, v));
  }
  public ConditionalScoreBoardListener(Class c, String id, String prop, ScoreBoardListener l) {
    this(c, id, prop, ScoreBoardCondition.ANY_VALUE, l);
  }
  public ConditionalScoreBoardListener(Class c, String id, String prop) {
    this(c, id, prop, ScoreBoardCondition.ANY_VALUE);
  }
  public ConditionalScoreBoardListener(Class c, String prop, ScoreBoardListener l) {
    this(c, ScoreBoardCondition.ANY_ID, prop, ScoreBoardCondition.ANY_VALUE, l);
  }
  public ConditionalScoreBoardListener(Class c, String prop) {
    this(c, ScoreBoardCondition.ANY_ID, prop, ScoreBoardCondition.ANY_VALUE);
  }
  public ConditionalScoreBoardListener(ScoreBoardEventProvider p, String prop, Object v, ScoreBoardListener l) {
    this(new ScoreBoardCondition(p, prop, v), l);
  }
  public ConditionalScoreBoardListener(ScoreBoardEventProvider p, String prop, Object v) {
    this(new ScoreBoardCondition(p, prop, v));
  }
  public ConditionalScoreBoardListener(ScoreBoardEventProvider p, String prop, ScoreBoardListener l) {
    this(p, prop, ScoreBoardCondition.ANY_VALUE, l);
  }
  public ConditionalScoreBoardListener(ScoreBoardEventProvider p, String prop) {
    this(p, prop, ScoreBoardCondition.ANY_VALUE);
  }
  public ConditionalScoreBoardListener(ScoreBoardEvent e, ScoreBoardListener l) {
    this(new ScoreBoardCondition(e), l);
  }
  public ConditionalScoreBoardListener(ScoreBoardEvent e) {
    this(new ScoreBoardCondition(e));
  }
  public ConditionalScoreBoardListener(ScoreBoardCondition c, ScoreBoardListener l) {
    condition = c;
    listener = l;
  }
  public ConditionalScoreBoardListener(ScoreBoardCondition c) {
    this(c, null);
  }

  public void scoreBoardChange(ScoreBoardEvent e) {
    if (checkScoreBoardEvent(e))
      matchedScoreBoardChange(e);
  }

  public ScoreBoardListener getScoreBoardListener() { return listener; }
  public void setScoreBoardListener(ScoreBoardListener sbL) { listener = sbL; }
  public ScoreBoardListener removeScoreBoardListener() {
    ScoreBoardListener sbL = listener;
    listener = null;
    return sbL;
  }

  protected boolean checkScoreBoardEvent(ScoreBoardEvent e) { return condition.equals(e); }
  protected void matchedScoreBoardChange(ScoreBoardEvent e) {
    if (null != getScoreBoardListener())
      getScoreBoardListener().scoreBoardChange(e);
  }

  protected ScoreBoardCondition condition;
  protected ScoreBoardListener listener;
}

