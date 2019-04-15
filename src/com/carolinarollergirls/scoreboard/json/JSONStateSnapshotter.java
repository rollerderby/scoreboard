package com.carolinarollergirls.scoreboard.json;

import io.prometheus.client.Histogram;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class JSONStateSnapshotter implements JSONStateListener {

    public JSONStateSnapshotter(JSONStateManager jsm, File directory) {
        this.directory = directory;
        jsm.register(this);
    }

    @Override
    public void sendUpdates(Map<String, Object> state, Set<String> changed) {
        // If the jam has just ended or the score is now official, write out a file.
        if ((inJam && !bool(state.get("ScoreBoard.Clock(Jam).Running")))
                || (!officialScore && bool(state.get("ScoreBoard.OfficialScore")))) {
            writeFile(state);
        }

        officialScore = bool(state.get("ScoreBoard.OfficialScore"));
        inJam = bool(state.get("ScoreBoard.Clock(Jam).Running"));
    }

    public void writeFile(Map<String, Object> state) {
        Histogram.Timer timer = updateStateDuration.startTimer();

        // Fallback to current time.
        long startTime = System.currentTimeMillis();
        Object periodState = state.get("ScoreBoard.Stats.Period(1).Jam(1).PeriodClockWalltimeStart");
        if (periodState != null) {
            startTime = (long)periodState;
        }

        String name = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(startTime)
                + " - " + state.get("ScoreBoard.Team(1).Name")
                + " vs " + state.get("ScoreBoard.Team(2).Name")
                + ".json";
        name = name.replaceAll("[^a-zA-Z0-9\\.\\- ]", "_");
        File file = new File(new File(directory, "game-data"), name);
        file.getParentFile().mkdirs();

        FileWriter out = null;
        try {
            // Put inside a "state" entry to match the WS.
            // Use a TreeMap so output is sorted.
            String json = JSON.std
                    .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                    .composeString()
                    .startObject()
                    .putObject("state", new TreeMap<>(state))
                    .end()
                    .finish();

            out = new FileWriter(file);
            out.write(json);
        } catch (Exception e) {
            ScoreBoardManager.printMessage("Error writing JSON snapshot: " + e.getMessage());
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception e) { }
            }
        }
        timer.observeDuration();
    }

    private boolean bool(Object b) {
        return b != null && ((Boolean)b).booleanValue();
    }

    private boolean inJam = false;
    private boolean officialScore = false;
    private File directory;

    private static final Histogram updateStateDuration = Histogram.build()
            .name("crg_json_state_disk_snapshot_duration_seconds").help("Time spent writing JSON state snapshots to disk").register();
}
