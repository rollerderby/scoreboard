package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;

public interface Period extends ScoreBoardEventProvider {
    public void ensureAtLeastNJams(int n);
    public void truncateAfterNJams(int n);

    public int getPeriodNumber();

    public Jam getJam(int j);

    public enum Child implements AddRemoveProperty {
        JAM;
    }
}
