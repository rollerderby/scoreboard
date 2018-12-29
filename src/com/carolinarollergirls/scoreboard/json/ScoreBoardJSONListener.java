package com.carolinarollergirls.scoreboard.json;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
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
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCode;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesDefinition;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.rules.AbstractRule;
import com.carolinarollergirls.scoreboard.rules.AbstractRule.Type;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.rules.BooleanRule;

/**
 * Converts a ScoreBoardEvent into a representative JSON Update
 */
public class ScoreBoardJSONListener implements ScoreBoardListener {
    public ScoreBoardJSONListener(ScoreBoard sb, JSONStateManager jsm) {
        this.jsm = jsm;
        initialize(sb);
        sb.addScoreBoardListener(this);
    }

    public void scoreBoardChange(ScoreBoardEvent event) {
        synchronized (this) {
            try {
                ScoreBoardEventProvider p = event.getProvider();
                String provider = p.getProviderName();
                Property prop = event.getProperty();
                boolean rem = event.isRemove();
                if (prop == BatchEvent.START) {
                    batch++;
                    return;
                }
                if (prop == BatchEvent.END) {
                    if (batch == 0) {
                        return;
                    }
                    if (--batch == 0) {
                        updateState();
                    }
                    return;
                }

                Object v = event.getValue();
                if (p instanceof ScoreBoard) {
                    // these properties are required for the XML listener, but are not used in the WS listener
                    // so they are ignored here
                    if(prop != ScoreBoard.Child.CLOCK && prop != ScoreBoard.Child.TEAM) {
                        update(getPath(p), prop, v);
                    }
                } else if (p instanceof Team) {
                    Team t = (Team)p;
                    if (prop == Team.Child.SKATER) {
                	processSkater((Skater)v, rem);
                    } else if (prop == Team.Child.ALTERNATE_NAME) {
                	processAlternateName((Team.AlternateName)v, rem);
                    } else if (prop == Team.Child.COLOR) {
                	processColor((Team.Color)v, rem);
                    }
                    // Fast path for jam start/end to avoid sending the entire team.
                    else if (prop instanceof Team.Value) {
                        update(getPath(t), prop, t.get((PermanentProperty)prop));
                    } else {
                        processTeam(t, false);
                    }
                } else if (p instanceof Skater) {
                    Skater s = (Skater)p;
                    processSkater(s, false);
                } else if (p instanceof Position) {
                    Position pos = (Position)p;
                    processPosition(pos, false);
                } else if (p instanceof Clock) {
                    processClock((Clock)p, false);
                } else if (p instanceof Rulesets) {
                    if (prop == Rulesets.Value.RULESET) {
                        processCurrentRuleset("ScoreBoard", (Rulesets)p);
                    } else {
                        processRuleset("ScoreBoard", (Rulesets.Ruleset)v, true);
                    }
                } else if (p instanceof Rulesets.Ruleset) {
                    processRuleset("ScoreBoard", (Rulesets.Ruleset)p, false);
                } else if (p instanceof Team.AlternateName) {
                    Team.AlternateName an = (Team.AlternateName)p;
                    update(getPath(an), prop, v);
                } else if (p instanceof Team.Color) {
                    Team.Color c = (Team.Color)p;
                    update(getPath(c), prop, v);
                } else if (p instanceof Stats) {
                    if (prop == Stats.Child.PERIOD && rem) {
                        Stats.PeriodStats ps = (Stats.PeriodStats)v;
                        updates.add(new WSUpdate(getPath(ps), null));
                    }
                } else if (p instanceof Stats.PeriodStats) {
                    if (prop == Stats.PeriodStats.Child.JAM && rem) {
                        Stats.JamStats js = (Stats.JamStats)v;
                        updates.add(new WSUpdate(getPath(js), null));
                    }
                } else if (p instanceof Stats.TeamStats && prop == Stats.TeamStats.Child.SKATER && rem) {
                    Stats.SkaterStats ss = (Stats.SkaterStats)v;
                    updates.add(new WSUpdate(getPath(ss), null));
                } else if (p instanceof Stats.JamStats) {
                    Stats.JamStats js = (Stats.JamStats)p;
                    processJamStats(js);
                } else if (p instanceof Stats.TeamStats) {
                    Stats.TeamStats ts = (Stats.TeamStats)p;
                    processTeamStats(ts);
                } else if (p instanceof Stats.SkaterStats) {
                    Stats.SkaterStats ss = (Stats.SkaterStats)p;
                    processSkaterStats(ss);
                } else if (p instanceof Settings) {
                    updates.add(new WSUpdate("ScoreBoard.Settings." + ((ValueWithId)v).getId(), ((ValueWithId)v).getValue()));
                } else if (p instanceof MediaType && prop == MediaType.Child.FILE && rem) {
                    Media.MediaFile mf = (Media.MediaFile)v;
                    updates.add(new WSUpdate(getPath(mf), null));
                } else if (p instanceof MediaType) {
                    Media.MediaFile mf = (Media.MediaFile)v;
                    processMediaFile(mf);
                } else {
                    ScoreBoardManager.printMessage(provider + " update of unknown kind.	prop: " + PropertyConversion.toFrontend(prop) + ", v: " + v);
                }

            } catch (Exception e) {
                ScoreBoardManager.printMessage("Error!  " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (batch == 0) {
                    updateState();
                }
            }
        }
    }

    private void updateState() {
        synchronized (this) {
            if (updates.isEmpty()) {
                return;
            }
            jsm.updateState(updates);
            updates.clear();
        }
    }

    private void update(String prefix, Property prop, Object v) {
	String path = prefix + "." + PropertyConversion.toFrontend(prop);
	if (prop == Settings.Child.SETTING) {
	    updates.add(new WSUpdate(prefix + "." + ((ValueWithId)prop).getId(), v));
    	} else if (v instanceof ScoreBoardEventProvider) {
            updates.add(new WSUpdate(path, ((ScoreBoardEventProvider) v).getProviderId()));
	} else if (v != null){
            updates.add(new WSUpdate(path, v.toString()));
        } else {
            updates.add(new WSUpdate(path, v));
        }
    }

    private void processSkater(Skater s, boolean remove) {
	String path = getPath(s);
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        for (PermanentProperty prop : Skater.Value.values()) {
            Object v = s.get(prop);
            if (v == null) { v = ""; }
            update(path, prop, v);
        }

        List<Skater.Penalty> penalties = s.getPenalties();
        for (int i = 0; i < 9; i++) {
            String base = path + ".Penalty(" + (i + 1) + ")";
            if (i < penalties.size()) {
                processPenalty(base, penalties.get(i), false);
            } else {
                processPenalty(base, null, true);
            }
        }

        processPenalty(path + ".Penalty(FO_EXP)", s.getFOEXPPenalty(), s.getFOEXPPenalty() == null);
    }

    private void processPenalty(String path, Skater.Penalty p, boolean remove) {
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }
        for (PermanentProperty prop : Skater.Penalty.Value.values()) {
            update(path, prop, p.get(prop));
        }
    }

    private void processTeam(Team t, boolean remove) {
	String path = getPath(t);
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        for (PermanentProperty prop : Team.Value.values()) {
            update(path, prop, t.get(prop));
        }

        // Skaters
        for (Skater s : t.getSkaters()) {
            processSkater(s, false);
        }

        // Positions
        for (Position p : t.getPositions()) {
            processPosition(p, false);
        }

        // Alternate Names
        for (Team.AlternateName an : t.getAlternateNames()) {
            processAlternateName(an, false);
        }

        // Colors
        for (Team.Color c : t.getColors()) {
            processColor(c, false);
        }
    }

    private void processClock(Clock c, boolean remove) {
	String path = getPath(c);
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        for (PermanentProperty prop : Clock.Value.values()) {
            update(path, prop, c.get(prop));
        }
    }

    private void processCurrentRuleset(String path, Rulesets r) {
        for (Map.Entry<Rule,String> e : r.getAll().entrySet()) {
            updates.add(new WSUpdate(path + "."
        	    + PropertyConversion.toFrontend(Rulesets.Value.RULE)
        	    + "(" + e.getKey().toString() + ")", e.getValue()));
        }
        path = path + "." + PropertyConversion.toFrontend(Rulesets.Value.RULESET);
        update(path, Rulesets.Value.ID, r.getId());
        update(path, Rulesets.Value.NAME, r.getName());
        processPenaltyCodes(r);
    }

    private void processRuleset(String path, Rulesets.Ruleset r, boolean remove) {
        path = path + "." + PropertyConversion.toFrontend(Rulesets.Child.KNOWN_RULESETS) + "(" + r.getId() + ")";
        updates.add(new WSUpdate(path, null));
        if (remove) {
            return;
        }

        for (Map.Entry<Rule,String> e : r.getAll().entrySet()) {
            updates.add(new WSUpdate(path + ".Rule(" + e.getKey() + ")", e.getValue()));
        }
        update(path, Rulesets.Ruleset.Value.ID, r.getId());
        update(path, Rulesets.Ruleset.Value.NAME, r.getName());
        update(path, Rulesets.Ruleset.Value.PARENT_ID, r.getParentRulesetId());
    }

    private void processAlternateName(Team.AlternateName an, boolean remove) {
	String path = getPath(an);
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        updates.add(new WSUpdate(path, an.getName()));
    }

    private void processColor(Team.Color c, boolean remove) {
	String path = getPath(c);
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        updates.add(new WSUpdate(path, c.getColor()));
    }

    private void processPosition(Position p, boolean remove) {
	String path = getPath(p);
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

	for (Position.Value prop : Position.Value.values()) {
	    update(path, prop, p.get(prop));
	}
    }

    private void processJamStats(Stats.JamStats js) {
	for (Stats.JamStats.Value prop : Stats.JamStats.Value.values()) {
	    update(getPath(js), prop, js.get(prop));
	}
    }

    private void processTeamStats(Stats.TeamStats ts) {
	for (Stats.TeamStats.Value prop : Stats.TeamStats.Value.values()) {
	    update(getPath(ts), prop, ts.get(prop));
	}
    }

    private void processSkaterStats(Stats.SkaterStats ss) {
	for (Stats.SkaterStats.Value prop : Stats.SkaterStats.Value.values()) {
	    update(getPath(ss), prop, ss.get(prop));
	}
    }

    private void processMediaFile(Media.MediaFile mf) {
	for (Media.MediaFile.Value prop : Media.MediaFile.Value.values()) {
	    update(getPath(mf), prop, mf.get(prop));
	}
    }

    private void processPenaltyCodes(Rulesets r) {
        updates.add(new WSUpdate("ScoreBoard.PenaltyCode", null));
        String file = r.get(Rule.PENALTIES_FILE);
        if(file != null && !file.isEmpty()) {
            PenaltyCodesDefinition penalties = pm.loadFromJSON(file);
            for(PenaltyCode p : penalties.getPenalties()) {
                updates.add(new WSUpdate("ScoreBoard.PenaltyCode."+p.getCode(), p.CuesForWS(p)));
            }
            updates.add(new WSUpdate("ScoreBoard.PenaltyCode.?","Unknown"));
        }

    }

    private void initialize(ScoreBoard sb) {
	for (ScoreBoard.Value prop : ScoreBoard.Value.values()) {
	    update(getPath(sb), prop, sb.get(prop));
	}

        // Process Rules
        int index = 0;
        for (Rule rule : Rule.values()) {
            AbstractRule r = rule.getRule();
            String prefix = "ScoreBoard." + PropertyConversion.toFrontend(Rulesets.Value.RULE_DEFINITIONS)
            	+ "(" + r.getFullName() + ")";
            updates.add(new WSUpdate(prefix + ".Name", r.getFullName()));
            updates.add(new WSUpdate(prefix + ".Description", r.getDescription()));
            updates.add(new WSUpdate(prefix + ".Type", r.getType().toString()));
            updates.add(new WSUpdate(prefix + ".Index", index)); // Used to preserve order of rules.
            if (r.getType() == Type.BOOLEAN) {
                updates.add(new WSUpdate(prefix + ".TrueValue", ((BooleanRule)r).getTrueValue()));
                updates.add(new WSUpdate(prefix + ".FalseValue", ((BooleanRule)r).getFalseValue()));
            }
            index++;
        }
        processCurrentRuleset("ScoreBoard", sb.getRulesets());
        for (Rulesets.Ruleset r : sb.getRulesets().getRulesets().values()) {
            processRuleset("ScoreBoard", r, false);
        }

        processPenaltyCodes(sb.getRulesets());

        // Process Teams
        for (Team t : sb.getTeams()) {
            processTeam(t, false);
        }

        // Process Clocks
        for (Clock c : sb.getClocks()) {
            processClock(c, false);
        }

        // Process Media
        Media m = sb.getMedia();
        for (String format : m.getFormats()) {
            for (String type : m.getTypes(format)) {
                updates.add(new WSUpdate("ScoreBoard.Media." + format + "." + type, ""));
                Map<String, Media.MediaFile> h = m.getMediaFiles(format, type);
                for (Media.MediaFile mf: h.values()) {
                    processMediaFile(mf);
                }
            }
        }


        updateState();
    }
    
    String getPath(ScoreBoardEventProvider p) {
	String path = "";
	if (p.getParent() != null) {
	    path = getPath(p.getParent()) + ".";
	}
	path = path + p.getProviderName();
	if (!"".equals(p.getProviderId()) && p.getProviderId() != null) {
	    path = path + "(" + p.getProviderId() + ")";
	}
	return path;
    }


    private JSONStateManager jsm;
    private PenaltyCodesManager pm = new PenaltyCodesManager();
    private List<WSUpdate> updates = new LinkedList<WSUpdate>();
    private long batch = 0;
}
