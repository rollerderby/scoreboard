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
import com.carolinarollergirls.scoreboard.core.Media.MediaType;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Team.AlternateName;
import com.carolinarollergirls.scoreboard.core.Team.Color;
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
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
        Element e = getElement(p);
        Property prop = event.getProperty();
        String v = (event.getValue()==null?null:event.getValue().toString());
        if (prop == BatchEvent.START) {
            batchStart();
        } else if (prop == BatchEvent.END) {
            batchEnd();
        } else if (p instanceof Settings) {
            ValueWithId s = (ValueWithId)event.getValue();
            if (s.getValue() == null) {
                editor.setRemovePI(editor.setElement(e, prop, s.getId()));
            } else {
                editor.setElement(e, prop, s.getId(), s.getValue());
            }
        } else if (p instanceof Rulesets) {
            if (prop == Rulesets.Child.KNOWN_RULESETS && v == null) {
                editor.setRemovePI(converter.toElement(e, (Rulesets.Ruleset)event.getPreviousValue()));
            } else {
                converter.toElement(getElement(p.getParent()), (Rulesets)p);
            }
        } else if (p instanceof Rulesets.Ruleset) {
            e = getElement(p.getParent());
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
        } else if (p instanceof ScoreBoard) {
            if (prop == ScoreBoard.Child.CLOCK) {
        	if (v != null) {
        	    converter.toElement(e, (Clock)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(e, (Clock)event.getPreviousValue()));
        	}
            } else if (prop == ScoreBoard.Child.TEAM) {
        	if (v != null) {
        	    converter.toElement(e, (Team)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(e, (Team)event.getPreviousValue()));
        	}
            } else {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Team) {
            if (prop == Team.Child.ALTERNATE_NAME) {
        	if (v != null) {
        	    converter.toElement(e, (Team.AlternateName)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(e, (Team.AlternateName)event.getPreviousValue()));
        	}
            } else if (prop == Team.Child.COLOR) {
        	if (v != null) {
        	    converter.toElement(e, (Team.Color)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(e, (Team.Color)event.getPreviousValue()));
        	}
            } else if (prop == Team.Child.SKATER) {
        	if (v != null) {
        	    converter.toElement(e, (Skater)event.getValue());
        	} else {
        	    editor.setRemovePI(converter.toElement(e, (Skater)event.getPreviousValue()));
        	}
            } else {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Position) {
            if (prop == Position.Value.SKATER) {
                Skater s = (Skater)event.getValue();
                editor.setElement(e, Position.Value.ID, null, (s==null?"":s.getId()));
                editor.setElement(e, Position.Value.NAME, null, (s==null?"":s.getName()));
                editor.setElement(e, Position.Value.NUMBER, null, (s==null?"":s.getNumber()));
                editor.setElement(e, Position.Value.PENALTY_BOX, null, String.valueOf(s==null?false:s.isPenaltyBox()));
                editor.setElement(e, Position.Value.FLAGS, null, (s==null?"":s.getFlags()));
            } else if (prop == Position.Value.PENALTY_BOX) {
                editor.setElement(e, prop, null, String.valueOf(event.getValue()));
            }
        } else if (p instanceof AlternateName) {
            editor.setElement(e, prop, null, v);
        } else if (p instanceof Color) {
            editor.setElement(e, prop, null, v);
        } else if (p instanceof Skater) {
            if ((prop == Skater.Child.PENALTY || prop == Skater.Value.PENALTY_FOEXP)
        	    && v != null) {
                // Replace whole skater.
                converter.toElement(getElement(p.getParent()), (Skater)p);
            } else if (prop == Skater.Child.PENALTY) {
                Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
                if (prev != null) {
                    editor.setRemovePI(editor.addElement(e, prop, prev.getId()));
                }
            } else if (prop.equals(Skater.Value.PENALTY_FOEXP)) {
                Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
                if (prev != null) {
                    editor.setRemovePI(editor.addElement(e, prop, prev.getId()));
                }
            } else {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof MediaType) {
            if (prop == MediaType.Child.FILE) {
        	if (v == null) {
                    Media.MediaFile mf = (Media.MediaFile)(event.getPreviousValue());
                    editor.setRemovePI(converter.toElement(e, mf));
        	} else {
                    Media.MediaFile mf = (Media.MediaFile)(event.getValue());
                    converter.toElement(e, mf);
        	}
            }
        } else if (p instanceof Stats) {
            if (prop == Stats.Child.PERIOD && v == null) {
                editor.setRemovePI(converter.toElement(e, (Stats.PeriodStats)(event.getPreviousValue())));
            } else if (prop == Stats.Child.PERIOD) {
                getElement((Stats.PeriodStats)(event.getValue()));
            }
        } else if (p instanceof Stats.PeriodStats) {
            if (prop == Stats.PeriodStats.Child.JAM && v == null) {
                editor.setRemovePI(converter.toElement(e, (Stats.JamStats)(event.getPreviousValue())));
            } else if (prop == Stats.PeriodStats.Child.JAM) {
                getElement((Stats.JamStats)(event.getValue()));
            }
        } else if (p instanceof Stats.JamStats) {
            if (prop == Stats.JamStats.Value.STATS) {
                Stats.JamStats js = (Stats.JamStats)event.getValue();
                editor.setElement(e, Stats.JamStats.Value.JAM_CLOCK_ELAPSED_END, null, String.valueOf(js.getJamClockElapsedEnd()));
                editor.setElement(e, Stats.JamStats.Value.PERIOD_CLOCK_ELAPSED_START, null, String.valueOf(js.getPeriodClockElapsedStart()));
                editor.setElement(e, Stats.JamStats.Value.PERIOD_CLOCK_ELAPSED_END, null, String.valueOf(js.getPeriodClockElapsedEnd()));
                editor.setElement(e, Stats.JamStats.Value.PERIOD_CLOCK_WALLTIME_START, null, String.valueOf(js.getPeriodClockWalltimeStart()));
                editor.setElement(e, Stats.JamStats.Value.PERIOD_CLOCK_WALLTIME_END, null, String.valueOf(js.getPeriodClockWalltimeEnd()));
            }
        } else if (p instanceof Stats.TeamStats) {
            if (prop == Stats.TeamStats.Value.STATS) {
                Stats.TeamStats ts = (Stats.TeamStats)event.getValue();
                editor.setElement(e, Stats.TeamStats.Value.JAM_SCORE, null, String.valueOf(ts.getJamScore()));
                editor.setElement(e, Stats.TeamStats.Value.TOTAL_SCORE, null, String.valueOf(ts.getTotalScore()));
                editor.setElement(e, Stats.TeamStats.Value.LEAD_JAMMER, null, ts.getLeadJammer());
                editor.setElement(e, Stats.TeamStats.Value.STAR_PASS, null, String.valueOf(ts.getStarPass()));
                editor.setElement(e, Stats.TeamStats.Value.TIMEOUTS, null, String.valueOf(ts.getTimeouts()));
                editor.setElement(e, Stats.TeamStats.Value.OFFICIAL_REVIEWS, null, String.valueOf(ts.getOfficialReviews()));
            } else if (prop == Stats.TeamStats.Child.SKATER && v == null) {
                Stats.SkaterStats ss = (Stats.SkaterStats)(event.getPreviousValue());
                editor.setRemovePI(converter.toElement(e, ss));
            }
        } else if (p instanceof Stats.SkaterStats) {
            if (prop == Stats.SkaterStats.Value.STATS) {
                Stats.SkaterStats ss = (Stats.SkaterStats)event.getValue();
                editor.setElement(e, Stats.SkaterStats.Value.POSITION, null, ss.getPosition());
                editor.setElement(e, Stats.SkaterStats.Value.PENALTY_BOX, null, String.valueOf(ss.getPenaltyBox()));
            }
        } else if (p instanceof Clock) {
            e = editor.setElement(e, prop, null, v);
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
    
    protected Element getElement(ScoreBoardEventProvider p) {
	if (p.getParent() == null) {
	    return editor.getElement(document.getRootElement(), p.getProviderName());
	} else {
	    return editor.getElement(getElement(p.getParent()), p.getProviderName(), p.getProviderId());
	}
    }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

    protected Document document = editor.createDocument("ScoreBoard");
    protected boolean empty = true;
}
