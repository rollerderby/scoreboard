package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class IntegerRule extends RuleDefinition {
    public IntegerRule(String fullname, String description, int defaultValue) {
        super(Type.INTEGER, fullname, description, defaultValue);
    }
    public IntegerRule(IntegerRule cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new IntegerRule(this, root);
    }

    @Override
    public boolean isValueValid(String v) {
        try {
            Integer.parseInt(v);
            return true;
        } catch (Exception e) { return false; }
    }
}
