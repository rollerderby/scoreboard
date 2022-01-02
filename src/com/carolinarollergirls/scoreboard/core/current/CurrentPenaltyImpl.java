package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.interfaces.CurrentPenalty;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.event.ReferenceOrderedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class CurrentPenaltyImpl
    extends ReferenceOrderedScoreBoardEventProviderImpl<CurrentPenalty> implements CurrentPenalty {
    CurrentPenaltyImpl(CurrentSkater s, Penalty p) {
        super(s, p, CurrentSkater.PENALTY);
        addProperties(PENALTY, Penalty.PERIOD_NUMBER, Penalty.JAM_NUMBER, Penalty.CODE, Penalty.SERVING,
                      Penalty.SERVED);
        setCopy(Penalty.PERIOD_NUMBER, this, PENALTY, Penalty.PERIOD_NUMBER, true);
        setCopy(Penalty.JAM_NUMBER, this, PENALTY, Penalty.JAM_NUMBER, true);
        setCopy(Penalty.CODE, this, PENALTY, Penalty.CODE, true);
        setCopy(Penalty.SERVING, this, PENALTY, Penalty.SERVING, true);
        setCopy(Penalty.SERVED, this, PENALTY, Penalty.SERVED, true);
        addWriteProtection(PENALTY);
        set(PENALTY, p);
    }
    public CurrentPenaltyImpl(CurrentPenaltyImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new CurrentPenaltyImpl(this, root);
    }

    @Override
    public int getPeriodNumber() {
        return get(Penalty.PERIOD_NUMBER);
    }
    @Override
    public int getJamNumber() {
        return get(Penalty.JAM_NUMBER);
    }
    @Override
    public String getCode() {
        return get(Penalty.CODE);
    }
    @Override
    public boolean isServed() {
        return get(Penalty.SERVED);
    }
}
