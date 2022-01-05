package com.carolinarollergirls.scoreboard.event;

public interface MirrorFactory {
    public <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T>
    createMirror(ScoreBoardEventProvider parent, T mirrored);
}
