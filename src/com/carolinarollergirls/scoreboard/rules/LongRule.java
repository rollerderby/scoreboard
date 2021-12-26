package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class LongRule extends RuleDefinition {
    public LongRule(String fullname, String description, int defaultValue) {
        super(Type.LONG, fullname, description, defaultValue);
    }
    public LongRule(LongRule cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new LongRule(this, root); }

    @Override
    public boolean isValueValid(String v) {
        try {
            Long.valueOf(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
