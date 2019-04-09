package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.utils.ClockConversion;

public class TimeRule extends RuleDefinition {
    public TimeRule(String fullname, String description, String defaultValue) {
        super(Type.TIME, fullname, description, defaultValue);
    }

    public boolean isValueValid(String v) {
        return ClockConversion.fromHumanReadable(v) != null;
    }
}
