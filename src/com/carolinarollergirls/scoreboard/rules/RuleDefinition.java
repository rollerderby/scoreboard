package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public abstract class RuleDefinition extends ScoreBoardEventProviderImpl<RuleDefinition> {
    public RuleDefinition(Type type, String name, String description, Object defaultValue) {
        super(null, name, Rulesets.RULE_DEFINITION);
        addProperties(NAME, TYPE, DEFAULT_VALUE, DESCRIPTION, INDEX);
        setCopy(NAME, this, ID, true);
        set(TYPE, type);
        set(DEFAULT_VALUE, defaultValue.toString());
        set(DESCRIPTION, description);
        for (Property<?> prop : getProperties()) {
            addWriteProtection(prop);
        }
    }

    public Type getType() { return get(TYPE); }
    public String getName() { return get(NAME); }
    public String getDefaultValue() { return get(DEFAULT_VALUE); }
    public String getDescription() { return get(DESCRIPTION); }
    public int getIndex() { return get(INDEX); }
    public void setIndex(Integer i) { values.put(INDEX, i); }

    public abstract boolean isValueValid(String v);

    public void setParent(Rulesets p) {
        parent = p;
        scoreBoard = parent.getScoreBoard();
    }

    public enum Type {
        BOOLEAN("Boolean"),
        INTEGER("Integer"),
        LONG("Long"),
        STRING("String"),
        TIME("Time");

        private Type(String s) { string = s; }

        @Override
        public String toString() { return string; }

        String string;
    }

    Value<String> NAME = new Value<>(String.class, "Name", "");
    Value<Type> TYPE = new Value<>(Type.class, "Type", null);
    Value<String> DEFAULT_VALUE = new Value<>(String.class, "DefaultValue", "");
    Value<String> DESCRIPTION = new Value<>(String.class, "Description", "");
    Value<Integer> INDEX = new Value<>(Integer.class, "Index", 0);
    Value<String> TRUE_VALUE = new Value<>(String.class, "TrueValue", "");
    Value<String> FALSE_VALUE = new Value<>(String.class, "FalseValue", "");
}
