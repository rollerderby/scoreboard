package com.carolinarollergirls.scoreboard.utils;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class ValWithId implements ValueWithId {
    public ValWithId(String i, String val) {
	id = i;
	value = val;
    }
    
    public String getId() { return id; }
    public String getValue() { return value; }
    
    private String id;
    private String value;
}
