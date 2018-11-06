package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;

import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.model.SettingsModel;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.Settings;
import com.carolinarollergirls.scoreboard.view.Position;
import com.carolinarollergirls.scoreboard.view.Rulesets;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Skater;
import com.carolinarollergirls.scoreboard.view.Stats;
import com.carolinarollergirls.scoreboard.view.Team;

/**
 * Converts a ScoreBoardEvent into a representative XML Document or XML String.
 *
 * This class is not synchronized.	Each event method modifies the same document.
 */
public class ScoreBoardXmlListener implements ScoreBoardListener {
    public ScoreBoardXmlListener() { }
    public ScoreBoardXmlListener(boolean p) {
        setPersistent(p);
    }
    public ScoreBoardXmlListener(ScoreBoard sb, boolean p) {
        setPersistent(p);
        sb.addScoreBoardListener(new AsyncScoreBoardListener(this));
    }
    public ScoreBoardXmlListener(ScoreBoard sb) {
        sb.addScoreBoardListener(new AsyncScoreBoardListener(this));
    }

    public boolean isEmpty() { return empty; }

    public Document getDocument() { return document; }

    public Document resetDocument() {
        Document oldDoc = document;
        empty = true;
        document = editor.createDocument("ScoreBoard");
        return oldDoc;
    }

    private void batchStart() {
        Element root = document.getRootElement();
        String b = root.getAttributeValue("BATCH_START");
        if (b == null) {
            b = "";
        }
        b = b + "X";
        root.setAttribute("BATCH_START", b);
    }

    private void batchEnd() {
        Element root = document.getRootElement();
        String b = root.getAttributeValue("BATCH_END");
        if (b == null) {
            b = "";
        }
        b = b + "X";
        root.setAttribute("BATCH_END", b);
    }

    public void scoreBoardChange(ScoreBoardEvent event) {
        ScoreBoardEventProvider p = event.getProvider();
        String prop = event.getProperty();
        String v = (event.getValue()==null?null:event.getValue().toString());
        if (prop.equals(ScoreBoardEvent.BATCH_START)) {
            batchStart();
        } else if (prop.equals(ScoreBoardEvent.BATCH_END)) {
            batchEnd();
        } else if (p.getProviderName().equals("Settings")) {
            Element e = editor.setElement(getScoreBoardElement(), "Settings");
            if (v == null) {
                if (isPersistent()) {
                    editor.removeElement(e, Settings.EVENT_SETTING, prop);
                } else {
                    editor.setRemovePI(editor.setElement(e, Settings.EVENT_SETTING, prop));
                }
            } else {
                editor.setElement(e, Settings.EVENT_SETTING, prop, v);
            }
        } else if (p.getProviderName().equals("Rulesets")) {
            if (prop.equals(Rulesets.EVENT_REMOVE_RULESET)) {
                Element e = editor.setElement(getScoreBoardElement(), "KnownRulesets");
                if (isPersistent()) {
                    editor.removeElement(e, "Ruleset", ((Rulesets.Ruleset)event.getValue()).getId());
                } else {
                    editor.setRemovePI(converter.toElement(e, (Rulesets.Ruleset)event.getValue()));
                }
            } else {
                converter.toElement(getScoreBoardElement(), (Rulesets)p);
            }
        } else if (p.getProviderName().equals("Ruleset")) {
            Element e = editor.setElement(getScoreBoardElement(), "KnownRulesets");
            Rulesets.Ruleset r = (Rulesets.Ruleset)p;
            converter.toElement(e, r);
            // Look for overrides that have been removed.
            Element re = editor.setElement(e, "Ruleset", r.getId());
            if (event.getPreviousValue() != null) {
                Set<String> newKeys = r.getAll().keySet();
                for (Object o : (Set<?>)event.getPreviousValue()) {
                    String k = (String)o;
                    if (!newKeys.contains(k)) {
                        if (isPersistent()) {
                            editor.removeElement(re, "Rule", k);
                        } else {
                            editor.setRemovePI(editor.setElement(re, "Rule", k));
                        }
                    }
                }
            }
        } else if (p.getProviderName().equals("ScoreBoard")) {
            if (prop.equals(ScoreBoard.EVENT_ADD_CLOCK)) {
                converter.toElement(getScoreBoardElement(), (Clock)event.getValue());
            } else if (prop.equals(ScoreBoard.EVENT_REMOVE_CLOCK)) {
                if (isPersistent()) {
                    editor.removeElement(getScoreBoardElement(), "Clock", ((Clock)event.getValue()).getId());
                } else {
                    editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Clock)event.getValue()));
                }
            } else if (prop.equals(ScoreBoard.EVENT_ADD_TEAM)) {
                converter.toElement(getScoreBoardElement(), (Team)event.getValue());
            } else if (prop.equals(ScoreBoard.EVENT_REMOVE_TEAM)) {
                if (isPersistent()) {
                    editor.removeElement(getScoreBoardElement(), "Team", ((Team)event.getValue()).getId());
                } else {
                    editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Team)event.getValue()));
                }
            } else {
                editor.setElement(getScoreBoardElement(), prop, null, v);
            }
        } else if (p.getProviderName().equals("Team")) {
            if (prop.equals(Team.EVENT_ADD_ALTERNATE_NAME)) {
                converter.toElement(getTeamElement((Team)p), (Team.AlternateName)event.getValue());
            } else if (prop.equals(Team.EVENT_REMOVE_ALTERNATE_NAME)) {
                if (isPersistent()) {
                    editor.removeElement(getTeamElement((Team)p), "AlternateName", ((Team.AlternateName)event.getValue()).getId());
                } else {
                    editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Team.AlternateName)event.getValue()));
                }
            } else if (prop.equals(Team.EVENT_ADD_COLOR)) {
                converter.toElement(getTeamElement((Team)p), (Team.Color)event.getValue());
            } else if (prop.equals(Team.EVENT_REMOVE_COLOR)) {
                if (isPersistent()) {
                    editor.removeElement(getTeamElement((Team)p), "Color", ((Team.Color)event.getValue()).getId());
                } else {
                    editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Team.Color)event.getValue()));
                }
            } else if (prop.equals(Team.EVENT_ADD_SKATER)) {
                converter.toElement(getTeamElement((Team)p), (Skater)event.getValue());
            } else if (prop.equals(Team.EVENT_REMOVE_SKATER)) {
                if (isPersistent()) {
                    editor.removeElement(getTeamElement((Team)p), "Skater", ((Skater)event.getValue()).getId());
                } else {
                    editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Skater)event.getValue()));
                }
            } else {
                editor.setElement(getTeamElement((Team)p), prop, null, v);
            }
        } else if (p.getProviderName().equals("Position")) {
            Element e = getPositionElement((Position)p);
            if (prop.equals(Position.EVENT_SKATER)) {
                Skater s = (Skater)event.getValue();
                editor.setElement(e, "Id", null, (s==null?"":s.getId()));
                editor.setElement(e, "Name", null, (s==null?"":s.getName()));
                editor.setElement(e, "Number", null, (s==null?"":s.getNumber()));
                editor.setElement(e, "PenaltyBox", null, String.valueOf(s==null?false:s.isPenaltyBox()));
                editor.setElement(e, "Flags", null, (s==null?"":s.getFlags()));
            } else if (prop.equals(Position.EVENT_PENALTY_BOX)) {
                editor.setElement(e, "PenaltyBox", null, String.valueOf(event.getValue()));
            }
        } else if (p.getProviderName().equals("AlternateName")) {
            editor.setElement(getAlternateNameElement((Team.AlternateName)p), prop, null, v);
        } else if (p.getProviderName().equals("Color")) {
            editor.setElement(getColorElement((Team.Color)p), prop, null, v);
        } else if (p.getProviderName().equals("Skater")) {
            if (prop.equals(Skater.EVENT_PENALTY) || prop.equals(Skater.EVENT_PENALTY_FOEXP)) {
                // Replace whole skater.
                converter.toElement(getTeamElement(((Skater)p).getTeam()), (Skater)p);
            } else if (prop.equals(Skater.EVENT_REMOVE_PENALTY)) {
                Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
                if (prev != null) {
                    if (isPersistent()) {
                        editor.removeElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY, prev.getId());
                    } else {
                        editor.setRemovePI(editor.addElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY, prev.getId()));
                    }
                }
            } else if (prop.equals(Skater.EVENT_PENALTY_REMOVE_FOEXP)) {
                if (isPersistent()) {
                    editor.removeElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY_FOEXP);
                } else {
                    Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
                    if (prev != null) {
                        editor.setRemovePI(editor.addElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY_FOEXP, prev.getId()));
                    }
                }
            } else {
                editor.setElement(getSkaterElement((Skater)p), prop, null, v);
            }
        } else if (p.getProviderName().equals("Stats")) {
            Stats.PeriodStats ps = (Stats.PeriodStats)(event.getValue());
            Element e = getStatsElement();
            if (prop.equals(Stats.EVENT_REMOVE_PERIOD)) {
                if (isPersistent()) {
                    editor.removeElement(e, "PeriodStats", String.valueOf(ps.getPeriodNumber()));
                } else {
                    editor.setRemovePI(converter.toElement(e, ps));
                }
            } else if (prop.equals(Stats.EVENT_ADD_PERIOD)) {
                getPeriodStatsElement(ps);
            }
        } else if (p.getProviderName().equals("PeriodStats")) {
            Element e = getPeriodStatsElement((Stats.PeriodStats)p);
            Stats.JamStats js = (Stats.JamStats)(event.getValue());
            if (prop.equals(Stats.PeriodStats.EVENT_REMOVE_JAM)) {
                if (isPersistent()) {
                    editor.removeElement(e, "JamStats", String.valueOf(js.getJamNumber()));
                } else {
                    editor.setRemovePI(converter.toElement(e, js));
                }
            } else if (prop.equals(Stats.PeriodStats.EVENT_ADD_JAM)) {
                getJamStatsElement(js);
            }
        } else if (p.getProviderName().equals("JamStats")) {
            Element e = getJamStatsElement((Stats.JamStats)p);
            if (prop.equals(Stats.JamStats.EVENT_STATS)) {
                Stats.JamStats js = (Stats.JamStats)event.getValue();
                editor.setElement(e, "JamClockElapsedEnd", null, String.valueOf(js.getJamClockElapsedEnd()));
                editor.setElement(e, "PeriodClockElapsedStart", null, String.valueOf(js.getPeriodClockElapsedStart()));
                editor.setElement(e, "PeriodClockElapsedEnd", null, String.valueOf(js.getPeriodClockElapsedEnd()));
                editor.setElement(e, "PeriodClockWalltimeStart", null, String.valueOf(js.getPeriodClockWalltimeStart()));
                editor.setElement(e, "PeriodClockWalltimeEnd", null, String.valueOf(js.getPeriodClockWalltimeEnd()));
            }
        } else if (p.getProviderName().equals("TeamStats")) {
            Element e = getTeamStatsElement((Stats.TeamStats)p);
            if (prop.equals(Stats.TeamStats.EVENT_STATS)) {
                Stats.TeamStats ts = (Stats.TeamStats)event.getValue();
                editor.setElement(e, "JamScore", null, String.valueOf(ts.getJamScore()));
                editor.setElement(e, "TotalScore", null, String.valueOf(ts.getTotalScore()));
                editor.setElement(e, "LeadJammer", null, ts.getLeadJammer());
                editor.setElement(e, "StarPass", null, String.valueOf(ts.getStarPass()));
                editor.setElement(e, "Timeouts", null, String.valueOf(ts.getTimeouts()));
                editor.setElement(e, "OfficialReviews", null, String.valueOf(ts.getOfficialReviews()));
            } else if (prop.equals(Stats.TeamStats.EVENT_REMOVE_SKATER)) {
                Stats.SkaterStats ss = (Stats.SkaterStats)(event.getValue());
                if (isPersistent()) {
                    editor.removeElement(e, "SkaterStats", ss.getSkaterId());
                } else {
                    editor.setRemovePI(converter.toElement(e, ss));
                }
            }
        } else if (p.getProviderName().equals("SkaterStats")) {
            Element e = getSkaterStatsElement((Stats.SkaterStats)p);
            if (prop.equals(Stats.SkaterStats.EVENT_STATS)) {
                Stats.SkaterStats ss = (Stats.SkaterStats)event.getValue();
                editor.setElement(e, "Position", null, ss.getPosition());
                editor.setElement(e, "PenaltyBox", null, String.valueOf(ss.getPenaltyBox()));
            }
        } else if (p.getProviderName().equals("Clock")) {
            Element e = editor.setElement(getClockElement((Clock)p), prop, null, v);
            if (prop.equals("Time")) {
                try {
                    long time = ((Long)event.getValue()).longValue();
                    long prevTime = ((Long)event.getPreviousValue()).longValue();
                    if (time % 1000 == 0 || Math.abs(prevTime - time) >= 1000) {
                        editor.setPI(e, "TimeUpdate", "sec");
                    } else {
                        editor.setPI(e, "TimeUpdate", "ms");
                    }
                } catch (Exception ee) { }
            }
        } else {
            return;
        }
        empty = false;
    }

    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean p) { persistent = p; }

    protected Element getScoreBoardElement() {
        return editor.getElement(document.getRootElement(), "ScoreBoard");
    }

    protected Element getClockElement(Clock clock) {
        return editor.getElement(getScoreBoardElement(), "Clock", clock.getId());
    }

    protected Element getTeamElement(Team team) {
        return editor.getElement(getScoreBoardElement(), "Team", team.getId());
    }

    protected Element getPositionElement(Position position) {
        return editor.getElement(getTeamElement(position.getTeam()), "Position", position.getId());
    }

    protected Element getSkaterElement(Skater skater) {
        return editor.getElement(getTeamElement(skater.getTeam()), "Skater", skater.getId());
    }

    protected Element getAlternateNameElement(Team.AlternateName alternateName) {
        return editor.getElement(getTeamElement(alternateName.getTeam()), "AlternateName", alternateName.getId());
    }

    protected Element getColorElement(Team.Color color) {
        return editor.getElement(getTeamElement(color.getTeam()), "Color", color.getId());
    }

    protected Element getStatsElement() {
        return editor.getElement(getScoreBoardElement(), "Stats");
    }

    protected Element getPeriodStatsElement(Stats.PeriodStats ps) {
        return editor.getElement(getStatsElement(), "Period", String.valueOf(ps.getPeriodNumber()));
    }

    protected Element getJamStatsElement(Stats.JamStats js) {
        return editor.getElement(
                   editor.getElement(
                       getStatsElement(), "Period", String.valueOf(js.getPeriodNumber())),
                   "Jam", String.valueOf(js.getJamNumber()));
    }

    protected Element getTeamStatsElement(Stats.TeamStats ts) {
        return editor.getElement(
                   editor.getElement(
                       editor.getElement(
                           getStatsElement(), "Period", String.valueOf(ts.getPeriodNumber())),
                       "Jam", String.valueOf(ts.getJamNumber())),
                   "Team", ts.getTeamId());
    }

    protected Element getSkaterStatsElement(Stats.SkaterStats ss) {
        return editor.getElement(
                   editor.getElement(
                       editor.getElement(
                           editor.getElement(
                               getStatsElement(), "Period", String.valueOf(ss.getPeriodNumber())),
                           "Jam", String.valueOf(ss.getJamNumber())),
                       "Team", ss.getTeamId()),
                   "Skater", ss.getSkaterId());
    }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

    protected Document document = editor.createDocument("ScoreBoard");
    protected boolean empty = true;
    protected boolean persistent = false;
}
