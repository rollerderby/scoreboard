package com.carolinarollergirls.scoreboard.core.prepared;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.OfficialsCrew;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public final class OfficialsCrewImpl extends ScoreBoardEventProviderImpl<OfficialsCrew> implements OfficialsCrew {
    public OfficialsCrewImpl(ScoreBoard parent, String id) {
        super(parent, id, ScoreBoard.OFFICIALS_CREW);
        addProperties(props);
    }
    public OfficialsCrewImpl(Game game) {
        super(game.getParent(), UUID.randomUUID().toString(), ScoreBoard.OFFICIALS_CREW);
        addProperties(props);
        set(NAME, game.get(Game.NAME));
        for (Official nso : game.getAll(Game.NSO)) { add(NSO, new MemberImpl(this, nso)); }
        for (Official ref : game.getAll(Game.REF)) { add(REF, new MemberImpl(this, ref)); }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == NSO) { return new MemberImpl(this, id, NSO); }
            if (prop == REF) { return new MemberImpl(this, id, REF); }
            return null;
        }
    }

    public class MemberImpl extends ScoreBoardEventProviderImpl<Member> implements Member {
        @Override
        public Child<Member> getType() {
            return ownType;
        }

        public MemberImpl(OfficialsCrew parent, String id, Child<Member> family) {
            super(parent, id, family);
            addProperties(props);
            setCopy(NAME, this, PREPARED_OFFICIAL, Official.NAME, true);
            setCopy(LEAGUE, this, PREPARED_OFFICIAL, Official.LEAGUE, true);
            setCopy(CERT, this, PREPARED_OFFICIAL, Official.CERT, true);
        }
        public MemberImpl(OfficialsCrew parent, Official official) {
            super(parent, UUID.randomUUID().toString(), official.getType() == Game.NSO ? NSO : REF);
            addProperties(props);
            setCopy(NAME, this, PREPARED_OFFICIAL, Official.NAME, true);
            setCopy(LEAGUE, this, PREPARED_OFFICIAL, Official.LEAGUE, true);
            setCopy(CERT, this, PREPARED_OFFICIAL, Official.CERT, true);
            set(ROLE, official.get(Official.ROLE));
            if (official.get(Official.PREPARED_OFFICIAL) == null) { official.execute(Official.STORE); }
            set(PREPARED_OFFICIAL, official.get(Official.PREPARED_OFFICIAL));
            if (official == official.getParent().get(Game.HEAD_NSO)) { parent.set(HEAD_NSO, this); }
            if (official == official.getParent().get(Game.HEAD_REF)) { parent.set(HEAD_REF, this); }
        }
    }
}
