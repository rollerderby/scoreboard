package com.carolinarollergirls.scoreboard.json;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.jetty.WS;

/**
 * Converts a ScoreBoardEvent into a representative JSON Update
 */
public class ScoreBoardJSONListener implements ScoreBoardListener
{
	public ScoreBoardJSONListener(ScoreBoard sb) {
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
					if (batch == 0)
						return;
					if (--batch == 0)
						updateState();
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
					if (v instanceof Skater)
						processSkater(childPath, (Skater)v, prop.equals(Team.EVENT_REMOVE_SKATER));
					else if (v instanceof Position)
						processPosition(childPath, (Position)v, false);
					else if (v instanceof Team.AlternateName)
						processAlternateName(childPath, (Team.AlternateName)v, prop.equals(Team.EVENT_REMOVE_ALTERNATE_NAME));
					else if (v instanceof Team.Color)
						processColor(childPath, (Team.Color)v, prop.equals(Team.EVENT_REMOVE_COLOR));
					// Fast path for jam start/end to avoid sending the entire team.
					else if (prop.equals(Team.EVENT_LAST_SCORE)) {
						updateMap.put(childPath + "." + Team.EVENT_LAST_SCORE, t.getLastScore());
						updateMap.put(childPath + ".JamScore", t.getScore() - t.getLastScore());
					} else if (prop.equals(Team.EVENT_LEAD_JAMMER))
						updateMap.put(childPath + "." + Team.EVENT_LEAD_JAMMER, t.getLeadJammer());
					else if (prop.equals(Team.EVENT_STAR_PASS))
						updateMap.put(childPath + "." + Team.EVENT_STAR_PASS, t.isStarPass());
					else
						processTeam("ScoreBoard", t, prop.equals(ScoreBoard.EVENT_REMOVE_TEAM));
				} else if (p instanceof Skater) {
					Skater s = (Skater)p;
					processSkater("ScoreBoard.Team(" + s.getTeam().getId() + ")", s, prop.equals(Team.EVENT_REMOVE_SKATER));
				} else if (p instanceof Position) {
					Position pos = (Position)p;
					processPosition("ScoreBoard.Team(" + pos.getTeam().getId() + ")", pos, false);
				} else if (p instanceof Clock) {
					processClock("ScoreBoard", (Clock)p, prop.equals(ScoreBoard.EVENT_REMOVE_CLOCK));
				} else if (p instanceof Policy) {
					processPolicy("ScoreBoard", (Policy)p, prop.equals(ScoreBoard.EVENT_REMOVE_POLICY));
				} else if (p instanceof Policy.Parameter) {
					Policy.Parameter param = (Policy.Parameter)p;
					Policy pol = param.getPolicy();
					update("ScoreBoard.Policy(" + pol.getId() + ")", param.getName(), v);
				} else if (p instanceof Settings) {
					Settings s = (Settings)p;
					String prefix = null;
					if (s.getParent() instanceof ScoreBoard)
						prefix = "ScoreBoard";
					if (prefix == null)
						ScoreBoardManager.printMessage(provider + " update of unknown kind.  prop: " + prop + ", v: " + v);
					else
						update(prefix, "Setting(" + prop + ")", v);
				} else if (p instanceof Team.AlternateName) {
					Team.AlternateName an = (Team.AlternateName)p;
					update("ScoreBoard.Team(" + an.getTeam().getId() + ")", "AlternateName(" + an.getId() + ")", v);
				} else if (p instanceof Team.Color) {
					Team.Color c = (Team.Color)p;
					update("ScoreBoard.Team(" + c.getTeam().getId() + ")", "Color(" + c.getId() + ")", v);
				} else
					ScoreBoardManager.printMessage(provider + " update of unknown kind.  prop: " + prop + ", v: " + v);

			} catch (Exception e) {
				ScoreBoardManager.printMessage("Error!  " + e.getMessage());
				e.printStackTrace();
			} finally {
				if (batch == 0)
					updateState();
			}
		}
	}

	private void updateState() {
		synchronized (this) {
			if (updateMap.size() == 0)
				return;
			WS.updateState(updateMap);
			updateMap.clear();
		}
	}

	private void update(String prefix, String prop, Object v) {
		if (v instanceof String)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Integer)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Long)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Boolean)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Skater)
			update(prefix, prop, (Skater)v);
		else {
			ScoreBoardManager.printMessage(prefix + " update of unknown type.  prop: " + prop + ", v: " + v + " v.getClass(): " + v.getClass());
		}
	}

	private void processPolicy(String path, Policy p, boolean remove) {
		path = path + ".Policy(" + p.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path + "." + Policy.EVENT_NAME, p.getName());
		updateMap.put(path + "." + Policy.EVENT_DESCRIPTION, p.getDescription());
		updateMap.put(path + "." + Policy.EVENT_ENABLED, p.isEnabled());
		for (Policy.Parameter param : p.getParameters()) {
			updateMap.put(path + "." + param.getName(), param.getValue());
		}
	}

	private void processSkater(String path, Skater s, boolean remove) {
		path = path + ".Skater(" + s.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path + "." + Skater.EVENT_NAME, s.getName());
		updateMap.put(path + "." + Skater.EVENT_NUMBER, s.getNumber());
		updateMap.put(path + "." + Skater.EVENT_POSITION, s.getPosition());
		updateMap.put(path + "." + Skater.EVENT_FLAGS, s.getFlags());
		updateMap.put(path + "." + Skater.EVENT_PENALTY_BOX, s.isPenaltyBox());

    List<Skater.Penalty> penalties = s.getPenalties();
    for (int i = 0; i < 9; i++) {
      String base = path + ".Penalty(" + (i + 1) + ")";
      if (i < penalties.size())
        processPenalty(base, penalties.get(i), false);
      else
        processPenalty(base, null, true);
    }
		processPenalty(path + ".Penalty(FO_EXP)", s.getFOEXPPenalty(), s.getFOEXPPenalty() == null);
	}

	private void processPenalty(String path, Skater.Penalty p, boolean remove) {
		if (remove) {
			updateMap.put(path, null);
			return;
		}
    updateMap.put(path + ".Id", p.getId());
    updateMap.put(path + ".Period", p.getPeriod());
    updateMap.put(path + ".Jam", p.getJam());
    updateMap.put(path + ".Code", p.getCode());
  }

	private void processTeam(String path, Team t, boolean remove) {
		path = path + ".Team(" + t.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path + "." + Team.EVENT_NAME, t.getName());
		updateMap.put(path + "." + Team.EVENT_LOGO, t.getLogo());
		updateMap.put(path + "." + Team.EVENT_SCORE, t.getScore());
		updateMap.put(path + "." + Team.EVENT_LAST_SCORE, t.getLastScore());
		updateMap.put(path + ".JamScore", t.getScore() - t.getLastScore());
		updateMap.put(path + "." + Team.EVENT_TIMEOUTS, t.getTimeouts());
		updateMap.put(path + "." + Team.EVENT_OFFICIAL_REVIEWS, t.getOfficialReviews());
		updateMap.put(path + "." + Team.EVENT_IN_TIMEOUT, t.inTimeout());
		updateMap.put(path + "." + Team.EVENT_IN_OFFICIAL_REVIEW, t.inOfficialReview());
		updateMap.put(path + "." + Team.EVENT_RETAINED_OFFICIAL_REVIEW, t.retainedOfficialReview());
		updateMap.put(path + "." + Team.EVENT_LEAD_JAMMER, t.getLeadJammer());
		updateMap.put(path + "." + Team.EVENT_STAR_PASS, t.isStarPass());

		// Skaters
		for (Skater s : t.getSkaters())
			processSkater(path, s, false);

		// Positions
		for (Position p : t.getPositions())
			processPosition(path, p, false);

		// Alternate Names
		for (Team.AlternateName an : t.getAlternateNames())
			processAlternateName(path, an, false);

		// Colors
		for (Team.Color c : t.getColors())
			processColor(path, c, false);
	}

	private void processClock(String path, Clock c, boolean remove) {
		path = path + ".Clock(" + c.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path + "." + Clock.EVENT_NAME, c.getName());
		updateMap.put(path + "." + Clock.EVENT_NUMBER, c.getNumber());
		updateMap.put(path + "." + Clock.EVENT_MINIMUM_NUMBER, c.getMinimumNumber());
		updateMap.put(path + "." + Clock.EVENT_MAXIMUM_NUMBER, c.getMaximumNumber());
		updateMap.put(path + "." + Clock.EVENT_TIME, c.getTime());
		updateMap.put(path + "." + Clock.EVENT_INVERTED_TIME, c.getInvertedTime());
		updateMap.put(path + "." + Clock.EVENT_MINIMUM_TIME, c.getMinimumTime());
		updateMap.put(path + "." + Clock.EVENT_MAXIMUM_TIME, c.getMaximumTime());
		updateMap.put(path + "." + Clock.EVENT_DIRECTION, c.isCountDirectionDown());
		updateMap.put(path + "." + Clock.EVENT_RUNNING, c.isRunning());
	}

	private void processSettings(String path, Settings s) {
		for (String key : s.getAll().keySet())
			updateMap.put(path + ".Setting(" + key + ")", s.get(key));
	}

	private void processAlternateName(String path, Team.AlternateName an, boolean remove) {
		path = path + ".AlternateName(" + an.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path, an.getName());
	}

	private void processColor(String path, Team.Color c, boolean remove) {
		path = path + ".Color(" + c.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path, c.getColor());
	}

	private void processPosition(String path, Position p, boolean remove) {
		path = path + ".Position(" + p.getId() + ")";
		if (remove) {
			updateMap.put(path, null);
			return;
		}

		updateMap.put(path + "." + Position.EVENT_SKATER, p.getSkater() == null ? null : p.getSkater().getId());
		updateMap.put(path + "." + Position.EVENT_PENALTY_BOX, p.getPenaltyBox());
	}

	private void initialize(ScoreBoard sb) {
		updateMap.put("ScoreBoard." + ScoreBoard.EVENT_IN_PERIOD, sb.isInPeriod());
		updateMap.put("ScoreBoard." + ScoreBoard.EVENT_IN_OVERTIME, sb.isInOvertime());
		updateMap.put("ScoreBoard." + ScoreBoard.EVENT_OFFICIAL_SCORE, sb.isOfficialScore());
		updateMap.put("ScoreBoard." + ScoreBoard.EVENT_RULESET, sb.getRuleset());
		updateMap.put("ScoreBoard." + ScoreBoard.EVENT_TIMEOUT_OWNER, sb.getTimeoutOwner());
		updateMap.put("ScoreBoard." + ScoreBoard.EVENT_OFFICIAL_REVIEW, sb.isOfficialReview());

		updateMap.put("PenaltyCode.Penalty(B)", "Back Block");
		updateMap.put("PenaltyCode.Penalty(A)", "High Block");
		updateMap.put("PenaltyCode.Penalty(L)", "Low Block");
		updateMap.put("PenaltyCode.Penalty(E)", "Leg Block");
		updateMap.put("PenaltyCode.Penalty(F)", "Forearm");
		updateMap.put("PenaltyCode.Penalty(H)", "Head Block");
		updateMap.put("PenaltyCode.Penalty(M)", "Multiplayer");
		updateMap.put("PenaltyCode.Penalty(C)", "Illegal Contact-Illegal Assist-OOP Block-Early/Late Hit");
		updateMap.put("PenaltyCode.Penalty(D)", "Direction-Stop Block");
		updateMap.put("PenaltyCode.Penalty(P)", "Illegal Position-Destruction-Skating OOB-Failure to...");
		updateMap.put("PenaltyCode.Penalty(X)", "Cut-Illegal Re-Entry");
		updateMap.put("PenaltyCode.Penalty(I)", "Illegal Procedure-Star Pass Violation-Pass Interference");
		updateMap.put("PenaltyCode.Penalty(N)", "Interference-Delay Of Game");
		updateMap.put("PenaltyCode.Penalty(G)", "Misconduct-Insubordination");
		updateMap.put("PenaltyCode.Penalty(?)", "Unknown");
		updateMap.put("PenaltyCode.FO_EXP(FO)", "Foul Out");
		updateMap.put("PenaltyCode.FO_EXP(EXP-B)", "Expulsion-Back Block");
		updateMap.put("PenaltyCode.FO_EXP(EXP-A)", "Expulsion-High Block");
		updateMap.put("PenaltyCode.FO_EXP(EXP-L)", "Expulsion-Low Block");
		updateMap.put("PenaltyCode.FO_EXP(EXP-E)", "Expulsion-Leg Block");
		updateMap.put("PenaltyCode.FO_EXP(EXP-F)", "Expulsion-Forearm");
		updateMap.put("PenaltyCode.FO_EXP(EXP-H)", "Expulsion-Head Block");
		updateMap.put("PenaltyCode.FO_EXP(EXP-M)", "Expulsion-Multiplayer");
		updateMap.put("PenaltyCode.FO_EXP(EXP-C)", "Expulsion-Illegal Contact");
		updateMap.put("PenaltyCode.FO_EXP(EXP-D)", "Expulsion-Direction");
		updateMap.put("PenaltyCode.FO_EXP(EXP-P)", "Expulsion-Illegal Position");
		updateMap.put("PenaltyCode.FO_EXP(EXP-N)", "Expulsion-Interference");
		updateMap.put("PenaltyCode.FO_EXP(EXP-G)", "Expulsion-Misconduct");
		updateMap.put("PenaltyCode.FO_EXP(EXP-?)", "Expulsion-Unknown");

		// Process Settings
		processSettings("ScoreBoard", sb.getSettings());

		// Process Teams
		for (Team t : sb.getTeams()) {
			processTeam("ScoreBoard", t, false);
		}

		// Process Clocks
		for (Clock c : sb.getClocks()) {
			processClock("ScoreBoard", c, false);
		}

		// Process Policies TODO DELETE POLICIES
		for (Policy p : sb.getPolicies()) {
			processPolicy("ScoreBoard", p, false);
		}

		updateState();
	}

	private Map<String, Object> updateMap = new LinkedHashMap<String, Object>();
	private long batch = 0;
}
