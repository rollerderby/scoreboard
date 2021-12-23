package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class StringRule extends RuleDefinition {
    public StringRule(String fullname, String description, String defaultValue) {
        super(Type.STRING, fullname, description, defaultValue);
    }
    public StringRule(StringRule cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new StringRule(this, root); }

    @Override
    public boolean isValueValid(String v) {
        return v != null;
    }
}
