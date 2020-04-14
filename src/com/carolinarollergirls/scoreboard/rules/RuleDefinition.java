package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public abstract class RuleDefinition extends ScoreBoardEventProviderImpl<RuleDefinition> {
    public RuleDefinition(Type type, String name, String description, Object defaultValue) {
        super(null, name, Rulesets.RULE_DEFINITION);
        addProperties(NAME, TYPE, DEFAULT_VALUE, DESCRIPTION, INDEX);
        setCopy(NAME, this, ID, true);
        set(TYPE, type);
        set(DEFAULT_VALUE, defaultValue.toString());
        set(DESCRIPTION, description);
        for (Property<?> prop : properties) {
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

    PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");
    PermanentProperty<Type> TYPE = new PermanentProperty<>(Type.class, "Type", null);
    PermanentProperty<String> DEFAULT_VALUE = new PermanentProperty<>(String.class, "DefaultValue", "");
    PermanentProperty<String> DESCRIPTION = new PermanentProperty<>(String.class, "Description", "");
    PermanentProperty<Integer> INDEX = new PermanentProperty<>(Integer.class, "Index", 0);
    PermanentProperty<String> TRUE_VALUE = new PermanentProperty<>(String.class, "TrueValue", "");
    PermanentProperty<String> FALSE_VALUE = new PermanentProperty<>(String.class, "FalseValue", "");
}
