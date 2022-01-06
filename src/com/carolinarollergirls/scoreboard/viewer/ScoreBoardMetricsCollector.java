package com.carolinarollergirls.scoreboard.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.MirrorScoreBoardEventProvider;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

public class ScoreBoardMetricsCollector extends Collector {
    private ScoreBoard sb;

    public ScoreBoardMetricsCollector(ScoreBoard sb) { this.sb = sb; }
    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        GaugeMetricFamily clockTime = new GaugeMetricFamily("crg_scoreboard_clock_time_seconds",
                                                            "Time on scoreboard clock.", Arrays.asList("clock"));
        mfs.add(clockTime);
        GaugeMetricFamily clockInvertedTime =
            new GaugeMetricFamily("crg_scoreboard_clock_inverted_time_seconds", "Time on scoreboard clock, inverted.",
                                  Arrays.asList("clock"));
        mfs.add(clockInvertedTime);
        GaugeMetricFamily clockRunning = new GaugeMetricFamily("crg_scoreboard_clock_running",
                                                               "Is scoreboard clock running.", Arrays.asList("clock"));
        mfs.add(clockRunning);
        GaugeMetricFamily clockNumber =
            new GaugeMetricFamily("crg_scoreboard_clock_number", "Number on scoreboard clock.", Arrays.asList("clock"));
        mfs.add(clockNumber);
        for (MirrorScoreBoardEventProvider<Clock> c : sb.getCurrentGame().getAllMirrors(Game.CLOCK)) {
            clockTime.addMetric(Arrays.asList(c.get(Clock.NAME)), (float) c.get(Clock.TIME) / 1000);
            clockInvertedTime.addMetric(Arrays.asList(c.get(Clock.NAME)), (float) c.get(Clock.INVERTED_TIME) / 1000);
            clockRunning.addMetric(Arrays.asList(c.get(Clock.NAME)), c.get(Clock.RUNNING) ? 1 : 0);
            clockNumber.addMetric(Arrays.asList(c.get(Clock.NAME)), c.get(Clock.NUMBER));
        }

        GaugeMetricFamily score =
            new GaugeMetricFamily("crg_scoreboard_team_score", "Score on scoreboard.", Arrays.asList("team", "name"));
        mfs.add(score);
        for (MirrorScoreBoardEventProvider<Team> t : sb.getCurrentGame().getAllMirrors(Game.TEAM)) {
            score.addMetric(Arrays.asList(t.get(Team.ID), t.get(Team.FULL_NAME)), t.get(Team.SCORE));
        }

        return mfs;
    }
}
