package com.carolinarollergirls.scoreboard;

import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.carolinarollergirls.scoreboard.rules.*;

public class RuleSet {
	public interface RuleSetReceiver {
		public void applyRule(String rule, Object value);
	}

	public static void registerRule(RuleSetReceiver rsr, String rule) {
		rule_receivers.put(rule, rsr);
	}

	private static void addRuleSet(RuleSet rs) {
		rule_sets.add(rs);
	}

	private static RuleSet initialize() {
		newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "PreGame",       "", "Time To Derby"));
		newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Intermission",  "", "Intermission"));
		newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Unofficial",    "", "Unofficial Score"));
		newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Official",      "", "Final Score"));
		newRule(   new TimeRule("Clock", Clock.ID_INTERMISSION, "Time",          "", "15:00"));

		newRule( new StringRule("Clock", Clock.ID_PERIOD,       "Name",          "", Clock.ID_PERIOD));
		newRule(new IntegerRule("Clock", Clock.ID_PERIOD,       "MinimumNumber", "", 1));
		newRule(new IntegerRule("Clock", Clock.ID_PERIOD,       "MaximumNumber", "", 2));
		newRule(new BooleanRule("Clock", Clock.ID_PERIOD,       "Direction",     "", true, "down", "up"));
		newRule(   new TimeRule("Clock", Clock.ID_PERIOD,       "MinimumTime",   "", "0:00"));
		newRule(   new TimeRule("Clock", Clock.ID_PERIOD,       "MaximumTime",   "", "30:00"));

		newRule( new StringRule("Clock", Clock.ID_JAM,          "Name",          "", Clock.ID_JAM));
		newRule(new IntegerRule("Clock", Clock.ID_JAM,          "MinimumNumber", "", 1));
		newRule(new IntegerRule("Clock", Clock.ID_JAM,          "MaximumNumber", "", 2));
		newRule(new BooleanRule("Clock", Clock.ID_JAM,          "Direction",     "", true, "down", "up"));
		newRule(   new TimeRule("Clock", Clock.ID_JAM,          "MinimumTime",   "", "0:00"));
		newRule(   new TimeRule("Clock", Clock.ID_JAM,          "MaximumTime",   "", "2:00"));

		newRule( new StringRule("Clock", Clock.ID_LINEUP,       "Name",          "", Clock.ID_LINEUP));
		newRule(new IntegerRule("Clock", Clock.ID_LINEUP,       "MinimumNumber", "", 1));
		newRule(new IntegerRule("Clock", Clock.ID_LINEUP,       "MaximumNumber", "", 2));
		newRule(new BooleanRule("Clock", Clock.ID_LINEUP,       "Direction",     "", false, "down", "up"));
		newRule(   new TimeRule("Clock", Clock.ID_LINEUP,       "MinimumTime",   "", "0:00"));
		newRule(   new TimeRule("Clock", Clock.ID_LINEUP,       "MaximumTime",   "", "60:00"));

		newRule( new StringRule("Clock", Clock.ID_TIMEOUT,      "Name",          "", Clock.ID_TIMEOUT));
		newRule(new IntegerRule("Clock", Clock.ID_TIMEOUT,      "MinimumNumber", "", 1));
		newRule(new IntegerRule("Clock", Clock.ID_TIMEOUT,      "MaximumNumber", "", 2));
		newRule(new BooleanRule("Clock", Clock.ID_TIMEOUT,      "Direction",     "", false, "down", "up"));
		newRule(   new TimeRule("Clock", Clock.ID_TIMEOUT,      "MinimumTime",   "", "0:00"));
		newRule(   new TimeRule("Clock", Clock.ID_TIMEOUT,      "MaximumTime",   "", "60:00"));

		newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Name",          "", Clock.ID_INTERMISSION));
		newRule(new IntegerRule("Clock", Clock.ID_INTERMISSION, "MinimumNumber", "", 0));
		newRule(new IntegerRule("Clock", Clock.ID_INTERMISSION, "MaximumNumber", "", 2));
		newRule(new BooleanRule("Clock", Clock.ID_INTERMISSION, "Direction",     "", true, "down", "up"));
		newRule(   new TimeRule("Clock", Clock.ID_INTERMISSION, "MinimumTime",   "", "0:00"));
		newRule(   new TimeRule("Clock", Clock.ID_INTERMISSION, "MaximumTime",   "", "60:00"));

		base_ruleset = new RuleSet();
		base_ruleset.name = "WFTDA Santioned";
		base_ruleset.parent = null;
		for (Rule r : rule_definitions.values()) {
			base_ruleset.setRule(r.getFullName(), r.getDefaultValue());
		}

		base_ruleset.immutable = true;
		addRuleSet(base_ruleset);
		saveAll();

		File file = new File(ScoreBoardManager.getDefaultPath(), "rules");
		for (File child : file.listFiles()) {
			if (child.getName().endsWith(".json")) {
				String childName = child.getName().replace(".json", "");
				load(childName);
			}
		}

		return base_ruleset;
	}

	public static void activateRuleSet(String name) {
		RuleSet rs = findRuleSet(name);
		if (rs != null) {
			ScoreBoardManager.printMessage("*** ACTIVATING RULESET " + name);
			active = rs;
			apply();
		}
	}

	public static void newRule(Rule rule) {
		rule_definitions.put(rule.getFullName(), rule);
	}

	public static void apply() {
		if (active != null)
			active._apply(null);
	}
	public static void apply(String base) {
		if (active != null)
			active._apply(base);
	}

	private void _apply() {
		apply(null);
	}
	private void _apply(String base) {
		for (String rule : rule_receivers.keySet()) {
			if (base == null || rule.startsWith(base)) {
				Object value = getRule(rule, true);
				rule_receivers.get(rule).applyRule(rule, value);
			}
		}
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

	public static void saveAll() {
		for (RuleSet rs : rule_sets) {
			rs.save();
		}
	}

	public static JSONArray toJSONDefinitions() throws JSONException {
		JSONArray json = new JSONArray();
		for (Rule r : rule_definitions.values())
			json.put(r.toJSON());

		return json;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("parent", parent == null ? "" : parent.name);
		json.put("immutable", immutable);
		json.put("name", name);

		JSONObject values = new JSONObject();
		for (Rule r : rule_definitions.values()) {
			String rule = r.getFullName();

			Object value = rules.get(rule);
			if (value != null)
				values.put(rule, r.toHumanReadable(value));
		}
		json.put("values", values);
		return json;
	}

	private static RuleSet findRuleSet(String name) {
		for (RuleSet rs : rule_sets)
			if (rs.name.equals(name)) {
				return rs;
			}
		return null;
	}

	private static RuleSet load(String name) {
		RuleSet rs = findRuleSet(name);
		if (rs != null)
			return rs;

		FileReader in = null;
		try {
			File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), name + ".json");
			if (!file.exists())
				return base_ruleset;

			in = new FileReader(file);
			JSONTokener tok = new JSONTokener(in);
			JSONObject json = new JSONObject(tok);

			rs = new RuleSet();
			rs.name = name;
			rs.immutable = false;

			String parent = json.optString("parent", null);
			if (parent != null && !parent.trim().equals(""))
				rs.parent = load(parent);
			else
				rs.parent = base_ruleset;

			JSONObject values = json.getJSONObject("values");
			for (Rule r : rule_definitions.values()) {
				String rule = r.getFullName();
				Object v = values.opt(rule);
				if (v != null) {
					boolean didSet = rs.setRule(rule, v);
				}
			}
			rs.immutable = json.optBoolean("immutable", false);

			addRuleSet(rs);
			return rs;
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error loading ruleset " + name + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception e) { }
			}
		}
		return null;
	}

	private void save() {
		FileWriter out = null;
		try {
			File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), name + ".json");
			file.getParentFile().mkdirs();

			out = new FileWriter(file);
			out.write(toJSON().toString(2));
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error saving ruleset " + name + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (out != null) {
				try { out.close(); } catch (Exception e) { }
			}
		}
	}

	private static Map<String, RuleSetReceiver> rule_receivers = new LinkedHashMap<String, RuleSetReceiver>();
	private static Map<String, Rule> rule_definitions = new LinkedHashMap<String, Rule>();
	private static List<RuleSet> rule_sets = new LinkedList<RuleSet>();
	private static RuleSet active = initialize();
	private static RuleSet base_ruleset = null;

	private RuleSet parent = null;
	private boolean immutable = false;
	private String name = "";
	private Map<String, Object> rules = new HashMap<String, Object>();
}
