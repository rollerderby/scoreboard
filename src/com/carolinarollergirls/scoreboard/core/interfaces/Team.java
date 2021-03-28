package com.carolinarollergirls.scoreboard.core.interfaces;
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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Managemnt of currently playing teams.
public interface Team extends ScoreBoardEventProvider, TimeoutOwner {
    public Game getGame();

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
    public static final String DISPLAY_NAME_SETTING = "ScoreBoard.Teams.DisplayName";

    Value<String> FULL_NAME = new Value<>(String.class, "FullName", "");
    Value<String> LEAGUE_NAME = new Value<>(String.class, "LeagueName", "");
    Value<String> TEAM_NAME = new Value<>(String.class, "TeamName", "");
    Value<String> DISPLAY_NAME = new Value<>(String.class, "Name", "");
    Value<String> INITIALS = new Value<>(String.class, "Initials", "");
    Value<String> UNIFORM_COLOR = new Value<>(String.class, "UniformColor", "");
    Value<String> LOGO = new Value<>(String.class, "Logo", "");
    Value<TeamJam> RUNNING_OR_UPCOMING_TEAM_JAM = new Value<>(TeamJam.class, "RunningOrUpcomingTeamJam", null);
    Value<TeamJam> RUNNING_OR_ENDED_TEAM_JAM = new Value<>(TeamJam.class, "RunningOrEndedTeamJam", null);
    Value<Boolean> FIELDING_ADVANCE_PENDING = new Value<>(Boolean.class, "FieldingAdvancePending", false);
    Value<ScoringTrip> CURRENT_TRIP = new Value<>(ScoringTrip.class, "CurrentTrip", null);
    Value<Integer> SCORE = new Value<>(Integer.class, "Score", 0);
    Value<Integer> JAM_SCORE = new Value<>(Integer.class, "JamScore", 0);
    Value<Integer> TRIP_SCORE = new Value<>(Integer.class, "TripScore", 0);
    Value<Integer> LAST_SCORE = new Value<>(Integer.class, "LastScore", 0);
    Value<Integer> TIMEOUTS = new Value<>(Integer.class, "Timeouts", 0);
    Value<Integer> OFFICIAL_REVIEWS = new Value<>(Integer.class, "OfficialReviews", 0);
    Value<Timeout> LAST_REVIEW = new Value<>(Timeout.class, "LastReview", null);
    Value<Boolean> IN_TIMEOUT = new Value<>(Boolean.class, "InTimeout", false);
    Value<Boolean> IN_OFFICIAL_REVIEW = new Value<>(Boolean.class, "InOfficialReview", false);
    Value<Boolean> NO_PIVOT = new Value<>(Boolean.class, "NoPivot", false);
    Value<Boolean> RETAINED_OFFICIAL_REVIEW = new Value<>(Boolean.class, "RetainedOfficialReview", false);
    Value<Boolean> LOST = new Value<>(Boolean.class, "Lost", false);
    Value<Boolean> LEAD = new Value<>(Boolean.class, "Lead", false);
    Value<Boolean> CALLOFF = new Value<>(Boolean.class, "Calloff", false);
    Value<Boolean> INJURY = new Value<>(Boolean.class, "Injury", false);
    Value<Boolean> NO_INITIAL = new Value<>(Boolean.class, "NoInitial", true);
    Value<Boolean> DISPLAY_LEAD = new Value<>(Boolean.class, "DisplayLead", false);
    Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false);
    Value<ScoringTrip> STAR_PASS_TRIP = new Value<>(ScoringTrip.class, "StarPassTrip", null);
    Value<PreparedTeam> PREPARED_TEAM = new Value<>(PreparedTeam.class, "PreparedTeam", null);
    Value<Boolean> PREPARED_TEAM_CONNECTED = new Value<>(Boolean.class, "PreparedTeamConnected", false);
    Value<Skater> CAPTAIN = new Value<>(Skater.class, "Captain", null);

    Child<ValWithId> ALTERNATE_NAME = new Child<>(ValWithId.class, "AlternateName");
    Child<ValWithId> COLOR = new Child<>(ValWithId.class, "Color");
    Child<Skater> SKATER = new Child<>(Skater.class, "Skater");
    Child<Position> POSITION = new Child<>(Position.class, "Position");
    Child<Timeout> TIME_OUT = new Child<>(Timeout.class, "TimeOut");
    Child<BoxTrip> BOX_TRIP = new Child<>(BoxTrip.class, "BoxTrip");

    Command ADD_TRIP = new Command("AddTrip");
    Command REMOVE_TRIP = new Command("RemoveTrip");
    Command ADVANCE_FIELDINGS = new Command("AdvanceFieldings");
    Command TIMEOUT = new Command("Timeout");
    Command OFFICIAL_REVIEW = new Command("OfficialReview");

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
