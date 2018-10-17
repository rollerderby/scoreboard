package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.utils.ClockConversion;

public class TimeRule extends Rule {
    public TimeRule(String fullname, String description, String defaultValue) {
        super("Time", fullname, description, "");
        this.defaultValue = ClockConversion.fromHumanReadable(defaultValue).toString();
    }

    public boolean isValueValid(String v) {
        return ClockConversion.fromHumanReadable(v) != null;
    }

    public String toHumanReadable(String v) {
        return ClockConversion.toHumanReadable(Long.parseLong(v));
    }
}
