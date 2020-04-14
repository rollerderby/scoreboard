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
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Managemnt of currently playing teams.
public interface Team extends ScoreBoardEventProvider, TimeoutOwner {
    public void reset();

    public String getName();
    public void setName(String name);

    public void startJam();
    public void stopJam();
    public TeamSnapshot snapshot();
    public void restoreSnapshot(TeamSnapshot s);

    public String getAlternateName(String id);
    public String getAlternateName(AlternateNameId id);
    public void setAlternateName(String id, String name);
    public void removeAlternateName(String id);

    public String getColor(String id);
    public void setColor(String id, String color);
    public void removeColor(String id);

    public String getLogo();
    public void setLogo(String logo);

    public void loadPreparedTeam(PreparedTeam pt);

    public void timeout();
    public void officialReview();

    public TeamJam getRunningOrUpcomingTeamJam();
    public TeamJam getRunningOrEndedTeamJam();
    public void updateTeamJams();

    public int getScore();

    public ScoringTrip getCurrentTrip();

    public int getTimeouts();
    public int getOfficialReviews();

    public boolean inTimeout();
    public boolean inOfficialReview();
    public boolean retainedOfficialReview();
    public void setRetainedOfficialReview(boolean retained_official_review);
    public void recountTimeouts();

    public Skater getSkater(String id);
    public void addSkater(Skater skater);
    public void removeSkater(String id);

    public Position getPosition(FloorPosition fp);

    public void field(Skater s, Role r);
    public void field(Skater s, Role r, TeamJam tj);
    public boolean hasFieldingAdvancePending();

    public boolean isLost();
    public boolean isLead();
    public boolean isCalloff();
    public boolean isInjury();
    public boolean isDisplayLead();
    public boolean isStarPass();
    public boolean hasNoPivot();

    public Team getOtherTeam();

    public static final String ID_1 = "1";
    public static final String ID_2 = "2";

    // @formatter:off
    PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");
    PermanentProperty<String> LOGO = new PermanentProperty<>(String.class, "Logo", "");
    PermanentProperty<TeamJam> RUNNING_OR_UPCOMING_TEAM_JAM = new PermanentProperty<>(TeamJam.class, "RunningOrUpcomingTeamJam", null);
    PermanentProperty<TeamJam> RUNNING_OR_ENDED_TEAM_JAM = new PermanentProperty<>(TeamJam.class, "RunningOrEndedTeamJam", null);
    PermanentProperty<Boolean> FIELDING_ADVANCE_PENDING = new PermanentProperty<>(Boolean.class, "FieldingAdvancePending", false);
    PermanentProperty<ScoringTrip> CURRENT_TRIP = new PermanentProperty<>(ScoringTrip.class, "CurrentTrip", null);
    PermanentProperty<Integer> SCORE = new PermanentProperty<>(Integer.class, "Score", 0);
    PermanentProperty<Integer> JAM_SCORE = new PermanentProperty<>(Integer.class, "JamScore", 0);
    PermanentProperty<Integer> TRIP_SCORE = new PermanentProperty<>(Integer.class, "TripScore", 0);
    PermanentProperty<Integer> LAST_SCORE = new PermanentProperty<>(Integer.class, "LastScore", 0);
    PermanentProperty<Integer> TIMEOUTS = new PermanentProperty<>(Integer.class, "Timeouts", 0);
    PermanentProperty<Integer> OFFICIAL_REVIEWS = new PermanentProperty<>(Integer.class, "OfficialReviews", 0);
    PermanentProperty<Timeout> LAST_REVIEW = new PermanentProperty<>(Timeout.class, "LastReview", null);
    PermanentProperty<Boolean> IN_TIMEOUT = new PermanentProperty<>(Boolean.class, "InTimeout", false);
    PermanentProperty<Boolean> IN_OFFICIAL_REVIEW = new PermanentProperty<>(Boolean.class, "InOfficialReview", false);
    PermanentProperty<Boolean> NO_PIVOT = new PermanentProperty<>(Boolean.class, "NoPivot", false);
    PermanentProperty<Boolean> RETAINED_OFFICIAL_REVIEW = new PermanentProperty<>(Boolean.class, "RetainedOfficialReview", false);
    PermanentProperty<Boolean> LOST = new PermanentProperty<>(Boolean.class, "Lost", false);
    PermanentProperty<Boolean> LEAD = new PermanentProperty<>(Boolean.class, "Lead", false);
    PermanentProperty<Boolean> CALLOFF = new PermanentProperty<>(Boolean.class, "Calloff", false);
    PermanentProperty<Boolean> INJURY = new PermanentProperty<>(Boolean.class, "Injury", false);
    PermanentProperty<Boolean> NO_INITIAL = new PermanentProperty<>(Boolean.class, "NoInitial", true);
    PermanentProperty<Boolean> DISPLAY_LEAD = new PermanentProperty<>(Boolean.class, "DisplayLead", false);
    PermanentProperty<Boolean> STAR_PASS = new PermanentProperty<>(Boolean.class, "StarPass", false);
    PermanentProperty<ScoringTrip> STAR_PASS_TRIP = new PermanentProperty<>(ScoringTrip.class, "StarPassTrip", null);

    AddRemoveProperty<ValWithId> ALTERNATE_NAME = new AddRemoveProperty<>(ValWithId.class, "AlternateName");
    AddRemoveProperty<ValWithId> COLOR = new AddRemoveProperty<>(ValWithId.class, "Color");
    AddRemoveProperty<Skater> SKATER = new AddRemoveProperty<>(Skater.class, "Skater");
    AddRemoveProperty<Position> POSITION = new AddRemoveProperty<>(Position.class, "Position");
    AddRemoveProperty<Timeout> TIME_OUT = new AddRemoveProperty<>(Timeout.class, "TimeOut");
    AddRemoveProperty<BoxTrip> BOX_TRIP = new AddRemoveProperty<>(BoxTrip.class, "BoxTrip");

    CommandProperty ADD_TRIP = new CommandProperty("AddTrip");
    CommandProperty REMOVE_TRIP = new CommandProperty("RemoveTrip");
    CommandProperty ADVANCE_FIELDINGS = new CommandProperty("AdvanceFieldings");
    CommandProperty TIMEOUT = new CommandProperty("Timeout");
    CommandProperty OFFICIAL_REVIEW = new CommandProperty("OfficialReview");
    // @formatter:on

    public enum AlternateNameId {
        OPERATOR("operator"),
        OVERLAY("overlay"),
        TWITTER("twitter");

        private AlternateNameId(String i) { id = i; }
        @Override
        public String toString() { return id; }

        private String id;
    }

    public static interface TeamSnapshot {
        public String getId();
        public boolean getFieldingAdvancePending();
    }
}
