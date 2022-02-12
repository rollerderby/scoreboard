package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;

public interface MirrorScoreBoardEventProvider<C extends ScoreBoardEventProvider> extends ScoreBoardEventProvider {
    public C getSourceElement();

    public <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T> getMirror(Child<T> prop, String id);
    public <T extends ScoreBoardEventProvider> Collection<MirrorScoreBoardEventProvider<T>>
    getAllMirrors(Child<T> prop);
}