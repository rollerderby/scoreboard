package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public interface ScoreBoard extends ScoreBoardEventProvider {
    /** Reset the ScoreBoard. */
    public void reset();

    /**
     * Id of Team who called Timeout.
     *
     * The Id is as returned from Team.getId().	 For Offical Timeouts, this returns an empty string.
     */
    public TimeoutOwner getTimeoutOwner();
    public void setTimeoutOwner(TimeoutOwner owner);

    /**
     * If this Timeout is an Official Review.
     *
     * This is true if the current timeout is actually a team-requested Official Review.
     * This is false if the current timeout is a normal team or official timeout.
     */
    public boolean isOfficialReview();
    public void setOfficialReview(boolean official);

    /**
     * If in a Period.
     *
     * This returns true if any Period has started,
     * until the Period is over.	A Period is considered
     * "started" once the first Jam starts.	 A Period has
     * not "ended" until its time has expired and the
     * Jam clock has stopped.
     * Note that the Period may end and then re-start
     * (the same Period) if Overtime is started.
     */
    public boolean isInPeriod();
    public void setInPeriod(boolean inPeriod);

    /**
     * If this bout is in Overtime.
     */
    public boolean isInOvertime();
    public void setInOvertime(boolean inOvertime);
    public void startOvertime();

    /**
     * If the score has been verified as Official.
     */
    public boolean isOfficialScore();
    public void setOfficialScore(boolean official);

    public void startJam();
    public void stopJamTO();

    public void timeout();
    public void setTimeoutType(TimeoutOwner owner, boolean review);

    public void clockUndo(boolean replace);

    public void penalty(String teamId, String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code);

// FIXME - clock and team getters should either return null or throw exception instead of creating new clock/team...
    public List<Clock> getClocks();
    public Clock getClock(String id);

    public List<Team> getTeams();
    public Team getTeam(String id);

    public TimeoutOwner getTimeoutOwner(String id);

    public Settings getSettings();

    public Rulesets getRulesets();

    public Stats getStats();

    public Media getMedia();

    public XmlScoreBoard getXmlScoreBoard();

    public static final String SETTING_CLOCK_AFTER_TIMEOUT = "ScoreBoard.ClockAfterTimeout";

    public static final String EVENT_IN_PERIOD = "InPeriod";
    public static final String EVENT_IN_OVERTIME = "InOvertime";
    public static final String EVENT_OFFICIAL_SCORE = "OfficialScore";
    public static final String EVENT_ADD_POLICY = "AddPolicy";
    public static final String EVENT_REMOVE_POLICY = "RemovePolicy";
    public static final String EVENT_TIMEOUT_OWNER = "TimeoutOwner";
    public static final String EVENT_OFFICIAL_REVIEW = "OfficialReview";
    public static final String EVENT_ADD_CLOCK = "AddClock";
    public static final String EVENT_REMOVE_CLOCK = "RemoveClock";
    public static final String EVENT_ADD_TEAM = "AddTeam";
    public static final String EVENT_REMOVE_TEAM = "RemoveTeam";
    public static final String EVENT_SETTING = "Setting";

    public static final String BUTTON_START = "ScoreBoard.Button.StartLabel";
    public static final String BUTTON_STOP = "ScoreBoard.Button.StopLabel";
    public static final String BUTTON_TIMEOUT = "ScoreBoard.Button.TimeoutLabel";
    public static final String BUTTON_UNDO = "ScoreBoard.Button.UndoLabel";
    public static final String BUTTON_REPLACED = "ScoreBoard.Button.ReplacedLabel";

    public static final String ACTION_NONE = "---";
    public static final String ACTION_NO_REPLACE = "No Action";
    public static final String ACTION_START_JAM = "Start Jam";
    public static final String ACTION_STOP_JAM = "Stop Jam";
    public static final String ACTION_STOP_TO = "End Timeout";
    public static final String ACTION_LINEUP = "Lineup";
    public static final String ACTION_TIMEOUT = "Timeout";
    public static final String ACTION_RE_TIMEOUT = "New Timeout";
    public static final String ACTION_OVERTIME = "Overtime";
    public static final String UNDO_PREFIX = "Un-";
}
