package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
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

    Value<String> NAME = new Value<>(String.class, "Name", "");
    Value<String> NAME_FORMAT = new Value<>(String.class, "NameFormat", "%1 - %2");
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
    Value<Ruleset> RULESET = new Value<>(Ruleset.class, "Ruleset", null);
    Value<String> RULESET_NAME = new Value<>(String.class, "RulesetName", "");

    Child<Clock> CLOCK = new Child<>(Clock.class, "Clock");
    Child<Team> TEAM = new Child<>(Team.class, "Team");
    Child<ValWithId> RULE = new Child<>(ValWithId.class, "Rule");
    Child<PenaltyCode> PENALTY_CODE = new Child<>(PenaltyCode.class, "PenaltyCode");
    Child<ValWithId> LABEL = new Child<>(ValWithId.class, "Label");

    NumberedChild<Period> PERIOD = new NumberedChild<>(Period.class, "Period");

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
