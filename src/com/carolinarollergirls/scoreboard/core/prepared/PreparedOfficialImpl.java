package com.carolinarollergirls.scoreboard.core.prepared;

import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedOfficial;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public class PreparedOfficialImpl extends ScoreBoardEventProviderImpl<PreparedOfficial> implements PreparedOfficial {
    public PreparedOfficialImpl(ScoreBoard parent, String id) {
        super(parent, id, ScoreBoard.PREPARED_OFFICIAL);
        addProperties(Official.preparedProps);
        addProperties(props);
        setRecalculated(FULL_INFO).addSource(this, Official.NAME).addSource(this, Official.LEAGUE);
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == FULL_INFO) { return get(Official.NAME) + " (" + get(Official.LEAGUE) + ")"; }
        return value;
    }

    @Override
    public boolean matches(String name, String league) {
        return get(Official.NAME).equals(name) && ("".equals(league) || get(Official.LEAGUE).equals(league));
    }
}
