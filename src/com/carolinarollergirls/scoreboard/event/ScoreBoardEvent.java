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

import java.lang.reflect.*;

public class ScoreBoardEvent extends EventObject implements Cloneable
{
  public ScoreBoardEvent(ScoreBoardEventProvider sbeP, String p, Object v) {
    super(sbeP);
    provider = sbeP;
    property = p;
    value = v;
  }

  public ScoreBoardEventProvider getProvider() { return provider; }
  public String getProperty() { return property; }
  public Object getValue() { return value; }

  public Object clone() { return new ScoreBoardEvent(getProvider(), getProperty(), getValue()); }

  public boolean equals(Object o) {
    if (null == o)
      return false;
    try { return equals((ScoreBoardEvent)o); }
    catch ( ClassCastException ccE ) { }
    try { return equals((ScoreBoardCondition)o); }
    catch ( ClassCastException ccE ) { }
    return false;
  }
  public boolean equals(ScoreBoardEvent e) {
    if (!getProvider().equals(e.getProvider()))
      return false;
    if (!getProperty().equals(e.getProperty()))
      return false;
    return (getValue().equals(e.getValue()));
  }
  public boolean equals(ScoreBoardCondition c) {
    return c.equals(this);
  }

  public boolean reflect(Object o) {
    try {
      reflectWithException(o);
      return true;
    } catch ( Exception e ) {
      return false;
    }
  }
  public void reflectWithException(Object o) throws Exception {
    Method m = o.getClass().getMethod("scoreBoardChange", new Class[]{getProvider().getProviderClass(), ScoreBoardEvent.class});
    m.invoke(o, new Object[]{getProvider(), this});
  }

  protected ScoreBoardEventProvider provider;
  protected String property;
  protected Object value;
}
