package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PenaltyImpl extends NumberedScoreBoardEventProviderImpl<Penalty> implements Penalty {
    public PenaltyImpl(Skater s, int n) {
        super(s, n, Skater.PENALTY);
        game = s.getTeam().getGame();
        addProperties(TIME, JAM, PERIOD_NUMBER, JAM_NUMBER, CODE, SERVING, SERVED, FORCE_SERVED, BOX_TRIP, REMOVE);
        set(TIME, ScoreBoardClock.getInstance().getCurrentWalltime());
        setInverseReference(JAM, Jam.PENALTY);
        setInverseReference(BOX_TRIP, BoxTrip.PENALTY);
        addWriteProtectionOverride(TIME, Source.ANY_FILE);
        setRecalculated(SERVED).addSource(this, BOX_TRIP).addSource(this, FORCE_SERVED);
        setCopy(SERVING, this, BOX_TRIP, BoxTrip.IS_CURRENT, true);
        setCopy(JAM_NUMBER, this, JAM, Jam.NUMBER, true);
        setCopy(PERIOD_NUMBER, this, JAM, Jam.PERIOD_NUMBER, true);
        if (s.isPenaltyBox()) { set(BOX_TRIP, s.getCurrentFielding().getCurrentBoxTrip()); }
        set(SERVED, get(BOX_TRIP) != null);
    }
    public PenaltyImpl(PenaltyImpl cloned, ScoreBoardEventProvider root) {
        super(cloned, root);
        game = toCloneIfInTree(cloned.game, root);
    }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new PenaltyImpl(this, root);
    }

    @Override
    public int compareTo(Penalty other) {
        if (other == null) { return -1; }
        if (getJam() == other.getJam()) { return (int) (get(Penalty.TIME) - other.get(Penalty.TIME)); }
        if (getJam() == null) { return 1; }
        return getJam().compareTo(other.getJam());
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == NEXT && getNumber() == 0) { return null; }
        if (prop == PREVIOUS && value != null && ((Penalty) value).getNumber() == 0) { return null; }
        if (prop == SERVED) { return (get(BOX_TRIP) != null || get(FORCE_SERVED)); }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == JAM && !Skater.FO_EXP_ID.equals(getProviderId())) {
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

            if (newPos == game.getInt(Rule.FO_LIMIT)) {
                Penalty fo = parent.get(Skater.PENALTY, Skater.FO_EXP_ID);
                if (fo != null && fo.get(CODE) == "FO") { fo.set(JAM, (Jam) value); }
            }
        }
        if (prop == CODE && value == null) { delete(source); }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == REMOVE) { delete(source); }
    }

    @Override
    public int getPeriodNumber() {
        return get(PERIOD_NUMBER);
    }
    @Override
    public int getJamNumber() {
        return get(JAM_NUMBER);
    }
    @Override
    public Jam getJam() {
        return get(JAM);
    }
    @Override
    public String getCode() {
        return get(CODE);
    }
    @Override
    public boolean isServed() {
        return get(SERVED);
    }

    private Game game;
}
