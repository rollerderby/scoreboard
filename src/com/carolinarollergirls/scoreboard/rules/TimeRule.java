package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.utils.ClockConversion;

public class TimeRule extends Rule {
    public TimeRule(String fullname, String description, String defaultValue) {
        super("Time", fullname, description, null);
        this.defaultValue = convertValue(defaultValue);
    }

    public Object convertValue(String v) {
        return ClockConversion.fromHumanReadable(v);
    }

    public String toHumanReadable(Object v) {
        if (v == null) {
            return "";
        }

        Object v2 = convertValue(v.toString());
        if (v2 != null) {
            return ClockConversion.toHumanReadable((Long)v2);
        }

        return v.toString();
    }
}
