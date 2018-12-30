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
        Boolean rem = event.isRemove();
        if (prop == BatchEvent.START) {
            batchStart();
        } else if (prop == BatchEvent.END) {
            batchEnd();
        } else if (p instanceof Settings) {
            ValueWithId s = (ValueWithId)event.getValue();
            Element ne = editor.setElement(e, prop, s.getId(), s.getValue());
            if (rem) {
                editor.setRemovePI(ne);
            }
        } else if (p instanceof Rulesets) {
            if (prop == Rulesets.Child.KNOWN_RULESETS && rem) {
                editor.setRemovePI(converter.toElement(e, (Rulesets.Ruleset)event.getValue()));
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
        	Element ne = converter.toElement(e, (Clock)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            } else if (prop == ScoreBoard.Child.TEAM) {
        	Element ne = converter.toElement(e, (Team)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            } else {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Team) {
            if (prop == Team.Child.ALTERNATE_NAME) {
        	Element ne = converter.toElement(e, (Team.AlternateName)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            } else if (prop == Team.Child.COLOR) {
        	Element ne = converter.toElement(e, (Team.Color)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            } else if (prop == Team.Child.SKATER) {
        	Element ne = converter.toElement(e, (Skater)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            } else {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Position) {
            if (prop instanceof Position.Value) {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof AlternateName) {
            editor.setElement(e, prop, null, v);
        } else if (p instanceof Color) {
            editor.setElement(e, prop, null, v);
        } else if (p instanceof Skater) {
            if (prop == Skater.Child.PENALTY) {
        	Element ne = converter.toElement(e, (Skater.Penalty)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            } else {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Skater.Penalty) {
            editor.setElement(e, prop, null, v);
        } else if (p instanceof MediaType) {
            if (prop == MediaType.Child.FILE) {
        	Element ne = converter.toElement(e, (Media.MediaFile)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            }
        } else if (p instanceof Media.MediaFile) {
            if (prop instanceof Media.MediaFile.Value) {
        	editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Stats) {
            if (prop == Stats.Child.PERIOD) {
        	Element ne = converter.toElement(e, (Stats.PeriodStats)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            }
        } else if (p instanceof Stats.PeriodStats) {
            if (prop == Stats.PeriodStats.Child.JAM) {
        	Element ne = converter.toElement(e, (Stats.JamStats)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            }
        } else if (p instanceof Stats.JamStats) {
            if (prop instanceof Stats.JamStats.Value) {
        	editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Stats.TeamStats) {
            if (prop instanceof Stats.TeamStats.Value) {
                editor.setElement(e, prop, null, v);
            } else if (prop == Stats.TeamStats.Child.SKATER) {
        	Element ne = converter.toElement(e, (Stats.SkaterStats)event.getValue());
        	if (rem) {
        	    editor.setRemovePI(ne);
        	}
            }
        } else if (p instanceof Stats.SkaterStats) {
            if (prop instanceof Stats.SkaterStats.Value) {
                editor.setElement(e, prop, null, v);
            }
        } else if (p instanceof Clock) {
            e = editor.setElement(e, prop, null, v);
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
