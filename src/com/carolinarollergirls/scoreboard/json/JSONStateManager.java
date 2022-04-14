package com.carolinarollergirls.scoreboard.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.prometheus.client.Histogram;

public class JSONStateManager {

    public synchronized void register(JSONStateListener source) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        sources.put(source, executor);
        // Send on the current state asynchronously.
        final Map<String, Object> localState = state;
        pending.incrementAndGet();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                source.sendUpdates(localState, localState.keySet());
                pending.decrementAndGet();
            }
        });
    }

    public synchronized void unregister(JSONStateListener source) {
        sources.get(source).shutdownNow();
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
        SortedMap<String, Object> newState = new TreeMap<>(state);

        for (WSUpdate update : updates) {
            if (update.getValue() == null) {
                String removedKey = update.getKey();
                if (newState.containsKey(removedKey)) {
                    toRemove.add(removedKey);
                    changed.add(removedKey);
                }
                for (String stateKey : newState.subMap(removedKey + ".", removedKey + "/").keySet()) {
                    toRemove.add(stateKey);
                    changed.add(stateKey);
                }
            } else {
                changed.add(update.getKey());
                toRemove.remove(update.getKey());
                newState.put(update.getKey(), update.getValue());
            }
        }

        // Modifications to the map have to be done after iteration.
        for (String key : toRemove) { newState.remove(key); }

        // Discard noop changes.
        Iterator<String> it = changed.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Object old = state.get(key);
            Object cur = newState.get(key);
            if (Objects.equals(cur, old)) { it.remove(); }
        }

        state = Collections.unmodifiableSortedMap(newState);
        if (!changed.isEmpty()) {
            final Map<String, Object> localState = state;
            final Set<String> immutableChanged = Collections.unmodifiableSet(changed);

            // Send updates async, as the WS connections can block if the
            // kernel TCP send buffer fills up.
            for (JSONStateListener source : sources.keySet()) {
                final JSONStateListener localSource = source;
                pending.incrementAndGet();
                sources.get(source).execute(new Runnable() {
                    @Override
                    public void run() {
                        localSource.sendUpdates(localState, immutableChanged);
                        pending.decrementAndGet();
                    }
                });
            }
        }
        timer.observeDuration();
        updateStateUpdates.observe(updates.size());
    }

    public synchronized Map<String, Object> getState() { return state; }

    // For unittests.
    protected void waitForSent() {
        while (pending.get() > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {};
        }
    }

    private Map<JSONStateListener, ExecutorService> sources = new HashMap<>();
    private SortedMap<String, Object> state = new TreeMap<>();
    private final AtomicInteger pending = new AtomicInteger();

    private static final Histogram updateStateDuration =
        Histogram.build()
            .name("crg_json_update_state_duration_seconds")
            .help("Time spent in JSONStateManager.updateState function")
            .register();
    private static final Histogram updateStateUpdates =
        Histogram.build()
            .name("crg_json_update_state_updates")
            .help("Updates sent to JSONStateManager.updateState function")
            .exponentialBuckets(1, 2, 10)
            .register();
}
