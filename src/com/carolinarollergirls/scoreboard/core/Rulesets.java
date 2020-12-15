package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Rulesets extends ScoreBoardEventProvider {
    public void reset();

    public void setCurrentRuleset(String id);
    // if rs is the current ruleset or an ancestor of it, refresh the current rules
    public void refreshRuleset(Ruleset rs);

    // Get information from current ruleset.
    public String get(Rule r);
    public boolean getBoolean(Rule r);
    public int getInt(Rule r);
    public long getLong(Rule r);
    public void set(Rule r, String v);

    // The last loaded ruleset.
    public Ruleset getCurrentRuleset();
    public String getCurrentRulesetName();

    public RuleDefinition getRuleDefinition(String id);

    public Ruleset getRuleset(String id);
    public void removeRuleset(String id);
    public Ruleset addRuleset(String name, Ruleset parent);
    public Ruleset addRuleset(String name, Ruleset parent, String id);

    Value<Ruleset> CURRENT_RULESET = new Value<>(Ruleset.class, "CurrentRuleset", null);
    Value<String> CURRENT_RULESET_NAME = new Value<>(String.class, "CurrentRulesetName", "");

    Child<ValWithId> CURRENT_RULE = new Child<>(ValWithId.class, "CurrentRule");
    Child<RuleDefinition> RULE_DEFINITION = new Child<>(RuleDefinition.class, "RuleDefinition");
    Child<Ruleset> RULESET = new Child<>(Ruleset.class, "Ruleset");

    public static interface Ruleset extends ScoreBoardEventProvider {
        public String get(Rule k);

        public String getName();
        public void setName(String n);
        public Ruleset getParentRuleset();
        public void setParentRuleset(Ruleset rs);

        Value<Ruleset> PARENT = new Value<>(Ruleset.class, "Parent", null);
        Value<String> NAME = new Value<>(String.class, "Name", "");

        Child<ValWithId> RULE = new Child<>(ValWithId.class, "Rule");
    }
}
