package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Expulsion extends ScoreBoardEventProvider {
    Value<String> INFO = new Value<>(String.class, "Info", "");
    Value<String> EXTRA_INFO = new Value<>(String.class, "ExtraInfo", "");
    Value<Boolean> SUSPENSION = new Value<>(Boolean.class, "Suspension", false);
}
