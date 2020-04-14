package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
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

    PermanentProperty<Integer> CURRENT_PERIOD_NUMBER = new PermanentProperty<>(Integer.class, "CurrentPeriodNumber", 0);
    PermanentProperty<Period> CURRENT_PERIOD = new PermanentProperty<>(Period.class, "CurrentPeriod", null);
    PermanentProperty<Jam> UPCOMING_JAM = new PermanentProperty<>(Jam.class, "UpcomingJam", null);
    PermanentProperty<Integer> UPCOMING_JAM_NUMBER = new PermanentProperty<>(Integer.class, "UpcomingJamNumber", 0);
    PermanentProperty<Boolean> IN_PERIOD = new PermanentProperty<>(Boolean.class, "InPeriod", false);
    PermanentProperty<Boolean> IN_JAM = new PermanentProperty<>(Boolean.class, "InJam", false);
    PermanentProperty<Boolean> IN_OVERTIME = new PermanentProperty<>(Boolean.class, "InOvertime", false);
    PermanentProperty<Boolean> OFFICIAL_SCORE = new PermanentProperty<>(Boolean.class, "OfficialScore", false);
    PermanentProperty<Timeout> CURRENT_TIMEOUT = new PermanentProperty<>(Timeout.class, "CurrentTimeout", null);
    PermanentProperty<TimeoutOwner> TIMEOUT_OWNER = new PermanentProperty<>(TimeoutOwner.class, "TimeoutOwner", null);
    PermanentProperty<Boolean> OFFICIAL_REVIEW = new PermanentProperty<>(Boolean.class, "OfficialReview", false);
    PermanentProperty<Boolean> NO_MORE_JAM = new PermanentProperty<>(Boolean.class, "NoMoreJam", false);

    // @formatter:off
    AddRemoveProperty<ValWithId> VERSION = new AddRemoveProperty<>(ValWithId.class, "Version");
    AddRemoveProperty<Settings> SETTINGS = new AddRemoveProperty<>(Settings.class, "Settings");
    AddRemoveProperty<Twitter> TWITTER = new AddRemoveProperty<>(Twitter.class, "Twitter");
    AddRemoveProperty<Rulesets> RULESETS = new AddRemoveProperty<>(Rulesets.class, "Rulesets");
    AddRemoveProperty<PenaltyCodesManager> PENALTY_CODES = new AddRemoveProperty<>(PenaltyCodesManager.class, "PenaltyCodes");
    AddRemoveProperty<Media> MEDIA = new AddRemoveProperty<>(Media.class, "Media");
    AddRemoveProperty<Clients> CLIENTS = new AddRemoveProperty<>(Clients.class, "Clients");
    AddRemoveProperty<Clock> CLOCK = new AddRemoveProperty<>(Clock.class, "Clock");
    AddRemoveProperty<Team> TEAM = new AddRemoveProperty<>(Team.class, "Team");
    AddRemoveProperty<PreparedTeam> PREPARED_TEAM = new AddRemoveProperty<>(PreparedTeam.class, "PreparedTeam");
    // @formatter:on

    NumberedProperty<Period> PERIOD = new NumberedProperty<>(Period.class, "Period");

    CommandProperty RESET = new CommandProperty("Reset");
    CommandProperty START_JAM = new CommandProperty("StartJam");
    CommandProperty STOP_JAM = new CommandProperty("StopJam");
    CommandProperty TIMEOUT = new CommandProperty("Timeout");
    CommandProperty CLOCK_UNDO = new CommandProperty("ClockUndo");
    CommandProperty CLOCK_REPLACE = new CommandProperty("ClockReplace");
    CommandProperty START_OVERTIME = new CommandProperty("StartOvertime");
    CommandProperty OFFICIAL_TIMEOUT = new CommandProperty("OfficialTimeout");

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
