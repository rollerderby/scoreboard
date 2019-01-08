package com.carolinarollergirls.scoreboard.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class JamImpl extends DefaultScoreBoardEventProvider implements Jam {
    public JamImpl(Period p, int j) {
        period = p;
        jam = j;
        children.put(Child.TEAM_JAM, new HashMap<String, ValueWithId>());
        add(Child.TEAM_JAM, new TeamJamImpl(Team.ID_1, this));
        add(Child.TEAM_JAM, new TeamJamImpl(Team.ID_2, this));
    }

    public String getProviderName() { return PropertyConversion.toFrontend(Period.Child.JAM); }
    public Class<Jam> getProviderClass() { return Jam.class; }
    public String getId() { return String.valueOf(jam); }
    public ScoreBoardEventProvider getParent() { return period; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public Object valueFromString(PermanentProperty prop, String sValue) {
        if (sValue == null) return null;
        return Long.parseLong(sValue);
    }
    
    public int getPeriodNumber() { return period.getPeriodNumber(); }
    public int getJamNumber() { return jam; }

    public long getJamClockElapsedEnd() { return (Long)get(Value.JAM_CLOCK_ELAPSED_END); }
    public void setJamClockElapsedEnd(long t) { set(Value.JAM_CLOCK_ELAPSED_END, t); }

    public long getPeriodClockElapsedStart() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_START); }
    public void setPeriodClockElapsedStart(long t) { set(Value.PERIOD_CLOCK_ELAPSED_START, t); }

    public long getPeriodClockElapsedEnd() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_END); }
    public void setPeriodClockElapsedEnd(long t) { set(Value.PERIOD_CLOCK_ELAPSED_END, t); }

    public long getPeriodClockWalltimeStart() { return (Long)get(Value.PERIOD_CLOCK_WALLTIME_START); }
    public void setPeriodClockWalltimeStart(long t) { set(Value.PERIOD_CLOCK_WALLTIME_START, t); }

    public long getPeriodClockWalltimeEnd() { return (Long)get(Value.PERIOD_CLOCK_WALLTIME_END); }
    public void setPeriodClockWalltimeEnd(long t) { set(Value.PERIOD_CLOCK_WALLTIME_END, t); }

    public TeamJam getTeamJam(String id) { return (TeamJam)get(Child.TEAM_JAM, id); }

    private Period period;
    private int jam;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
        add(Value.class);
        add(Child.class);
    }};
}
