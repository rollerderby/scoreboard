package com.carolinarollergirls.scoreboard.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.carolinarollergirls.scoreboard.json.JSONStateListener.StateTrie;

import io.prometheus.client.Histogram;

public class JSONStateManager {
    public JSONStateManager(boolean useMetrics) {
        this.useMetrics = useMetrics;
        updateStateDuration = useMetrics ? Histogram.build()
                                               .name("crg_json_state_disk_snapshot_duration_seconds")
                                               .help("Time spent writing JSON state snapshots to disk")
                                               .register()
                                         : null;
        updateStateUpdates = useMetrics ? Histogram.build()
                                              .name("crg_json_update_state_updates")
                                              .help("Updates sent to JSONStateManager.updateState function")
                                              .exponentialBuckets(1, 2, 10)
                                              .register()
                                        : null;
    }

    public synchronized void register(JSONStateListener source) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        sources.put(source, executor);
        // Send on the current state asynchronously.
        final StateTrie localState = state.clone();
        pending.incrementAndGet();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                source.sendUpdates(localState, localState);
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
        Histogram.Timer timer = useMetrics ? updateStateDuration.startTimer() : null;
        StateTrie changedState = new StateTrie();

        for (WSUpdate update : updates) { changedState.add(update.getKey(), update.getValue()); }

        state.mergeChangeTrie(changedState);
        if (!changedState.isEmpty()) {
            final StateTrie localState = state.clone();
            final StateTrie localChanged = changedState.clone();

            // Send updates async, as the WS connections can block if the
            // kernel TCP send buffer fills up.
            for (JSONStateListener source : sources.keySet()) {
                final JSONStateListener localSource = source;
                pending.incrementAndGet();
                sources.get(source).execute(new Runnable() {
                    @Override
                    public void run() {
                        localSource.sendUpdates(localState, localChanged);
                        pending.decrementAndGet();
                    }
                });
            }
        }
        if (useMetrics) { timer.observeDuration(); }
        if (useMetrics) { updateStateUpdates.observe(updates.size()); }
    }

    public synchronized Map<String, Object> getState() { return state.getAll(false); }

    // For unittests.
    protected void waitForSent() {
        while (pending.get() > 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {};
        }
    }

    private Map<JSONStateListener, ExecutorService> sources = new HashMap<>();
    private StateTrie state = new StateTrie();
    private final AtomicInteger pending = new AtomicInteger();

    private boolean useMetrics;
    private final Histogram updateStateDuration;
    private final Histogram updateStateUpdates;
}
