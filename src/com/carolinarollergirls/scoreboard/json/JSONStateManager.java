package com.carolinarollergirls.scoreboard.json;

import io.prometheus.client.Histogram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JSONStateManager {

    public synchronized void register(JSONStateListener source) {
        sources.put(source, new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()));
        // Send on the current state.
        source.sendUpdates(state, state.keySet());
    }

    public synchronized void unregister(JSONStateListener source) {
        sources.get(source).shutdown();
        sources.remove(source);
    }

    public void updateState(String key, Object value) {
        List<WSUpdate> updates = new ArrayList<>();
        updates.add(new WSUpdate(key, value));
        updateState(updates);
    }

    public synchronized void updateState(List<WSUpdate> updates) {
        Histogram.Timer timer = updateStateDuration.startTimer();
        Set<String> changed = new HashSet<>();
        Set<String> toRemove = new HashSet<>();
        Map<String, Object> newState = new HashMap<>(state);

        for (WSUpdate update : updates) {
            if (update.getValue() == null) {
                for (String stateKey: newState.keySet()) {
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
            if (Objects.equals(cur, old)) {
                it.remove();
            }
        }

        state = Collections.unmodifiableMap(newState);
        final Map<String, Object> localState = state;
        if (!changed.isEmpty()) {
            final Set<String> immutableChanged = Collections.unmodifiableSet(changed);

            // Send updates async, as the WS connections can block if the
            // kernel TCP send buffer fills up.
            for (JSONStateListener source : sources.keySet()) {
                final JSONStateListener localSource = source;
                pending.incrementAndGet();
                sources.get(source).execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                localSource.sendUpdates(localState, immutableChanged);
                                pending.decrementAndGet();
                            }
                        }
                        );
            }
        }
        timer.observeDuration();
        updateStateUpdates.observe(updates.size());
    }

    // For unittests.
    protected void waitForSent() {
        while(pending.get() > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {};
        }
    }

    private Map<JSONStateListener, ThreadPoolExecutor> sources = new HashMap<>();
    private Map<String, Object> state = new HashMap<>();
    private final AtomicInteger pending = new AtomicInteger();

    private static final Histogram updateStateDuration = Histogram.build()
            .name("crg_json_update_state_duration_seconds").help("Time spent in JSONStateManager.updateState function").register();
    private static final Histogram updateStateUpdates = Histogram.build()
            .name("crg_json_update_state_updates").help("Updates sent to JSONStateManager.updateState function")
            .exponentialBuckets(1, 2, 10).register();
}
