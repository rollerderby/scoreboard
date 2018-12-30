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
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Stats.JamStats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl.TimeoutOwners;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class ScoreBoardXmlConverter {
    /*****************************/
    /* ScoreBoard to XML methods */

    public String toString(ScoreBoard scoreBoard) {
        return rawXmlOutputter.outputString(toDocument(scoreBoard));
    }

    public Document toDocument(ScoreBoard scoreBoard) {
        Element sb = new Element(scoreBoard.getProviderName());
        Document d = new Document(new Element("document").addContent(sb));

        for (ScoreBoard.Command c : ScoreBoard.Command.values()) {
            editor.setElement(sb, c, null, "");
        }
        
        for (ScoreBoard.Value v : ScoreBoard.Value.values()) {
            editor.setElement(sb, v, null, String.valueOf(scoreBoard.get(v)));
        }

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
        Element e = editor.setElement(p, s.getProviderName());
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
        editor.setElement(e, Rulesets.Value.ID, null, rs.getId());
        editor.setElement(e, Rulesets.Value.NAME, null, rs.getName());

        e = editor.setElement(p, Rulesets.Child.KNOWN_RULESETS);
        for (Rulesets.Ruleset r :  rs.getRulesets().values()) {
            toElement(e, r);
        }
        return e;
    }

    public Element toElement(Element p, Rulesets.Ruleset r) {
        Element e = editor.setElement(p, r.getProviderName(), r.getProviderId());
        for (Rule k : r.getAll().keySet()) {
            editor.setElement(e, Rulesets.Value.RULE, k.toString(), r.get(k));
        }
        editor.setElement(e, Rulesets.Ruleset.Value.NAME, null, r.getName());
        editor.setElement(e, Rulesets.Ruleset.Value.PARENT_ID, null, r.getParentRulesetId());
        return e;
    }

    public Element toElement(Element sb, Media m) {
        Element e = editor.setElement(sb, m.getProviderName());
        for (String format : m.getFormats()) {
            Element f = editor.setElement(e, format);
            for (String type : m.getTypes(format)) {
                Element t = editor.setElement(f, type);
                for (Media.MediaFile mf: m.getMediaFiles(format, type).values()) {
                    toElement(t, mf);
                }
            }
        }
        return e;
    }

    public Element toElement(Element p, Media.MediaFile mf) {
        Element e = editor.setElement(p, mf.getProviderName(), mf.getProviderId());
        for(Media.MediaFile.Value v : Media.MediaFile.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(mf.get(v)));
        }
        return e;
    }

    public Element toElement(Element sb, Clock c) {
        Element e = editor.setElement(sb, c.getProviderName(), c.getProviderId());

        for (Clock.Command co : Clock.Command.values()) {
            editor.setElement(e, co, null, "");
        }

        for(Clock.Value v : Clock.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(c.get(v)));
        }
        return e;
    }

    public Element toElement(Element sb, Team t) {
        Element e = editor.setElement(sb, t.getProviderName(), t.getProviderId());

        for (Team.Command c : Team.Command.values()) {
            editor.setElement(e, c, null, "");
        }

        for(Team.Value v : Team.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(t.get(v)));
        }

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
        Element e = editor.setElement(team, n.getProviderName(), n.getId());

        for(Team.AlternateName.Value v : Team.AlternateName.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(n.get(v)));
        }

        return e;
    }

    public Element toElement(Element team, Team.Color c) {
        Element e = editor.setElement(team, c.getProviderName(), c.getId());

        for(Team.Color.Value v : Team.Color.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(c.get(v)));
        }

        return e;
    }

    public Element toElement(Element team, Position p) {
        Element e = editor.setElement(team, p.getProviderName(), p.getId());

        for (Position.Command c : Position.Command.values()) {
            editor.setElement(e, c, null, "");
        }
        
        for(Position.Value v : Position.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(p.get(v)));
        }

        return e;
    }

    public Element toElement(Element t, Skater s) {
        Element e = editor.setElement(t, s.getProviderName(), s.getId());

        for(Skater.Value v : Skater.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(s.get(v)));
        }

        for (Skater.Penalty p: s.getPenalties()) {
            toElement(e, p);
        }

        if (s.getFOEXPPenalty() != null) {
            toElement(e, s.getFOEXPPenalty());
        }

        return e;
    }

    public Element toElement(Element s, Skater.Penalty p) {
        Element e = editor.setElement(s, p.getProviderName(), p.getId());
        for(Skater.Penalty.Value v : Skater.Penalty.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(p.get(v)));
        }
        return e;
    }

    public Element toElement(Element sb, Stats s) {
        Element e = editor.setElement(sb, s.getProviderName());
        for (Stats.PeriodStats p: s.getPeriodStats()) {
            toElement(e, p);
        }
        return e;
    }

    public Element toElement(Element s, Stats.PeriodStats p) {
        Element e = editor.setElement(s, p.getProviderName(), String.valueOf(p.getPeriodNumber()));
        for (Stats.JamStats j: p.getJamStats()) {
            toElement(e, j);
        }
        return e;
    }

    public Element toElement(Element p, Stats.JamStats j) {
        Element e = editor.setElement(p, j.getProviderName(), String.valueOf(j.getJamNumber()));
        for(Stats.JamStats.Value v : Stats.JamStats.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(j.get(v)));
        }
        for (Stats.TeamStats t: j.getTeamStats()) {
            toElement(e, t);
        }
        return e;
    }

    public Element toElement(Element j, Stats.TeamStats t) {
        Element e = editor.setElement(j, t.getProviderName(), t.getTeamId());
        for(Stats.TeamStats.Value v : Stats.TeamStats.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(t.get(v)));
        }
        for (Stats.SkaterStats s: t.getSkaterStats()) {
            toElement(e, s);
        }
        return e;
    }

    public Element toElement(Element t, Stats.SkaterStats s) {
        Element e = editor.setElement(t, s.getProviderName(), s.getSkaterId());
        for(Stats.SkaterStats.Value v : Stats.SkaterStats.Value.values()) {
            editor.setElement(e, v, null, String.valueOf(s.get(v)));
        }
        return e;
    }

    /*****************************/
    /* XML to ScoreBoard methods */

    public void processDocument(ScoreBoard scoreBoard, Document document) {
        Iterator<?> children = document.getRootElement().getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            if (child.getName().equals(scoreBoard.getProviderName())) {
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

                if (name.equals("Settings")) {
                    processSettings(scoreBoard, child);
                } else if (name.equals("Rules")) {
                    processRules(scoreBoard, child);
                } else if (name.equals("KnownRulesets")) {
                    processRulesets(scoreBoard, child);
                } else if (name.equals("Media")) {
                    processMedia(scoreBoard.getMedia(), child);
                } else if (name.equals("Stats")) {
                    processStats(scoreBoard.getStats(), child);
                } else if (prop instanceof ScoreBoard.Child) {
                    switch ((ScoreBoard.Child)prop) {
		    case CLOCK:
			processClock(scoreBoard, child);
			break;
		    case TEAM:
			processTeam(scoreBoard, child);
			break;
                    }
                } else if (null == value) {
                    continue;
                } else if (prop instanceof ScoreBoard.Value) {
                    scoreBoard.set((ScoreBoard.Value)prop, scoreBoard.valueFromString((PermanentProperty)prop, value));
                } else if (bVal && prop instanceof ScoreBoard.Command) {
                    switch((ScoreBoard.Command)prop) {
                    case RESET:
                        scoreBoard.reset();
                	break;
		    case START_JAM:
                        scoreBoard.startJam();
			break;
		    case STOP_JAM:
                        scoreBoard.stopJamTO();
			break;
		    case TIMEOUT:
                        scoreBoard.timeout();
			break;
		    case CLOCK_UNDO:
                        scoreBoard.clockUndo(false);
			break;
		    case CLOCK_REPLACE:
                        scoreBoard.clockUndo(true);
			break;
		    case START_OVERTIME:
                        scoreBoard.startOvertime();
			break;
		    case OFFICIAL_TIMEOUT:
                        scoreBoard.setTimeoutType(TimeoutOwners.OTO, false);
			break;
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
                Property prop = PropertyConversion.fromFrontend(name, rs.getProperties());
                if (prop == Rulesets.Value.RULE) {
                    rs.set(k, v);
                } else if (prop == Rulesets.Value.ID) {
                    rs.setId(v);
                } else if (prop == Rulesets.Value.NAME) {
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
                if (n.equals(PropertyConversion.toFrontend(Rulesets.Ruleset.Child.RULE))) {
                    rules.put(k, v);
                } else if (n.equals(PropertyConversion.toFrontend(Rulesets.Ruleset.Value.PARENT_ID))) {
                    parentId = v;
                } else if (n.equals(PropertyConversion.toFrontend(Rulesets.Ruleset.Value.NAME))) {
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
                        if (prop.getName() == PropertyConversion.toFrontend(Media.MediaFile.Value.NAME)) {
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
                Flag flag = null;
                if (Boolean.parseBoolean(child.getAttributeValue("change"))) { flag = Flag.CHANGE; }
                if (Boolean.parseBoolean(child.getAttributeValue("reset"))) { flag = Flag.RESET; }

//FIXME - might be better way to handle changes/resets than an attribute...
                if ((null == value) && flag != Flag.RESET) {
                    continue;
                } else if (prop instanceof Clock.Command) {
                    switch ((Clock.Command)prop) {
		    case START:
			requestStart = true;
			break;
		    case STOP:
			requestStop = true;
			break;
		    case RESET_TIME:
			clock.resetTime();
			break;
                    }
                } else if (prop instanceof Clock.Value) {
                    if (prop == Clock.Value.RUNNING) {
			if (Boolean.parseBoolean(value)) {
			    requestStart = true;
			} else {
			    requestStop = true;
			}
                    } else {
                	clock.set((PermanentProperty)prop, clock.valueFromString((PermanentProperty)prop, value), flag);
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
                Flag flag = null;
                if (Boolean.parseBoolean(child.getAttributeValue("change"))) { flag = Flag.CHANGE; }


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
                } else if (prop instanceof Team.Command) {
                    switch ((Team.Command)prop) {
                    case TIMEOUT:
                	team.timeout();
                	break;
                    case OFFICIAL_REVIEW:
                	team.officialReview();
                	break;
                    }
                } else if (prop instanceof Team.Value) {
                    team.set((PermanentProperty)prop, team.valueFromString((PermanentProperty)prop, value), flag);
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
                } else if (prop == Position.Command.CLEAR && Boolean.parseBoolean(value)) {
                    team.field(null, position);
                } else if (prop == Position.Value.ID) {
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
                    processPenalty(skater, child);
                } else if (null == value) {
                    continue;
                } else if (prop instanceof Skater.Value) {
                    skater.set((PermanentProperty)prop, skater.valueFromString((PermanentProperty)prop, value));
                }
            } catch ( Exception e ) {
            }
        }
    }

    public void processPenalty(Skater skater, Element element) {
        String num = element.getAttributeValue("Id");
        String id = null;
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
                } else if (name.equals(PropertyConversion.toFrontend(Skater.Penalty.Value.ID))) {
                    id = value;
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
        skater.penalty(id, num, period, jam, code);
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
                Property prop = PropertyConversion.fromFrontend(name, jamStats.getProperties());

                if (prop == JamStats.Child.TEAM) {
                    processTeamStats(jamStats.getTeamStats(id), child);
                } else if (null == value) {
                    continue;
                } else if (prop instanceof JamStats.Value) {
                    jamStats.set((PermanentProperty)prop, jamStats.valueFromString((PermanentProperty)prop, value));
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
                Property prop = PropertyConversion.fromFrontend(name, teamStats.getProperties());

                if (name.equals("Skater")) {
                    teamStats.addSkaterStats(id);
                    processSkaterStats(teamStats.getSkaterStats(id), child);
                } else if (null == value) {
                    continue;
                } else if (prop instanceof Stats.TeamStats.Value) {
                    teamStats.set((PermanentProperty)prop, teamStats.valueFromString((PermanentProperty)prop, value));
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
                Property prop = PropertyConversion.fromFrontend(name, skaterStats.getProperties());

                if (null == value) {
                    continue;
                } else if (prop instanceof Stats.SkaterStats.Value) {
                    skaterStats.set((PermanentProperty)prop, skaterStats.valueFromString((PermanentProperty)prop, value));
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
