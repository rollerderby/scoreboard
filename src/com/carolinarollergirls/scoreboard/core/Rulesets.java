package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.rules.Rule;

public interface Rulesets extends ScoreBoardEventProvider {
    public void reset();

    public void setCurrentRuleset(String id);

    // Get information from current ruleset.
    public Map<String, String> getAll();
    public String get(String k);
    public boolean getBoolean(String k);
    public int getInt(String k);
    public long getLong(String k);
    public void set(String k, String v);

    // The last loaded ruleset.
    public String getId();
    public void setId(String id);
    public String getName();
    public void setName(String n);

    public Map<String, Rule> getRules();

    public Map<String, Ruleset> getRulesets();
    public Ruleset getRuleset(String id);
    public void removeRuleset(String id);
    public Ruleset addRuleset(String name, String parentId);
    public Ruleset addRuleset(String name, String parentId, String id);


    public static interface Ruleset extends ScoreBoardEventProvider {
        public Map<String, String> getAll();
        public String get(String k);

        public String getId();
        public String getName();
        public void setName(String n);
        public String getParentRulesetId();
        public void setParentRulesetId(String id);

        // A missing entry means no override for that rule.
        public void setAll(Map<String, String> s);
    }

    public ScoreBoardEventProvider getParent();

    public static final String EVENT_RULE_DEFINITIONS = "RuleDefinitions";
    public static final String EVENT_CURRENT_RULES = "Rule";
    public static final String EVENT_CURRENT_RULESET = "Ruleset";
    public static final String EVENT_RULESET = "KnownRulesets";
    public static final String EVENT_REMOVE_RULESET = "RemoveKnownRuleset";
}
