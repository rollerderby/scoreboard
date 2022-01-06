package com.carolinarollergirls.scoreboard.event;

import java.util.EventListener;

public interface ScoreBoardListener extends EventListener {
    public void scoreBoardChange(ScoreBoardEvent<?> event);
}
