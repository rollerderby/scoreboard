package com.carolinarollergirls.scoreboard.core;

public enum FloorPosition {
    JAMMER(Role.JAMMER, "Jammer"),
    PIVOT(Role.PIVOT, "Pivot"),
    BLOCKER1(Role.BLOCKER, "Blocker1"),
    BLOCKER2(Role.BLOCKER, "Blocker2"),
    BLOCKER3(Role.BLOCKER, "Blocker3");
    
    FloorPosition(Role r, String str) {
	role = r;
	string = str;
    }
    
    public Role getRole() { return role; }
    public Role getRole(Team team) {
	if (role == Role.PIVOT && team.isStarPass()) {
	    return Role.JAMMER;
	} else if (role == Role.JAMMER && team.isStarPass() ||
		role == Role.PIVOT && team.hasNoPivot()) {
	    return Role.BLOCKER;
	} else {
	    return role; 
	}
    }
    
    public String toString() { return string; }
    public static FloorPosition fromString(String s) {
	for (FloorPosition fp : values()) {
	    if (fp.toString().equals(s)) { return fp; }
	}
	return null;
    }
    
    private Role role;
    private String string;
}
