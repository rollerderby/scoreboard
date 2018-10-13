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

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCode;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesDefinition;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.FrontendSettings;
import com.carolinarollergirls.scoreboard.view.Position;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Settings;
import com.carolinarollergirls.scoreboard.view.Skater;
import com.carolinarollergirls.scoreboard.view.Stats;
import com.carolinarollergirls.scoreboard.view.Team;

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
                String prop = event.getProperty();
                if (prop.equals(ScoreBoardEvent.BATCH_START)) {
                    batch++;
                    return;
                }
                if (prop.equals(ScoreBoardEvent.BATCH_END)) {
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
                    if(!prop.equals(ScoreBoard.EVENT_ADD_CLOCK) && !prop.equals(ScoreBoard.EVENT_ADD_TEAM)) {
                        update("ScoreBoard", prop, v);
                    }
                } else if (p instanceof Team) {
                    Team t = (Team)p;
                    String childPath = "ScoreBoard.Team(" + t.getId() + ")";
                    if (v instanceof Skater) {
                        processSkater(childPath, (Skater)v, prop.equals(Team.EVENT_REMOVE_SKATER));
                    } else if (v instanceof Position) {
                        processPosition(childPath, (Position)v, false);
                    } else if (v instanceof Team.AlternateName) {
                        processAlternateName(childPath, (Team.AlternateName)v, prop.equals(Team.EVENT_REMOVE_ALTERNATE_NAME));
                    } else if (v instanceof Team.Color) {
                        processColor(childPath, (Team.Color)v, prop.equals(Team.EVENT_REMOVE_COLOR));
                    }
                    // Fast path for jam start/end to avoid sending the entire team.
                    else if (prop.equals(Team.EVENT_LAST_SCORE)) {
                        updates.add(new WSUpdate(childPath + "." + Team.EVENT_LAST_SCORE, t.getLastScore()));
                        updates.add(new WSUpdate(childPath + ".JamScore", t.getScore() - t.getLastScore()));
                    } else if (prop.equals(Team.EVENT_LEAD_JAMMER)) {
                        updates.add(new WSUpdate(childPath + "." + Team.EVENT_LEAD_JAMMER, t.getLeadJammer()));
                    } else if (prop.equals(Team.EVENT_STAR_PASS)) {
                        updates.add(new WSUpdate(childPath + "." + Team.EVENT_STAR_PASS, t.isStarPass()));
                    } else {
                        processTeam("ScoreBoard", t, prop.equals(ScoreBoard.EVENT_REMOVE_TEAM));
                    }
                } else if (p instanceof Skater) {
                    Skater s = (Skater)p;
                    processSkater("ScoreBoard.Team(" + s.getTeam().getId() + ")", s, prop.equals(Team.EVENT_REMOVE_SKATER));
                } else if (p instanceof Position) {
                    Position pos = (Position)p;
                    processPosition("ScoreBoard.Team(" + pos.getTeam().getId() + ")", pos, false);
                } else if (p instanceof Clock) {
                    processClock("ScoreBoard", (Clock)p, prop.equals(ScoreBoard.EVENT_REMOVE_CLOCK));
                } else if (p instanceof Settings) {
                    Settings s = (Settings)p;
                    String prefix = null;
                    if (s.getParent() instanceof ScoreBoard) {
                        prefix = "ScoreBoard";
                    }
                    if(prop.equals(PenaltyCodesManager.SETTING_PENALTIES_FILE)) {
                        update(prefix, "Setting(" + prop + ")", v);
                        processPenaltyCodes(s);
                    } else if (prefix == null) {
                        ScoreBoardManager.printMessage(provider + " update of unknown kind.  prop: " + prop + ", v: " + v);
                    } else {
                        update(prefix, "Setting(" + prop + ")", v);
                    }
                } else if (p instanceof Team.AlternateName) {
                    Team.AlternateName an = (Team.AlternateName)p;
                    update("ScoreBoard.Team(" + an.getTeam().getId() + ")", "AlternateName(" + an.getId() + ")", v);
                } else if (p instanceof Team.Color) {
                    Team.Color c = (Team.Color)p;
                    update("ScoreBoard.Team(" + c.getTeam().getId() + ")", "Color(" + c.getId() + ")", v);
                } else if (p instanceof Stats) {
                    if (prop.equals(Stats.EVENT_REMOVE_PERIOD)) {
                        Stats.PeriodStats ps = (Stats.PeriodStats)v;
                        updates.add(new WSUpdate("ScoreBoard.Stats.Period(" + ps.getPeriodNumber() + ")", null));
                    }
                } else if (p instanceof Stats.PeriodStats) {
                    if (prop.equals(Stats.PeriodStats.EVENT_REMOVE_JAM)) {
                        Stats.JamStats js = (Stats.JamStats)v;
                        updates.add(new WSUpdate("ScoreBoard.Stats.Period(" + js.getPeriodNumber() + ").Jam(" + js.getJamNumber() + ")", null));
                    }
                } else if (p instanceof Stats.TeamStats && prop.equals(Stats.TeamStats.EVENT_REMOVE_SKATER)) {
                    Stats.SkaterStats ss = (Stats.SkaterStats)v;
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
                } else if (p instanceof FrontendSettings) {
                    updates.add(new WSUpdate("ScoreBoard.FrontendSettings." + prop, v));
                } else {
                    ScoreBoardManager.printMessage(provider + " update of unknown kind.	prop: " + prop + ", v: " + v);
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

    private void update(String prefix, String prop, Object v) {
        if (v instanceof String) {
            updates.add(new WSUpdate(prefix + "." + prop, v));
        } else if (v instanceof Integer) {
            updates.add(new WSUpdate(prefix + "." + prop, v));
        } else if (v instanceof Long) {
            updates.add(new WSUpdate(prefix + "." + prop, v));
        } else if (v instanceof Boolean) {
            updates.add(new WSUpdate(prefix + "." + prop, v));
        } else if (v instanceof Skater) {
            update(prefix, prop, (Skater)v);
        } else {
            ScoreBoardManager.printMessage(prefix + " update of unknown type.  prop: " + prop + ", v: " + v + " v.getClass(): " + v.getClass());
        }
    }

    private void processSkater(String path, Skater s, boolean remove) {
        path = path + ".Skater(" + s.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        updates.add(new WSUpdate(path + "." + Skater.EVENT_NAME, s.getName()));
        updates.add(new WSUpdate(path + "." + Skater.EVENT_NUMBER, s.getNumber()));
        updates.add(new WSUpdate(path + "." + Skater.EVENT_POSITION, s.getPosition()));
        updates.add(new WSUpdate(path + "." + Skater.EVENT_FLAGS, s.getFlags()));
        updates.add(new WSUpdate(path + "." + Skater.EVENT_PENALTY_BOX, s.isPenaltyBox()));

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
        updates.add(new WSUpdate(path + ".Id", p.getId()));
        updates.add(new WSUpdate(path + ".Period", p.getPeriod()));
        updates.add(new WSUpdate(path + ".Jam", p.getJam()));
        updates.add(new WSUpdate(path + ".Code", p.getCode()));
    }

    private void processTeam(String path, Team t, boolean remove) {
        path = path + ".Team(" + t.getId() + ")";
        if (remove) {
            updates.add(new WSUpdate(path, null));
            return;
        }

        updates.add(new WSUpdate(path + "." + Team.EVENT_NAME, t.getName()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_LOGO, t.getLogo()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_SCORE, t.getScore()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_LAST_SCORE, t.getLastScore()));
        updates.add(new WSUpdate(path + ".JamScore", t.getScore() - t.getLastScore()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_TIMEOUTS, t.getTimeouts()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_OFFICIAL_REVIEWS, t.getOfficialReviews()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_IN_TIMEOUT, t.inTimeout()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_IN_OFFICIAL_REVIEW, t.inOfficialReview()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_RETAINED_OFFICIAL_REVIEW, t.retainedOfficialReview()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_LEAD_JAMMER, t.getLeadJammer()));
        updates.add(new WSUpdate(path + "." + Team.EVENT_STAR_PASS, t.isStarPass()));

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

        updates.add(new WSUpdate(path + "." + Clock.EVENT_NAME, c.getName()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_NUMBER, c.getNumber()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_MINIMUM_NUMBER, c.getMinimumNumber()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_MAXIMUM_NUMBER, c.getMaximumNumber()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_TIME, c.getTime()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_INVERTED_TIME, c.getInvertedTime()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_MINIMUM_TIME, c.getMinimumTime()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_MAXIMUM_TIME, c.getMaximumTime()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_DIRECTION, c.isCountDirectionDown()));
        updates.add(new WSUpdate(path + "." + Clock.EVENT_RUNNING, c.isRunning()));
    }

    private void processSettings(String path, Settings s) {
        for (String key : s.getAll().keySet()) {
            updates.add(new WSUpdate(path + ".Setting(" + key + ")", s.get(key)));
        }
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

        updates.add(new WSUpdate(path + "." + Position.EVENT_SKATER, p.getSkater() == null ? null : p.getSkater().getId()));
        updates.add(new WSUpdate(path + "." + Position.EVENT_PENALTY_BOX, p.getPenaltyBox()));
    }

    private void processJamStats(String path, Stats.JamStats js) {
        updates.add(new WSUpdate(path + ".JamClockElapsedEnd", js.getJamClockElapsedEnd()));
        updates.add(new WSUpdate(path + ".PeriodClockElapsedStart", js.getPeriodClockElapsedStart()));
        updates.add(new WSUpdate(path + ".PeriodClockElapsedEnd", js.getPeriodClockElapsedEnd()));
        updates.add(new WSUpdate(path + ".PeriodClockWalltimeStart", js.getPeriodClockWalltimeStart()));
        updates.add(new WSUpdate(path + ".PeriodClockWalltimeEnd", js.getPeriodClockWalltimeEnd()));
    }

    private void processTeamStats(String path, Stats.TeamStats ts) {
        updates.add(new WSUpdate(path + ".TotalScore", ts.getTotalScore()));
        updates.add(new WSUpdate(path + ".JamScore", ts.getJamScore()));
        updates.add(new WSUpdate(path + ".LeadJammer", ts.getLeadJammer()));
        updates.add(new WSUpdate(path + ".StarPass", ts.getStarPass()));
        updates.add(new WSUpdate(path + ".Timeouts", ts.getTimeouts()));
        updates.add(new WSUpdate(path + ".OfficialReviews", ts.getOfficialReviews()));
    }

    private void processSkaterStats(String path, Stats.SkaterStats ss) {
        updates.add(new WSUpdate(path + ".Id", ss.getSkaterId()));
        updates.add(new WSUpdate(path + ".Position", ss.getPosition()));
        updates.add(new WSUpdate(path + ".PenaltyBox", ss.getPenaltyBox()));
    }

    private void processPenaltyCodes(Settings s) {
        updates.add(new WSUpdate("ScoreBoard.PenaltyCode", null));
        String file = s.get(PenaltyCodesManager.SETTING_PENALTIES_FILE);
        if(file != null && !file.isEmpty()) {
            PenaltyCodesDefinition penalties = pm.loadFromJSON(file);
            for(PenaltyCode p : penalties.getPenalties()) {
                updates.add(new WSUpdate("ScoreBoard.PenaltyCode."+p.getCode(), p.CuesForWS(p)));
            }
            updates.add(new WSUpdate("ScoreBoard.PenaltyCode.?","Unknown"));
        }

    }

    private void initialize(ScoreBoard sb) {
        updates.add(new WSUpdate("ScoreBoard." + ScoreBoard.EVENT_IN_PERIOD, sb.isInPeriod()));
        updates.add(new WSUpdate("ScoreBoard." + ScoreBoard.EVENT_IN_OVERTIME, sb.isInOvertime()));
        updates.add(new WSUpdate("ScoreBoard." + ScoreBoard.EVENT_OFFICIAL_SCORE, sb.isOfficialScore()));
        updates.add(new WSUpdate("ScoreBoard." + ScoreBoard.EVENT_RULESET, sb.getRuleset()));
        updates.add(new WSUpdate("ScoreBoard." + ScoreBoard.EVENT_TIMEOUT_OWNER, sb.getTimeoutOwner()));
        updates.add(new WSUpdate("ScoreBoard." + ScoreBoard.EVENT_OFFICIAL_REVIEW, sb.isOfficialReview()));



        // Process Settings
        processSettings("ScoreBoard", sb.getSettings());

        processPenaltyCodes(sb.getSettings());

        // Process Teams
        for (Team t : sb.getTeams()) {
            processTeam("ScoreBoard", t, false);
        }

        // Process Clocks
        for (Clock c : sb.getClocks()) {
            processClock("ScoreBoard", c, false);
        }

        updateState();
    }


    private JSONStateManager jsm;
    private PenaltyCodesManager pm = new PenaltyCodesManager();
    private List<WSUpdate> updates = new LinkedList<WSUpdate>();
    private long batch = 0;
}
