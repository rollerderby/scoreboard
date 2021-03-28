package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface TeamJam extends ParentOrderedScoreBoardEventProvider<TeamJam> {
    public Jam getJam();
    public Team getTeam();

    public TeamJam getOtherTeam();

    public boolean isRunningOrEnded();
    public boolean isRunningOrUpcoming();

    public int getLastScore();
    public void setLastScore(int l);

    public int getOsOffset();
    public void setOsOffset(int o);
    public void changeOsOffset(int c);

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

    Value<ScoringTrip> CURRENT_TRIP = new Value<>(ScoringTrip.class, "CurrentTrip", null);
    Value<Integer> CURRENT_TRIP_NUMBER = new Value<>(Integer.class, "CurrentTripNumber", 0);
    Value<Integer> LAST_SCORE = new Value<>(Integer.class, "LastScore", 0);
    Value<Integer> OS_OFFSET = new Value<>(Integer.class, "OsOffset", 0);
    Value<Integer> OS_OFFSET_REASON = new Value<>(Integer.class, "OsOffsetReason", 0);
    Value<Integer> JAM_SCORE = new Value<>(Integer.class, "JamScore", 0);
    Value<Integer> AFTER_S_P_SCORE = new Value<>(Integer.class, "AfterSPScore", 0);
    Value<Integer> TOTAL_SCORE = new Value<>(Integer.class, "TotalScore", 0);
    Value<Boolean> LOST = new Value<>(Boolean.class, "Lost", false);
    Value<Boolean> LEAD = new Value<>(Boolean.class, "Lead", false);
    Value<Boolean> CALLOFF = new Value<>(Boolean.class, "Calloff", false);
    Value<Boolean> NO_INITIAL = new Value<>(Boolean.class, "NoInitial", true);
    Value<Boolean> INJURY = new Value<>(Boolean.class, "Injury", false);
    Value<Boolean> DISPLAY_LEAD = new Value<>(Boolean.class, "DisplayLead", false);
    Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false);
    Value<ScoringTrip> STAR_PASS_TRIP = new Value<>(ScoringTrip.class, "StarPassTrip", null);
    Value<Boolean> NO_PIVOT = new Value<>(Boolean.class, "NoPivot", false);

    Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding");

    NumberedChild<ScoringTrip> SCORING_TRIP = new NumberedChild<>(ScoringTrip.class, "ScoringTrip");
}
