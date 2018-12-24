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

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl.TimeoutOwners;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

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

        editor.setElement(sb, ScoreBoard.Value.TIMEOUT_OWNER, null, scoreBoard.getTimeoutOwner().getId());
        editor.setElement(sb, ScoreBoard.Value.OFFICIAL_REVIEW, null, String.valueOf(scoreBoard.isOfficialReview()));
        editor.setElement(sb, ScoreBoard.Value.IN_OVERTIME, null, String.valueOf(scoreBoard.isInOvertime()));
        editor.setElement(sb, ScoreBoard.Value.IN_PERIOD, null, String.valueOf(scoreBoard.isInPeriod()));
        editor.setElement(sb, ScoreBoard.Value.OFFICIAL_SCORE, null, String.valueOf(scoreBoard.isOfficialScore()));

        toElement(sb, scoreBoard.getSettings());
        toElement(sb, scoreBoard.getRulesets());
        toElement(sb, scoreBoard.getMedia());

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
                editor.setElement(e, Settings.Child.SETTING, k, v);
            }
        }
        return e;
    }

    public Element toElement(Element p, Rulesets rs) {
        Element e = editor.setElement(p, "Rules");
        for (Rule r : Rule.values()) {
            editor.setElement(e, Rulesets.Value.RULE, r.toString(), rs.get(r));
        }
        editor.setElement(e, "Id", null, rs.getId());
        editor.setElement(e, "Name", null, rs.getName());

        e = editor.setElement(p, Rulesets.Child.KNOWN_RULESETS);
        for (Rulesets.Ruleset r :  rs.getRulesets().values()) {
            toElement(e, r);
        }
        return e;
    }

    public Element toElement(Element p, Rulesets.Ruleset r) {
        Element e = editor.setElement(p, "Ruleset", r.getId());
        for (Rule k : r.getAll().keySet()) {
            editor.setElement(e, Rulesets.Value.RULE, k.toString(), r.get(k));
        }
        editor.setElement(e, "Name", null, r.getName());
        editor.setElement(e, "ParentId", null, r.getParentRulesetId());
        return e;
    }

    public Element toElement(Element sb, Media m) {
        Element e = editor.setElement(sb, "Media");
        for (String format : m.getFormats()) {
            Element f = editor.setElement(e, format);
            for (String type : m.getTypes(format)) {
                Element t = editor.setElement(f, type);
                for (Media.MediaFile mf: m.getMediaFiles(format, type).values()) {
                    Element fi = editor.setElement(t, "File", mf.getId());
                    toElement(fi, mf);
                }
            }
        }
        return e;
    }

    public Element toElement(Element e, Media.MediaFile mf) {
        editor.setElement(e, "Name", null, mf.getName());
        editor.setElement(e, "Src", null, mf.getSrc());
        return e;
    }

    public Element toElement(Element sb, Clock c) {
        Element e = editor.setElement(sb, "Clock", c.getId());

        editor.setElement(e, "Start", null, "");
        editor.setElement(e, "UnStart", null, "");
        editor.setElement(e, "Stop", null, "");
        editor.setElement(e, "UnStop", null, "");
        editor.setElement(e, "ResetTime", null, "");

        editor.setElement(e, Clock.Value.NAME, null, c.getName());
        editor.setElement(e, Clock.Value.NUMBER, null, String.valueOf(c.getNumber()));
        editor.setElement(e, Clock.Value.MINIMUM_NUMBER, null, String.valueOf(c.getMinimumNumber()));
        editor.setElement(e, Clock.Value.MAXIMUM_NUMBER, null, String.valueOf(c.getMaximumNumber()));
        editor.setElement(e, Clock.Value.TIME, null, String.valueOf(c.getTime()));
        editor.setElement(e, Clock.Value.INVERTED_TIME, null, String.valueOf(c.getInvertedTime()));
        editor.setElement(e, Clock.Value.MINIMUM_TIME, null, String.valueOf(c.getMinimumTime()));
        editor.setElement(e, Clock.Value.MAXIMUM_TIME, null, String.valueOf(c.getMaximumTime()));
        editor.setElement(e, Clock.Value.RUNNING, null, String.valueOf(c.isRunning()));
        editor.setElement(e, Clock.Value.DIRECTION, null, String.valueOf(c.isCountDirectionDown()));
        return e;
    }

    public Element toElement(Element sb, Team t) {
        Element e = editor.setElement(sb, "Team", t.getId());

        editor.setElement(e, "Timeout", null, "");
        editor.setElement(e, "OfficialReview", null, "");

        editor.setElement(e, Team.Value.NAME, null, t.getName());
        editor.setElement(e, Team.Value.LOGO, null, t.getLogo());
        editor.setElement(e, Team.Value.SCORE, null, String.valueOf(t.getScore()));
        editor.setElement(e, Team.Value.LAST_SCORE, null, String.valueOf(t.getLastScore()));
        editor.setElement(e, Team.Value.TIMEOUTS, null, String.valueOf(t.getTimeouts()));
        editor.setElement(e, Team.Value.OFFICIAL_REVIEWS, null, String.valueOf(t.getOfficialReviews()));
        editor.setElement(e, Team.Value.IN_TIMEOUT, null, String.valueOf(t.inTimeout()));
        editor.setElement(e, Team.Value.IN_OFFICIAL_REVIEW, null, String.valueOf(t.inOfficialReview()));
        editor.setElement(e, Team.Value.RETAINED_OFFICIAL_REVIEW, null, String.valueOf(t.retainedOfficialReview()));
        editor.setElement(e, Team.Value.LEAD_JAMMER, null, t.getLeadJammer());
        editor.setElement(e, Team.Value.STAR_PASS, null, String.valueOf(t.isStarPass()));
        editor.setElement(e, Team.Value.NO_PIVOT, null, String.valueOf(t.hasNoPivot()));

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

        editor.setElement(e, Team.AlternateName.Value.NAME, null, n.getName());

        return e;
    }

    public Element toElement(Element team, Team.Color c) {
        Element e = editor.setElement(team, "Color", c.getId());

        editor.setElement(e, Team.Color.Value.COLOR, null, c.getColor());

        return e;
    }

    public Element toElement(Element team, Position p) {
        Element e = editor.setElement(team, "Position", p.getId());

        editor.setElement(e, "Clear", null, "");

        Skater s = p.getSkater();
        editor.setElement(e, "Id", null, (s==null?"":s.getId()));
        editor.setElement(e, Skater.Value.NAME, null, (s==null?"":s.getName()));
        editor.setElement(e, Skater.Value.NUMBER, null, (s==null?"":s.getNumber()));
        editor.setElement(e, Skater.Value.PENALTY_BOX, null, String.valueOf(s==null?false:s.isPenaltyBox()));
        editor.setElement(e, Skater.Value.FLAGS, null, (s==null?"":s.getFlags()));

        return e;
    }

    public Element toElement(Element t, Skater s) {
        Element e = editor.setElement(t, "Skater", s.getId());
        editor.setElement(e, Skater.Value.NAME, null, s.getName());
        editor.setElement(e, Skater.Value.NUMBER, null, s.getNumber());
        editor.setElement(e, Skater.Value.POSITION, null, s.getPosition() == null ? "" : 
            s.getPosition().getFloorPosition().toString());
        editor.setElement(e, Skater.Value.ROLE, null, s.getRole().toString());
        editor.setElement(e, Skater.Value.PENALTY_BOX, null, String.valueOf(s.isPenaltyBox()));
        editor.setElement(e, Skater.Value.FLAGS, null, s.getFlags());

        for (Skater.Penalty p: s.getPenalties()) {
            toElement(e, p);
        }

        if (s.getFOEXPPenalty() != null) {
            Element fe = editor.setElement(e, Skater.Value.PENALTY_FOEXP, s.getFOEXPPenalty().getId());
            toElement(fe, s.getFOEXPPenalty());
        }

        return e;
    }

    public Element toElement(Element s, Skater.Penalty p) {
        Element e = editor.setElement(s, "Penalty", p.getId());
        editor.setElement(e, Skater.Penalty.Value.PERIOD, null, String.valueOf(p.getPeriod()));
        editor.setElement(e, Skater.Penalty.Value.JAM, null, String.valueOf(p.getJam()));
        editor.setElement(e, Skater.Penalty.Value.CODE, null, p.getCode());
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

    public void processDocument(ScoreBoard scoreBoard, Document document) {
        Iterator<?> children = document.getRootElement().getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            if (child.getName().equals("ScoreBoard")) {
                processScoreBoard(scoreBoard, child);
            }
        }
    }

    public void processScoreBoard(ScoreBoard scoreBoard, Element element) {
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, scoreBoard.getProperties());
                boolean bVal = Boolean.parseBoolean(value);

                if (name.equals("Clock")) {
                    processClock(scoreBoard, child);
                } else if (name.equals("Team")) {
                    processTeam(scoreBoard, child);
                } else if (name.equals("Settings")) {
                    processSettings(scoreBoard, child);
                } else if (name.equals("Rules")) {
                    processRules(scoreBoard, child);
                } else if (name.equals("KnownRulesets")) {
                    processRulesets(scoreBoard, child);
                } else if (name.equals("Media")) {
                    processMedia(scoreBoard.getMedia(), child);
                } else if (name.equals("Stats")) {
                    processStats(scoreBoard.getStats(), child);
                } else if (null == value) {
                    continue;
                } else if (prop instanceof ScoreBoard.Value) {
                    switch ((ScoreBoard.Value)prop) {
		    case TIMEOUT_OWNER:
			scoreBoard.setTimeoutOwner(scoreBoard.getTimeoutOwner(value));
			break;
		    case OFFICIAL_REVIEW:
			scoreBoard.setOfficialReview(bVal);
			break;
		    case IN_OVERTIME:
			scoreBoard.setInOvertime(bVal);
			break;
		    case IN_PERIOD:
			scoreBoard.setInPeriod(bVal);
			break;
		    case OFFICIAL_SCORE:
			scoreBoard.setOfficialScore(bVal);
			break;
                    }
                } else if (bVal) {
                    if (name.equals("Reset")) {
                        scoreBoard.reset();
                    } else if (name.equals("StartJam")) {
                        scoreBoard.startJam();
                    } else if (name.equals("StopJam")) {
                        scoreBoard.stopJamTO();
                    } else if (name.equals("Timeout")) {
                        scoreBoard.timeout();
                    } else if (name.equals("ClockUndo")) {
                        scoreBoard.clockUndo(false);
                    } else if (name.equals("ClockReplace")) {
                        scoreBoard.clockUndo(true);
                    } else if (name.equals("StartOvertime")) {
                        scoreBoard.startOvertime();
                    } else if (name.equals("OfficialTimeout")) {
                        scoreBoard.setTimeoutType(TimeoutOwners.OTO, false);
                    }
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processSettings(ScoreBoard scoreBoard, Element element) {
        Settings sm = scoreBoard.getSettings();
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String k = child.getAttributeValue("Id");
                String v = editor.getText(child);
                if (v == null) {
                    v = "";
                }
                sm.set(k, v);
            } catch ( Exception e ) {
            }
        }
    }

    public void processRules(ScoreBoard scoreBoard, Element element) {
        Rulesets rs = scoreBoard.getRulesets();
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                Rule k = rs.getRule(child.getAttributeValue("Id"));
                String v = editor.getText(child);
                if (v == null) {
                    v = "";
                }
                if (name.equals(PropertyConversion.toFrontend(Rulesets.Value.RULE))) {
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

    public void processRulesets(ScoreBoard scoreBoard, Element element) {
        Rulesets rs = scoreBoard.getRulesets();
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                Property prop = PropertyConversion.fromFrontend(child.getName(), rs.getProperties());
                if (prop == Rulesets.Value.RULESET) {
                    processRuleset(rs, child);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processRuleset(Rulesets rulesets, Element element) {
        String name = "";
        String parentId = "";
        String id = element.getAttributeValue("Id");
        Map<Rule, String> rules = new HashMap<Rule, String>();


        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String n = child.getName();
                Rule k = rulesets.getRule(child.getAttributeValue("Id"));
                String v = editor.getText(child);
                if (v == null) {
                    v = "";
                }
                if (n.equals(PropertyConversion.toFrontend(Rulesets.Value.RULE))) {
                    rules.put(k, v);
                } else if (n.equals("ParentId")) {
                    parentId = v;
                } else if (n.equals("Name")) {
                    name = v;
                }
            } catch ( Exception e ) {
            }
        }
        Rulesets.Ruleset r = rulesets.addRuleset(name, parentId, id);
        r.setAll(rules);
    }

    public void processMedia(Media media, Element element) {
        for (Object f: element.getChildren()) {
            Element format = (Element)f;
            for (Object t: format.getChildren()) {
                Element type = (Element)t;
                Map<String, Media.MediaFile> tm = media.getMediaFiles(format.getName(), type.getName());
                if (tm == null) {
                    continue;  // Invalid type.
                }
                for (Object fi: type.getChildren()) {
                    Element file = (Element)fi;
                    Media.MediaFile mf = tm.get(file.getAttributeValue("Id"));
                    if (mf == null) {
                        continue;  // The file has been deleted.
                    }
                    for (Object p: file.getChildren()) {
                        Element prop = (Element)p;
                        if (prop.getName() == "Name") {
                            // Only the name can come from the XML.
                            mf.setName(editor.getText(prop));
                        }
                    }
                }
            }
        }
    }

    public void processClock(ScoreBoard scoreBoard, Element element) {
        String id = element.getAttributeValue("Id");
        Clock clock = scoreBoard.getClock(id);
        boolean requestStart = false;
        boolean requestStop = false;

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, clock.getProperties());

                boolean isChange = Boolean.parseBoolean(child.getAttributeValue("change"));
                boolean isReset = Boolean.parseBoolean(child.getAttributeValue("reset"));

//FIXME - might be better way to handle changes/resets than an attribute...
                if ((null == value) && !isReset) {
                    continue;
                } else if (name.equals("Start") && Boolean.parseBoolean(value)) {
                    requestStart = true;
                } else if (name.equals("Stop") && Boolean.parseBoolean(value)) {
                    requestStop = true;
                } else if (name.equals("ResetTime") && Boolean.parseBoolean(value)) {
                    clock.resetTime();
                } else if (prop instanceof Clock.Value) {
                    switch ((Clock.Value)prop) {
		    case NAME:
			clock.setName(value);
			break;
		    case NUMBER:
			if (isChange) {
	                    clock.changeNumber(Integer.parseInt(value));
			} else {
	                    clock.setNumber(Integer.parseInt(value));
			}
			break;
		    case MINIMUM_NUMBER:
			clock.setMinimumNumber(Integer.parseInt(value));
			break;
		    case MAXIMUM_NUMBER:
			clock.setMaximumNumber(Integer.parseInt(value));
			break;
		    case TIME:
			if (isChange) {
	                    clock.changeTime(Long.parseLong(value));
			} else if (isReset) {
	                    clock.resetTime();
			} else {
	                    clock.setTime(Long.parseLong(value));
			}
			break;
		    case MINIMUM_TIME:
			if (isChange) {
	                    clock.changeMinimumTime(Long.parseLong(value));
			} else {
	                    clock.setMinimumTime(Long.parseLong(value));
			}
			break;
		    case MAXIMUM_TIME:
			if (isChange) {
	                    clock.changeMaximumTime(Long.parseLong(value));
			} else {
	                    clock.setMaximumTime(Long.parseLong(value));
			}
			break;
		    case RUNNING:
			if (Boolean.parseBoolean(value)) {
			    requestStart = true;
			} else {
			    requestStop = true;
			}
			break;
		    case DIRECTION:
			clock.setCountDirectionDown(Boolean.parseBoolean(value));
			break;
		    case INVERTED_TIME:
			//read only value
			break;
                    }
                }
            } catch ( Exception e ) {
            }
        }
        // Process start/stops at the end to allow setting of options (direction/min/max/etc) on load
        if (requestStart) { clock.start(); }
        if (requestStop) { clock.stop(); }
    }

    public void processTeam(ScoreBoard scoreBoard, Element element) {
        String id = element.getAttributeValue("Id");
        Team team = scoreBoard.getTeam(id);

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, team.getProperties());

                boolean isChange = Boolean.parseBoolean(child.getAttributeValue("change"));

                if (prop instanceof Team.Child) {
                    switch ((Team.Child)prop) {
		    case ALTERNATE_NAME:
			processAlternateName(team, child);
			break;
		    case COLOR:
			processColor(team, child);
			break;
		    case SKATER:
			processSkater(team, child);
			break;
		    case POSITION:
			processPosition(team, child);
			break;
                    }
                } else if (null == value) {
                    continue;
                } else if (name.equals("Timeout") && Boolean.parseBoolean(value)) {
                    team.timeout();
                } else if (name.equals("OfficialReview") && Boolean.parseBoolean(value)) {
                    team.officialReview();
                } else if (prop instanceof Team.Value) {
                    switch ((Team.Value)prop) {
		    case NAME:
			team.setName(value);
			break;
		    case LOGO:
			team.setLogo(value);
			break;
		    case SCORE:
			if (isChange) {
	                    team.changeScore(Integer.parseInt(value));
			} else {
	                    team.setScore(Integer.parseInt(value));
			}
			break;
		    case LAST_SCORE:
			if (isChange) {
	                    team.changeLastScore(Integer.parseInt(value));
			} else {
	                    team.setLastScore(Integer.parseInt(value));
			}
			break;
		    case NO_PIVOT:
			break;
		    case TIMEOUTS:
			if (isChange) {
	                    team.changeTimeouts(Integer.parseInt(value));
			} else {
	                    team.setTimeouts(Integer.parseInt(value));
			}
			break;
		    case OFFICIAL_REVIEWS:
			if (isChange) {
	                    team.changeOfficialReviews(Integer.parseInt(value));
			} else {
			    team.setOfficialReviews(Integer.parseInt(value));
			}
			break;
		    case IN_TIMEOUT:
			team.setInTimeout(Boolean.parseBoolean(value));
			break;
		    case IN_OFFICIAL_REVIEW:
			team.setInOfficialReview(Boolean.parseBoolean(value));
			break;
		    case RETAINED_OFFICIAL_REVIEW:
			team.setRetainedOfficialReview(Boolean.parseBoolean(value));
			break;
		    case LEAD_JAMMER:
			team.setLeadJammer(value);
			break;
		    case STAR_PASS:
			team.setStarPass(Boolean.parseBoolean(value));
			break;
                    }
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processAlternateName(Team team, Element element) {
        String id = element.getAttributeValue("Id");
        Team.AlternateName alternateName = team.getAlternateName(id);

        if (editor.hasRemovePI(element)) {
            team.removeAlternateName(id);
            return;
        }

        if (null == alternateName) {
            team.setAlternateName(id, "");
            alternateName = team.getAlternateName(id);
        }

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, alternateName.getProperties());

                if (null == value) {
                    continue;
                } else if (prop == Team.AlternateName.Value.NAME) {
                    alternateName.setName(value);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processColor(Team team, Element element) {
        String id = element.getAttributeValue("Id");
        Team.Color color = team.getColor(id);

        if (editor.hasRemovePI(element)) {
            team.removeColor(id);
            return;
        }

        if (null == color) {
            team.setColor(id, "");
            color = team.getColor(id);
        }

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, color.getProperties());

                if (null == value) {
                    continue;
                } else if (prop == Team.Color.Value.COLOR) {
                    color.setColor(value);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPosition(Team team, Element element) {
        String id = element.getAttributeValue("Id");
        Position position = team.getPosition(FloorPosition.fromString(id));

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, position.getProperties());

                if (null == value) {
                    continue;
                } else if (name.equals("Clear") && Boolean.parseBoolean(value)) {
                    team.field(null, position);
                } else if (name.equals("Id")) {
                    team.field(team.getSkater(value), position);
                } else if (prop == Position.Value.PENALTY_BOX) {
                    position.setPenaltyBox(Boolean.parseBoolean(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processSkater(Team team, Element element) {
        String id = element.getAttributeValue("Id");
        Skater skater;

        if (editor.hasRemovePI(element)) {
            team.removeSkater(id);
            return;
        }

        try {
            skater = team.getSkater(id);
        } catch ( SkaterNotFoundException snfE ) {
            Element nameE = element.getChild(PropertyConversion.toFrontend(Skater.Value.NAME));
            String name = (nameE == null ? "" : editor.getText(nameE));
            Element numberE = element.getChild(PropertyConversion.toFrontend(Skater.Value.NUMBER));
            String number = (numberE == null ? "" : editor.getText(numberE));
            Element flagsE = element.getChild(PropertyConversion.toFrontend(Skater.Value.FLAGS));
            String flags = (flagsE == null ? "" : editor.getText(flagsE));
            team.addSkater(id, name, number, flags);
            skater = team.getSkater(id);
        }

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);
                Property prop = PropertyConversion.fromFrontend(name, skater.getProperties());

                if (prop == Skater.Child.PENALTY) {
                    processPenalty(skater, child, false);
                } else if (prop == Skater.Value.PENALTY_FOEXP) {
                    processPenalty(skater, child.getChild(
                	    PropertyConversion.toFrontend(Skater.Child.PENALTY)), true);
                } else if (null == value) {
                    continue;
                } else if (prop instanceof Skater.Value) {
                    switch ((Skater.Value)prop) {
		    case NAME:
			skater.setName(value);
			break;
		    case NUMBER:
			skater.setNumber(value);
			break;
		    case POSITION:
			team.field(skater, team.getPosition(FloorPosition.fromString(value)));
			break;
		    case ROLE:
			team.field(skater, Role.fromString(value));
			break;
		    case BASE_ROLE:
			//read only
			break;
		    case PENALTY_BOX:
			skater.setPenaltyBox(Boolean.parseBoolean(value));
			break;
		    case PENALTY_FOEXP:
			//already handled
			break;
		    case FLAGS:
			skater.setFlags(value);
			break;
                    }
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPenalty(Skater skater, Element element, boolean foulout_exp) {
        String id = element.getAttributeValue("Id");
        int period = 0;
        int jam = 0;
        String code = "";

        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);

                if (null == value) {
                    continue;
                } else if (name.equals(PropertyConversion.toFrontend(Skater.Penalty.Value.PERIOD))) {
                    period = Integer.parseInt(value);
                } else if (name.equals(PropertyConversion.toFrontend(Skater.Penalty.Value.JAM))) {
                    jam = Integer.parseInt(value);
                } else if (name.equals(PropertyConversion.toFrontend(Skater.Penalty.Value.CODE))) {
                    code = value;
                }
            } catch ( Exception e ) {
            }
        }
        skater.AddPenalty(id, foulout_exp, period, jam, code);
    }

    public void processStats(Stats stats, Element element) {
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String id = child.getAttributeValue("Id");
                String name = child.getName();

                if (name.equals("Period")) {
                    int p = Integer.parseInt(id);
                    stats.ensureAtLeastNPeriods(p);
                    processPeriodStats(stats.getPeriodStats(p), child);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPeriodStats(Stats.PeriodStats periodStats, Element element) {
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String id = child.getAttributeValue("Id");
                String name = child.getName();

                if (name.equals("Jam")) {
                    int j = Integer.parseInt(id);
                    periodStats.ensureAtLeastNJams(j);
                    processJamStats(periodStats.getJamStats(j), child);
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processJamStats(Stats.JamStats jamStats, Element element) {
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String id = child.getAttributeValue("Id");
                String name = child.getName();
                String value = editor.getText(child);

                if (name.equals("Team")) {
                    processTeamStats(jamStats.getTeamStats(id), child);
                } else if (null == value) {
                    continue;
                } else if (name.equals("JamClockElapsedEnd")) {
                    jamStats.setJamClockElapsedEnd(Long.parseLong(value));
                } else if (name.equals("PeriodClockElapsedStart")) {
                    jamStats.setPeriodClockElapsedStart(Long.parseLong(value));
                } else if (name.equals("PeriodClockElapsedEnd")) {
                    jamStats.setPeriodClockElapsedEnd(Long.parseLong(value));
                } else if (name.equals("PeriodClockWalltimeStart")) {
                    jamStats.setPeriodClockWalltimeStart(Long.parseLong(value));
                } else if (name.equals("PeriodClockWalltimeEnd")) {
                    jamStats.setPeriodClockWalltimeEnd(Long.parseLong(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processTeamStats(Stats.TeamStats teamStats, Element element) {
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String id = child.getAttributeValue("Id");
                String name = child.getName();
                String value = editor.getText(child);

                if (name.equals("Skater")) {
                    teamStats.addSkaterStats(id);
                    processSkaterStats(teamStats.getSkaterStats(id), child);
                } else if (null == value) {
                    continue;
                } else if (name.equals("JamScore")) {
                    teamStats.setJamScore(Integer.parseInt(value));
                } else if (name.equals("TotalScore")) {
                    teamStats.setTotalScore(Integer.parseInt(value));
                } else if (name.equals("LeadJammer")) {
                    teamStats.setLeadJammer(value);
                } else if (name.equals("StarPass")) {
                    teamStats.setStarPass(Boolean.parseBoolean(value));
                } else if (name.equals("Timeouts")) {
                    teamStats.setTimeouts(Integer.parseInt(value));
                } else if (name.equals("OfficialReviews")) {
                    teamStats.setOfficialReviews(Integer.parseInt(value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processSkaterStats(Stats.SkaterStats skaterStats, Element element) {
        Iterator<?> children = element.getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            try {
                String name = child.getName();
                String value = editor.getText(child);

                if (null == value) {
                    continue;
                } else if (name.equals("PenaltyBox")) {
                    skaterStats.setPenaltyBox(Boolean.parseBoolean(value));
                } else if (name.equals("Position")) {
                    skaterStats.setPosition(value);
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
