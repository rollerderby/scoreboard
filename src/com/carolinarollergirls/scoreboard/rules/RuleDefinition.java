package com.carolinarollergirls.scoreboard.rules;

import java.util.ArrayList;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class RuleDefinition extends DefaultScoreBoardEventProvider {
    public RuleDefinition(Type type, String name, String description, Object defaultValue) {
	values.put(Value.TYPE, type);
	values.put(Value.NAME, name);
	values.put(Value.DEFAULT_VALUE, defaultValue.toString());
	values.put(Value.DESCRIPTION, description);
    }

    public String getProviderName() { return PropertyConversion.toFrontend(Rulesets.Child.RULE_DEFINITION); }
    public Class<Rulesets> getProviderClass() { return Rulesets.class; }
    public String getId() { return getName(); }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	return false;
    }

    public Type getType() { return (Type)get(Value.TYPE); }
    public String getName() { return (String)get(Value.NAME); }
    public String getDefaultValue() { return (String)get(Value.DEFAULT_VALUE); }
    public String getDescription() { return (String)get(Value.DESCRIPTION); }
    public int getIndex() { return (Integer)get(Value.INDEX); }
    public void setIndex(Integer i) { values.put(Value.INDEX, i); }

    public abstract boolean isValueValid(String v);

    public void setParent(Rulesets p) { parent = p; }
    
    protected Rulesets parent;
    
    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
    }};

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
