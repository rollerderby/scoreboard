package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentClock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class CurrentClockImpl extends ScoreBoardEventProviderImpl<CurrentClock> implements CurrentClock {
    CurrentClockImpl(CurrentGame g, String id) {
        super(g, id, CurrentGame.CLOCK);
        addProperties(CLOCK, Clock.NAME, Clock.NUMBER, Clock.TIME, Clock.INVERTED_TIME, Clock.MAXIMUM_TIME,
                Clock.DIRECTION, Clock.RUNNING, Clock.START, Clock.STOP, Clock.RESET_TIME);
        setCopy(Clock.NAME, this, CLOCK, Clock.NAME, false);
        setCopy(Clock.NUMBER, this, CLOCK, Clock.NUMBER, false);
        setCopy(Clock.TIME, this, CLOCK, Clock.TIME, false);
        setCopy(Clock.INVERTED_TIME, this, CLOCK, Clock.INVERTED_TIME, false);
        setCopy(Clock.MAXIMUM_TIME, this, CLOCK, Clock.MAXIMUM_TIME, false);
        setCopy(Clock.DIRECTION, this, CLOCK, Clock.DIRECTION, false);
        setCopy(Clock.RUNNING, this, CLOCK, Clock.RUNNING, false);
    }

    @Override
    public void execute(Command prop, Source source) { get(CLOCK).execute(prop, source); }

    @Override
    public void load(Clock t) { set(CLOCK, t); }
}
