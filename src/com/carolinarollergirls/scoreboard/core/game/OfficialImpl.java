package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public class OfficialImpl extends ScoreBoardEventProviderImpl<Official> implements Official {
    public OfficialImpl(Game g, String id, Child<Official> type) {
        super(g, id, type);
        game = g;
        addProperties(ROLE, NAME, LEAGUE, CERT, P1_TEAM, SWAP);
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == ROLE && value != null) {
            String role = (String) value;
            if (ROLE_HR.equals(role)) { game.set(Game.HEAD_REF, this); }
            if (ROLE_PLT.equals(role) || ROLE_LT.equals(role) || ROLE_SK.equals(role) || ROLE_JR.equals(role)) {
                for (Official other : game.getAll(ownType)) {
                    if (other != this && role.equals(other.get(ROLE))) {
                        set(SWAP, other.get(SWAP));
                        Team ot = other.get(P1_TEAM);
                        if (ot != null) {
                            set(P1_TEAM, ot.getOtherTeam());
                        }
                        return;
                    }
                }
                // first Official with this position
                set(SWAP, ROLE_SK.equals(role) || ROLE_JR.equals(role));
            }
        }
        if (prop == SWAP && get(ROLE) != null) {
            for (Official other : game.getAll(ownType)) {
                if (other != this && get(ROLE).equals(other.get(ROLE))) {
                    other.set(SWAP, (Boolean) value);
                }
            }
        }
        if (prop == P1_TEAM && value != null && !"".equals(get(ROLE))) {
            Team t = (Team) value;
            for (Official other : game.getAll(ownType)) {
                if (other != this && get(ROLE).equals(other.get(ROLE))) {
                    other.set(P1_TEAM, t.getOtherTeam());
                }
            }
        }
    }

    private Game game;
}
