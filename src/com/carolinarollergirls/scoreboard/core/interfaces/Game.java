package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCode;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Game extends ScoreBoardEventProvider {
    public void postAutosaveUpdate();

    public Timeout getCurrentTimeout();

    public TimeoutOwner getTimeoutOwner();
    public void setTimeoutOwner(TimeoutOwner owner);

    public boolean isOfficialReview();
    public void setOfficialReview(boolean official);

    public boolean isInPeriod();
    public void setInPeriod(boolean inPeriod);
    public Period getOrCreatePeriod(int p);
    public Period getCurrentPeriod();
    public int getCurrentPeriodNumber();

    public boolean isInJam();
    public Jam getUpcomingJam();

    // update the references to current/upcoming/just ended TeamJams
    public void updateTeamJams();

    public boolean isInOvertime();
    public void setInOvertime(boolean inOvertime);
    public void startOvertime();

    public boolean isInSuddenScoring();

    public boolean isLastTwoMinutes();

    public boolean isOfficialScore();
    public void setOfficialScore(boolean official);

    public void startJam();
    public void stopJamTO();

    public void timeout();
    public void setTimeoutType(TimeoutOwner owner, boolean review);

    public void clockUndo(boolean replace);

    public Clock getClock(String id);

    public Team getTeam(String id);

    public void setRuleset(Ruleset rs);
    // if rs is the current ruleset or an ancestor of it, refresh the current rules
    public void refreshRuleset(Ruleset rs);

    // Get information from current ruleset.
    public String get(Rule r);
    public boolean getBoolean(Rule r);
    public int getInt(Rule r);
    public long getLong(Rule r);
    public void set(Rule r, String v);

    // The last loaded ruleset.
    public Ruleset getRuleset();
    public String getRulesetName();

    public String getFilename();

    public void exportDone(boolean success);

    public enum State {
        PREPARED("Prepared"),
        RUNNING("Running"),
        FINISHED("Finished");

        State(String str) { string = str; }

        @Override
        public String toString() {
            return string;
        }
        public static State fromString(String s) {
            for (State r : values()) {
                if (r.toString().equals(s)) { return r; }
            }
            return null;
        }

        private String string;
    }

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    public static final Value<String> NAME_FORMAT = new Value<>(String.class, "NameFormat", "", props);
    public static final Value<State> STATE = new Value<>(State.class, "State", State.PREPARED, props);
    public static final Value<Integer> CURRENT_PERIOD_NUMBER =
        new Value<>(Integer.class, "CurrentPeriodNumber", 0, props);
    public static final Value<Period> CURRENT_PERIOD = new Value<>(Period.class, "CurrentPeriod", null, props);
    public static final Value<Jam> UPCOMING_JAM = new Value<>(Jam.class, "UpcomingJam", null, props);
    public static final Value<Integer> UPCOMING_JAM_NUMBER = new Value<>(Integer.class, "UpcomingJamNumber", 0, props);
    public static final Value<Boolean> IN_PERIOD = new Value<>(Boolean.class, "InPeriod", false, props);
    public static final Value<Boolean> IN_JAM = new Value<>(Boolean.class, "InJam", false, props);
    public static final Value<Boolean> IN_OVERTIME = new Value<>(Boolean.class, "InOvertime", false, props);
    public static final Value<Boolean> IN_SUDDEN_SCORING = new Value<>(Boolean.class, "InSuddenScoring", false, props);
    public static final Value<Boolean> INJURY_CONTINUATION_UPCOMING =
        new Value<>(Boolean.class, "InjuryContinuationUpcoming", false, props);
    public static final Value<Boolean> OFFICIAL_SCORE = new Value<>(Boolean.class, "OfficialScore", false, props);
    public static final Value<String> ABORT_REASON = new Value<>(String.class, "AbortReason", "", props);
    public static final Value<Timeout> CURRENT_TIMEOUT = new Value<>(Timeout.class, "CurrentTimeout", null, props);
    public static final Value<TimeoutOwner> TIMEOUT_OWNER =
        new Value<>(TimeoutOwner.class, "TimeoutOwner", null, props);
    public static final Value<Boolean> OFFICIAL_REVIEW = new Value<>(Boolean.class, "OfficialReview", false, props);
    public static final Value<Boolean> NO_MORE_JAM = new Value<>(Boolean.class, "NoMoreJam", false, props);
    public static final Value<Ruleset> RULESET = new Value<>(Ruleset.class, "Ruleset", null, props);
    public static final Value<String> RULESET_NAME = new Value<>(String.class, "RulesetName", "Custom", props);
    public static final Value<Official> HEAD_NSO = new Value<>(Official.class, "HNSO", null, props);
    public static final Value<Official> HEAD_REF = new Value<>(Official.class, "HR", null, props);
    public static final Value<String> SUSPENSIONS_SERVED = new Value<>(String.class, "SuspensionsServed", "", props);
    public static final Value<String> FILENAME =
        new Value<>(String.class, "Filename", "STATS-0000-00-00_Team1_vs_Team_2", props);
    public static final Value<String> LAST_FILE_UPDATE = new Value<>(String.class, "LastFileUpdate", "Never", props);
    public static final Value<Boolean> UPDATE_IN_PROGRESS =
        new Value<>(Boolean.class, "UpdateInProgress", false, props);
    public static final Value<Boolean> STATSBOOK_EXISTS = new Value<>(Boolean.class, "StatsbookExists", false, props);
    public static final Value<Boolean> JSON_EXISTS = new Value<>(Boolean.class, "JsonExists", false, props);
    public static final Value<Boolean> CLOCK_DURING_FINAL_SCORE =
        new Value<>(Boolean.class, "ClockDuringFinalScore", false, props);

    public static final Child<Clock> CLOCK = new Child<>(Clock.class, "Clock", props);
    public static final Child<Team> TEAM = new Child<>(Team.class, "Team", props);
    public static final Child<ValWithId> RULE = new Child<>(ValWithId.class, "Rule", props);
    public static final Child<PenaltyCode> PENALTY_CODE = new Child<>(PenaltyCode.class, "PenaltyCode", props);
    public static final Child<ValWithId> LABEL = new Child<>(ValWithId.class, "Label", props);
    public static final Child<ValWithId> EVENT_INFO = new Child<>(ValWithId.class, "EventInfo", props);
    public static final Child<Official> NSO = new Child<>(Official.class, "Nso", props);
    public static final Child<Official> REF = new Child<>(Official.class, "Ref", props);
    public static final Child<Expulsion> EXPULSION = new Child<>(Expulsion.class, "Expulsion", props);

    public static final NumberedChild<Period> PERIOD = new NumberedChild<>(Period.class, "Period", props);

    public static final Command START_JAM = new Command("StartJam", props);
    public static final Command STOP_JAM = new Command("StopJam", props);
    public static final Command TIMEOUT = new Command("Timeout", props);
    public static final Command CLOCK_UNDO = new Command("ClockUndo", props);
    public static final Command CLOCK_REPLACE = new Command("ClockReplace", props);
    public static final Command START_OVERTIME = new Command("StartOvertime", props);
    public static final Command OFFICIAL_TIMEOUT = new Command("OfficialTimeout", props);
    public static final Command EXPORT = new Command("Export", props);

    public static final String SETTING_DEFAULT_NAME_FORMAT = "ScoreBoard.Game.DefaultNameFormat";

    public static final String INFO_VENUE = "Venue";
    public static final String INFO_CITY = "City";
    public static final String INFO_STATE = "State";
    public static final String INFO_TOURNAMENT = "Tournament";
    public static final String INFO_HOST = "HostLeague";
    public static final String INFO_GAME_NUMBER = "GameNo";
    public static final String INFO_DATE = "Date";
    public static final String INFO_START_TIME = "StartTime";

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
