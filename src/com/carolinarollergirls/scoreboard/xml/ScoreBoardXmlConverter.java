package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.SettingsModel;
import com.carolinarollergirls.scoreboard.model.PositionModel;
import com.carolinarollergirls.scoreboard.model.RulesetsModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.SkaterModel;
import com.carolinarollergirls.scoreboard.model.StatsModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.Settings;
import com.carolinarollergirls.scoreboard.view.Position;
import com.carolinarollergirls.scoreboard.view.Rulesets;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Skater;
import com.carolinarollergirls.scoreboard.view.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.view.Stats;
import com.carolinarollergirls.scoreboard.view.Team;

public class ScoreBoardXmlConverter {
    /*****************************/
    /* ScoreBoard to XML methods */

    public String toString(ScoreBoard scoreBoard) {
        return rawXmlOutputter.outputString(toDocument(scoreBoard));
    }

    public Document toDocument(ScoreBoard scoreBoard) {
        Element sb = new Element("ScoreBoard");
        Document d = new Document(new Element("document").addContent(sb));

        editor.setElement(sb, "Reset", null, "");
        editor.setElement(sb, "StartJam", null, "");
        editor.setElement(sb, "StopJam", null, "");
        editor.setElement(sb, "Timeout", null, "");
        editor.setElement(sb, "ClockUndo", null, "");
        editor.setElement(sb, "ClockReplace", null, "");
        editor.setElement(sb, "StartOvertime", null, "");
        editor.setElement(sb, "OfficialTimeout", null, "");

        editor.setElement(sb, ScoreBoard.EVENT_TIMEOUT_OWNER, null, scoreBoard.getTimeoutOwner());
        editor.setElement(sb, ScoreBoard.EVENT_OFFICIAL_REVIEW, null, String.valueOf(scoreBoard.isOfficialReview()));
        editor.setElement(sb, ScoreBoard.EVENT_IN_OVERTIME, null, String.valueOf(scoreBoard.isInOvertime()));
        editor.setElement(sb, ScoreBoard.EVENT_IN_PERIOD, null, String.valueOf(scoreBoard.isInPeriod()));
        editor.setElement(sb, ScoreBoard.EVENT_OFFICIAL_SCORE, null, String.valueOf(scoreBoard.isOfficialScore()));

        toElement(sb, scoreBoard.getSettings());
        toElement(sb, scoreBoard.getRulesets());

        Iterator<Clock> clocks = scoreBoard.getClocks().iterator();
        while (clocks.hasNext()) {
            toElement(sb, clocks.next());
        }

        Iterator<Team> teams = scoreBoard.getTeams().iterator();
        while (teams.hasNext()) {
            toElement(sb, teams.next());
        }

        toElement(sb, scoreBoard.getStats());

        return d;
    }

    public Element toElement(Element p, Settings s) {
        Element e = editor.setElement(p, "Settings");
        Iterator<String> keys = s.getAll().keySet().iterator();
        while (keys.hasNext()) {
            String k = keys.next();
            String v = s.get(k);
            if (v != null) {
                editor.setElement(e, Settings.EVENT_SETTING, k, v);
            }
        }
        return e;
    }

    public Element toElement(Element p, Rulesets rs) {
        Element e = editor.setElement(p, "Rules");
        Iterator<String> keys = rs.getAll().keySet().iterator();
        while (keys.hasNext()) {
            String k = keys.next();
            String v = rs.get(k);
            editor.setElement(e, Rulesets.EVENT_CURRENT_RULES, k, v);
        }
        editor.setElement(e, "Id", null, rs.getId());
        editor.setElement(e, "Name", null, rs.getName());

        e = editor.setElement(p, Rulesets.EVENT_RULESET);
        for (Rulesets.Ruleset r :  rs.getRulesets().values()) {
            toElement(e, r);
        }
        return e;
    }

    public Element toElement(Element p, Rulesets.Ruleset r) {
        Element e = editor.setElement(p, "Ruleset", r.getId());
        Iterator<String> keys = r.getAll().keySet().iterator();
        while (keys.hasNext()) {
            String k = keys.next();
            String v = r.get(k);
            editor.setElement(e, Rulesets.EVENT_CURRENT_RULES, k, v);
        }
        editor.setElement(e, "Name", null, r.getName());
        editor.setElement(e, "ParentId", null, r.getParentRulesetId());
        return e;
    }

    public Element toElement(Element sb, Clock c) {
        Element e = editor.setElement(sb, "Clock", c.getId());

        editor.setElement(e, "Start", null, "");
        editor.setElement(e, "UnStart", null, "");
        editor.setElement(e, "Stop", null, "");
        editor.setElement(e, "UnStop", null, "");
        editor.setElement(e, "ResetTime", null, "");

        editor.setElement(e, Clock.EVENT_NAME, null, c.getName());
        editor.setElement(e, Clock.EVENT_NUMBER, null, String.valueOf(c.getNumber()));
        editor.setElement(e, Clock.EVENT_MINIMUM_NUMBER, null, String.valueOf(c.getMinimumNumber()));
        editor.setElement(e, Clock.EVENT_MAXIMUM_NUMBER, null, String.valueOf(c.getMaximumNumber()));
        editor.setElement(e, Clock.EVENT_TIME, null, String.valueOf(c.getTime()));
        editor.setElement(e, Clock.EVENT_INVERTED_TIME, null, String.valueOf(c.getInvertedTime()));
        editor.setElement(e, Clock.EVENT_MINIMUM_TIME, null, String.valueOf(c.getMinimumTime()));
        editor.setElement(e, Clock.EVENT_MAXIMUM_TIME, null, String.valueOf(c.getMaximumTime()));
        editor.setElement(e, Clock.EVENT_RUNNING, null, String.valueOf(c.isRunning()));
        editor.setElement(e, Clock.EVENT_DIRECTION, null, String.valueOf(c.isCountDirectionDown()));
        return e;
    }

    public Element toElement(Element sb, Team t) {
        Element e = editor.setElement(sb, "Team", t.getId());

        editor.setElement(e, "Timeout", null, "");
        editor.setElement(e, "OfficialReview", null, "");

        editor.setElement(e, Team.EVENT_NAME, null, t.getName());
        editor.setElement(e, Team.EVENT_LOGO, null, t.getLogo());
        editor.setElement(e, Team.EVENT_SCORE, null, String.valueOf(t.getScore()));
        editor.setElement(e, Team.EVENT_LAST_SCORE, null, String.valueOf(t.getLastScore()));
        editor.setElement(e, Team.EVENT_TIMEOUTS, null, String.valueOf(t.getTimeouts()));
        editor.setElement(e, Team.EVENT_OFFICIAL_REVIEWS, null, String.valueOf(t.getOfficialReviews()));
        editor.setElement(e, Team.EVENT_IN_TIMEOUT, null, String.valueOf(t.inTimeout()));
        editor.setElement(e, Team.EVENT_IN_OFFICIAL_REVIEW, null, String.valueOf(t.inOfficialReview()));
        editor.setElement(e, Team.EVENT_RETAINED_OFFICIAL_REVIEW, null, String.valueOf(t.retainedOfficialReview()));
        editor.setElement(e, Team.EVENT_LEAD_JAMMER, null, t.getLeadJammer());
        editor.setElement(e, Team.EVENT_STAR_PASS, null, String.valueOf(t.isStarPass()));

        Iterator<Team.AlternateName> alternateNames = t.getAlternateNames().iterator();
        while (alternateNames.hasNext()) {
            toElement(e, alternateNames.next());
        }

        Iterator<Team.Color> colors = t.getColors().iterator();
        while (colors.hasNext()) {
            toElement(e, colors.next());
        }

        Iterator<Position> positions = t.getPositions().iterator();
        while (positions.hasNext()) {
            toElement(e, positions.next());
        }

        Iterator<Skater> skaters = t.getSkaters().iterator();
        while (skaters.hasNext()) {
            toElement(e, skaters.next());
        }

        return e;
    }

    public Element toElement(Element team, Team.AlternateName n) {
        Element e = editor.setElement(team, "AlternateName", n.getId());

        editor.setElement(e, Team.AlternateName.EVENT_NAME, null, n.getName());

        return e;
    }

    public Element toElement(Element team, Team.Color c) {
        Element e = editor.setElement(team, "Color", c.getId());

        editor.setElement(e, Team.Color.EVENT_COLOR, null, c.getColor());

        return e;
    }

    public Element toElement(Element team, Position p) {
        Element e = editor.setElement(team, "Position", p.getId());

        editor.setElement(e, "Clear", null, "");

        Skater s = p.getSkater();
        editor.setElement(e, "Id", null, (s==null?"":s.getId()));
        editor.setElement(e, Skater.EVENT_NAME, null, (s==null?"":s.getName()));
        editor.setElement(e, Skater.EVENT_NUMBER, null, (s==null?"":s.getNumber()));
        editor.setElement(e, Skater.EVENT_PENALTY_BOX, null, String.valueOf(s==null?false:s.isPenaltyBox()));
        editor.setElement(e, Skater.EVENT_FLAGS, null, (s==null?"":s.getFlags()));

        return e;
    }

    public Element toElement(Element t, Skater s) {
        Element e = editor.setElement(t, "Skater", s.getId());
        editor.setElement(e, Skater.EVENT_NAME, null, s.getName());
        editor.setElement(e, Skater.EVENT_NUMBER, null, s.getNumber());
        editor.setElement(e, Skater.EVENT_POSITION, null, s.getPosition());
        editor.setElement(e, Skater.EVENT_PENALTY_BOX, null, String.valueOf(s.isPenaltyBox()));
        editor.setElement(e, Skater.EVENT_FLAGS, null, s.getFlags());

        for (Skater.Penalty p: s.getPenalties()) {
            toElement(e, p);
        }

        if (s.getFOEXPPenalty() != null) {
            Element fe = editor.setElement(e, Skater.EVENT_PENALTY_FOEXP, s.getFOEXPPenalty().getId());
            toElement(fe, s.getFOEXPPenalty());
        }

        return e;
    }

    public Element toElement(Element s, Skater.Penalty p) {
        Element e = editor.setElement(s, "Penalty", p.getId());
        editor.setElement(e, Skater.EVENT_PENALTY_PERIOD, null, String.valueOf(p.getPeriod()));
        editor.setElement(e, Skater.EVENT_PENALTY_JAM, null, String.valueOf(p.getJam()));
        editor.setElement(e, Skater.EVENT_PENALTY_CODE, null, p.getCode());
        return e;
    }

    public Element toElement(Element sb, Stats s) {
        Element e = editor.setElement(sb, "Stats");
        for (Stats.PeriodStats p: s.getPeriodStats()) {
            toElement(e, p);
        }
        return e;
    }

    public Element toElement(Element s, Stats.PeriodStats p) {
        Element e = editor.setElement(s, "Period", String.valueOf(p.getPeriodNumber()));
        for (Stats.JamStats j: p.getJamStats()) {
            toElement(e, j);
        }
        return e;
    }

    public Element toElement(Element p, Stats.JamStats j) {
        Element e = editor.setElement(p, "Jam", String.valueOf(j.getJamNumber()));
        editor.setElement(e, "JamClockElapsedEnd", null, String.valueOf(j.getJamClockElapsedEnd()));
        editor.setElement(e, "PeriodClockElapsedStart", null, String.valueOf(j.getPeriodClockElapsedStart()));
        editor.setElement(e, "PeriodClockElapsedEnd", null, String.valueOf(j.getPeriodClockElapsedEnd()));
        editor.setElement(e, "PeriodClockWalltimeStart", null, String.valueOf(j.getPeriodClockWalltimeStart()));
        editor.setElement(e, "PeriodClockWalltimeEnd", null, String.valueOf(j.getPeriodClockWalltimeEnd()));
        for (Stats.TeamStats t: j.getTeamStats()) {
            toElement(e, t);
        }
        return e;
    }

    public Element toElement(Element j, Stats.TeamStats t) {
        Element e = editor.setElement(j, "Team", t.getTeamId());
        editor.setElement(e, "JamScore", null, String.valueOf(t.getJamScore()));
        editor.setElement(e, "TotalScore", null, String.valueOf(t.getTotalScore()));
        editor.setElement(e, "LeadJammer", null, t.getLeadJammer());
        editor.setElement(e, "StarPass", null, String.valueOf(t.getStarPass()));
        editor.setElement(e, "Timeouts", null, String.valueOf(t.getTimeouts()));
        editor.setElement(e, "OfficialReviews", null, String.valueOf(t.getOfficialReviews()));
        for (Stats.SkaterStats s: t.getSkaterStats()) {
            toElement(e, s);
        }
        return e;
    }

    public Element toElement(Element t, Stats.SkaterStats s) {
        Element e = editor.setElement(t, "Skater", s.getSkaterId());
        editor.setElement(e, "Position", null, s.getPosition());
        editor.setElement(e, "PenaltyBox", null, String.valueOf(s.getPenaltyBox()));
        return e;
    }

    /*****************************/
    /* XML to ScoreBoard methods */

    public void processDocument(ScoreBoardModel scoreBoardModel, Document document) {
        Iterator<?> children = document.getRootElement().getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            if (element.getName().equals("ScoreBoard")) {
                processScoreBoard(scoreBoardModel, element);
            }
        }
    }

    public void processScoreBoard(ScoreBoardModel scoreBoardModel, Element scoreBoard) {
        Iterator<?> children = scoreBoard.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);
                boolean bVal = Boolean.parseBoolean(value);

                if (name.equals("Clock")) {
                    processClock(scoreBoardModel, element);
                } else if (name.equals("Team")) {
                    processTeam(scoreBoardModel, element);
                } else if (name.equals("Settings")) {
                    processSettings(scoreBoardModel, element);
                } else if (name.equals("Rules")) {
                    processRules(scoreBoardModel, element);
                } else if (name.equals("KnownRulesets")) {
                    processRulesets(scoreBoardModel, element);
                } else if (name.equals("Stats")) {
                    processStats(scoreBoardModel.getStatsModel(), element);
                } else if (null == value) {
                    continue;
                } else if (name.equals(ScoreBoard.EVENT_TIMEOUT_OWNER)) {
                    scoreBoardModel.setTimeoutOwner(value);
                } else if (name.equals(ScoreBoard.EVENT_OFFICIAL_REVIEW)) {
                    scoreBoardModel.setOfficialReview(bVal);
                } else if (name.equals(ScoreBoard.EVENT_IN_OVERTIME)) {
                    scoreBoardModel.setInOvertime(bVal);
                } else if (name.equals(ScoreBoard.EVENT_IN_PERIOD)) {
                    scoreBoardModel.setInPeriod(bVal);
                } else if (name.equals(ScoreBoard.EVENT_OFFICIAL_SCORE)) {
                    scoreBoardModel.setOfficialScore(bVal);
                } else if (bVal) {
                    if (name.equals("Reset")) {
                        scoreBoardModel.reset();
                    } else if (name.equals("StartJam")) {
                        scoreBoardModel.startJam();
                    } else if (name.equals("StopJam")) {
                        scoreBoardModel.stopJamTO();
                    } else if (name.equals("Timeout")) {
                        scoreBoardModel.timeout();
                    } else if (name.equals("ClockUndo")) {
                        scoreBoardModel.clockUndo(false);
                    } else if (name.equals("ClockReplace")) {
                        scoreBoardModel.clockUndo(true);
                    } else if (name.equals("StartOvertime")) {
                        scoreBoardModel.startOvertime();
                    } else if (name.equals("OfficialTimeout")) {
                        scoreBoardModel.setTimeoutType("O", false);
                    }
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processSettings(ScoreBoardModel scoreBoardModel, Element settings) {
        SettingsModel sm = scoreBoardModel.getSettingsModel();
        Iterator<?> children = settings.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String k = element.getAttributeValue("Id");
                String v = editor.getText(element);
                if (v == null) {
                    v = "";
                }
                sm.set(k, v);
            } catch ( Exception e ) {
            }
        }
    }

    public void processRules(ScoreBoardModel scoreBoardModel, Element rules) {
        RulesetsModel rs = scoreBoardModel.getRulesetsModel();
        Iterator<?> children = rules.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String k = element.getAttributeValue("Id");
                String v = editor.getText(element);
                if (v == null) {
                    v = "";
                }
                if (name.equals(Rulesets.EVENT_CURRENT_RULES)) {
                    rs.set(k, v);
                } else if (name.equals("Id")) {
                    rs.setId(v);
                } else if (name.equals("Name")) {
                    rs.setName(v);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processRulesets(ScoreBoardModel scoreBoardModel, Element rulesets) {
        RulesetsModel rs = scoreBoardModel.getRulesetsModel();
        Iterator<?> children = rulesets.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                if (name.equals(Rulesets.EVENT_CURRENT_RULESET)) {
                    processRuleset(rs, element);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processRuleset(RulesetsModel rulesetsModel, Element ruleset) {
        String name = "";
        String parentId = "";
        String id = ruleset.getAttributeValue("Id");
        Map<String, String> rules = new HashMap<String, String>();


        Iterator<?> children = ruleset.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String n = element.getName();
                String k = element.getAttributeValue("Id");
                String v = editor.getText(element);
                if (v == null) {
                    v = "";
                }
                if (n.equals(Rulesets.EVENT_CURRENT_RULES)) {
                    rules.put(k, v);
                } else if (n.equals("ParentId")) {
                    parentId = v;
                } else if (n.equals("Name")) {
                    name = v;
                }
            } catch ( Exception e ) {
            }
        }
        RulesetsModel.RulesetModel r = rulesetsModel.addRuleset(name, parentId, id);
        r.setAll(rules);
    }

    public void processClock(ScoreBoardModel scoreBoardModel, Element clock) {
        String id = clock.getAttributeValue("Id");
        ClockModel clockModel = scoreBoardModel.getClockModel(id);
        boolean requestStart = false;
        boolean requestStop = false;

        Iterator<?> children = clock.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));
                boolean isReset = Boolean.parseBoolean(element.getAttributeValue("reset"));

//FIXME - might be better way to handle changes/resets than an attribute...
                if ((null == value) && !isReset) {
                    continue;
                } else if (name.equals("Start") && Boolean.parseBoolean(value)) {
                    requestStart = true;
                } else if (name.equals("Stop") && Boolean.parseBoolean(value)) {
                    requestStop = true;
                } else if (name.equals("ResetTime") && Boolean.parseBoolean(value)) {
                    clockModel.resetTime();
                } else if (name.equals(Clock.EVENT_NAME)) {
                    clockModel.setName(value);
                } else if (name.equals(Clock.EVENT_NUMBER) && isChange) {
                    clockModel.changeNumber(Integer.parseInt(value));
                } else if (name.equals(Clock.EVENT_NUMBER) && !isChange) {
                    clockModel.setNumber(Integer.parseInt(value));
                } else if (name.equals(Clock.EVENT_MINIMUM_NUMBER)) {
                    clockModel.setMinimumNumber(Integer.parseInt(value));
                } else if (name.equals(Clock.EVENT_MAXIMUM_NUMBER)) {
                    clockModel.setMaximumNumber(Integer.parseInt(value));
                } else if (name.equals(Clock.EVENT_TIME) && isChange) {
                    clockModel.changeTime(Long.parseLong(value));
                } else if (name.equals(Clock.EVENT_TIME) && isReset) {
                    clockModel.resetTime();
                } else if (name.equals(Clock.EVENT_TIME) && !isChange && !isReset) {
                    clockModel.setTime(Long.parseLong(value));
                } else if (name.equals(Clock.EVENT_MINIMUM_TIME) && isChange) {
                    clockModel.changeMinimumTime(Long.parseLong(value));
                } else if (name.equals(Clock.EVENT_MINIMUM_TIME)) {
                    clockModel.setMinimumTime(Long.parseLong(value));
                } else if (name.equals(Clock.EVENT_MAXIMUM_TIME) && isChange) {
                    clockModel.changeMaximumTime(Long.parseLong(value));
                } else if (name.equals(Clock.EVENT_MAXIMUM_TIME)) {
                    clockModel.setMaximumTime(Long.parseLong(value));
                } else if (name.equals(Clock.EVENT_RUNNING) && Boolean.parseBoolean(value)) {
                    requestStart = true;
                } else if (name.equals(Clock.EVENT_RUNNING) && !Boolean.parseBoolean(value)) {
                    requestStop = true;
                } else if (name.equals(Clock.EVENT_DIRECTION)) {
                    clockModel.setCountDirectionDown(Boolean.parseBoolean(value));
                }
            } catch ( Exception e ) {
            }
        }
        // Process start/stops at the end to allow setting of options (direction/min/max/etc) on load
        if (requestStart) { clockModel.start(); }
        if (requestStop) { clockModel.stop(); }
    }

    public void processTeam(ScoreBoardModel scoreBoardModel, Element team) {
        String id = team.getAttributeValue("Id");
        TeamModel teamModel = scoreBoardModel.getTeamModel(id);

        Iterator<?> children = team.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));

                if (name.equals("AlternateName")) {
                    processAlternateName(teamModel, element);
                } else if (name.equals("Color")) {
                    processColor(teamModel, element);
                } else if (name.equals("Skater")) {
                    processSkater(teamModel, element);
                } else if (name.equals("Position")) {
                    processPosition(teamModel, element);
                } else if (null == value) {
                    continue;
                } else if (name.equals("Timeout") && Boolean.parseBoolean(value)) {
                    teamModel.timeout();
                } else if (name.equals("OfficialReview") && Boolean.parseBoolean(value)) {
                    teamModel.officialReview();
                } else if (name.equals(Team.EVENT_NAME)) {
                    teamModel.setName(value);
                } else if (name.equals(Team.EVENT_LOGO)) {
                    teamModel.setLogo(value);
                } else if (name.equals(Team.EVENT_SCORE) && isChange) {
                    teamModel.changeScore(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_LAST_SCORE) && isChange) {
                    teamModel.changeLastScore(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_SCORE) && !isChange) {
                    teamModel.setScore(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_LAST_SCORE) && !isChange) {
                    teamModel.setLastScore(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_TIMEOUTS) && isChange) {
                    teamModel.changeTimeouts(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_TIMEOUTS) && !isChange) {
                    teamModel.setTimeouts(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_OFFICIAL_REVIEWS) && isChange) {
                    teamModel.changeOfficialReviews(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_OFFICIAL_REVIEWS) && !isChange) {
                    teamModel.setOfficialReviews(Integer.parseInt(value));
                } else if (name.equals(Team.EVENT_IN_TIMEOUT)) {
                    teamModel.setInTimeout(Boolean.parseBoolean(value));
                } else if (name.equals(Team.EVENT_IN_OFFICIAL_REVIEW)) {
                    teamModel.setInOfficialReview(Boolean.parseBoolean(value));
                } else if (name.equals(Team.EVENT_RETAINED_OFFICIAL_REVIEW)) {
                    teamModel.setRetainedOfficialReview(Boolean.parseBoolean(value));
                } else if (name.equals(Team.EVENT_LEAD_JAMMER)) {
                    teamModel.setLeadJammer(value);
                } else if (name.equals(Team.EVENT_STAR_PASS)) {
                    teamModel.setStarPass(Boolean.parseBoolean(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processAlternateName(TeamModel teamModel, Element alternateName) {
        String id = alternateName.getAttributeValue("Id");
        TeamModel.AlternateNameModel alternateNameModel = teamModel.getAlternateNameModel(id);

        if (editor.hasRemovePI(alternateName)) {
            teamModel.removeAlternateNameModel(id);
            return;
        }

        if (null == alternateNameModel) {
            teamModel.setAlternateNameModel(id, "");
            alternateNameModel = teamModel.getAlternateNameModel(id);
        }

        Iterator<?> children = alternateName.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                if (null == value) {
                    continue;
                } else if (name.equals(Team.AlternateName.EVENT_NAME)) {
                    alternateNameModel.setName(value);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processColor(TeamModel teamModel, Element color) {
        String id = color.getAttributeValue("Id");
        TeamModel.ColorModel colorModel = teamModel.getColorModel(id);

        if (editor.hasRemovePI(color)) {
            teamModel.removeColorModel(id);
            return;
        }

        if (null == colorModel) {
            teamModel.setColorModel(id, "");
            colorModel = teamModel.getColorModel(id);
        }

        Iterator<?> children = color.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                if (null == value) {
                    continue;
                } else if (name.equals(Team.Color.EVENT_COLOR)) {
                    colorModel.setColor(value);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPosition(TeamModel teamModel, Element position) {
        String id = position.getAttributeValue("Id");
        PositionModel positionModel = teamModel.getPositionModel(id);

        Iterator<?> children = position.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                if (null == value) {
                    continue;
                } else if (name.equals("Clear") && Boolean.parseBoolean(value)) {
                    positionModel.clear();
                } else if (name.equals("Id")) {
                    positionModel.setSkaterModel(value);
                } else if (name.equals(Position.EVENT_PENALTY_BOX)) {
                    positionModel.setPenaltyBox(Boolean.parseBoolean(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processSkater(TeamModel teamModel, Element skater) {
        String id = skater.getAttributeValue("Id");
        SkaterModel skaterModel;

        if (editor.hasRemovePI(skater)) {
            teamModel.removeSkaterModel(id);
            return;
        }

        try {
            skaterModel = teamModel.getSkaterModel(id);
        } catch ( SkaterNotFoundException snfE ) {
            Element nameE = skater.getChild(Skater.EVENT_NAME);
            String name = (nameE == null ? "" : editor.getText(nameE));
            Element numberE = skater.getChild(Skater.EVENT_NUMBER);
            String number = (numberE == null ? "" : editor.getText(numberE));
            Element flagsE = skater.getChild(Skater.EVENT_FLAGS);
            String flags = (flagsE == null ? "" : editor.getText(flagsE));
            teamModel.addSkaterModel(id, name, number, flags);
            skaterModel = teamModel.getSkaterModel(id);
        }

        Iterator<?> children = skater.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                if (name.equals(Skater.EVENT_PENALTY)) {
                    processPenalty(skaterModel, element, false);
                } else if (name.equals(Skater.EVENT_PENALTY_FOEXP)) {
                    processPenalty(skaterModel, element.getChild(Skater.EVENT_PENALTY), true);
                } else if (null == value) {
                    continue;
                } else if (name.equals(Skater.EVENT_NAME)) {
                    skaterModel.setName(value);
                } else if (name.equals(Skater.EVENT_NUMBER)) {
                    skaterModel.setNumber(value);
                } else if (name.equals(Skater.EVENT_POSITION)) {
                    skaterModel.setPosition(value);
                } else if (name.equals(Skater.EVENT_PENALTY_BOX)) {
                    skaterModel.setPenaltyBox(Boolean.parseBoolean(value));
                } else if (name.equals(Skater.EVENT_FLAGS)) {
                    skaterModel.setFlags(value);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPenalty(SkaterModel skaterModel, Element penalty, boolean foulout_exp) {
        String id = penalty.getAttributeValue("Id");
        int period = 0;
        int jam = 0;
        String code = "";

        Iterator<?> children = penalty.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                if (null == value) {
                    continue;
                } else if (name.equals(Skater.EVENT_PENALTY_PERIOD)) {
                    period = Integer.parseInt(value);
                } else if (name.equals(Skater.EVENT_PENALTY_JAM)) {
                    jam = Integer.parseInt(value);
                } else if (name.equals(Skater.EVENT_PENALTY_CODE)) {
                    code = value;
                }
            } catch ( Exception e ) {
            }
        }
        skaterModel.AddPenaltyModel(id, foulout_exp, period, jam, code);
    }

    public void processStats(StatsModel statsModel, Element stats) {
        Iterator<?> children = stats.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String id = element.getAttributeValue("Id");
                String name = element.getName();

                if (name.equals("Period")) {
                    int p = Integer.parseInt(id);
                    statsModel.ensureAtLeastNPeriods(p);
                    processPeriodStats(statsModel.getPeriodStatsModel(p), element);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPeriodStats(StatsModel.PeriodStatsModel periodStatsModel, Element periodStats) {
        Iterator<?> children = periodStats.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String id = element.getAttributeValue("Id");
                String name = element.getName();

                if (name.equals("Jam")) {
                    int j = Integer.parseInt(id);
                    periodStatsModel.ensureAtLeastNJams(j);
                    processJamStats(periodStatsModel.getJamStatsModel(j), element);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processJamStats(StatsModel.JamStatsModel jamStatsModel, Element jamStats) {
        Iterator<?> children = jamStats.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String id = element.getAttributeValue("Id");
                String name = element.getName();
                String value = editor.getText(element);

                if (name.equals("Team")) {
                    processTeamStats(jamStatsModel.getTeamStatsModel(id), element);
                } else if (null == value) {
                    continue;
                } else if (name.equals("JamClockElapsedEnd")) {
                    jamStatsModel.setJamClockElapsedEnd(Long.parseLong(value));
                } else if (name.equals("PeriodClockElapsedStart")) {
                    jamStatsModel.setPeriodClockElapsedStart(Long.parseLong(value));
                } else if (name.equals("PeriodClockElapsedEnd")) {
                    jamStatsModel.setPeriodClockElapsedEnd(Long.parseLong(value));
                } else if (name.equals("PeriodClockWalltimeStart")) {
                    jamStatsModel.setPeriodClockWalltimeStart(Long.parseLong(value));
                } else if (name.equals("PeriodClockWalltimeEnd")) {
                    jamStatsModel.setPeriodClockWalltimeEnd(Long.parseLong(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processTeamStats(StatsModel.TeamStatsModel teamStatsModel, Element teamStats) {
        Iterator<?> children = teamStats.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String id = element.getAttributeValue("Id");
                String name = element.getName();
                String value = editor.getText(element);

                if (name.equals("Skater")) {
                    teamStatsModel.addSkaterStatsModel(id);
                    processSkaterStats(teamStatsModel.getSkaterStatsModel(id), element);
                } else if (null == value) {
                    continue;
                } else if (name.equals("JamScore")) {
                    teamStatsModel.setJamScore(Integer.parseInt(value));
                } else if (name.equals("TotalScore")) {
                    teamStatsModel.setTotalScore(Integer.parseInt(value));
                } else if (name.equals("LeadJammer")) {
                    teamStatsModel.setLeadJammer(value);
                } else if (name.equals("StarPass")) {
                    teamStatsModel.setStarPass(Boolean.parseBoolean(value));
                } else if (name.equals("Timeouts")) {
                    teamStatsModel.setTimeouts(Integer.parseInt(value));
                } else if (name.equals("OfficialReviews")) {
                    teamStatsModel.setOfficialReviews(Integer.parseInt(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processSkaterStats(StatsModel.SkaterStatsModel skaterStatsModel, Element skaterStats) {
        Iterator<?> children = skaterStats.getChildren().iterator();
        while (children.hasNext()) {
            Element element = (Element)children.next();
            try {
                String name = element.getName();
                String value = editor.getText(element);

                if (null == value) {
                    continue;
                } else if (name.equals("PenaltyBox")) {
                    skaterStatsModel.setPenaltyBox(Boolean.parseBoolean(value));
                } else if (name.equals("Position")) {
                    skaterStatsModel.setPosition(value);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public static ScoreBoardXmlConverter getInstance() { return scoreBoardXmlConverter; }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected XMLOutputter rawXmlOutputter = XmlDocumentEditor.getRawXmlOutputter();

    private static ScoreBoardXmlConverter scoreBoardXmlConverter = new ScoreBoardXmlConverter();
}
