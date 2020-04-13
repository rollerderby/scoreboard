package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.BoxTrip;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PenaltyImpl extends NumberedScoreBoardEventProviderImpl<Penalty> implements Penalty {
    public PenaltyImpl(Skater s, int n) {
        super(s, n, Skater.NChild.PENALTY, Penalty.class, Value.class, Command.class);
        set(Value.TIME, ScoreBoardClock.getInstance().getCurrentWalltime());
        setInverseReference(Value.JAM, Jam.Child.PENALTY);
        setInverseReference(Value.BOX_TRIP, BoxTrip.Child.PENALTY);
        addWriteProtectionOverride(Value.TIME, Source.ANY_FILE);
        setRecalculated(Value.SERVED).addSource(this, Value.BOX_TRIP).addSource(this, Value.FORCE_SERVED);
        setCopy(Value.SERVING, this, Value.BOX_TRIP, BoxTrip.Value.IS_CURRENT, true);
        setCopy(Value.JAM_NUMBER, this, Value.JAM, IValue.NUMBER, true);
        setCopy(Value.PERIOD_NUMBER, this, Value.JAM, Jam.Value.PERIOD_NUMBER, true);
        if (s.isPenaltyBox()) { set(Value.BOX_TRIP, s.getCurrentFielding().getCurrentBoxTrip()); }
        set(Value.SERVED, get(Value.BOX_TRIP) != null);
    }

    @Override
    public int compareTo(Penalty other) {
        if (other == null) { return -1; }
        if (getJam() == other.getJam()) {
            return (int) ((Long) get(Penalty.Value.TIME) - (Long) other.get(Penalty.Value.TIME));
        }
        if (getJam() == null) { return 1; }
        return getJam().compareTo(other.getJam());
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == IValue.NEXT && getNumber() == 0) { return null; }
        if (prop == IValue.PREVIOUS && value != null && ((Penalty) value).getNumber() == 0) { return null; }
        if (prop == Value.SERVED) { return (get(Value.BOX_TRIP) != null || (Boolean) get(Value.FORCE_SERVED)); }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Value.JAM && !Skater.FO_EXP_ID.equals(getProviderId())) {
            int newPos = getNumber();
            if (value == null || ((Jam) value).compareTo((Jam) last) > 0) {
                Penalty comp = getNext();
                while (compareTo(comp) > 0) { // will be false if comp == null
                    newPos = comp.getNumber();
                    comp = comp.getNext();
                }
            } else {
                Penalty comp = getPrevious();
                while (comp != null && compareTo(comp) < 0) {
                    newPos = comp.getNumber();
                    comp = comp.getPrevious();
                }
            }
            moveToNumber(newPos);

            if (newPos == scoreBoard.getRulesets().getInt(Rule.FO_LIMIT)) {
                Penalty fo = parent.get(Skater.NChild.PENALTY, Penalty.class, Skater.FO_EXP_ID);
                if (fo != null && fo.get(Value.CODE) == "FO") {
                    fo.set(Value.JAM, value);
                }
            }
        }
        if (prop == Value.CODE && value == null) {
            delete(source);
        }
    }

    @Override
    public void execute(CommandProperty prop, Source source) {
        if (prop == Command.REMOVE) { delete(source); }
    }

    @Override
    public int getPeriodNumber() { return (Integer) get(Value.PERIOD_NUMBER); }
    @Override
    public int getJamNumber() { return (Integer) get(Value.JAM_NUMBER); }
    @Override
    public Jam getJam() { return (Jam) get(Value.JAM); }
    @Override
    public String getCode() { return (String) get(Value.CODE); }
    @Override
    public boolean isServed() { return (Boolean) get(Value.SERVED); }
}
