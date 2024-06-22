package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class PositionImpl extends ScoreBoardEventProviderImpl<Position> implements Position {
    public PositionImpl(Team t, FloorPosition fp) {
        super(t, t.getId() + "_" + fp.toString(), Team.POSITION);
        addProperties(props);
        floorPosition = fp;
        setCopy(NAME, this, SKATER, Skater.NAME, true);
        setCopy(ROSTER_NUMBER, this, SKATER, Skater.ROSTER_NUMBER, true);
        setCopy(FLAGS, this, SKATER, Skater.FLAGS, true);
        setCopy(SKATER, this, CURRENT_FIELDING, Fielding.SKATER, false);
        setCopy(PENALTY_BOX, this, CURRENT_FIELDING, Fielding.PENALTY_BOX, false);
        setCopy(PENALTY_TIME, this, CURRENT_FIELDING, Fielding.PENALTY_TIME, true);
        setCopy(CURRENT_BOX_SYMBOLS, this, CURRENT_FIELDING, Fielding.BOX_TRIP_SYMBOLS, true);
        setCopy(CURRENT_PENALTIES, this, SKATER, Skater.CURRENT_PENALTIES, true);
        setCopy(HAS_UNSERVED, this, SKATER, Skater.HAS_UNSERVED, true);
        setCopy(PENALTY_COUNT, this, SKATER, Skater.PENALTY_COUNT, true);
        setCopy(ANNOTATION, this, CURRENT_FIELDING, Fielding.ANNOTATION, false);
        setCopy(PENALTY_DETAILS, this, SKATER, Skater.PENALTY_DETAILS, true);
        addWriteProtectionOverride(CURRENT_FIELDING, Source.NON_WS);
    }

    @Override
    public String getProviderId() {
        return floorPosition.toString();
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == CLEAR) { set(SKATER, null); }
        if (prop == UNEND_BOX_TRIP) { getCurrentFielding().execute(Fielding.UNEND_BOX_TRIP, source); }
        if (prop == START_BOX_CLOCK) {
            Game g = getTeam().getGame();
            BoxTrip bt = new BoxTripImpl(g);
            g.add(Team.BOX_TRIP, bt);
            bt.add(BoxTrip.FIELDING, getCurrentFielding());
        }
    }

    @Override
    public Team getTeam() {
        return (Team) parent;
    }

    @Override
    public FloorPosition getFloorPosition() {
        return floorPosition;
    }

    @Override
    public void updateCurrentFielding() {
        synchronized (coreLock) {
            setCurrentFielding(getTeam().getRunningOrUpcomingTeamJam().getFielding(floorPosition));
        }
    }

    @Override
    public Skater getSkater() {
        return get(SKATER);
    }
    @Override
    public void setSkater(Skater s) {
        set(SKATER, s);
    }

    @Override
    public Fielding getCurrentFielding() {
        return get(CURRENT_FIELDING);
    }
    @Override
    public void setCurrentFielding(Fielding f) {
        set(CURRENT_FIELDING, f);
    }

    @Override
    public boolean isPenaltyBox() {
        return get(PENALTY_BOX);
    }
    @Override
    public void setPenaltyBox(boolean box) {
        set(PENALTY_BOX, box);
    }

    protected FloorPosition floorPosition;
}
