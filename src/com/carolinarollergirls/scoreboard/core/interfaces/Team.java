package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
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

    public void applyScoreAdjustment(ScoreAdjustment adjustment);

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
    public static final String SETTING_DISPLAY_NAME = "ScoreBoard.Teams.DisplayName";
    public static final String SETTING_FILE_NAME = "ScoreBoard.Teams.FileName";
    public static final String OPTION_TEAM_NAME = "Team";
    public static final String OPTION_LEAGUE_NAME = "League";
    public static final String OPTION_FULL_NAME = "Full";

    public static Collection<Property<?>> props = new ArrayList<>();
    public static Collection<Property<?>> preparedProps = new ArrayList<>(); // also present on PreparedTeam

    public static final Value<String> DISPLAY_NAME = new Value<>(String.class, "Name", "", preparedProps);
    public static final Value<String> FULL_NAME = new Value<>(String.class, "FullName", "", preparedProps);
    public static final Value<String> LEAGUE_NAME = new Value<>(String.class, "LeagueName", "", preparedProps);
    public static final Value<String> TEAM_NAME = new Value<>(String.class, "TeamName", "", preparedProps);
    public static final Value<String> FILE_NAME = new Value<>(String.class, "FileName", "", props);
    public static final Value<String> INITIALS = new Value<>(String.class, "Initials", "", props);
    public static final Value<String> UNIFORM_COLOR = new Value<>(String.class, "UniformColor", "", props);
    public static final Value<String> LOGO = new Value<>(String.class, "Logo", "", preparedProps);
    public static final Value<TeamJam> RUNNING_OR_UPCOMING_TEAM_JAM =
        new Value<>(TeamJam.class, "RunningOrUpcomingTeamJam", null, props);
    public static final Value<TeamJam> RUNNING_OR_ENDED_TEAM_JAM =
        new Value<>(TeamJam.class, "RunningOrEndedTeamJam", null, props);
    public static final Value<Boolean> FIELDING_ADVANCE_PENDING =
        new Value<>(Boolean.class, "FieldingAdvancePending", false, props);
    public static final Value<ScoringTrip> CURRENT_TRIP = new Value<>(ScoringTrip.class, "CurrentTrip", null, props);
    public static final Value<Integer> SCORE = new Value<>(Integer.class, "Score", 0, props);
    public static final Value<Integer> JAM_SCORE = new Value<>(Integer.class, "JamScore", 0, props);
    public static final Value<Integer> TRIP_SCORE = new Value<>(Integer.class, "TripScore", 0, props);
    public static final Value<Integer> LAST_SCORE = new Value<>(Integer.class, "LastScore", 0, props);
    public static final Value<Integer> TIMEOUTS = new Value<>(Integer.class, "Timeouts", 0, props);
    public static final Value<Integer> OFFICIAL_REVIEWS = new Value<>(Integer.class, "OfficialReviews", 0, props);
    public static final Value<Timeout> LAST_REVIEW = new Value<>(Timeout.class, "LastReview", null, props);
    public static final Value<Boolean> IN_TIMEOUT = new Value<>(Boolean.class, "InTimeout", false, props);
    public static final Value<Boolean> IN_OFFICIAL_REVIEW =
        new Value<>(Boolean.class, "InOfficialReview", false, props);
    public static final Value<Boolean> NO_PIVOT = new Value<>(Boolean.class, "NoPivot", false, props);
    public static final Value<Boolean> RETAINED_OFFICIAL_REVIEW =
        new Value<>(Boolean.class, "RetainedOfficialReview", false, props);
    public static final Value<Boolean> LOST = new Value<>(Boolean.class, "Lost", false, props);
    public static final Value<Boolean> LEAD = new Value<>(Boolean.class, "Lead", false, props);
    public static final Value<Boolean> CALLOFF = new Value<>(Boolean.class, "Calloff", false, props);
    public static final Value<Boolean> INJURY = new Value<>(Boolean.class, "Injury", false, props);
    public static final Value<Boolean> NO_INITIAL = new Value<>(Boolean.class, "NoInitial", true, props);
    public static final Value<Boolean> DISPLAY_LEAD = new Value<>(Boolean.class, "DisplayLead", false, props);
    public static final Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false, props);
    public static final Value<ScoringTrip> STAR_PASS_TRIP = new Value<>(ScoringTrip.class, "StarPassTrip", null, props);
    public static final Value<PreparedTeam> PREPARED_TEAM =
        new Value<>(PreparedTeam.class, "PreparedTeam", null, props);
    public static final Value<Boolean> PREPARED_TEAM_CONNECTED =
        new Value<>(Boolean.class, "PreparedTeamConnected", false, props);
    public static final Value<Skater> CAPTAIN = new Value<>(Skater.class, "Captain", null, props);
    public static final Value<ScoreAdjustment> ACTIVE_SCORE_ADJUSTMENT =
        new Value<>(ScoreAdjustment.class, "ActiveScoreAdjustment", null, props);
    public static final Value<Integer> ACTIVE_SCORE_ADJUSTMENT_AMOUNT =
        new Value<>(Integer.class, "ActiveScoreAdjustmentAmount", 0, props);

    public static final Child<ValWithId> ALTERNATE_NAME = new Child<>(ValWithId.class, "AlternateName", preparedProps);
    public static final Child<ValWithId> COLOR = new Child<>(ValWithId.class, "Color", preparedProps);
    public static final Child<Skater> SKATER = new Child<>(Skater.class, "Skater", props);
    public static final Child<Position> POSITION = new Child<>(Position.class, "Position", props);
    public static final Child<Timeout> TIME_OUT = new Child<>(Timeout.class, "TimeOut", props);
    public static final Child<BoxTrip> BOX_TRIP = new Child<>(BoxTrip.class, "BoxTrip", props);
    public static final Child<ScoreAdjustment> SCORE_ADJUSTMENT =
        new Child<>(ScoreAdjustment.class, "ScoreAdjustment", props);

    public static final Command ADD_TRIP = new Command("AddTrip", props);
    public static final Command REMOVE_TRIP = new Command("RemoveTrip", props);
    public static final Command ADVANCE_FIELDINGS = new Command("AdvanceFieldings", props);
    public static final Command TIMEOUT = new Command("Timeout", props);
    public static final Command OFFICIAL_REVIEW = new Command("OfficialReview", props);

    public enum AlternateNameId {
        SCOREBOARD("scoreboard"),
        WHITEBOARD("whiteboard"),
        OPERATOR("operator"),
        OVERLAY("overlay"),
        TWITTER("twitter");

        private AlternateNameId(String i) { id = i; }
        @Override
        public String toString() {
            return id;
        }

        private String id;
    }

    public static interface TeamSnapshot {
        public String getId();
        public boolean getFieldingAdvancePending();
    }
}
