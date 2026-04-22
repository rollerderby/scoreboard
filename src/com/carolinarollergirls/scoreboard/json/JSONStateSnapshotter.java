package com.carolinarollergirls.scoreboard.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.Logger;

import io.prometheus.client.Histogram;

public final class JSONStateSnapshotter implements JSONStateListener {

    public JSONStateSnapshotter(JSONStateManager jsm, Game g, boolean useMetrics) {
        this.directory = BasePath.get();
        filename = g.getFilename();
        this.useMetrics = useMetrics;
        if (useMetrics && updateStateDuration == null) {
            updateStateDuration = Histogram.build()
                                      .name("crg_json_state_disk_snapshot_duration_seconds")
                                      .help("Time spent writing JSON state snapshots to disk")
                                      .register();
        }
        filters.add("ScoreBoard.Version");
        filters.add("ScoreBoard.Game(" + g.getId() + ")");
        jsm.register(this);
    }

    @Override
    public void sendUpdates(StateTrie newState, StateTrie changedState) {
        state = newState;
        if (writeOnNextUpdate.getAndSet(false)) { writeFile(); }
    }

    public void writeOnNextUpdate() { writeOnNextUpdate.set(true); }

    public synchronized void setFilename(String newName) { filename = newName; }

    public synchronized void writeFile() {
        Histogram.Timer timer = useMetrics ? updateStateDuration.startTimer() : null;

        File file = new File(new File(directory, "html/game-data/json"), filename + ".json");
        File prev = new File(new File(directory, "html/game-data/json"), filename + "_prev.json");
        file.getParentFile().mkdirs();

        File tmp = null;
        OutputStreamWriter out = null;
        try {
            // Put inside a "state" entry to match the WS.
            String json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                              .composeString()
                              .startObject()
                              .putObject("state", state.filter(filters, true))
                              .end()
                              .finish();
            tmp = File.createTempFile(file.getName(), ".tmp", directory);
            out = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
            out.write(json);
            out.close();
            prev.delete();
            file.renameTo(prev);
            if (tmp.renameTo(file)) { prev.delete(); }
        } catch (Exception e) { Logger.printStackTrace("writing JSON snapshot", e); } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {}
            }
            if (tmp != null) {
                try {
                    tmp.delete();
                } catch (Exception e) {}
            }
        }
        if (useMetrics) { timer.observeDuration(); }
    }

    private File directory;
    private String filename;
    private AtomicBoolean writeOnNextUpdate = new AtomicBoolean(false);
    private StateTrie state = new StateTrie();
    private PathTrie filters = new PathTrie();

    private boolean useMetrics;
    private static Histogram updateStateDuration = null;
}
