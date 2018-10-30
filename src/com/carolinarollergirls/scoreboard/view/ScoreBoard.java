package com.carolinarollergirls.scoreboard.view;
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
    /**
     * Id of Team who called Timeout.
     *
     * The Id is as returned from Team.getId().	 For Offical Timeouts, this returns an empty string.
     */
    public String getTimeoutOwner();

    /**
     * If this Timeout is an Official Review.
     *
     * This is true if the current timeout is actually a team-requested Official Review.
     * This is false if the current timeout is a normal team or official timeout.
     */
    public boolean isOfficialReview();

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

    /**
     * If this bout is in Overtime.
     */
    public boolean isInOvertime();

    /**
     * If the score has been verified as Official.
     */
    public boolean isOfficialScore();

// FIXME - clock and team getters should either return null or throw exception instead of creating new clock/team...
    public List<Clock> getClocks();
    public Clock getClock(String id);

    public List<Team> getTeams();
    public Team getTeam(String id);

    // Frontend (i.e. javascript only) settings.
    public FrontendSettings getFrontendSettings();

    public Rulesets getRulesets();

    public Stats getStats();

    public XmlScoreBoard getXmlScoreBoard();

    public static final String FRONTEND_SETTING_CLOCK_AFTER_TIMEOUT = "ScoreBoard.ClockAfterTimeout";

    public static final String SETTING_NUMBER_PERIODS = "Rule." + Clock.ID_PERIOD + ".Number";
    public static final String SETTING_PERIOD_DURATION = "Rule." + Clock.ID_PERIOD + ".Duration";
    public static final String SETTING_PERIOD_DIRECTION = "Rule." + Clock.ID_PERIOD + ".Direction";
    public static final String SETTING_PERIOD_END_BETWEEN_JAMS = "Rule." + Clock.ID_PERIOD + ".EndBetweenJams";
    public static final String SETTING_JAM_NUMBER_PER_PERIOD = "Rule." + Clock.ID_JAM + ".ResetNumberEachPeriod";
    public static final String SETTING_JAM_DURATION = "Rule." + Clock.ID_JAM + ".Duration";
    public static final String SETTING_JAM_DIRECTION = "Rule." + Clock.ID_JAM + ".Direction";
    public static final String SETTING_LINEUP_DURATION = "Rule." + Clock.ID_LINEUP + ".Duration";
    public static final String SETTING_OVERTIME_LINEUP_DURATION = "Rule." + Clock.ID_LINEUP + ".OvertimeDuration";
    public static final String SETTING_LINEUP_DIRECTION = "Rule." + Clock.ID_LINEUP + ".Direction";
    public static final String SETTING_TTO_DURATION = "Rule." + Clock.ID_TIMEOUT + ".TeamTODuration";
    public static final String SETTING_TIMEOUT_DIRECTION = "Rule." + Clock.ID_TIMEOUT + ".Direction";
    public static final String SETTING_STOP_PC_ON_TO = "Rule." + Clock.ID_TIMEOUT + ".StopPeriodClockAlways";
    public static final String SETTING_STOP_PC_ON_OTO = "Rule." + Clock.ID_TIMEOUT + ".StopPeriodClockOnOTO";
    public static final String SETTING_STOP_PC_ON_TTO = "Rule." + Clock.ID_TIMEOUT + ".StopPeriodClockOnTTO";
    public static final String SETTING_STOP_PC_ON_OR = "Rule." + Clock.ID_TIMEOUT + ".StopPeriodClockOnOR";
    public static final String SETTING_STOP_PC_AFTER_TO_DURATION = "Rule." + Clock.ID_TIMEOUT + ".StopPeriodClockAfterTODuration";
    public static final String SETTING_INTERMISSION_DURATIONS = "Rule." + Clock.ID_INTERMISSION + ".Durations";
    public static final String SETTING_INTERMISSION_DIRECTION = "Rule." + Clock.ID_INTERMISSION + ".Direction";
    public static final String SETTING_AUTO_START = "Rule.ClockControl.AutoStart";
    public static final String SETTING_AUTO_START_JAM = "Rule.ClockControl.AutoStartType";
    public static final String SETTING_AUTO_START_BUFFER = "Rule.ClockControl.AutoStartBuffer";
    public static final String SETTING_AUTO_END_JAM = "Rule.ClockControl.AutoEndJam";
    public static final String SETTING_AUTO_END_TTO = "Rule.ClockControl.AutoEndTTO";

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

    public static final String TIMEOUT_OWNER_OTO = "O";
    public static final String TIMEOUT_OWNER_NONE = "";

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
