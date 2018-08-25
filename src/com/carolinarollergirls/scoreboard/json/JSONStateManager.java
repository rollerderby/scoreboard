package com.carolinarollergirls.scoreboard.json;

import io.prometheus.client.Histogram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONStateManager {

  public synchronized void register(JSONStateListener source) {
    sources.add(source);
    // Send on the current state.
    source.sendUpdates(state, state.keySet());
  }

  public synchronized void unregister(JSONStateListener source) {
    sources.remove(source);
  }

  public void updateState(String key, Object value) {
    List<WSUpdate> updates = new ArrayList<WSUpdate>();
    updates.add(new WSUpdate(key, value));
    updateState(updates);
  }

  public synchronized void updateState(List<WSUpdate> updates) {
    Histogram.Timer timer = updateStateDuration.startTimer();
    Set<String> changed = new HashSet<String>();
    Set<String> toRemove = new HashSet<String>();
    Map<String, Object> newState = new HashMap<String, Object>(state);

    for (WSUpdate update : updates) {
      if (update.getValue() == null) {
        for (String stateKey: state.keySet()) {
          if (stateKey.equals(update.getKey()) || stateKey.startsWith(update.getKey()+".")) {
            toRemove.add(stateKey);
            changed.add(stateKey);
          }
        } 
      } else {
        changed.add(update.getKey());
        toRemove.remove(update.getKey());
        newState.put(update.getKey(), update.getValue());
      }
    }

    // Modifications to the map have to be done after iteration.
    for (String key: toRemove) {
      newState.remove(key);
    }

    // Discard noop changes.
    Iterator<String> it = changed.iterator();
    while (it.hasNext()) {
      String key = it.next();
      Object old = state.get(key);
      Object cur = newState.get(key);
      if ((old == null && cur == null)
          || (old != null && old.equals(cur))) {
        it.remove();
          }
    }

    state = Collections.unmodifiableMap(newState);
    if (!changed.isEmpty()) {
      Set<String> immutableChanged = Collections.unmodifiableSet(changed);

      for (JSONStateListener source : sources) {
        source.sendUpdates(state, immutableChanged);
      }
    }
    timer.observeDuration();
    updateStateUpdates.observe(updates.size());
  }

  private Set<JSONStateListener> sources = new HashSet<JSONStateListener>();
  private Map<String, Object> state = new HashMap<String, Object>();

  private static final Histogram updateStateDuration = Histogram.build()
    .name("crg_json_update_state_duration_seconds").help("Time spent in JSONStateManager.updateState function").register();
  private static final Histogram updateStateUpdates = Histogram.build()
    .name("crg_json_update_state_updates").help("Updates sent to JSONStateManager.updateState function")
    .exponentialBuckets(1, 2, 10).register();
}
