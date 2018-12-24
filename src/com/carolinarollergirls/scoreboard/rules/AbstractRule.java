package com.carolinarollergirls.scoreboard.rules;

public abstract class AbstractRule {
    public AbstractRule(Type type, String fullname, String description, Object defaultValue) {
        this.type = type;
        this.fullname = fullname;
        this.defaultValue = defaultValue.toString();
        this.description = description;
    }

    public Type getType() { return type; }
    public String getFullName() { return fullname; }
    public String getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }

    public abstract boolean isValueValid(String v);

    protected Type type;
    protected String fullname;
    protected String defaultValue;
    protected String description;
    
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
}
