package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.interfaces.CurrentPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class CurrentPositionImpl extends ScoreBoardEventProviderImpl<CurrentPosition> implements CurrentPosition {
    CurrentPositionImpl(CurrentTeam t, FloorPosition fp) {
        super(t, t.getId() + "_" + fp.toString(), CurrentTeam.POSITION);
        floorPosition = fp;
        addProperties(POSITION, Position.CURRENT_BOX_SYMBOLS, Position.ANNOTATION, Position.SKATER, Position.NAME,
                      Position.ROSTER_NUMBER, Position.FLAGS, Position.PENALTY_BOX, Position.CLEAR);
        setCopy(Position.CURRENT_BOX_SYMBOLS, this, POSITION, Position.CURRENT_BOX_SYMBOLS, true);
        setCopy(Position.ANNOTATION, this, POSITION, Position.ANNOTATION, true);
        setCopy(Position.SKATER, this, POSITION, Position.SKATER, true);
        setCopy(Position.NAME, this, POSITION, Position.NAME, true);
        setCopy(Position.ROSTER_NUMBER, this, POSITION, Position.ROSTER_NUMBER, true);
        setCopy(Position.FLAGS, this, POSITION, Position.FLAGS, true);
        setCopy(Position.PENALTY_BOX, this, POSITION, Position.PENALTY_BOX, true);
    }
    public CurrentPositionImpl(CurrentPositionImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new CurrentPositionImpl(this, root);
    }

    @Override
    public String getProviderId() {
        return floorPosition.toString();
    }

    @Override
    public void execute(Command prop, Source source) {
        get(POSITION).execute(prop, source);
    }

    @Override
    public void load(Position p) {
        set(POSITION, p);
    }

    @Override
    public FloorPosition getFloorPosition() {
        return floorPosition;
    }

    private FloorPosition floorPosition;
}
