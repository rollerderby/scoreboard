package com.carolinarollergirls.scoreboard.event;

/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard. The CRG
 * ScoreBoard is licensed under either the GNU General Public License version 3
 * (or later), or the Apache License 2.0, at your option. See the file COPYING
 * for details.
 */

public class ConditionalScoreBoardListener<T> implements ScoreBoardListener {
    public ConditionalScoreBoardListener(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> prop, T v,
                                         ScoreBoardListener l) {
        this(new ScoreBoardCondition<>(c, id, prop, v), l);
    }
    public ConditionalScoreBoardListener(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> prop, T v) {
        this(new ScoreBoardCondition<>(c, id, prop, v));
    }
    public ConditionalScoreBoardListener(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> prop,
                                         ScoreBoardListener l) {
        this(new ScoreBoardCondition<>(c, id, prop), l);
    }
    public ConditionalScoreBoardListener(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> prop) {
        this(new ScoreBoardCondition<>(c, id, prop));
    }
    public ConditionalScoreBoardListener(Class<? extends ScoreBoardEventProvider> c, Property<T> prop,
                                         ScoreBoardListener l) {
        this(c, ScoreBoardCondition.ANY_ID, prop, l);
    }
    public ConditionalScoreBoardListener(Class<? extends ScoreBoardEventProvider> c, Property<T> prop) {
        this(c, ScoreBoardCondition.ANY_ID, prop);
    }
    public ConditionalScoreBoardListener(ScoreBoardEventProvider p, Property<T> prop, T v, ScoreBoardListener l) {
        this(new ScoreBoardCondition<>(p, prop, v), l);
    }
    public ConditionalScoreBoardListener(ScoreBoardEventProvider p, Property<T> prop, T v) {
        this(new ScoreBoardCondition<>(p, prop, v));
    }
    public ConditionalScoreBoardListener(ScoreBoardEventProvider p, Property<T> prop, ScoreBoardListener l) {
        this(new ScoreBoardCondition<>(p, prop), l);
    }
    public ConditionalScoreBoardListener(ScoreBoardEventProvider p, Property<T> prop) {
        this(new ScoreBoardCondition<>(p, prop));
    }
    public ConditionalScoreBoardListener(ScoreBoardEvent<T> e, ScoreBoardListener l) {
        this(new ScoreBoardCondition<>(e), l);
    }
    public ConditionalScoreBoardListener(ScoreBoardEvent<T> e) { this(new ScoreBoardCondition<>(e)); }
    public ConditionalScoreBoardListener(ScoreBoardCondition<T> c, ScoreBoardListener l) {
        condition = c;
        listener = l;
    }
    public ConditionalScoreBoardListener(ScoreBoardCondition<T> c) { this(c, null); }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> e) {
        if (checkScoreBoardEvent(e)) { matchedScoreBoardChange(e); }
    }

    public ScoreBoardListener getScoreBoardListener() { return listener; }
    public void setScoreBoardListener(ScoreBoardListener sbL) { listener = sbL; }
    public ScoreBoardListener removeScoreBoardListener() {
        ScoreBoardListener sbL = listener;
        listener = null;
        return sbL;
    }

    @SuppressWarnings("unlikely-arg-type")
    protected boolean checkScoreBoardEvent(ScoreBoardEvent<?> e) {
        return condition.equals(e);
    }
    protected void matchedScoreBoardChange(ScoreBoardEvent<?> e) {
        if (null != getScoreBoardListener()) { getScoreBoardListener().scoreBoardChange(e); }
    }

    protected ScoreBoardCondition<T> condition;
    protected ScoreBoardListener listener;
}
