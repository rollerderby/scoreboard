package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentClock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class CurrentClockImpl extends ScoreBoardEventProviderImpl<CurrentClock> implements CurrentClock {
    CurrentClockImpl(CurrentGame g, String id) {
        super(g, id, CurrentGame.CLOCK);
        addProperties(props);
        addProperties(Clock.NAME, Clock.NUMBER, Clock.TIME, Clock.INVERTED_TIME, Clock.MAXIMUM_TIME, Clock.DIRECTION,
                      Clock.RUNNING, Clock.START, Clock.STOP, Clock.RESET_TIME);
        setCopy(Clock.NAME, this, CLOCK, Clock.NAME, true);
        setCopy(Clock.NUMBER, this, CLOCK, Clock.NUMBER, true);
        setCopy(Clock.TIME, this, CLOCK, Clock.TIME, !Clock.ID_PERIOD.equals(id));
        setCopy(Clock.INVERTED_TIME, this, CLOCK, Clock.INVERTED_TIME, true);
        setCopy(Clock.MAXIMUM_TIME, this, CLOCK, Clock.MAXIMUM_TIME, true);
        setCopy(Clock.DIRECTION, this, CLOCK, Clock.DIRECTION, true);
        setCopy(Clock.RUNNING, this, CLOCK, Clock.RUNNING, true);
        addWriteProtectionOverride(CLOCK, Source.ANY_INTERNAL);
    }
    public CurrentClockImpl(CurrentClockImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new CurrentClockImpl(this, root);
    }

    @Override
    public void execute(Command prop, Source source) {
        get(CLOCK).execute(prop, source);
    }

    @Override
    public void load(Clock t) {
        set(CLOCK, t);
    }
}
