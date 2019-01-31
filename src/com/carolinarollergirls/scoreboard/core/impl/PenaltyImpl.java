package com.carolinarollergirls.scoreboard.core.impl;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PenaltyImpl extends ScoreBoardEventProviderImpl implements Penalty {
    public PenaltyImpl(Skater s, String n) {
        super(s, Value.ID, Skater.Child.PENALTY, Penalty.class, Value.class);
        skater = s;
        set(Value.NUMBER, n);
        set(Value.ID, UUID.randomUUID().toString());
        writeProtectionOverride.put(Value.ID, Flag.FROM_AUTOSAVE);
        set(Value.TIME, ScoreBoardClock.getInstance().getCurrentWalltime());
        writeProtectionOverride.put(Value.TIME, Flag.FROM_AUTOSAVE);
        addReference(new ElementReference(Value.JAM, Jam.class, Jam.Child.PENALTY));
        addReference(new IndirectPropertyReference(this, Value.JAM_NUMBER, this, Value.JAM, Jam.Value.NUMBER, true, 0));
        addReference(new IndirectPropertyReference(this, Value.PERIOD_NUMBER, this, Value.JAM, Jam.Value.PERIOD_NUMBER, true, 0));
    }
    public int getPeriodNumber() { return (Integer)get(Value.PERIOD_NUMBER); }
    public int getJamNumber() { return (Integer)get(Value.JAM_NUMBER); }
    public Jam getJam() { return (Jam)get(Value.JAM); }
    public String getCode() { return (String)get(Value.CODE); }

    public String getProviderId() { return (String)get(Value.NUMBER); }

    protected void valueChanged(PermanentProperty prop, Object value, Object last) {
        if (prop == Value.NUMBER) {
            scoreBoardChange(new ScoreBoardEvent(skater, Skater.Child.PENALTY, this, false));
        } else if (prop == Value.JAM) {
            skater.sortPenalties();
        }
    }
    
    protected Skater skater;
}
