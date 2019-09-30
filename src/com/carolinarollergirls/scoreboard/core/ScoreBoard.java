package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.utils.Version;

public interface ScoreBoard extends ScoreBoardEventProvider {
    /** Reset the ScoreBoard. */
    public void reset();

    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    public Timeout getCurrentTimeout();

    /**
     * Id of Team who called Timeout.
     *
     * The Id is as returned from Team.getId().	 For Offical Timeouts, this returns the letter "O".
     * If the type of timeout hasn't been set, it returns an empty string.
     */
    public TimeoutOwner getTimeoutOwner();
    public void setTimeoutOwner(TimeoutOwner owner);

    /**
     * If this Timeout is an Official Review.
     *
     * This is true if the current timeout is actually a team-requested Official Review.
     * This is false if the current timeout is a normal team or official timeout or the type
     * of timeout hasn't been set.
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

    // FIXME - clock and team getters should either return null or throw exception instead of creating new clock/team...
    public Clock getClock(String id);

    public Team getTeam(String id);
    public PreparedTeam getPreparedTeam(String id);

    public TimeoutOwner getTimeoutOwner(String id);

    public Settings getSettings();

    public Rulesets getRulesets();

    public PenaltyCodesManager getPenaltyCodesManager();

    public Media getMedia();

    public enum Value implements PermanentProperty {
        VERSION(String.class, Version.get()),
        CURRENT_PERIOD_NUMBER(Integer.class, 0),
        CURRENT_PERIOD(Period.class, null),
        UPCOMING_JAM(Jam.class, null),
        UPCOMING_JAM_NUMBER(Integer.class, 0),
        IN_PERIOD(Boolean.class, false),
        IN_JAM(Boolean.class, false),
        IN_OVERTIME(Boolean.class, false),
        OFFICIAL_SCORE(Boolean.class, false),
        CURRENT_TIMEOUT(Timeout.class, null),
        TIMEOUT_OWNER(TimeoutOwner.class, null),
        OFFICIAL_REVIEW(Boolean.class, false),
        NO_MORE_JAM(Boolean.class, false);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        SETTINGS(Settings.class),
        TWITTER(Twitter.class),
        RULESETS(Rulesets.class),
        PENALTY_CODES(PenaltyCodesManager.class),
        MEDIA(Media.class),
        CLOCK(Clock.class),
        TEAM(Team.class),
        PREPARED_TEAM(PreparedTeam.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum NChild implements NumberedProperty {
        PERIOD(Period.class);

        private NChild(Class<? extends OrderedScoreBoardEventProvider<?>> t) { type = t; }
        private final Class<? extends OrderedScoreBoardEventProvider<?>> type;
        @Override
        public Class<? extends OrderedScoreBoardEventProvider<?>> getType() { return type; }
    }
    public enum Command implements CommandProperty {
        RESET,
        START_JAM,
        STOP_JAM,
        TIMEOUT,
        CLOCK_UNDO,
        CLOCK_REPLACE,
        START_OVERTIME,
        OFFICIAL_TIMEOUT;

        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }

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
