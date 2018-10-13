package com.carolinarollergirls.scoreboard.json;

import java.util.Map;
import java.util.Set;

public interface JSONStateListener {
    // A snapshot of the current state, and which keys it it have changed.
    // Keys with a value of null are considered deleted, and will not be present
    // in state.
    public void sendUpdates(Map<String, Object> state, Set<String> changed);
}
