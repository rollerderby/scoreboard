package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.RulesetsModel;
import com.carolinarollergirls.scoreboard.rules.BooleanRule;
import com.carolinarollergirls.scoreboard.rules.IntegerRule;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.StringRule;
import com.carolinarollergirls.scoreboard.rules.TimeRule;
import com.carolinarollergirls.scoreboard.view.Rulesets;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Skater.Penalty;
import com.carolinarollergirls.scoreboard.view.Team;

public class DefaultRulesetsModel extends DefaultScoreBoardEventProvider implements RulesetsModel {
    public DefaultRulesetsModel(ScoreBoardModel s) {
        sbm = s;
        parent = s;
        initialize();
        reset();
    }

    public String getProviderName() { return "Rulesets"; }
    public Class<Rulesets> getProviderClass() { return Rulesets.class; }
    public String getProviderId() { return ""; }

    public ScoreBoardEventProvider getParent() { return parent; }

    private void initialize() {
        Rule[] knownRules = {
            new IntegerRule(ScoreBoard.SETTING_NUMBER_PERIODS, "Number of periods", 2),
            new TimeRule(ScoreBoard.SETTING_PERIOD_DURATION, "Duration of a period", "30:00"),
            new BooleanRule(ScoreBoard.SETTING_PERIOD_DIRECTION, "Which way should the period clock count?", true, "Count Down", "Count Up"),
            new BooleanRule(ScoreBoard.SETTING_PERIOD_END_BETWEEN_JAMS, "When can a period end?", true, "Anytime outside a jam", "Only on jam end"),

            new BooleanRule(ScoreBoard.SETTING_JAM_NUMBER_PER_PERIOD, "How to handle Jam Numbers", true, "Reset each period", "Continue counting"),
            new TimeRule(ScoreBoard.SETTING_JAM_DURATION, "Maximum duration of a jam", "2:00"),
            new BooleanRule(ScoreBoard.SETTING_JAM_DIRECTION, "Which way should the jam clock count?", true, "Count Down", "Count Up"),

            new TimeRule(ScoreBoard.SETTING_LINEUP_DURATION, "Duration of lineup", "0:30"),
            new TimeRule(ScoreBoard.SETTING_OVERTIME_LINEUP_DURATION, "Duration of lineup before an overtime jam", "1:00"),
            new BooleanRule(ScoreBoard.SETTING_LINEUP_DIRECTION, "Which way should the lineup clock count?", false, "Count Down", "Count Up"),

            new TimeRule(ScoreBoard.SETTING_TTO_DURATION, "Duration of a team timeout", "1:00"),
            new BooleanRule(ScoreBoard.SETTING_TIMEOUT_DIRECTION, "Which way should the timeout clock count?", false, "Count Down", "Count Up"),
            new BooleanRule(ScoreBoard.SETTING_STOP_PC_ON_TO, "Stop the period clock on every timeout? If false, the options below control the behaviour per type of timeout.", true, "True", "False"),
            new BooleanRule(ScoreBoard.SETTING_STOP_PC_ON_OTO, "Stop the period clock on official timeouts?", false, "True", "False"),
            new BooleanRule(ScoreBoard.SETTING_STOP_PC_ON_TTO, "Stop the period clock on team timeouts?", false, "True", "False"),
            new BooleanRule(ScoreBoard.SETTING_STOP_PC_ON_OR, "Stop the period clock on official reviews?", false, "True", "False"),
            new TimeRule(ScoreBoard.SETTING_STOP_PC_AFTER_TO_DURATION, "Stop the period clock, if a timeout lasts longer than this time. Set to a high value to disable.", "60:00"),

            new StringRule(ScoreBoard.SETTING_INTERMISSION_DURATIONS, "List of the duration of intermissions as they appear in the game, separated by commas.", "15:00,60:00"),
            new BooleanRule(ScoreBoard.SETTING_INTERMISSION_DIRECTION, "Which way should the intermission clock count?", true, "Count Down", "Count Up"),

            new BooleanRule(ScoreBoard.SETTING_AUTO_START, "Start a Jam or Timeout when the Linup time is over its maximum by BufferTime start a Jam or Timeout as defined below. Jam/Timeout/Period Clocks will be adjusted by the buffer time. This only works if the lineup clock is counting up.", false, "Enabled", "Disabled"),
            new TimeRule(ScoreBoard.SETTING_AUTO_START_BUFFER, "How long to wait after end of lineup before auto start is triggered.", "0:02"),
            new BooleanRule(ScoreBoard.SETTING_AUTO_START_JAM, "What to start after lineup is up", false, "Jam", "Timeout"),
            new BooleanRule(ScoreBoard.SETTING_AUTO_END_JAM, "End a jam, when the jam clock has run down", true, "Enabled", "Disabled"),
            new BooleanRule(ScoreBoard.SETTING_AUTO_END_TTO, "End a team timeout, after it's defined duration has elapsed", false, "Enabled", "Disabled"),

            new IntegerRule(Team.SETTING_NUMBER_TIMEOUTS, "How many timeouts each team is granted per game or period", 3),
            new BooleanRule(Team.SETTING_TIMEOUTS_PER_PERIOD, "Are timeouts granted per period or per game?", false, "Period", "Game"),
            new IntegerRule(Team.SETTING_NUMBER_REVIEWS, "How many official reviews each team is granted per game or period", 1),
            new BooleanRule(Team.SETTING_REVIEWS_PER_PERIOD, "Are official reviews granted per period or per game?", true, "Period", "Game"),

            new StringRule(PenaltyCodesManager.SETTING_PENALTIES_FILE, "File that contains the penalty code definitions to be used", "/config/penalties/wftda2018.json"),
            new IntegerRule(Penalty.SETTING_FO_LIMIT, "After how many penalties a skater has fouled out of the game. Note that the software currently does not support more than 9 penalties per skater.", 7),
        };
        Map<String, String> rootSettings = new HashMap<String, String>();
        for (Rule r : knownRules) {
            rules.put(r.getFullName(), r);
            rootSettings.put(r.getFullName(), r.getDefaultValue());
        }
        // The root ruleset is always created from the above list,
        // so that as rules are added over time there will always
        // be a value for them.
        RulesetModel root = addRuleset("WFTDA Sanctioned", "", rootId);
        root.setAll(rootSettings);
    }

    public void reset() {
        synchronized (coreLock) {
            setCurrentRuleset(rootId);
        }
    }

    public String getId() {
        synchronized (coreLock) {
            return id;
        }
    }
    public void setId(String i) {
        synchronized (coreLock) {
            id = i;
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_CURRENT_RULESET, "", ""));
        }
    }
    public String getName() {
        synchronized (coreLock) {
            return name;
        }
    }
    public void setName(String n) {
        synchronized (coreLock) {
            name = n;
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_CURRENT_RULESET, "", ""));
        }
    }
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            current.clear();
            setCurrentRulesetRecurse(id);
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_CURRENT_RULESET, "", ""));
        }
    }

    private void setCurrentRulesetRecurse(String i) {
        Ruleset rs = getRuleset(i);
        if (!rs.getId().equals(rootId)) {
            setCurrentRulesetRecurse(rs.getParentRulesetId());
        }
        current.putAll(rs.getAll());
        name = rs.getName();
        id = rs.getId();
    }

    public Map<String, String> getAll() {
        synchronized (coreLock) {
            return Collections.unmodifiableMap(current);
        }
    }
    public String get(String k) {
        synchronized (coreLock) {
            return current.get(k);
        }
    }
    public boolean getBoolean(String k) {
        synchronized (coreLock) {
            return Boolean.parseBoolean(get(k));
        }
    }
    public int getInt(String k) {
        synchronized (coreLock) {
            return Integer.parseInt(get(k));
        }
    }
    public long getLong(String k) {
        synchronized (coreLock) {
            return Long.parseLong(get(k));
        }
    }
    public void set(String k, String v) {
        synchronized (coreLock) {
            Rule r = rules.get(k);
            if (r == null || !r.isValueValid(v)) {
                return;
            }
            current.put(k, v);
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_CURRENT_RULESET, "", ""));
        }
    }

    public Map<String, Rule> getRules() {
        synchronized (coreLock) {
            return Collections.unmodifiableMap(rules);
        }
    }

    public Ruleset getRuleset(String id) {
        return getRulesetModel(id);
    }
    public RulesetModel getRulesetModel(String id) {
        synchronized (coreLock) {
            RulesetModel r = rulesets.get(id);
            if (r == null) {
                r = rulesets.get(rootId);
            }
            return r;
        }
    }
    public Map<String, Ruleset> getRulesets() {
        synchronized (coreLock) {
            return Collections.unmodifiableMap(new HashMap<String, Ruleset>(rulesets));
        }
    }
    public RulesetModel addRuleset(String name, String parentId) {
        return addRuleset(name, parentId, UUID.randomUUID().toString());
    }
    public RulesetModel addRuleset(String name, String parentId, String id) {
        synchronized (coreLock) {
            RulesetModel r = new DefaultRulesetModel(name, parentId, id);
            rulesets.put(id, r);
            r.addScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(r, EVENT_RULESET, r, null));
            return r;
        }
    }
    public void removeRuleset(String id) {
        synchronized (coreLock) {
            if (id.equals(rootId)) {
                return;
            }
            Ruleset r = rulesets.get(id);
            if (r == null) {
                return;
            }
            requestBatchStart();
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            String parent = r.getParentRulesetId();
            for (RulesetModel rm : rulesets.values()) {
                if (id.equals(rm.getParentRulesetId())) {
                    rm.setParentRulesetId(parent);
                }
            }

            rulesets.remove(id);
            r.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_RULESET, r, ""));
            requestBatchEnd();
        }
    }

    private ScoreBoardModel sbm = null;
    private Map<String, String> current = new HashMap<String, String>();
    private Map<String, RulesetModel> rulesets = new HashMap<String, RulesetModel>();
    private String id = null;
    private String name = null;
    // Ordering is preserved for the UI.
    private Map<String, Rule> rules = new LinkedHashMap<String, Rule>();
    private ScoreBoardEventProvider parent = null;
    private static Object coreLock = DefaultScoreBoardModel.getCoreLock();
    public static final String rootId = "00000000-0000-0000-0000-000000000000";

    public class DefaultRulesetModel extends DefaultScoreBoardEventProvider implements RulesetModel {
        private DefaultRulesetModel(String name, String parentId, String id) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
        }

        public Map<String, String> getAll() {
            synchronized (coreLock) {
                return Collections.unmodifiableMap(settings);
            }
        }
        public String get(String k) {
            synchronized (coreLock) {
                return settings.get(k);
            }
        }

        public String getId() {
            synchronized (coreLock) {
                return id;
            }
        }
        public String getName() {
            synchronized (coreLock) {
                return name;
            }
        }
        public String getParentRulesetId() {
            synchronized (coreLock) {
                return parentId;
            }
        }
        public void setName(String n) {
            synchronized (coreLock) {
                if (id.equals(rootId)) {
                    return;
                }
                name = n;
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_RULESET, this, null));
            }
        }
        public void setParentRulesetId(String pi) {
            synchronized (coreLock) {
                if (id.equals(rootId)) {
                    return;
                }
                parentId = pi;
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_RULESET, this, null));
            }
        }


        public void setAll(Map<String, String> s) {
            synchronized (coreLock) {
                if (id.equals(rootId) && !settings.isEmpty()) {
                    return;  // Don't allow changing root after initial setup.
                }
                Set<String> oldKeys = settings.keySet();
                // Check all values are valid.
                for (Iterator<String> it = s.keySet().iterator(); it.hasNext();) {
                    String k = it.next();
                    Rule r = rules.get(k);
                    if (r == null || !r.isValueValid(s.get(k))) {
                        it.remove();
                        oldKeys.add(k);  // Allow the XML to remove this.
                    }
                }
                settings = s;
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_RULESET, this, oldKeys));
            }
        }

        public String getProviderName() { return "Ruleset"; }
        public Class<Ruleset> getProviderClass() { return Ruleset.class; }
        public String getProviderId() { return getId(); }

        private String id;
        private String name;
        private String parentId;
        private Map<String, String> settings = new HashMap<String, String>();
    }
}
