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

public class ScoreBoardCondition implements Cloneable
{
  public ScoreBoardCondition(ScoreBoardEvent e) {
    this(e.getProvider(), e.getProperty(), e.getValue());
  }
  public ScoreBoardCondition(ScoreBoardEventProvider sbeP, String p, Object v) {
    this(sbeP.getProviderClass(), sbeP.getProviderId(), p, v);
  }
  public ScoreBoardCondition(Class c, String id, String p, Object v) {
    providerClass = c;
    providerId = id;
    property = p;
    value = v;
  }

  public Class getProviderClass() { return providerClass; }
  public String getProviderId() { return providerId; }
  public String getProperty() { return property; }
  public Object getValue() { return value; }

  public Object clone() { return new ScoreBoardCondition(getProviderClass(), getProviderId(), getProperty(), getValue()); }

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
    if (!getProviderClass().equals(e.getProvider().getProviderClass()))
      return false;
    if (!getProviderId().equals(e.getProvider().getProviderId()))
      return false;
    if (!getProperty().equals(e.getProperty()))
      return false;
    if (ANY_VALUE == getValue())
      return true;
    return (getValue().equals(e.getValue()));
  }
  public boolean equals(ScoreBoardCondition c) {
    if (!getProviderClass().equals(c.getProviderClass()))
      return false;
    if (!getProviderId().equals(c.getProviderId()))
      return false;
    if (!getProperty().equals(c.getProperty()))
      return false;
    return (getValue().equals(c.getValue()));
  }

  protected Class providerClass;
  protected String providerId;
  protected String property;
  protected Object value;

  public static final Object ANY_VALUE = new Object();
}
