package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;


public class ScoreBoardMetricsCollector extends Collector {
  private ScoreBoardModel sbm;
  ScoreBoardMetricsCollector(ScoreBoardModel sbm) {
    this.sbm = sbm;
  }
  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();

    GaugeMetricFamily clockTime = new GaugeMetricFamily("crg_scoreboard_clock_time_seconds", 
        "Time on scoreboard clock.", Arrays.asList("clock"));
    mfs.add(clockTime);
    GaugeMetricFamily clockInvertedTime = new GaugeMetricFamily("crg_scoreboard_clock_inverted_time_seconds", 
        "Time on scoreboard clock, inverted.", Arrays.asList("clock"));
    mfs.add(clockInvertedTime);
    GaugeMetricFamily clockRunning = new GaugeMetricFamily("crg_scoreboard_clock_running", 
        "Is scoreboard clock running.", Arrays.asList("clock"));
    mfs.add(clockRunning);
    GaugeMetricFamily clockNumber = new GaugeMetricFamily("crg_scoreboard_clock_number", 
        "Number on scoreboard clock.", Arrays.asList("clock"));
    mfs.add(clockNumber);
    for (Clock c : sbm.getClocks()) {
        clockTime.addMetric(Arrays.asList(c.getName()), (float)c.getTime() / 1000);
        clockInvertedTime.addMetric(Arrays.asList(c.getName()), (float)c.getInvertedTime() / 1000);
        clockRunning.addMetric(Arrays.asList(c.getName()), c.isRunning() ? 1 : 0);
        clockNumber.addMetric(Arrays.asList(c.getName()), c.getNumber());
    }

    GaugeMetricFamily score = new GaugeMetricFamily("crg_scoreboard_team_score", 
        "Score on scoreboard.", Arrays.asList("team", "name"));
    mfs.add(score);
    for (Team t : sbm.getTeams()) {
        score.addMetric(Arrays.asList(t.getId(), t.getName()), t.getScore());
    }

    return mfs;
  }
}

