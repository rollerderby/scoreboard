package com.carolinarollergirls.scoreboard;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.carolinarollergirls.scoreboard.rules.*;

public class Ruleset {
	public interface RulesetReceiver {
		public void applyRule(String rule, Object value);
	}

	public static boolean registerRule(RulesetReceiver rsr, String rule) {
		Rule r = rule_definitions.get(rule);
		if (r == null)
			return false;
		List<RulesetReceiver> receivers = rule_receivers.get(r);
		if (receivers == null) {
			receivers = new LinkedList<RulesetReceiver>();
			rule_receivers.put(r, receivers);
		}
		if (!receivers.contains(rsr))
			receivers.add(rsr);
		return true;
	}

	private static void addRuleset(Ruleset rs) {
		rulesets.add(rs);
	}

	private static Ruleset initialize() {
		synchronized (baseLock) {
			if (base != null) {
				return base;
			}

			newRule(   new TimeRule(false, "ScoreBoard", Clock.ID_INTERMISSION, "Time",
					"Duration of Intermissions", "15:00"));
			newRule( new StringRule(false, "ScoreBoard", Clock.ID_INTERMISSION, "PreGame",
					"Text Displayed for clock before first period", "Time To Derby"));
			newRule( new StringRule(false, "ScoreBoard", Clock.ID_INTERMISSION, "Intermission",
					"Text Displayed for clock between periods", "Intermission"));
			newRule( new StringRule(false, "ScoreBoard", Clock.ID_INTERMISSION, "Unofficial",
					"Text Displayed immediately after last period", "Unofficial Score"));
			newRule( new StringRule(false, "ScoreBoard", Clock.ID_INTERMISSION, "Official",
					"Text Displayed when score is declared official", "Final Score"));
			
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_TIMEOUT,      "ClockAfter",
					"Which clock to show between the end of a timeout and the start of the next Jam", true, "Show Lineup Clock", "Show Timeout Clock"));
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_TIMEOUT,      "StopOnUnspecifiedTO",
					"If this is set to Stop, period clock will stop for all timeouts. If this is set to Keep Running, the following options can override for specific types or based in duration. Period Clock will be corrected, if stopping type is selected within 15s.", true, "Stop Period Clock", "Keep Period Clock Running"));
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_TIMEOUT,      "StopOnOfficialTO",
					"Stop the period clock for Official Timeouts", true, "Stop Period Clock", "Keep Period Clock Running"));
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_TIMEOUT,      "StopOnTeamTO",
					"Stop the period clock for Team Timeouts", true, "Stop Period Clock", "Keep Period Clock Running"));
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_TIMEOUT,      "StopOnOR",
					"Stop the period clock for Official Reviews", true, "Stop Period Clock", "Keep Period Clock Running"));
			newRule(   new TimeRule(false, "ScoreBoard", Clock.ID_TIMEOUT,      "StopAfter",
					"Stop the period clock for timeouts longer than this value. Set to a high value to disable.", "02:00"));
			
			newRule(   new TimeRule(false, "ScoreBoard", Clock.ID_LINEUP,       "Time",
					"(Maximum) duration of Lineup before a regular Jam", "00:30"));
			newRule(   new TimeRule(false, "ScoreBoard", Clock.ID_LINEUP,       "OvertimeTime",
					"Duration of Lineup before an overtime Jam", "01:00"));
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_LINEUP,       "AutoStart",
					"Automatically start a Jam or Timeout after lineup time has elapsed?", false, "Auto Start", "Stay in Lineup"));
			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_LINEUP,       "AutoStartType",
					"What to start after lineup is over. (Only has an effect if AutoStart is enabled)", false, "Auto Start Jam", "Auto Start Timeout"));
			newRule(   new TimeRule(false, "ScoreBoard", Clock.ID_LINEUP, "AutoStartBufferTime",
					"Time to wait after the end of lineup before auto starting. Period/Jam clock will be corrected accordingly. Must be 0 if lineup clock is counting down", "00:02"));

			newRule(new BooleanRule(false, "ScoreBoard", Clock.ID_JAM,          "Number",
					"How to count Jam numbers", true, "Reset Each Period", "Continue Counting"));

			newRule(new IntegerRule(false, "Team", null, "Timeouts",
					"How many timeouts is each tem granted each game or period?", 3));
			newRule(new BooleanRule(false, "Team", null, "TimeoutsPer",
					"Are timeouts granted per period or per game?", false, "Period", "Game"));
			newRule(new IntegerRule(false, "Team", null, "OfficialReviews",
					"How many official reviews is each tem granted each game or period", 1));
			newRule(new BooleanRule(false, "Team", null, "OfficialReviewsPer",
					"Are official reviews per period or per game?", true, "Period", "Game"));
			
			newRule(new BooleanRule(false, "Clock", null,           "Sync",
					"Should seconds tick over at the same time for all clocks? When true, newly starting clocks will be adjusted by up to half a second to get in sync", true, "Sync Clocks", "Don't Sync Clocks"));

			newRule(new BooleanRule(false, "ScoreBoard", null,      "HideJamTotals",
					"Should the score for the current Jam be displayed?", false, "Hide Jam Totals", "Show Jam Totals"));
			newRule( new StringRule(false, "ScoreBoard", null,      "BackgroundStyle",
					"Background style to be used. Possible values: bg_blacktowhite, bg_whitetoblack, bg_black", "bg_blacktowhite"));
			newRule( new StringRule(false, "ScoreBoard", null,      "BoxStyle",
					"Style to use for the score and time boxes. Possible values: box_rounded, box_flat, box_flat_bright", "box_flat"));
			newRule(new BooleanRule(false, "ScoreBoard", null,      "SwapTeams",
					"Swap public Team display compared to operator display?", false, "Teams Swapped", "Teams Normal"));
			newRule(new IntegerRule(false, "ScoreBoard", null,      "SidePadding",
					"How much padding to use on the side (in % of display width).", 0));
			newRule( new StringRule(false, "ScoreBoard", null,      "CurrentView",
					"Default view to be shown. Possible values: scoreboard, image, video, html", "scoreboard"));
			newRule( new StringRule(false, "ScoreBoard", null,      "CustomHtml",
					"Page to display on the html view", "/customhtml/fullscreen/example.html"));
			newRule( new StringRule(false, "ScoreBoard", null,      "Image",
					"Image to show on the image view", "/images/fullscreen/American Flag.jpg"));
			newRule( new StringRule(false, "ScoreBoard", null,      "Video",
					"Video to display on the video view", "/videos/fullscreen/American Flag.webm"));

			newRule(new BooleanRule(false, "ScoreBoard", "Overlay", "TeamLogos",
					"Display Teamlogos on Overlays that support it?", true, "Display", "Hide"));
			newRule(new BooleanRule(false, "ScoreBoard", "Overlay", "LogoBackground",
					"Background color used for teamlogo area.", true, "Black", "Default"));

			newRule( new StringRule(false, "Clock", Clock.ID_PERIOD,       "Name",
					"The name displayed on the operator screen", Clock.ID_PERIOD));
			newRule(new IntegerRule(false, "Clock", Clock.ID_PERIOD,       "MinimumNumber",
					"Don't change unless you know what you are doing", 1));
			newRule(new IntegerRule(false, "Clock", Clock.ID_PERIOD,       "MaximumNumber",
					"Number of periods", 2));
			newRule(new BooleanRule(false, "Clock", Clock.ID_PERIOD,       "Direction",
					"Which way should this clock count?", true, "Count Down", "Count Up"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_PERIOD,       "MinimumTime",
					"Don't change unless you know what you are doing", "0:00"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_PERIOD,       "MaximumTime",
					"Duration of a period.", "30:00"));

			newRule( new StringRule(false, "Clock", Clock.ID_JAM,          "Name",
					"The name displayed on the operator screen", Clock.ID_JAM));
			newRule(new IntegerRule(false, "Clock", Clock.ID_JAM,          "MinimumNumber",
					"Don't change unless you know what you are doing", 0));
			newRule(new IntegerRule(false, "Clock", Clock.ID_JAM,          "MaximumNumber",
					"Must be larger than the number of Jams", 999));
			newRule(new BooleanRule(false, "Clock", Clock.ID_JAM,          "Direction",
					"Which way should this clock count?", true, "Count Down", "Count Up"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_JAM,          "MinimumTime",
					"Don't change unless you know what you are doing", "0:00"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_JAM,          "MaximumTime",
					"Maximum duration of a Jam.", "2:00"));

			newRule( new StringRule(false, "Clock", Clock.ID_LINEUP,       "Name",
					"The name displayed on the operator screen", Clock.ID_LINEUP));
			newRule(new IntegerRule(false, "Clock", Clock.ID_LINEUP,       "MinimumNumber",
					"Lineup number is not currently used for anything", 0));
			newRule(new IntegerRule(false, "Clock", Clock.ID_LINEUP,       "MaximumNumber",
					"Lineup number is not currently used for anything", 999));
			newRule(new BooleanRule(false, "Clock", Clock.ID_LINEUP,       "Direction",
					"Which way should this clock count?", false, "Count Down", "Count Up"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_LINEUP,       "MinimumTime",
					"Don't change unless you know what you are doing", "0:00"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_LINEUP,       "MaximumTime",
					"Must be length of lineup if counting down (will be automatically adjusted for overtime, if necessary). Must be larger than lineup time + buffer time if auto start is used", "60:00"));

			newRule( new StringRule(false, "Clock", Clock.ID_TIMEOUT,      "Name",
					"The name displayed on the operator screen", Clock.ID_TIMEOUT));
			newRule(new IntegerRule(false, "Clock", Clock.ID_TIMEOUT,      "MinimumNumber",
					"Timeout number is not currently used for anything", 0));
			newRule(new IntegerRule(false, "Clock", Clock.ID_TIMEOUT,      "MaximumNumber",
					"Timeout number is not currently used for anything", 999));
			newRule(new BooleanRule(false, "Clock", Clock.ID_TIMEOUT,      "Direction",
					"Which way should this clock count?", false, "Count Down", "Count Up"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_TIMEOUT,      "MinimumTime",
					"Don't change unless you know what you are doing", "0:00"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_TIMEOUT,      "MaximumTime",
					"Timeout clock will stop after this time, possibly causing unexpected side effects", "60:00"));

			newRule( new StringRule(false, "Clock", Clock.ID_INTERMISSION, "Name",
					"The name displayed on the operator screen", Clock.ID_INTERMISSION));
			newRule(new IntegerRule(false, "Clock", Clock.ID_INTERMISSION, "MinimumNumber",
					"Don't change unless you know what you are doing", 0));
			newRule(new IntegerRule(false, "Clock", Clock.ID_INTERMISSION, "MaximumNumber",
					"Should be equal to the number of periods set above.", 2));
			newRule(new BooleanRule(false, "Clock", Clock.ID_INTERMISSION, "Direction",     "", true, "Count Down", "Count Up"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_INTERMISSION, "MinimumTime",
					"Don't change unless you know what you are doing", "0:00"));
			newRule(   new TimeRule(false, "Clock", Clock.ID_INTERMISSION, "MaximumTime",
					"Value will be ignored. Set intermission duration under ScoreBoard - Intermission above.", "60:00"));

			newRule( new StringRule( true, "Team", "1", "Name",
					"Team name to display on scoreboard reset", "Team 1"));
			newRule( new StringRule( true, "Team", "2", "Name",
					"Team name to display on scoreboard reset", "Team 2"));

			base = new Ruleset();
			base.name = "WFTDA Sanctioned";
			base.parent = null;
			for (Rule r : rule_definitions.values()) {
				base.setRule(r.getFullName(), r.getDefaultValue());
			}

			base.immutable = true;
			addRuleset(base);

			File file = new File(ScoreBoardManager.getDefaultPath(), "rules");
			file.mkdirs();
			for (File child : file.listFiles()) {
				if (child.getName().endsWith(".json")) {
					String childName = child.getName().replace(".json", "");
					load(childName);
				}
			}

			return base;
		}
	}

	public static Ruleset DefaultRuleset() {
		synchronized (baseLock) {
			if (base == null)
				return initialize();
			return base;
		}
	}

	public static void newRule(Rule rule) {
		rule_definitions.put(rule.getFullName(), rule);
	}

	public void apply(boolean isReset) {
		apply(isReset, null);
	}
	public void apply(boolean isReset, RulesetReceiver target) {
		for (Rule rule : rule_definitions.values()) {
			if (isReset || !rule.isResetOnly()) {
				Object value = getRule(rule, true);
				List<RulesetReceiver> receivers = rule_receivers.get(rule);
				if (receivers != null) {
					for (RulesetReceiver rsr : receivers) {
						if (target == null || rsr == target)
							rsr.applyRule(rule.getFullName(), value);
					}
				}
			}
		}
	}

	public Object getRule(Rule rule, boolean allowInherit) {
		return getRule(rule.getFullName(), allowInherit);
	}
	public Object getRule(String rule, boolean allowInherit) {
		if (rules.containsKey(rule))
			return rules.get(rule);
		if (allowInherit && parent != null)
			return parent.getRule(rule, allowInherit);
		return null;
	}

	public boolean setRule(String rule, Object value) {
		if (immutable) {
			return false;
		}

		for (Rule r : rule_definitions.values()) {
			if (r.getFullName().equals(rule)) {
				value = r.convertValue(value.toString());
				if (value != null) {
					rules.put(rule, value);
					return true;
				}
				// Invalid value
				return false;
			}
		}

		// Rule not found
		return false;
	}

	public boolean clearRule(String rule) {
		if (immutable) {
			return false;
		}

		if (rules.remove(rule) != null)
			return true;

		// Rule not found
		return false;
	}

	public static void saveAll() {
		for (Ruleset rs : rulesets) {
			rs.save();
		}
	}

	public JSONObject toJSON() throws JSONException {
		return toJSON(true);
	}

	public JSONObject toJSON(boolean includeInheritValues) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("parent", parent == null ? "" : parent.id);
		json.put("immutable", immutable);
		json.put("name", name);
		json.put("id", id);

		JSONObject values = new JSONObject();
		JSONObject inherit_values = new JSONObject();
		for (Rule r : rule_definitions.values()) {
			String rule = r.getFullName();

			Object value = rules.get(rule);
			if (value != null)
				values.put(rule, r.toHumanReadable(value));
			if (parent != null)
				inherit_values.put(rule, r.toHumanReadable(parent.getRule(rule, true)));
		}
		json.put("values", values);
		if (includeInheritValues)
			json.put("inherit_values", inherit_values);
		return json;
	}

	public static Ruleset findRuleset(String idStr) { return findRuleset(idStr, false); }
	public static Ruleset findRuleset(String idStr, boolean defaultToBase) {
		try {
			UUID id = UUID.fromString(idStr);
			for (Ruleset rs : rulesets)
				if (rs.id.equals(id))
					return rs;
		} catch (Exception e) { }
		if (defaultToBase)
			return DefaultRuleset();
		return null;
	}

	private static Ruleset load(String id) {
		Ruleset rs = findRuleset(id);
		if (rs != null)
			return rs;

		FileReader in = null;
		BufferedReader bufferedReader = null;
		try {
			File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), id + ".json");
			if (!file.exists())
				return DefaultRuleset();

			in = new FileReader(file);
			bufferedReader = new BufferedReader(in);

			StringBuffer sb = new StringBuffer();
			String line = null;

			while (null != (line = bufferedReader.readLine())) {
				sb.append(line).append("\n");
			}

			JSONTokener tok = new JSONTokener(sb.toString());
			JSONObject json = new JSONObject(tok);

			String _id = json.optString("id", id);

			rs = new Ruleset();
			if (_id.equals(""))
				rs.id = UUID.randomUUID();
			else
				rs.id = UUID.fromString(_id);
			rs.name = json.optString("name", id);
			rs.immutable = false;

			String parent = json.optString("parent", null);
			if (parent != null && !parent.trim().equals(""))
				rs.parent = load(parent);
			else
				rs.parent = DefaultRuleset();

			JSONObject values = json.getJSONObject("values");
			for (Rule r : rule_definitions.values()) {
				String rule = r.getFullName();
				Object v = values.opt(rule);
				if (v != null) {
					rs.setRule(rule, v);
				}
			}
			rs.immutable = json.optBoolean("immutable", false);

			addRuleset(rs);
			return rs;
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error loading ruleset " + id + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try { bufferedReader.close(); } catch (Exception e) { }
			}
			if (in != null) {
				try { in.close(); } catch (Exception e) { }
			}
		}
		return null;
	}

	private void save() {
		FileWriter out = null;
		try {
			File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), id + ".json");
			file.getParentFile().mkdirs();

			out = new FileWriter(file);
			out.write(toJSON(false).toString(2));
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error saving ruleset " + id + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (out != null) {
				try { out.close(); } catch (Exception e) { }
			}
		}
	}

	public static Ruleset New(String data) {
		try {
			JSONObject json = new JSONObject(data);
			String parentID = json.optString("parent", null);

			Ruleset rs = new Ruleset();
			rs.id = UUID.randomUUID();
			rs.parent = findRuleset(parentID);
			if (rs.parent == null)
				rs.parent = DefaultRuleset();
			rs.name = json.optString("name", rs.id.toString());

			rulesets.add(rs);

			rs.save();
			return rs;
		} catch (JSONException je) {
			ScoreBoardManager.printMessage("Error creating new ruleset: " + je.toString());
			je.printStackTrace();
			return null;
		}
	}

	public static Ruleset Update(String data) {
		try {
			JSONObject json = new JSONObject(data);
			String id = json.optString("id", null);

			if (id == null)
				return null;
			Ruleset rs = findRuleset(id);
			if (rs == null)
				return null;

			String n = json.optString("name", null);
			if (n != null)
				rs.name = n;
			JSONObject values = json.optJSONObject("values");
			if (values != null) {
				for (Rule r : rule_definitions.values()) {
					String rule = r.getFullName();
					Object v = values.opt(rule);
					if (v != null)
						rs.setRule(rule, v);
					else
						rs.clearRule(rule);
				}
			}

			rs.save();
			return rs;
		} catch (JSONException je) {
			ScoreBoardManager.printMessage("Error creating new ruleset: " + je.toString());
			je.printStackTrace();
			return null;
		}
	}

	public static boolean Delete(String data) {
		try {
			JSONObject json = new JSONObject(data);
			String id = json.optString("id", null);

			if (id == null)
				return false;
			Ruleset rs = findRuleset(id);
			if (rs == null)
				return false;

			for (Ruleset rs2 : rulesets) {
				if (rs2.parent == rs) {
					rs2.parent = rs.parent;
					rs2.save();
				}
			}

			rulesets.remove(rs);
			File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), rs.id + ".json");
			file.delete();
			return true;
		} catch (JSONException je) {
			ScoreBoardManager.printMessage("Error creating new ruleset: " + je.toString());
			je.printStackTrace();
			return false;
		}
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public Ruleset getParent() { return parent; }

	private static Map<Rule, List<RulesetReceiver>> rule_receivers = new HashMap<Rule, List<RulesetReceiver>>();
	private static Map<String, Rule> rule_definitions = new LinkedHashMap<String, Rule>();
	private static List<Ruleset> rulesets = new LinkedList<Ruleset>();
	private static Ruleset base = null;
	private static Object baseLock = new Object();

	private Ruleset parent = null;
	private boolean immutable = false;
	private UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private String name = "";
	private Map<String, Object> rules = new HashMap<String, Object>();

	public enum RequestType {
		LIST_DEFINITIONS {
			@Override
			public String toJSON() {
				try {
					JSONArray json = new JSONArray();
					for (Rule r : rule_definitions.values())
						json.put(r.toJSON());
					return json.toString(2);
				} catch (JSONException je) {
					ScoreBoardManager.printMessage("Error creating JSON: " + je.toString());
					je.printStackTrace();
					return "";
				}
			}
		},
		LIST_ALL_RULESETS {
			@Override
			public String toJSON() {
				try {
					JSONArray json = new JSONArray();
					for (Ruleset rs : rulesets)
						json.put(rs.toJSON());
					return json.toString(2);
				} catch (JSONException je) {
					ScoreBoardManager.printMessage("Error creating JSON: " + je.toString());
					je.printStackTrace();
					return "";
				}
			}
		};

		public String toJSON() { return ""; }
	}
}
