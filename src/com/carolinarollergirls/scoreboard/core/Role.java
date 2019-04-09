package com.carolinarollergirls.scoreboard.core;

public enum Role {
    NOT_IN_GAME("NotInGame"),
    INELIGIBLE("Ineligible"),
    BENCH("Bench"),
    JAMMER("Jammer"),
    PIVOT("Pivot"),
    BLOCKER("Blocker");

    Role(String str) {
        string = str;
    }

    public String toString() { return string; }
    public static Role fromString(String s) {
        for (Role r : values()) {
            if (r.toString().equals(s)) { return r; }
        }
        return null;
    }

    private String string;
}
