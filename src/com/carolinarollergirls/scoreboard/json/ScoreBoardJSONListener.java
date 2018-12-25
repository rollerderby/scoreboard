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
import com.carolinarollergirls.scoreboard.core.TimeoutOwner;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
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
                Object pv = event.getPreviousValue();
                if (p instanceof ScoreBoard) {
                    // these properties are required for the XML listener, but are not used in the WS listener
                    // so they are ignored here
                    if(prop != ScoreBoard.Child.CLOCK && prop != ScoreBoard.Child.TEAM) {
                        update("ScoreBoard", prop, v);
                    }
                } else if (p instanceof Team) {
                    Team t = (Team)p;
                    String childPath = "ScoreBoard.Team(" + t.getId() + ")";
                    if (prop == Team.Child.SKATER) {
                	if (v != null) {
                	    processSkater(childPath, (Skater)v, false);
                	} else {
                	    processSkater(childPath, (Skater)pv, true);
                	}
                    } else if (prop == Team.Child.ALTERNATE_NAME) {
                	if (v != null) {
                	    processAlternateName(childPath, (Team.AlternateName)v, false);
                	} else {
                	    processAlternateName(childPath, (Team.AlternateName)pv, true);
                	}
                    } else if (prop == Team.Child.COLOR) {
                	if (v != null) {
                	    processColor(childPath, (Team.Color)v, false);
                	} else {
                	    processColor(childPath, (Team.Color)pv, true);
                	}
                    }
                    // Fast path for jam start/end to avoid sending the entire team.
                    else if (prop == Team.Value.LAST_SCORE) {
                        update(childPath, prop, t.getLastScore());
                        updates.add(new WSUpdate(childPath + ".JamScore", t.getScore() - t.getLastScore()));
                    } else if (prop == Team.Value.LEAD_JAMMER) {
                        update(childPath, prop, t.getLeadJammer());
                    } else if (prop == Team.Value.STAR_PASS) {
                        update(childPath, prop, t.isStarPass());
                    } else {
                        processTeam("ScoreBoard", t, false);
                    }
                } else if (p instanceof Skater) {
                    Skater s = (Skater)p;
                    processSkater("ScoreBoard.Team(" + s.getTeam().getId() + ")", s, false);
                } else if (p instanceof Position) {
                    Position pos = (Position)p;
                    processPosition("ScoreBoard.Team(" + pos.getTeam().getId() + ")", pos, false);
                } else if (p instanceof Clock) {
                    processClock("ScoreBoard", (Clock)p, false);
                } else if (p instanceof Rulesets) {
                    if (prop == Rulesets.Value.RULESET) {
                        processCurrentRuleset("ScoreBoard", (Rulesets)p);
                    } else {
                        processRuleset("ScoreBoard", (Rulesets.Ruleset)pv, true);
                    }
                } else if (p instanceof Rulesets.Ruleset) {
                    processRuleset("ScoreBoard", (Rulesets.Ruleset)p, false);
                } else if (p instanceof Team.AlternateName) {
                    Team.AlternateName an = (Team.AlternateName)p;
                    updates.add(new WSUpdate("ScoreBoard.Team(" + an.getTeam().getId() + ").AlternateName(" + an.getId() + ")", v));
                } else if (p instanceof Team.Color) {
                    Team.Color c = (Team.Color)p;
                    updates.add(new WSUpdate("ScoreBoard.Team(" + c.getTeam().getId() + ").Color(" + c.getId() + ")", v));
                } else if (p instanceof Stats) {
                    if (prop == Stats.Child.PERIOD && v == null) {
                        Stats.PeriodStats ps = (Stats.PeriodStats)pv;
                        updates.add(new WSUpdate("ScoreBoard.Stats.Period(" + ps.getPeriodNumber() + ")", null));
                    }
                } else if (p instanceof Stats.PeriodStats) {
                    if (prop == Stats.PeriodStats.Child.JAM && v == null) {
                        Stats.JamStats js = (Stats.JamStats)pv;
                        updates.add(new WSUpdate("ScoreBoard.Stats.Period(" + js.getPeriodNumber() + ").Jam(" + js.getJamNumber() + ")", null));
                    }
                } else if (p instanceof Stats.TeamStats && prop == Stats.TeamStats.Child.SKATER && v == null) {
                    Stats.SkaterStats ss = (Stats.SkaterStats)pv;
                    updates.add(new WSUpdate("ScoreBoard.Stats.Period(" + ss.getPeriodNumber() + ").Jam(" + ss.getJamNumber() + ").Team(" + ss.getTeamId() + ").Skater(" + ss.getSkaterId() + ")", null));
                } else if (p instanceof Stats.JamStats) {
                    Stats.JamStats js = (Stats.JamStats)p;
                    processJamStats("ScoreBoard.Stats.Period(" + js.getPeriodNumber() + ").Jam(" + js.getJamNumber() + ")", js);
                } else if (p instanceof Stats.TeamStats) {
                    Stats.TeamStats ts = (Stats.TeamStats)p;
                    processTeamStats("ScoreBoard.Stats.Period(" + ts.getPeriodNumber() + ").Jam(" + ts.getJamNumber() + ").Team(" + ts.getTeamId() + ")", ts);
                } else if (p instanceof Stats.SkaterStats) {
                    Stats.SkaterStats ts = (Stats.SkaterStats)p;
                    processSkaterStats("ScoreBoard.Stats.Period(" + ts.getPeriodNumber() + ").Jam(" + ts.getJamNumber() + ").Team(" + ts.getTeamId() + ").Skater(" + ts.getSkaterId() + ")", ts);
                } else if (p instanceof Settings) {
                    updates.add(new WSUpdate("ScoreBoard.Settings." + ((ValueWithId)v).getId(), ((ValueWithId)v).getValue()));
                } else if (p instanceof MediaType && prop == MediaType.Child.FILE && v == null) {
                    Media.MediaFile mf = (Media.MediaFile)pv;
                    updates.add(new WSUpdate("ScoreBoard.Media." + mf.getFormat() + "." + mf.getType() + ".File(" + mf.getId() + ")", null));
                } else if (p instanceof MediaType) {
                    Media.MediaFile mf = (Media.MediaFile)v;
                    processMediaFile("ScoreBoard.Media." + mf.getFormat() + "." + mf.getType() + ".File(" + mf.getId() + ")", mf);
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
        } else if (v instanceof TimeoutOwner) {
            updates.add(new WSUpdate(path, ((TimeoutOwner) v).getId()));
	} else {
            updates.add(new WSUpdate(path, v));
        }
    }

    private void processSkater(String path, Skater s, boolean remove) {
        path = path + ".Skater(" + s.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        update(path, Skater.Value.NAME, s.getName());
        update(path, Skater.Value.NUMBER, s.getNumber());
        update(path, Skater.Value.POSITION, s.getPosition() == null ? "" :
            s.getPosition().getFloorPosition().toString());
        update(path, Skater.Value.ROLE, s.getRole().toString());
        update(path, Skater.Value.FLAGS, s.getFlags());
        update(path, Skater.Value.PENALTY_BOX, s.isPenaltyBox());

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
        update(path, Skater.Penalty.Value.ID, p.getId());
        update(path, Skater.Penalty.Value.PERIOD, p.getPeriod());
        update(path, Skater.Penalty.Value.JAM, p.getJam());
        update(path, Skater.Penalty.Value.CODE, p.getCode());
    }

    private void processTeam(String path, Team t, boolean remove) {
        path = path + ".Team(" + t.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        update(path, Team.Value.NAME, t.getName());
        update(path, Team.Value.LOGO, t.getLogo());
        update(path, Team.Value.SCORE, t.getScore());
        update(path, Team.Value.LAST_SCORE, t.getLastScore());
        update(path, Team.Value.JAM_SCORE, t.getScore() - t.getLastScore());
        update(path, Team.Value.TIMEOUTS, t.getTimeouts());
        update(path, Team.Value.OFFICIAL_REVIEWS, t.getOfficialReviews());
        update(path, Team.Value.IN_TIMEOUT, t.inTimeout());
        update(path, Team.Value.IN_OFFICIAL_REVIEW, t.inOfficialReview());
        update(path, Team.Value.RETAINED_OFFICIAL_REVIEW, t.retainedOfficialReview());
        update(path, Team.Value.LEAD_JAMMER, t.getLeadJammer());
        update(path, Team.Value.STAR_PASS, t.isStarPass());
        update(path, Team.Value.NO_PIVOT, t.hasNoPivot());

        // Skaters
        for (Skater s : t.getSkaters()) {
            processSkater(path, s, false);
        }

        // Positions
        for (Position p : t.getPositions()) {
            processPosition(path, p, false);
        }

        // Alternate Names
        for (Team.AlternateName an : t.getAlternateNames()) {
            processAlternateName(path, an, false);
        }

        // Colors
        for (Team.Color c : t.getColors()) {
            processColor(path, c, false);
        }
    }

    private void processClock(String path, Clock c, boolean remove) {
        path = path + ".Clock(" + c.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        update(path, Clock.Value.NAME, c.getName());
        update(path, Clock.Value.NUMBER, c.getNumber());
        update(path, Clock.Value.MINIMUM_NUMBER, c.getMinimumNumber());
        update(path, Clock.Value.MAXIMUM_NUMBER, c.getMaximumNumber());
        update(path, Clock.Value.TIME, c.getTime());
        update(path, Clock.Value.INVERTED_TIME, c.getInvertedTime());
        update(path, Clock.Value.MINIMUM_TIME, c.getMinimumTime());
        update(path, Clock.Value.MAXIMUM_TIME, c.getMaximumTime());
        update(path, Clock.Value.DIRECTION, c.isCountDirectionDown());
        update(path, Clock.Value.RUNNING, c.isRunning());
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

    private void processAlternateName(String path, Team.AlternateName an, boolean remove) {
        path = path + ".AlternateName(" + an.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        updates.add(new WSUpdate(path, an.getName()));
    }

    private void processColor(String path, Team.Color c, boolean remove) {
        path = path + ".Color(" + c.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        updates.add(new WSUpdate(path, c.getColor()));
    }

    private void processPosition(String path, Position p, boolean remove) {
        path = path + ".Position(" + p.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        update(path, Position.Value.SKATER, p.getSkater() == null ? null : p.getSkater().getId());
        update(path, Position.Value.PENALTY_BOX, p.isPenaltyBox());
    }

    private void processJamStats(String path, Stats.JamStats js) {
        update(path, Stats.JamStats.Value.JAM_CLOCK_ELAPSED_END, js.getJamClockElapsedEnd());
        update(path, Stats.JamStats.Value.PERIOD_CLOCK_ELAPSED_START, js.getPeriodClockElapsedStart());
        update(path, Stats.JamStats.Value.PERIOD_CLOCK_ELAPSED_END, js.getPeriodClockElapsedEnd());
        update(path, Stats.JamStats.Value.PERIOD_CLOCK_WALLTIME_START, js.getPeriodClockWalltimeStart());
        update(path, Stats.JamStats.Value.PERIOD_CLOCK_WALLTIME_END, js.getPeriodClockWalltimeEnd());
    }

    private void processTeamStats(String path, Stats.TeamStats ts) {
        update(path, Stats.TeamStats.Value.TOTAL_SCORE, ts.getTotalScore());
        update(path, Stats.TeamStats.Value.JAM_SCORE, ts.getJamScore());
        update(path, Stats.TeamStats.Value.LEAD_JAMMER, ts.getLeadJammer());
        update(path, Stats.TeamStats.Value.STAR_PASS, ts.getStarPass());
        update(path, Stats.TeamStats.Value.NO_PIVOT, ts.getNoPivot());
        update(path, Stats.TeamStats.Value.TIMEOUTS, ts.getTimeouts());
        update(path, Stats.TeamStats.Value.OFFICIAL_REVIEWS, ts.getOfficialReviews());
    }

    private void processSkaterStats(String path, Stats.SkaterStats ss) {
        update(path, Stats.SkaterStats.Value.ID, ss.getSkaterId());
        update(path, Stats.SkaterStats.Value.POSITION, ss.getPosition());
        update(path, Stats.SkaterStats.Value.PENALTY_BOX, ss.getPenaltyBox());
    }

    private void processMediaFile(String path, Media.MediaFile mf) {
        update(path, Media.MediaFile.Value.ID, mf.getId());
        update(path, Media.MediaFile.Value.NAME, mf.getName());
        update(path, Media.MediaFile.Value.SRC, mf.getSrc());
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
        update("ScoreBoard", ScoreBoard.Value.IN_PERIOD, sb.isInPeriod());
        update("ScoreBoard", ScoreBoard.Value.IN_OVERTIME, sb.isInOvertime());
        update("ScoreBoard", ScoreBoard.Value.OFFICIAL_SCORE, sb.isOfficialScore());
        update("ScoreBoard", ScoreBoard.Value.TIMEOUT_OWNER, sb.getTimeoutOwner());
        update("ScoreBoard", ScoreBoard.Value.OFFICIAL_REVIEW, sb.isOfficialReview());

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
            processTeam("ScoreBoard", t, false);
        }

        // Process Clocks
        for (Clock c : sb.getClocks()) {
            processClock("ScoreBoard", c, false);
        }

        // Process Media
        Media m = sb.getMedia();
        for (String format : m.getFormats()) {
            for (String type : m.getTypes(format)) {
                updates.add(new WSUpdate("ScoreBoard.Media." + format + "." + type, ""));
                Map<String, Media.MediaFile> h = m.getMediaFiles(format, type);
                for (Media.MediaFile mf: h.values()) {
                    processMediaFile("ScoreBoard.Media." + mf.getFormat() + "." + mf.getType() + ".File(" + mf.getId() + ")", mf);
                }
            }
        }


        updateState();
    }


    private JSONStateManager jsm;
    private PenaltyCodesManager pm = new PenaltyCodesManager();
    private List<WSUpdate> updates = new LinkedList<WSUpdate>();
    private long batch = 0;
}
