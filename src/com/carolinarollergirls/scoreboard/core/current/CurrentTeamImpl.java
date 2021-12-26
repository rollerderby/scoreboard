package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.IndirectScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;

public class CurrentTeamImpl extends ScoreBoardEventProviderImpl<CurrentTeam> implements CurrentTeam {
    public CurrentTeamImpl(CurrentGame g, String id) {
        super(g, id, CurrentGame.TEAM);
        addProperties(TEAM, Team.DISPLAY_NAME, Team.INITIALS, Team.LOGO, Team.FIELDING_ADVANCE_PENDING, Team.SCORE,
                      Team.JAM_SCORE, Team.TRIP_SCORE, Team.LAST_SCORE, Team.TIMEOUTS, Team.OFFICIAL_REVIEWS,
                      Team.IN_TIMEOUT, Team.IN_OFFICIAL_REVIEW, Team.NO_PIVOT, Team.RETAINED_OFFICIAL_REVIEW, Team.LOST,
                      Team.LEAD, Team.CALLOFF, Team.INJURY, Team.NO_INITIAL, Team.DISPLAY_LEAD, Team.STAR_PASS,
                      Team.ALTERNATE_NAME, Team.COLOR, SKATER, POSITION, Team.ADD_TRIP, Team.REMOVE_TRIP,
                      Team.ADVANCE_FIELDINGS, Team.TIMEOUT, Team.OFFICIAL_REVIEW);
        setCopy(Team.DISPLAY_NAME, this, TEAM, Team.DISPLAY_NAME, true);
        setCopy(Team.INITIALS, this, TEAM, Team.INITIALS, true);
        setCopy(Team.LOGO, this, TEAM, Team.LOGO, true);
        setCopy(Team.FIELDING_ADVANCE_PENDING, this, TEAM, Team.FIELDING_ADVANCE_PENDING, true);
        setCopy(Team.SCORE, this, TEAM, Team.SCORE, true);
        setCopy(Team.JAM_SCORE, this, TEAM, Team.JAM_SCORE, true);
        setCopy(Team.TRIP_SCORE, this, TEAM, Team.TRIP_SCORE, true);
        setCopy(Team.LAST_SCORE, this, TEAM, Team.LAST_SCORE, true);
        setCopy(Team.TIMEOUTS, this, TEAM, Team.TIMEOUTS, true);
        setCopy(Team.OFFICIAL_REVIEWS, this, TEAM, Team.OFFICIAL_REVIEWS, true);
        setCopy(Team.IN_TIMEOUT, this, TEAM, Team.IN_TIMEOUT, true);
        setCopy(Team.IN_OFFICIAL_REVIEW, this, TEAM, Team.IN_OFFICIAL_REVIEW, true);
        setCopy(Team.NO_PIVOT, this, TEAM, Team.NO_PIVOT, true);
        setCopy(Team.RETAINED_OFFICIAL_REVIEW, this, TEAM, Team.RETAINED_OFFICIAL_REVIEW, true);
        setCopy(Team.LOST, this, TEAM, Team.LOST, true);
        setCopy(Team.LEAD, this, TEAM, Team.LEAD, true);
        setCopy(Team.CALLOFF, this, TEAM, Team.CALLOFF, true);
        setCopy(Team.INJURY, this, TEAM, Team.INJURY, true);
        setCopy(Team.NO_INITIAL, this, TEAM, Team.NO_INITIAL, true);
        setCopy(Team.DISPLAY_LEAD, this, TEAM, Team.DISPLAY_LEAD, true);
        setCopy(Team.STAR_PASS, this, TEAM, Team.STAR_PASS, true);
        setCopy(Team.ALTERNATE_NAME, this, TEAM, Team.ALTERNATE_NAME, true);
        setCopy(Team.COLOR, this, TEAM, Team.COLOR, true);
        for (FloorPosition fp : FloorPosition.values()) { add(POSITION, new CurrentPositionImpl(this, fp)); }
        addWriteProtection(POSITION);
        providers.put(skaterListener, null);
    }
    public CurrentTeamImpl(CurrentTeamImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new CurrentTeamImpl(this, root);
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == TEAM) {
            if (value != null) {
                Team t = (Team) value;
                for (CurrentPosition p : getAll(POSITION)) { p.load(t.getPosition(p.getFloorPosition())); }
            }
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == SKATER && !source.isFile()) {
                get(TEAM).getOrCreate(Team.SKATER, id);
                return get(SKATER, id);
            }
            return null;
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == SKATER) {
            CurrentSkater s = ((CurrentSkater) item);
            get(TEAM).remove(Team.SKATER, s.get(CurrentSkater.SKATER));
            s.delete();
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        get(TEAM).execute(prop, source);
    }

    @Override
    public void load(Team t) {
        set(TEAM, t);
    }

    @Override
    public CurrentPosition getPosition(FloorPosition fp) {
        return get(POSITION, fp.toString());
    }

    protected ScoreBoardListener skaterListener =
        new IndirectScoreBoardListener<>(this, TEAM, Team.SKATER, new ScoreBoardListener() {
            @Override
            public void scoreBoardChange(ScoreBoardEvent<?> event) {
                Skater s = (Skater) event.getValue();
                if (event.isRemove()) {
                    remove(SKATER, s.getCurrentSkater());
                } else {
                    add(SKATER, new CurrentSkaterImpl(CurrentTeamImpl.this, s));
                }
            }
        });
}
