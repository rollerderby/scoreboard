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
  public ConditionalScoreBoardListener() { }
  public ConditionalScoreBoardListener(ScoreBoardEvent e, ScoreBoardListener l) {
    addConditionalListener(e, l);
  }

  public void scoreBoardChange(ScoreBoardEvent event) {
    synchronized (lock) {
      if (map.containsKey(event)) {
        Iterator<ScoreBoardListener> listeners = map.get(event).iterator();
        while (listeners.hasNext())
          listeners.next().scoreBoardChange(event);
      }
    }
  }

  public void addConditionalListener(ScoreBoardEvent e, ScoreBoardListener l) {
    synchronized (lock) {
      if (!map.containsKey(e))
        map.put(e, new LinkedList<ScoreBoardListener>());
      if (!map.get(e).contains(l))
        map.get(e).add(l);
    }
  }

  public void removeConditionalListener(ScoreBoardEvent e, ScoreBoardListener l) {
    synchronized (lock) {
      if (!map.containsKey(e))
        return;
      map.get(e).remove(l);
    }
  }

  protected Map<ScoreBoardEvent,List<ScoreBoardListener>> map = new LinkedHashMap<ScoreBoardEvent,List<ScoreBoardListener>>();
  protected Object lock = new Object();
}

