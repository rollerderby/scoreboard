package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;

public class TimeRule extends RuleDefinition {
    public TimeRule(String fullname, String description, String defaultValue) {
        super(Type.TIME, fullname, description, defaultValue);
    }
    public TimeRule(TimeRule cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new TimeRule(this, root); }

    @Override
    public boolean isValueValid(String v) {
        return ClockConversion.fromHumanReadable(v) != null;
    }
}
