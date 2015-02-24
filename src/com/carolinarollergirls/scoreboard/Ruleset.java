package com.carolinarollergirls.scoreboard;

import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

	public static void registerRule(RulesetReceiver rsr, String rule) {
		rule_receivers.put(rule, rsr);
	}

	private static void addRuleset(Ruleset rs) {
		rulesets.add(rs);
	}

	private static Ruleset initialize() {
		synchronized (baseLock) {
			if (base != null) {
				return base;
			}

			newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "PreGame",       "", "Time To Derby"));
			newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Intermission",  "", "Intermission"));
			newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Unofficial",    "", "Unofficial Score"));
			newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Official",      "", "Final Score"));
			newRule(   new TimeRule("Clock", Clock.ID_INTERMISSION, "Time",          "", "15:00"));
	
			newRule( new StringRule("Clock", Clock.ID_PERIOD,       "Name",          "", Clock.ID_PERIOD));
			newRule(new IntegerRule("Clock", Clock.ID_PERIOD,       "MinimumNumber", "", 1));
			newRule(new IntegerRule("Clock", Clock.ID_PERIOD,       "MaximumNumber", "", 2));
			newRule(new BooleanRule("Clock", Clock.ID_PERIOD,       "Direction",     "", true, "Count Down", "Count Up"));
			newRule(   new TimeRule("Clock", Clock.ID_PERIOD,       "MinimumTime",   "", "0:00"));
			newRule(   new TimeRule("Clock", Clock.ID_PERIOD,       "MaximumTime",   "", "30:00"));
	
			newRule( new StringRule("Clock", Clock.ID_JAM,          "Name",          "", Clock.ID_JAM));
			newRule(new IntegerRule("Clock", Clock.ID_JAM,          "MinimumNumber", "", 1));
			newRule(new IntegerRule("Clock", Clock.ID_JAM,          "MaximumNumber", "", 999));
			newRule(new BooleanRule("Clock", Clock.ID_JAM,          "Direction",     "", true, "Count Down", "Count Up"));
			newRule(   new TimeRule("Clock", Clock.ID_JAM,          "MinimumTime",   "", "0:00"));
			newRule(   new TimeRule("Clock", Clock.ID_JAM,          "MaximumTime",   "", "2:00"));
	
			newRule( new StringRule("Clock", Clock.ID_LINEUP,       "Name",          "", Clock.ID_LINEUP));
			newRule(new IntegerRule("Clock", Clock.ID_LINEUP,       "MinimumNumber", "", 1));
			newRule(new IntegerRule("Clock", Clock.ID_LINEUP,       "MaximumNumber", "", 999));
			newRule(new BooleanRule("Clock", Clock.ID_LINEUP,       "Direction",     "", false, "Count Down", "Count Up"));
			newRule(   new TimeRule("Clock", Clock.ID_LINEUP,       "MinimumTime",   "", "0:00"));
			newRule(   new TimeRule("Clock", Clock.ID_LINEUP,       "MaximumTime",   "", "60:00"));
	
			newRule( new StringRule("Clock", Clock.ID_TIMEOUT,      "Name",          "", Clock.ID_TIMEOUT));
			newRule(new IntegerRule("Clock", Clock.ID_TIMEOUT,      "MinimumNumber", "", 1));
			newRule(new IntegerRule("Clock", Clock.ID_TIMEOUT,      "MaximumNumber", "", 999));
			newRule(new BooleanRule("Clock", Clock.ID_TIMEOUT,      "Direction",     "", false, "Count Down", "Count Up"));
			newRule(   new TimeRule("Clock", Clock.ID_TIMEOUT,      "MinimumTime",   "", "0:00"));
			newRule(   new TimeRule("Clock", Clock.ID_TIMEOUT,      "MaximumTime",   "", "60:00"));
	
			newRule( new StringRule("Clock", Clock.ID_INTERMISSION, "Name",          "", Clock.ID_INTERMISSION));
			newRule(new IntegerRule("Clock", Clock.ID_INTERMISSION, "MinimumNumber", "", 0));
			newRule(new IntegerRule("Clock", Clock.ID_INTERMISSION, "MaximumNumber", "", 2));
			newRule(new BooleanRule("Clock", Clock.ID_INTERMISSION, "Direction",     "", true, "Count Down", "Count Up"));
			newRule(   new TimeRule("Clock", Clock.ID_INTERMISSION, "MinimumTime",   "", "0:00"));
			newRule(   new TimeRule("Clock", Clock.ID_INTERMISSION, "MaximumTime",   "", "60:00"));
	
			newRule(new IntegerRule("Team", null, "Timeouts", "", 3));
			newRule(new IntegerRule("Team", null, "OfficialReviews", "", 1));
			newRule(new StringRule("Team", "Team 1", "Name", "", "Team 1"));
			newRule(new StringRule("Team", "Team 2", "Name", "", "Team 2"));
	
			base = new Ruleset();
			base.name = "WFTDA Santioned";
			base.parent = null;
			for (Rule r : rule_definitions.values()) {
				base.setRule(r.getFullName(), r.getDefaultValue());
			}
	
			base.immutable = true;
			addRuleset(base);
			saveAll();
	
			File file = new File(ScoreBoardManager.getDefaultPath(), "rules");
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

	public void apply() {
		apply(null);
	}
	public void apply(String base) {
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
		ScoreBoardManager.printMessage("findRuleset(" + idStr + ", " + defaultToBase + ")  base: " + base);
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
		try {
			File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), id + ".json");
			if (!file.exists())
				return DefaultRuleset();

			in = new FileReader(file);
			JSONTokener tok = new JSONTokener(in);
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
					boolean didSet = rs.setRule(rule, v);
				}
			}
			rs.immutable = json.optBoolean("immutable", false);

			addRuleset(rs);
			return rs;
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error loading ruleset " + id + ": " + e.getMessage());
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
	}

	public static Ruleset Update(String data) {
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
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public Ruleset getParent() { return parent; }

	private static Map<String, RulesetReceiver> rule_receivers = new LinkedHashMap<String, RulesetReceiver>();
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
				JSONArray json = new JSONArray();
				for (Rule r : rule_definitions.values())
					json.put(r.toJSON());
				return json.toString(2);
			}
		},
		LIST_ALL_RULESETS {
			@Override
			public String toJSON() {
				JSONArray json = new JSONArray();
				for (Ruleset rs : rulesets)
					json.put(rs.toJSON());
				return json.toString(2);
			}
		};

		public String toJSON() { return ""; }
	}
}
