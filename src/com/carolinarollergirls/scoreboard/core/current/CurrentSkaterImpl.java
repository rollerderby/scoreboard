package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.interfaces.CurrentSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class CurrentSkaterImpl extends ScoreBoardEventProviderImpl<CurrentSkater> implements CurrentSkater {
    CurrentSkaterImpl(CurrentTeam t, Skater s) {
        super(t, s.getId(), CurrentTeam.SKATER);
        addProperties(SKATER, Skater.NAME, Skater.ROSTER_NUMBER, Skater.CURRENT_BOX_SYMBOLS, Skater.ROLE,
                      Skater.BASE_ROLE, Skater.PENALTY_BOX, Skater.FLAGS);
        setCopy(Skater.NAME, this, SKATER, Skater.NAME, true);
        setCopy(Skater.ROSTER_NUMBER, this, SKATER, Skater.ROSTER_NUMBER, true);
        setCopy(Skater.CURRENT_BOX_SYMBOLS, this, SKATER, Skater.CURRENT_BOX_SYMBOLS, true);
        setCopy(Skater.ROLE, this, SKATER, Skater.ROLE, true);
        setCopy(Skater.BASE_ROLE, this, SKATER, Skater.BASE_ROLE, true);
        setCopy(Skater.PENALTY_BOX, this, SKATER, Skater.PENALTY_BOX, true);
        setCopy(Skater.FLAGS, this, SKATER, Skater.FLAGS, true);
        set(SKATER, s);
    }
    public CurrentSkaterImpl(CurrentSkaterImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new CurrentSkaterImpl(this, root);
    }

    @Override
    public int compareTo(CurrentSkater other) {
        if (other == null) { return -1; }
        if (getNumber() == other.getNumber()) { return 0; }
        if (getNumber() == null) { return 1; }
        if (other.getNumber() == null) { return -1; }
        return getNumber().compareTo(other.getNumber());
    }

    @Override
    public String getNumber() {
        return get(Skater.ROSTER_NUMBER);
    }
}
