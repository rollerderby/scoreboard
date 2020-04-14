package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Rulesets extends ScoreBoardEventProvider {
    public void reset();

    public void setCurrentRuleset(String id);
    public void refreshRuleset(String id);

    // Get information from current ruleset.
    public String get(Rule r);
    public boolean getBoolean(Rule r);
    public int getInt(Rule r);
    public long getLong(Rule r);
    public void set(Rule r, String v);

    // The last loaded ruleset.
    public String getCurrentRulesetId();
    public String getCurrentRulesetName();

    public RuleDefinition getRuleDefinition(String id);

    public Ruleset getRuleset(String id);
    public void removeRuleset(String id);
    public Ruleset addRuleset(String name, String parentId);
    public Ruleset addRuleset(String name, String parentId, String id);

    PermanentProperty<Ruleset> CURRENT_RULESET = new PermanentProperty<>(Ruleset.class, "CurrentRuleset", null);
    PermanentProperty<String> CURRENT_RULESET_ID = new PermanentProperty<>(String.class, "CurrentRulesetId", "");
    PermanentProperty<String> CURRENT_RULESET_NAME = new PermanentProperty<>(String.class, "CurrentRulesetName", "");

    AddRemoveProperty<ValWithId> CURRENT_RULE = new AddRemoveProperty<>(ValWithId.class, "CurrentRule");
    AddRemoveProperty<RuleDefinition> RULE_DEFINITION = new AddRemoveProperty<>(RuleDefinition.class, "RuleDefinition");
    AddRemoveProperty<Ruleset> RULESET = new AddRemoveProperty<>(Ruleset.class, "Ruleset");

    public static interface Ruleset extends ScoreBoardEventProvider {
        public String get(Rule k);

        public String getName();
        public void setName(String n);
        public String getParentRulesetId();
        public void setParentRulesetId(String id);

        PermanentProperty<String> PARENT_ID = new PermanentProperty<>(String.class, "ParentId", "");
        PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");

        AddRemoveProperty<ValWithId> RULE = new AddRemoveProperty<>(ValWithId.class, "Rule");
    }
}
