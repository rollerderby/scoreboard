package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.Expulsion;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCode;

public class ExpulsionImpl extends ScoreBoardEventProviderImpl<Expulsion> implements Expulsion {
    ExpulsionImpl(Game g, Penalty p) {
        super(g, p.getId(), Game.EXPULSION);
        game = g;
        penalty = p;
        addProperties(props);
        setRecalculated(INFO)
            .addSource(p, Penalty.CODE)
            .addSource(p, Penalty.JAM_NUMBER)
            .addSource(p, Penalty.PERIOD_NUMBER)
            .addSource(p.getParent(), Skater.ROSTER_NUMBER)
            .addSource(p.getParent().getParent(), Team.DISPLAY_NAME);
        set(INFO, "");
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == INFO) {
            PenaltyCode code = game.get(Game.PENALTY_CODE, penalty.getCode());
            String penaltyName = code == null ? "Unknown Penalty" : code.getVerbalCues().get(0);
            value = penalty.getParent().getParent().get(Team.DISPLAY_NAME) + " #" +
                    penalty.getParent().get(Skater.ROSTER_NUMBER) + " Period " + penalty.getPeriodNumber() + " Jam " +
                    penalty.getJamNumber() + " for " + penaltyName + ".";
        }
        return value;
    }

    private Game game;
    private Penalty penalty;
}
