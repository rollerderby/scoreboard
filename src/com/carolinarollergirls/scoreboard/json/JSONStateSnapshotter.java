package com.carolinarollergirls.scoreboard.json;

import io.prometheus.client.Histogram;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
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
    public synchronized void sendUpdates(Map<String, Object> state, Set<String> changed) {
        if (state.get("ScoreBoard.CurrentPeriodNumber") != "0") {
            // If the jam has just ended or the score is now official, write out a file.
            if ((inJam && !bool(state.get("ScoreBoard.Clock(Jam).Running")))
                    || (!officialScore && bool(state.get("ScoreBoard.OfficialScore")))) {
                writeFile(state);
            }
        }

        officialScore = bool(state.get("ScoreBoard.OfficialScore"));
        inJam = bool(state.get("ScoreBoard.Clock(Jam).Running"));
    }

    public void writeFile(Map<String, Object> state) {
        Histogram.Timer timer = updateStateDuration.startTimer();

        // Fallback to current time.
        long startTime = System.currentTimeMillis();
        Object periodStart = state.get("ScoreBoard.Period(1).WalltimeStart");
        if (periodStart != null) {
            startTime = (long)periodStart;
        }
        Object period = state.get("ScoreBoard.CurrentPeriodNumber");

        String name = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(startTime)
        + " - " + state.get("ScoreBoard.Team(1).Name")
        + " vs " + state.get("ScoreBoard.Team(2).Name")
        + " P" + period
        + ".json";
        name = name.replaceAll("[^a-zA-Z0-9\\.\\- ]", "_");
        File file = new File(new File(directory, "html/game-data/json"), name);
        file.getParentFile().mkdirs();

        // The state includes secrets (Twitter auth) and
        // details not relevant to one particular game (e.g. preapred teams)
        // so trim things down.
        Map<String, Object> cleanedState = new TreeMap<>(state);
        for (Iterator<String> it = cleanedState.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            if (key.startsWith("ScoreBoard.Twitter.")
                    || key.startsWith("ScoreBoard.Media." )
                    || key.startsWith("ScoreBoard.Settings." )
                    || key.startsWith("ScoreBoard.PreparedTeams(" )) {
                it.remove();
            }
        }

        File tmp = null;
        FileWriter out = null;
        try {
            // Put inside a "state" entry to match the WS.
            // Use a TreeMap so output is sorted.
            String json = JSON.std
                          .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                          .composeString()
                          .startObject()
                          .putObject("state", cleanedState)
                          .end()
                          .finish();
            tmp = File.createTempFile(file.getName(), ".tmp", directory);
            out = new FileWriter(tmp);
            out.write(json);
            out.close();
            tmp.renameTo(file); // This is atomic.
        } catch (Exception e) {
            ScoreBoardManager.printMessage("Error writing JSON snapshot: " + e.getMessage());
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception e) { }
            }
            if (tmp != null ) {
                try { tmp.delete(); } catch (Exception e) { }
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
