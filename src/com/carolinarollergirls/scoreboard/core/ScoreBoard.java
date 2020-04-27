package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface ScoreBoard extends ScoreBoardEventProvider {
    /** Reset the ScoreBoard. */
    public void reset();

    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    public Timeout getCurrentTimeout();

    /**
     * Id of Team who called Timeout.
     *
     * The Id is as returned from Team.getId(). For Offical Timeouts, this returns
     * the letter "O". If the type of timeout hasn't been set, it returns an empty
     * string.
     */
    public TimeoutOwner getTimeoutOwner();
    public void setTimeoutOwner(TimeoutOwner owner);

    /**
     * If this Timeout is an Official Review.
     *
     * This is true if the current timeout is actually a team-requested Official
     * Review. This is false if the current timeout is a normal team or official
     * timeout or the type of timeout hasn't been set.
     */
    public boolean isOfficialReview();
    public void setOfficialReview(boolean official);

    /**
     * If in a Period.
     *
     * This returns true if any Period has started, until the Period is over. A
     * Period is considered "started" once the first Jam starts. A Period has not
     * "ended" until its time has expired and the Jam clock has stopped. Note that
     * the Period may end and then re-start (the same Period) if Overtime is
     * started.
     */
    public boolean isInPeriod();
    public void setInPeriod(boolean inPeriod);
    public Period getOrCreatePeriod(int p);
    public Period getCurrentPeriod();
    public int getCurrentPeriodNumber();

    public boolean isInJam();
    public Jam getUpcomingJam();

    // update the references to current/upcoming/just ended TeamJams
    public void updateTeamJams();

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

    // FIXME - clock and team getters should either return null or throw exception
    // instead of creating new clock/team...
    public Clock getClock(String id);

    public Team getTeam(String id);
    public PreparedTeam getPreparedTeam(String id);

    public TimeoutOwner getTimeoutOwner(String id);

    public Settings getSettings();

    public Rulesets getRulesets();

    public PenaltyCodesManager getPenaltyCodesManager();

    public Media getMedia();

    public Clients getClients();

    Value<Integer> CURRENT_PERIOD_NUMBER = new Value<>(Integer.class, "CurrentPeriodNumber", 0);
    Value<Period> CURRENT_PERIOD = new Value<>(Period.class, "CurrentPeriod", null);
    Value<Jam> UPCOMING_JAM = new Value<>(Jam.class, "UpcomingJam", null);
    Value<Integer> UPCOMING_JAM_NUMBER = new Value<>(Integer.class, "UpcomingJamNumber", 0);
    Value<Boolean> IN_PERIOD = new Value<>(Boolean.class, "InPeriod", false);
    Value<Boolean> IN_JAM = new Value<>(Boolean.class, "InJam", false);
    Value<Boolean> IN_OVERTIME = new Value<>(Boolean.class, "InOvertime", false);
    Value<Boolean> OFFICIAL_SCORE = new Value<>(Boolean.class, "OfficialScore", false);
    Value<Timeout> CURRENT_TIMEOUT = new Value<>(Timeout.class, "CurrentTimeout", null);
    Value<TimeoutOwner> TIMEOUT_OWNER = new Value<>(TimeoutOwner.class, "TimeoutOwner", null);
    Value<Boolean> OFFICIAL_REVIEW = new Value<>(Boolean.class, "OfficialReview", false);
    Value<Boolean> NO_MORE_JAM = new Value<>(Boolean.class, "NoMoreJam", false);

    Child<ValWithId> VERSION = new Child<>(ValWithId.class, "Version");
    Child<Settings> SETTINGS = new Child<>(Settings.class, "Settings");
    Child<Twitter> TWITTER = new Child<>(Twitter.class, "Twitter");
    Child<Rulesets> RULESETS = new Child<>(Rulesets.class, "Rulesets");
    Child<PenaltyCodesManager> PENALTY_CODES = new Child<>(PenaltyCodesManager.class, "PenaltyCodes");
    Child<Media> MEDIA = new Child<>(Media.class, "Media");
    Child<Clients> CLIENTS = new Child<>(Clients.class, "Clients");
    Child<Clock> CLOCK = new Child<>(Clock.class, "Clock");
    Child<Team> TEAM = new Child<>(Team.class, "Team");
    Child<PreparedTeam> PREPARED_TEAM = new Child<>(PreparedTeam.class, "PreparedTeam");

    NumberedChild<Period> PERIOD = new NumberedChild<>(Period.class, "Period");

    Command RESET = new Command("Reset");
    Command START_JAM = new Command("StartJam");
    Command STOP_JAM = new Command("StopJam");
    Command TIMEOUT = new Command("Timeout");
    Command CLOCK_UNDO = new Command("ClockUndo");
    Command CLOCK_REPLACE = new Command("ClockReplace");
    Command START_OVERTIME = new Command("StartOvertime");
    Command OFFICIAL_TIMEOUT = new Command("OfficialTimeout");

    public static final String SETTING_CLOCK_AFTER_TIMEOUT = "ScoreBoard.ClockAfterTimeout";

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
