package com.carolinarollergirls.scoreboard.rules;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public abstract class RuleDefinition extends ScoreBoardEventProviderImpl {
    public RuleDefinition(Type type, String name, String description, Object defaultValue) {
	super(null, Value.NAME, Rulesets.Child.RULE_DEFINITION, RuleDefinition.class, Value.class);
	set(Value.TYPE, type);
	set(Value.NAME, name);
	set(Value.DEFAULT_VALUE, defaultValue.toString());
	set(Value.DESCRIPTION, description);
	for (Value prop : Value.values()) {
	    writeProtectionOverride.put(prop, null);
	}
    }
    
    public Type getType() { return (Type)get(Value.TYPE); }
    public String getName() { return (String)get(Value.NAME); }
    public String getDefaultValue() { return (String)get(Value.DEFAULT_VALUE); }
    public String getDescription() { return (String)get(Value.DESCRIPTION); }
    public int getIndex() { return (Integer)get(Value.INDEX); }
    public void setIndex(Integer i) { values.put(Value.INDEX, i); }

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
	
	public String toString() { return string; }
	
	String string;
    }
    
    public enum Value implements PermanentProperty {
	NAME,
	TYPE,
	DEFAULT_VALUE,
	DESCRIPTION,
	INDEX,
	TRUE_VALUE,
	FALSE_VALUE;
    }
}
