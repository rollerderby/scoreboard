package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ScoringTripImpl extends NumberedScoreBoardEventProviderImpl<ScoringTrip> implements ScoringTrip {
    ScoringTripImpl(TeamJam parent, int number) {
        super(parent, number, TeamJam.NChild.SCORING_TRIP, ScoringTrip.class, Value.class, Command.class);
        setCopy(Value.JAM_CLOCK_START, this, IValue.PREVIOUS, Value.JAM_CLOCK_END, true);
        setRecalculated(Value.DURATION).addSource(this, Value.JAM_CLOCK_END).addSource(this, Value.JAM_CLOCK_START);
        set(Value.AFTER_S_P, hasPrevious() ? getPrevious().get(Value.AFTER_S_P) : false);
    }

    public Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.SCORE && (Integer)value < 0) { return 0; }
        if (prop == Value.DURATION) {
            if ((Long)get(Value.JAM_CLOCK_END) > 0L) {
                return (Long)get(Value.JAM_CLOCK_END) - (Long)get(Value.JAM_CLOCK_START);
            } else {
                return 0L;
            }
        }
        return value;
    }
    public void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if ((prop == Value.SCORE || (prop == Value.CURRENT && !(Boolean)value)) &&
                (Long)get(Value.JAM_CLOCK_END) == 0L) {
            set(Value.JAM_CLOCK_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        }
        if (prop == Value.CURRENT && (Boolean)value && (Integer)get(Value.SCORE) == 0) {
            set(Value.JAM_CLOCK_END, 0L);
        }
        if (prop == Value.AFTER_S_P) {
            if ((Boolean)value && hasNext()) {
                getNext().set(Value.AFTER_S_P, true);
            }
            if (!(Boolean)value && hasPrevious()) {
                getPrevious().set(Value.AFTER_S_P, false);
            }
            if (flag != Flag.INTERNAL) {
                if ((Boolean)value && (!hasPrevious() || !(Boolean)getPrevious().get(Value.AFTER_S_P))) {
                    parent.set(TeamJam.Value.STAR_PASS_TRIP, this);
                } else if(!(Boolean)value && (!hasNext() || (Boolean)getNext().get(Value.AFTER_S_P))) {
                    parent.set(TeamJam.Value.STAR_PASS_TRIP, getNext());
                }
            }
        }
    }
    
    public void execute(CommandProperty prop) {
        if (prop == Command.REMOVE) { unlink(); }
    }
    
    public int getScore() { return (Integer)get(Value.SCORE); }
}
