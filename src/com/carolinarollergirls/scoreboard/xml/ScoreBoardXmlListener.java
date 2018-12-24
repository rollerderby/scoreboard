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

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.SettingsImpl.Setting;
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;

/**
 * Converts a ScoreBoardEvent into a representative XML Document or XML String.
 *
 * This class is not synchronized.	Each event method modifies the same document.
 */
public class ScoreBoardXmlListener implements ScoreBoardListener {
    public ScoreBoardXmlListener() { }
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
        Property prop = event.getProperty();
        String v = (event.getValue()==null?null:event.getValue().toString());
        if (prop == BatchEvent.START) {
            batchStart();
        } else if (prop == BatchEvent.END) {
            batchEnd();
        } else if (p.getProviderName().equals("Settings")) {
            Element e = editor.setElement(getScoreBoardElement(), "Settings");
            Setting s = (Setting)event.getValue();
            if (v == null) {
                editor.setRemovePI(editor.setElement(e, prop, s.getId()));
            } else {
                editor.setElement(e, prop, s.getId(), s.getValue());
            }
        } else if (p.getProviderName().equals("Rulesets")) {
            if (prop == Rulesets.Child.KNOWN_RULESETS && v == null) {
                Element e = editor.setElement(getScoreBoardElement(), "KnownRulesets");
                editor.setRemovePI(converter.toElement(e, (Rulesets.Ruleset)event.getPreviousValue()));
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
                Set<Rule> newKeys = r.getAll().keySet();
                for (Object o : (Set<?>)event.getPreviousValue()) {
                    Rule k = (Rule)o;
                    if (!newKeys.contains(k)) {
                        editor.setRemovePI(editor.setElement(re, "Rule", k.toString()));
                    }
                }
            }
        } else if (p.getProviderName().equals("ScoreBoard")) {
            if (prop == ScoreBoard.Child.CLOCK) {
        	if (v != null) {
        	    converter.toElement(getScoreBoardElement(), (Clock)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Clock)event.getPreviousValue()));
        	}
            } else if (prop == ScoreBoard.Child.TEAM) {
        	if (v != null) {
        	    converter.toElement(getScoreBoardElement(), (Team)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Team)event.getPreviousValue()));
        	}
            } else {
                editor.setElement(getScoreBoardElement(), prop, null, v);
            }
        } else if (p.getProviderName().equals("Team")) {
            if (prop == Team.Child.ALTERNATE_NAME) {
        	if (v != null) {
        	    converter.toElement(getTeamElement((Team)p), (Team.AlternateName)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Team.AlternateName)event.getPreviousValue()));
        	}
            } else if (prop == Team.Child.COLOR) {
        	if (v != null) {
        	    converter.toElement(getTeamElement((Team)p), (Team.Color)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Team.Color)event.getPreviousValue()));
        	}
            } else if (prop == Team.Child.SKATER) {
        	if (v != null) {
        	    converter.toElement(getTeamElement((Team)p), (Skater)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Skater)event.getPreviousValue()));
        	}
            } else {
                editor.setElement(getTeamElement((Team)p), prop, null, v);
            }
        } else if (p.getProviderName().equals("Position")) {
            Element e = getPositionElement((Position)p);
            if (prop == Position.Value.SKATER) {
                Skater s = (Skater)event.getValue();
                editor.setElement(e, "Id", null, (s==null?"":s.getId()));
                editor.setElement(e, "Name", null, (s==null?"":s.getName()));
                editor.setElement(e, "Number", null, (s==null?"":s.getNumber()));
                editor.setElement(e, "PenaltyBox", null, String.valueOf(s==null?false:s.isPenaltyBox()));
                editor.setElement(e, "Flags", null, (s==null?"":s.getFlags()));
            } else if (prop == Position.Value.PENALTY_BOX) {
                editor.setElement(e, "PenaltyBox", null, String.valueOf(event.getValue()));
            }
        } else if (p.getProviderName().equals("AlternateName")) {
            editor.setElement(getAlternateNameElement((Team.AlternateName)p), prop, null, v);
        } else if (p.getProviderName().equals("Color")) {
            editor.setElement(getColorElement((Team.Color)p), prop, null, v);
        } else if (p.getProviderName().equals("Skater")) {
            if ((prop == Skater.Child.PENALTY || prop == Skater.Value.PENALTY_FOEXP)
        	    && v != null) {
                // Replace whole skater.
                converter.toElement(getTeamElement(((Skater)p).getTeam()), (Skater)p);
            } else if (prop == Skater.Child.PENALTY) {
                Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
                if (prev != null) {
                    editor.setRemovePI(editor.addElement(getSkaterElement((Skater)p), prop, prev.getId()));
                }
            } else if (prop.equals(Skater.Value.PENALTY_FOEXP)) {
                Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
                if (prev != null) {
                    editor.setRemovePI(editor.addElement(getSkaterElement((Skater)p), prop, prev.getId()));
                }
            } else {
                editor.setElement(getSkaterElement((Skater)p), prop, null, v);
            }
        } else if (p.getProviderName().equals("Media")) {
            Media.MediaFile mf = (Media.MediaFile)(event.getPreviousValue());
            Element e = getMediaFileElement(mf);
            if (prop == Media.Child.FILE && v == null) {
                editor.setRemovePI(converter.toElement(e, mf));
            }
        } else if (p.getProviderName().equals("MediaFile")) {
            Media.MediaFile mf = (Media.MediaFile)(event.getValue());
            Element e = getMediaFileElement(mf);
            if (prop.equals(Media.Child.FILE)) {
                converter.toElement(e, mf);
            }
        } else if (p.getProviderName().equals("Stats")) {
            Element e = getStatsElement();
            if (prop == Stats.Child.PERIOD && v == null) {
                editor.setRemovePI(converter.toElement(e, (Stats.PeriodStats)(event.getPreviousValue())));
            } else if (prop == Stats.Child.PERIOD) {
                getPeriodStatsElement((Stats.PeriodStats)(event.getValue()));
            }
        } else if (p.getProviderName().equals("PeriodStats")) {
            Element e = getPeriodStatsElement((Stats.PeriodStats)p);
            if (prop == Stats.PeriodStats.Child.JAM && v == null) {
                editor.setRemovePI(converter.toElement(e, (Stats.JamStats)(event.getPreviousValue())));
            } else if (prop == Stats.PeriodStats.Child.JAM) {
                getJamStatsElement((Stats.JamStats)(event.getValue()));
            }
        } else if (p.getProviderName().equals("JamStats")) {
            Element e = getJamStatsElement((Stats.JamStats)p);
            if (prop == Stats.JamStats.Value.STATS) {
                Stats.JamStats js = (Stats.JamStats)event.getValue();
                editor.setElement(e, "JamClockElapsedEnd", null, String.valueOf(js.getJamClockElapsedEnd()));
                editor.setElement(e, "PeriodClockElapsedStart", null, String.valueOf(js.getPeriodClockElapsedStart()));
                editor.setElement(e, "PeriodClockElapsedEnd", null, String.valueOf(js.getPeriodClockElapsedEnd()));
                editor.setElement(e, "PeriodClockWalltimeStart", null, String.valueOf(js.getPeriodClockWalltimeStart()));
                editor.setElement(e, "PeriodClockWalltimeEnd", null, String.valueOf(js.getPeriodClockWalltimeEnd()));
            }
        } else if (p.getProviderName().equals("TeamStats")) {
            Element e = getTeamStatsElement((Stats.TeamStats)p);
            if (prop == Stats.TeamStats.Value.STATS) {
                Stats.TeamStats ts = (Stats.TeamStats)event.getValue();
                editor.setElement(e, "JamScore", null, String.valueOf(ts.getJamScore()));
                editor.setElement(e, "TotalScore", null, String.valueOf(ts.getTotalScore()));
                editor.setElement(e, "LeadJammer", null, ts.getLeadJammer());
                editor.setElement(e, "StarPass", null, String.valueOf(ts.getStarPass()));
                editor.setElement(e, "Timeouts", null, String.valueOf(ts.getTimeouts()));
                editor.setElement(e, "OfficialReviews", null, String.valueOf(ts.getOfficialReviews()));
            } else if (prop == Stats.TeamStats.Child.SKATER && v == null) {
                Stats.SkaterStats ss = (Stats.SkaterStats)(event.getPreviousValue());
                editor.setRemovePI(converter.toElement(e, ss));
            }
        } else if (p.getProviderName().equals("SkaterStats")) {
            Element e = getSkaterStatsElement((Stats.SkaterStats)p);
            if (prop == Stats.SkaterStats.Value.STATS) {
                Stats.SkaterStats ss = (Stats.SkaterStats)event.getValue();
                editor.setElement(e, "Position", null, ss.getPosition());
                editor.setElement(e, "PenaltyBox", null, String.valueOf(ss.getPenaltyBox()));
            }
        } else if (p.getProviderName().equals("Clock")) {
            Element e = editor.setElement(getClockElement((Clock)p), prop, null, v);
            if (prop == Clock.Value.TIME) {
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

    protected Element getMediaFileElement(Media.MediaFile mf) {
        return editor.getElement(
                   editor.getElement(
                       editor.getElement(
                           editor.getElement(
                               getScoreBoardElement(), "Media"),
                           mf.getFormat()),
                       mf.getType()),
                   "File", mf.getId());
    }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

    protected Document document = editor.createDocument("ScoreBoard");
    protected boolean empty = true;
}
