package com.carolinarollergirls.scoreboard.event;

public interface SelfRemovingScoreBoardListener extends ScoreBoardListener {
    public void delete();
}
