package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.utils.Comparators;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PenaltyImpl extends NumberedScoreBoardEventProviderImpl<Penalty> implements Penalty {
    public PenaltyImpl(Skater s, String n) {
        super(s, n, Skater.NChild.PENALTY, Penalty.class, Value.class);
        set(Value.TIME, ScoreBoardClock.getInstance().getCurrentWalltime());
        addWriteProtectionOverride(Value.TIME, Flag.FROM_AUTOSAVE);
        addReference(new ElementReference(Value.JAM, Jam.class, Jam.Child.PENALTY));
        addReference(new IndirectValueReference(this, Value.JAM_NUMBER, this, Value.JAM, IValue.NUMBER, true, 0));
        addReference(new IndirectValueReference(this, Value.PERIOD_NUMBER, this, Value.JAM, Jam.Value.PERIOD_NUMBER, true, 0));
    }
    public int getPeriodNumber() { return (Integer)get(Value.PERIOD_NUMBER); }
    public int getJamNumber() { return (Integer)get(Value.JAM_NUMBER); }
    public Jam getJam() { return (Jam)get(Value.JAM); }
    public String getCode() { return (String)get(Value.CODE); }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == IValue.NEXT && getNumber() == 0) { return null; }
        if (prop == IValue.PREVIOUS && getNumber() == 1) { return null; }
        return value;
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.JAM && !Skater.FO_EXP_ID.equals(getProviderId())) {
            int newPos = getNumber();
            if (Comparators.JamComparator.compare((Jam)value, (Jam)last) > 0) {
                Penalty comp = getNext(); 
                while (Comparators.PenaltyComparator.compare(this, comp) > 0) { // will be false if comp == null
                    newPos = comp.getNumber();
                    comp = comp.getNext();
                }
            } else {
                Penalty comp = getPrevious(); 
                while (comp != null && Comparators.PenaltyComparator.compare(this, comp) < 0) {
                    newPos = comp.getNumber();
                    comp = comp.getPrevious();
                }
            }
            moveToNumber(newPos);
        }
        if (prop == Value.CODE && value == null) {
            unlink();
        }
    }
}
