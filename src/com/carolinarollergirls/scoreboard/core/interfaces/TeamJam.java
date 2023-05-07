package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.Value;

public interface TeamJam extends ParentOrderedScoreBoardEventProvider<TeamJam> {
    public Jam getJam();
    public Team getTeam();

    public TeamJam getOtherTeam();

    public void setupInjuryContinuation();

    public boolean isRunningOrEnded();
    public boolean isRunningOrUpcoming();

    public int getLastScore();
    public void setLastScore(int l);

    public int getOsOffset();
    public void setOsOffset(int o);
    public void changeOsOffset(int c);
    public void possiblyChangeOsOffset(int amount);
    public boolean possiblyChangeOsOffset(int amount, Jam jamRecorded, boolean recordedInJam,
                                          boolean recordedInLastTwoMins);

    public int getJamScore();
    public int getTotalScore();

    public ScoringTrip getCurrentScoringTrip();
    public void addScoringTrip();
    public void removeScoringTrip();

    public boolean isLost();
    public boolean isLead();
    public boolean isCalloff();
    public boolean isInjury();
    public boolean isDisplayLead();

    public boolean isStarPass();
    public ScoringTrip getStarPassTrip();

    public boolean hasNoPivot();
    public void setNoPivot(boolean np);

    public Fielding getFielding(FloorPosition fp);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<ScoringTrip> CURRENT_TRIP = new Value<>(ScoringTrip.class, "CurrentTrip", null, props);
    public static final Value<Integer> CURRENT_TRIP_NUMBER = new Value<>(Integer.class, "CurrentTripNumber", 0, props);
    public static final Value<Integer> LAST_SCORE = new Value<>(Integer.class, "LastScore", 0, props);
    public static final Value<Integer> OS_OFFSET = new Value<>(Integer.class, "OsOffset", 0, props);
    public static final Value<String> OS_OFFSET_REASON = new Value<>(String.class, "OsOffsetReason", "", props);
    public static final Value<Integer> JAM_SCORE = new Value<>(Integer.class, "JamScore", 0, props);
    public static final Value<Integer> AFTER_S_P_SCORE = new Value<>(Integer.class, "AfterSPScore", 0, props);
    public static final Value<Integer> TOTAL_SCORE = new Value<>(Integer.class, "TotalScore", 0, props);
    public static final Value<Boolean> LOST = new Value<>(Boolean.class, "Lost", false, props);
    public static final Value<Boolean> LEAD = new Value<>(Boolean.class, "Lead", false, props);
    public static final Value<Boolean> CALLOFF = new Value<>(Boolean.class, "Calloff", false, props);
    public static final Value<Boolean> NO_INITIAL = new Value<>(Boolean.class, "NoInitial", true, props);
    public static final Value<Boolean> INJURY = new Value<>(Boolean.class, "Injury", false, props);
    public static final Value<Boolean> DISPLAY_LEAD = new Value<>(Boolean.class, "DisplayLead", false, props);
    public static final Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false, props);
    public static final Value<ScoringTrip> STAR_PASS_TRIP = new Value<>(ScoringTrip.class, "StarPassTrip", null, props);
    public static final Value<Boolean> NO_PIVOT = new Value<>(Boolean.class, "NoPivot", false, props);

    public static final Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding", props);

    public static final NumberedChild<ScoringTrip> SCORING_TRIP =
        new NumberedChild<>(ScoringTrip.class, "ScoringTrip", props);

    public static final Command COPY_LINEUP_TO_CURRENT = new Command("CopyLineupToCurrent", props);
}
