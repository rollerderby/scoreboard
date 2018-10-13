package com.carolinarollergirls.scoreboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.rules.BooleanRule;
import com.carolinarollergirls.scoreboard.rules.IntegerRule;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.StringRule;
import com.carolinarollergirls.scoreboard.rules.TimeRule;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Skater.Penalty;
import com.carolinarollergirls.scoreboard.view.Team;

public class Ruleset {
    public interface RulesetReceiver {
        public void applyRule(String rule, Object value);
    }

    public static boolean registerRule(RulesetReceiver rsr, String rule) {
        Rule r = rule_definitions.get(rule);
        if (r == null) {
            return false;
        }
        List<RulesetReceiver> receivers = rule_receivers.get(r);
        if (receivers == null) {
            receivers = new LinkedList<RulesetReceiver>();
            rule_receivers.put(r, receivers);
        }
        if (!receivers.contains(rsr)) {
            receivers.add(rsr);
        }
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

            newRule(new IntegerRule(false, ScoreBoard.SETTING_NUMBER_PERIODS, "Number of periods", 2));
            newRule(   new TimeRule(false, ScoreBoard.SETTING_PERIOD_DURATION, "Duration of a period", "30:00"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_PERIOD_DIRECTION, "Which way should the period clock count?", true, "Count Down", "Count Up"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_PERIOD_END_BETWEEN_JAMS, "When can a period end?", true, "Anytime outside a jam", "Only on jam end"));

            newRule(new BooleanRule(false, ScoreBoard.SETTING_JAM_NUMBER_PER_PERIOD,   "How to handle Jam Numbers", true, "Reset each period", "Continue counting"));
            newRule(   new TimeRule(false, ScoreBoard.SETTING_JAM_DURATION, "Maximum duration of a jam", "2:00"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_JAM_DIRECTION, "Which way should the jam clock count?", true, "Count Down", "Count Up"));

            newRule(   new TimeRule(false, ScoreBoard.SETTING_LINEUP_DURATION, "Duration of lineup", "0:30"));
            newRule(   new TimeRule(false, ScoreBoard.SETTING_OVERTIME_LINEUP_DURATION, "Duration of lineup before an overtime jam", "1:00"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_LINEUP_DIRECTION, "Which way should the lineup clock count?", false, "Count Down", "Count Up"));

            newRule(   new TimeRule(false, ScoreBoard.SETTING_TTO_DURATION, "Duration of a team timeout", "1:00"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_TIMEOUT_DIRECTION, "Which way should the timeout clock count?", false, "Count Down", "Count Up"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_STOP_PC_ON_TO, "Stop the period clock on every timeout? If false, the options below control the behaviour per type of timeout.", true, "True", "False"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_STOP_PC_ON_OTO, "Stop the period clock on official timeouts?", false, "True", "False"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_STOP_PC_ON_TTO, "Stop the period clock on team timeouts?", false, "True", "False"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_STOP_PC_ON_OR, "Stop the period clock on official reviews?", false, "True", "False"));
            newRule(   new TimeRule(false, ScoreBoard.SETTING_STOP_PC_AFTER_TO_DURATION, "Stop the period clock, if a timeout lasts longer than this time. Set to a high value to disable.", "60:00"));

            newRule( new StringRule(false, ScoreBoard.SETTING_INTERMISSION_DURATIONS, "List of the duration of intermissions as they appear in the game, separated by commas.", "15:00,60:00"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_INTERMISSION_DIRECTION, "Which way should the intermission clock count?", true, "Count Down", "Count Up"));

            newRule(new BooleanRule(false, ScoreBoard.SETTING_AUTO_START,   "Start a Jam or Timeout when the Linup time is over its maximum by BufferTime start a Jam or Timeout as defined below. Jam/Timeout/Period Clocks will be adjusted by the buffer time. This only works if the lineup clock is counting up.", false, "Enabled", "Disabled"));
            newRule(   new TimeRule(false, ScoreBoard.SETTING_AUTO_START_BUFFER,   "How long to wait after end of lineup before auto start is triggered.", "0:02"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_AUTO_START_JAM,   "What to start after lineup is up", false, "Jam", "Timeout"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_AUTO_END_JAM,   "End a jam, when the jam clock has run down", true, "Enabled", "Disabled"));
            newRule(new BooleanRule(false, ScoreBoard.SETTING_AUTO_END_TTO,   "End a team timeout, after it's defined duration has elapsed", false, "Enabled", "Disabled"));

            newRule(new IntegerRule(false, Team.SETTING_NUMBER_TIMEOUTS, "How many timeouts each team is granted per game or period", 3));
            newRule(new BooleanRule(false, Team.SETTING_TIMEOUTS_PER_PERIOD, "Are timeouts granted per period or per game?", false, "Period", "Game"));
            newRule(new IntegerRule(false, Team.SETTING_NUMBER_REVIEWS, "How many official reviews each team is granted per game or period", 1));
            newRule(new BooleanRule(false, Team.SETTING_REVIEWS_PER_PERIOD, "Are official reviews granted per period or per game?", true, "Period", "Game"));

            newRule( new StringRule(false, PenaltyCodesManager.SETTING_PENALTIES_FILE, "File that contains the penalty code definitions to be used", "/config/penalties/wftda2018.json"));
            newRule(new IntegerRule(false, Penalty.SETTING_FO_LIMIT, "After how many penalties a skater has fouled out of the game. Note that the software currently does not suppert more than 9 penalties per skater.", 7));

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
            if (base == null) {
                return initialize();
            }
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
                        if (target == null || rsr == target) {
                            rsr.applyRule(rule.getFullName(), value);
                        }
                    }
                }
            }
        }
    }

    public Object getRule(Rule rule, boolean allowInherit) {
        return getRule(rule.getFullName(), allowInherit);
    }
    public Object getRule(String rule, boolean allowInherit) {
        if (rules.containsKey(rule)) {
            return rules.get(rule);
        }
        if (allowInherit && parent != null) {
            return parent.getRule(rule, allowInherit);
        }
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

        if (rules.remove(rule) != null) {
            return true;
        }

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
            if (value != null) {
                values.put(rule, r.toHumanReadable(value));
            }
            if (parent != null) {
                inherit_values.put(rule, r.toHumanReadable(parent.getRule(rule, true)));
            }
        }
        json.put("values", values);
        if (includeInheritValues) {
            json.put("inherit_values", inherit_values);
        }
        return json;
    }

    public static Ruleset findRuleset(String idStr) { return findRuleset(idStr, false); }
    public static Ruleset findRuleset(String idStr, boolean defaultToBase) {
        try {
            UUID id = UUID.fromString(idStr);
            for (Ruleset rs : rulesets)
                if (rs.id.equals(id)) {
                    return rs;
                }
        } catch (Exception e) { }
        if (defaultToBase) {
            return DefaultRuleset();
        }
        return null;
    }

    private static Ruleset load(String id) {
        Ruleset rs = findRuleset(id);
        if (rs != null) {
            return rs;
        }

        FileReader in = null;
        BufferedReader bufferedReader = null;
        try {
            File file = new File(new File(ScoreBoardManager.getDefaultPath(), "rules"), id + ".json");
            if (!file.exists()) {
                return DefaultRuleset();
            }

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
            if (_id.equals("")) {
                rs.id = UUID.randomUUID();
            } else {
                rs.id = UUID.fromString(_id);
            }
            rs.name = json.optString("name", id);
            rs.immutable = false;

            String parent = json.optString("parent", null);
            if (parent != null && !parent.trim().equals("")) {
                rs.parent = load(parent);
            } else {
                rs.parent = DefaultRuleset();
            }

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
            if (rs.parent == null) {
                rs.parent = DefaultRuleset();
            }
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

            if (id == null) {
                return null;
            }
            Ruleset rs = findRuleset(id);
            if (rs == null) {
                return null;
            }

            String n = json.optString("name", null);
            if (n != null) {
                rs.name = n;
            }
            JSONObject values = json.optJSONObject("values");
            if (values != null) {
                for (Rule r : rule_definitions.values()) {
                    String rule = r.getFullName();
                    Object v = values.opt(rule);
                    if (v != null) {
                        rs.setRule(rule, v);
                    } else {
                        rs.clearRule(rule);
                    }
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

            if (id == null) {
                return false;
            }
            Ruleset rs = findRuleset(id);
            if (rs == null) {
                return false;
            }

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
                    for (Rule r : rule_definitions.values()) {
                        json.put(r.toJSON());
                    }
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
                    for (Ruleset rs : rulesets) {
                        json.put(rs.toJSON());
                    }
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
