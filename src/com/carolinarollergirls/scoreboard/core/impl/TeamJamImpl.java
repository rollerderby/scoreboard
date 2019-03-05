package com.carolinarollergirls.scoreboard.core.impl;

import java.util.Arrays;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class TeamJamImpl extends ParentOrderedScoreBoardEventProviderImpl<TeamJam> implements TeamJam {
    public TeamJamImpl(Jam j, String teamId) {
        super(j, Value.ID, teamId, Jam.Child.TEAM_JAM, TeamJam.class, Value.class, Child.class);
        team = scoreBoard.getTeam(teamId);
        values.put(Value.LAST_SCORE, 0);
        values.put(Value.OS_OFFSET, 0);
        values.put(Value.JAM_SCORE, 0);
        values.put(Value.TOTAL_SCORE, 0);
        values.put(Value.NO_PIVOT, true);
        values.put(Value.STAR_PASS, false);
        values.put(Value.LEAD_JAMMER, Team.LEAD_NO_LEAD);
        addReference(new IndirectValueReference(this, Value.LAST_SCORE, this, IValue.PREVIOUS,
                Value.TOTAL_SCORE, true, 0));
        for (Value prop : Arrays.asList(Value.LAST_SCORE, Value.JAM_SCORE, Value.OS_OFFSET)) {
            addReference(new UpdateReference(this, Value.TOTAL_SCORE, this, prop));
        }
        for (ValueWithId p : team.getAll(Team.Child.POSITION)) {
            add(Child.FIELDING, new FieldingImpl(this, (Position)p));
        }
        addWriteProtection(Child.FIELDING);
    }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.TOTAL_SCORE) {
            return getLastScore() + getJamScore() + getOsOffset();
        }
        if (value instanceof Integer && prop != Value.OS_OFFSET && (Integer)value < 0) { return 0; }
        return value;
    }
    
    public Jam getJam() { return (Jam)parent; }
    public Team getTeam() { return team; }

    public boolean isRunningOrEnded() { return this == team.getRunningOrEndedTeamJam(); }
    public boolean isRunningOrUpcoming() { return this == team.getRunningOrUpcomingTeamJam(); }

    public int getLastScore() { return (Integer)get(Value.LAST_SCORE); }
    public void setLastScore(int l) { set(Value.LAST_SCORE, l); }

    public int getOsOffset() { return (Integer)get(Value.OS_OFFSET); }
    public void setOsOffset(int o) { set(Value.OS_OFFSET, o); }
    public void changeOsOffset(int c) { set(Value.OS_OFFSET, c, Flag.CHANGE); }

    public int getJamScore() { return (Integer)get(Value.JAM_SCORE); }
    public void setJamScore(int s) { set(Value.JAM_SCORE, s); }
    public void changeJamScore(int c) { set(Value.JAM_SCORE, c, Flag.CHANGE); }

    public int getTotalScore() { return (Integer)get(Value.TOTAL_SCORE); }

    public String getLeadJammer() { return (String)get(Value.LEAD_JAMMER); }
    public void setLeadJammer(String ls) { set(Value.LEAD_JAMMER, ls); }

    public boolean isStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

    public boolean hasNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
    public void setNoPivot(boolean np) { set(Value.NO_PIVOT, np); }

    public Fielding getFielding (FloorPosition fp) { return (Fielding)get(Child.FIELDING, fp.toString()); }

    private Team team;
}
