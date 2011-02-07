package com.carolinarollergirls.scoreboard.event;

public interface ScoreBoardEventProvider
{
	public String getProviderName();
	public Class getProviderClass();

	public void addScoreBoardListener(ScoreBoardListener listener);
	public void removeScoreBoardListener(ScoreBoardListener listener);
}
