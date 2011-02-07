package com.carolinarollergirls.scoreboard.event;

import java.util.*;

public interface ScoreBoardListener extends EventListener
{
	public void scoreBoardChange(ScoreBoardEvent event);
}

