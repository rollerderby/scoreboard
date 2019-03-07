package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ScoringTripImpl extends NumberedScoreBoardEventProviderImpl<ScoringTrip> implements ScoringTrip {
    ScoringTripImpl(TeamJam parent, String id) {
        super(parent, id, TeamJam.NChild.SCORING_TRIP, ScoringTrip.class, Value.class);
        addReference(new IndirectValueReference(this, Value.JAM_CLOCK_START, this, IValue.PREVIOUS, Value.JAM_CLOCK_END, true, 0L));
        addReference(new UpdateReference(this, Value.DURATION, this, Value.JAM_CLOCK_END));
        addReference(new UpdateReference(this, Value.DURATION, this, Value.JAM_CLOCK_START));
        set(Value.JAM_CLOCK_END, 0L);
        addReference(new UpdateReference(parent, TeamJam.Value.JAM_SCORE, this, Value.SCORE));
        set(Value.SCORE, 0);
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
        if (prop == Value.SCORE && (Long)get(Value.JAM_CLOCK_END) == 0L) {
            set(Value.JAM_CLOCK_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        }
        if (prop == Value.AFTER_S_P) {
            if ((Boolean)value && hasNext()) {
                getNext().set(Value.AFTER_S_P, true);
            }
            if (!(Boolean)value && hasPrevious()) {
                getPrevious().set(Value.AFTER_S_P, false);
            }
        }
    }
    
    public int getScore() { return (Integer)get(Value.SCORE); }
}
