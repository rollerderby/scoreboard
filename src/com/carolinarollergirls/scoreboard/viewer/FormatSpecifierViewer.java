package com.carolinarollergirls.scoreboard.viewer;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;

public class FormatSpecifierViewer implements ScoreBoardViewer
{
  public FormatSpecifierViewer(ScoreBoard sb) { setScoreBoard(sb); }

  public void setScoreBoard(ScoreBoard sb) {
    scoreBoard = sb;
    setupScoreBoardValues();
  }

  public String parse(String format) {
    StringBuffer buffer = new StringBuffer();
    Matcher m = formatPattern.matcher(format);
    while (m.find())
      m.appendReplacement(buffer, getFormatSpecifierValue(m.group()));
    m.appendTail(buffer);
    return buffer.toString();
  }

  public boolean checkCondition(String format, ScoreBoardEvent event) {
    Matcher m = conditionPattern.matcher(format);
    if (!m.find())
      throw new IllegalArgumentException("No conditions in format : "+format);
    do {
      String specifier = m.group(1);
      String comparator = m.group(2);
      String targetValue = m.group(3);
      if (null == comparator || null == targetValue)
        continue;
      String value = scoreBoardValues.get(specifier).getValue();
      try {
        if (!checkConditionValue(value, comparator, targetValue))
          return false;
      } catch ( IllegalArgumentException iaE ) {
        return false;
      }
    } while (m.find());
    return true;
  }

  protected boolean checkConditionValue(String value, String comparator, String target) throws IllegalArgumentException {
    if ("=".equals(comparator)) {
      return value.equals(target);
    } else if ("!=".equals(comparator)) {
      return !value.equals(target);
    } else if ("<".equals(comparator)) {
      try { return (Long.parseLong(value) < Long.parseLong(target)); } catch ( NumberFormatException nfE ) { }
      try { return (Double.parseDouble(value) < Double.parseDouble(target)); } catch ( NumberFormatException nfE ) { }
      return (value.compareTo(target) < 0);
    } else if ("<=".equals(comparator)) {
      try { return (Long.parseLong(value) <= Long.parseLong(target)); } catch ( NumberFormatException nfE ) { }
      try { return (Double.parseDouble(value) <= Double.parseDouble(target)); } catch ( NumberFormatException nfE ) { }
      return (value.compareTo(target) <= 0);
    } else if (">".equals(comparator)) {
      try { return (Long.parseLong(value) > Long.parseLong(target)); } catch ( NumberFormatException nfE ) { }
      try { return (Double.parseDouble(value) > Double.parseDouble(target)); } catch ( NumberFormatException nfE ) { }
      return (value.compareTo(target) > 0);
    } else if (">=".equals(comparator)) {
      try { return (Long.parseLong(value) >= Long.parseLong(target)); } catch ( NumberFormatException nfE ) { }
      try { return (Double.parseDouble(value) >= Double.parseDouble(target)); } catch ( NumberFormatException nfE ) { }
      return (value.compareTo(target) >= 0);
    }
    throw new IllegalArgumentException("Invalid comparator : "+comparator);
  }

  public ScoreBoardCondition getScoreBoardCondition(String format) throws IllegalArgumentException {
    Matcher m = eventPattern.matcher(format);
    if (!m.find())
      throw new IllegalArgumentException("No valid event specified");
    return getFormatSpecifierScoreBoardCondition(m.group(1));    
  }

  protected ScoreBoardCondition getFormatSpecifierScoreBoardCondition(String formatSpecifier) throws IllegalArgumentException {
    ScoreBoardValue value = scoreBoardValues.get(formatSpecifier);
    if (null == value)
      throw new IllegalArgumentException("Not a valid format specifier : "+formatSpecifier);
    return value.getScoreBoardCondition();
  }

  protected String getFormatSpecifierValue(String formatSpecifier) {
    ScoreBoardValue value = scoreBoardValues.get(formatSpecifier);
    if (null == value)
      return formatSpecifier;
    return value.getValue();
  }

  protected void setupScoreBoardValues() {
    setupTeamValues("1", Team.ID_1);
    setupTeamValues("2", Team.ID_2);
    setupClockValues("p", Clock.ID_PERIOD);
    setupClockValues("j", Clock.ID_JAM);
    setupClockValues("l", Clock.ID_LINEUP);
    setupClockValues("t", Clock.ID_TIMEOUT);
    setupClockValues("i", Clock.ID_INTERMISSION);

    StringBuffer patternBuffer = new StringBuffer();
    Iterator<String> patterns = scoreBoardValues.keySet().iterator();
    while (patterns.hasNext())
      patternBuffer.append(patterns.next()+"|");
    String specifiersRegex = patternBuffer.toString().replaceAll("[|]$", "");
    formatPattern = Pattern.compile(specifiersRegex);
    eventPattern = Pattern.compile("^\\s*("+specifiersRegex+")");
    conditionPattern = Pattern.compile("("+specifiersRegex+")(?:("+comparatorRegex+")(\\S+))?");
  }

  protected void setupTeamValues(String t, final String id) {
    new ScoreBoardValue("%t"+t+"n", getTeam(id), "Name") {
      public String getValue() { return getTeam(id).getName(); }
    };
    new ScoreBoardValue("%t"+t+"s", getTeam(id), "Score") {
      public String getValue() { return String.valueOf(getTeam(id).getScore()); }
    };
    new ScoreBoardValue("%t"+t+"t", getTeam(id), "Timeouts") {
      public String getValue() { return String.valueOf(getTeam(id).getTimeouts()); }
    };
    new ScoreBoardValue("%t"+t+"l", getTeam(id), "LeadJammer") {
      public String getValue() { return String.valueOf(getTeam(id).getTimeouts()); }
    };
    new ScoreBoardValue("%t"+t+"jn", getTeam(id).getPosition(Position.ID_JAMMER), "Skater") {
      public String getValue() { return getSkaterName(getPositionSkater(id, Position.ID_JAMMER)); }
    };
    new ScoreBoardValue("%t"+t+"jN", getTeam(id).getPosition(Position.ID_JAMMER), "Skater") {
      public String getValue() { return getSkaterNumber(getPositionSkater(id, Position.ID_JAMMER)); }
    };
    new ScoreBoardValue("%t"+t+"pn", getTeam(id).getPosition(Position.ID_PIVOT), "Skater") {
      public String getValue() { return getSkaterName(getPositionSkater(id, Position.ID_PIVOT)); }
    };
    new ScoreBoardValue("%t"+t+"pN", getTeam(id).getPosition(Position.ID_PIVOT), "Skater") {
      public String getValue() { return getSkaterNumber(getPositionSkater(id, Position.ID_PIVOT)); }
    };
    new ScoreBoardValue("%t"+t+"b1n", getTeam(id).getPosition(Position.ID_BLOCKER1), "Skater") {
      public String getValue() { return getSkaterName(getPositionSkater(id, Position.ID_BLOCKER1)); }
    };
    new ScoreBoardValue("%t"+t+"b1N", getTeam(id).getPosition(Position.ID_BLOCKER1), "Skater") {
      public String getValue() { return getSkaterNumber(getPositionSkater(id, Position.ID_BLOCKER1)); }
    };
    new ScoreBoardValue("%t"+t+"b2n", getTeam(id).getPosition(Position.ID_BLOCKER2), "Skater") {
      public String getValue() { return getSkaterName(getPositionSkater(id, Position.ID_BLOCKER2)); }
    };
    new ScoreBoardValue("%t"+t+"b2N", getTeam(id).getPosition(Position.ID_BLOCKER2), "Skater") {
      public String getValue() { return getSkaterNumber(getPositionSkater(id, Position.ID_BLOCKER2)); }
    };
    new ScoreBoardValue("%t"+t+"b3n", getTeam(id).getPosition(Position.ID_BLOCKER3), "Skater") {
      public String getValue() { return getSkaterName(getPositionSkater(id, Position.ID_BLOCKER3)); }
    };
    new ScoreBoardValue("%t"+t+"b3N", getTeam(id).getPosition(Position.ID_BLOCKER3), "Skater") {
      public String getValue() { return getSkaterNumber(getPositionSkater(id, Position.ID_BLOCKER3)); }
    };
  }

  protected Team getTeam(String id) { return scoreBoard.getTeam(id); }

  protected Skater getPositionSkater(String id, String position) {
    return getTeam(id).getPosition(position).getSkater();
  }

  protected String getSkaterName(Skater s) { return (null == s ? NO_SKATER_NAME_VALUE : s.getName()); }
  protected String getSkaterNumber(Skater s) { return (null == s ? NO_SKATER_NUMBER_VALUE : s.getNumber()); }

  protected void setupClockValues(String c, final String id) {
    new ScoreBoardValue("%c"+c+"n", getClock(id), "Name") {
      public String getValue() { return getClock(id).getName(); }
    };
    new ScoreBoardValue("%c"+c+"N", getClock(id), "Number") {
      public String getValue() { return String.valueOf(getClock(id).getNumber()); }
    };
    new ScoreBoardValue("%c"+c+"r", getClock(id), "Running") {
      public String getValue() { return String.valueOf(getClock(id).isRunning()); }
    };
    new ScoreBoardValue("%c"+c+"ts", getClock(id), "Time") {
      public String getValue() { return getClockSecs(id); }
    };
    new ScoreBoardValue("%c"+c+"tms", getClock(id), "Time") {
      public String getValue() { return getClockMinSecs(id); }
    };
  }

  protected Clock getClock(String id) { return scoreBoard.getClock(id); }

  protected String getClockSecs(String id) { return String.valueOf(getClock(id).getTime()/1000); }
  protected String getClockMinSecs(String id) {
    long time = (getClock(id).getTime()/1000);
    return (time/60)+":"+(time%60);
  }

  protected ScoreBoard scoreBoard = null;

  protected Pattern formatPattern;
  protected Pattern eventPattern;
  protected Pattern conditionPattern;

  protected String comparatorRegex = "=|!=|<|<=|>|>=";

  protected Map<String,ScoreBoardValue> scoreBoardValues = new ConcurrentHashMap<String,ScoreBoardValue>();

  protected abstract class ScoreBoardValue
  {
    public ScoreBoardValue(String f, ScoreBoardEventProvider p, String prop) {
      format = f;
      provider = p;
      property = prop;
      scoreBoardValues.put(format, this);
    }
    public abstract String getValue();
    public ScoreBoardCondition getScoreBoardCondition() {
      return new ScoreBoardCondition(provider, property, ScoreBoardCondition.ANY_VALUE);
    }
    protected String format;
    protected ScoreBoardEventProvider provider;
    protected String property;
  }

  public static final String NO_SKATER_NAME_VALUE = "";
  public static final String NO_SKATER_NUMBER_VALUE = "";
}

