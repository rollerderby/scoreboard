package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;

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

    // @formatter:off
    PermanentProperty<ScoringTrip> CURRENT_TRIP = new PermanentProperty<>(ScoringTrip.class, "CurrentTrip", null);
    PermanentProperty<Integer> CURRENT_TRIP_NUMBER = new PermanentProperty<>(Integer.class, "CurrentTripNumber", 0);
    PermanentProperty<Integer> LAST_SCORE = new PermanentProperty<>(Integer.class, "LastScore", 0);
    PermanentProperty<Integer> OS_OFFSET = new PermanentProperty<>(Integer.class, "OsOffset", 0);
    PermanentProperty<Integer> JAM_SCORE = new PermanentProperty<>(Integer.class, "JamScore", 0);
    PermanentProperty<Integer> AFTER_S_P_SCORE = new PermanentProperty<>(Integer.class, "AfterSPScore", 0);
    PermanentProperty<Integer> TOTAL_SCORE = new PermanentProperty<>(Integer.class, "TotalScore", 0);
    PermanentProperty<Boolean> LOST = new PermanentProperty<>(Boolean.class, "Lost", false);
    PermanentProperty<Boolean> LEAD = new PermanentProperty<>(Boolean.class, "Lead", false);
    PermanentProperty<Boolean> CALLOFF = new PermanentProperty<>(Boolean.class, "Calloff", false);
    PermanentProperty<Boolean> NO_INITIAL = new PermanentProperty<>(Boolean.class, "NoInitial", true);
    PermanentProperty<Boolean> INJURY = new PermanentProperty<>(Boolean.class, "Injury", false);
    PermanentProperty<Boolean> DISPLAY_LEAD = new PermanentProperty<>(Boolean.class, "DisplayLead", false);
    PermanentProperty<Boolean> STAR_PASS = new PermanentProperty<>(Boolean.class, "StarPass", false);
    PermanentProperty<ScoringTrip> STAR_PASS_TRIP = new PermanentProperty<>(ScoringTrip.class, "StarPassTrip", null);
    PermanentProperty<Boolean> NO_PIVOT = new PermanentProperty<>(Boolean.class, "NoPivot", false);

    AddRemoveProperty<Fielding> FIELDING = new AddRemoveProperty<>(Fielding.class, "Fielding");

    NumberedProperty<ScoringTrip> SCORING_TRIP = new NumberedProperty<>(ScoringTrip.class, "ScoringTrip");
    // @formatter:on
}
