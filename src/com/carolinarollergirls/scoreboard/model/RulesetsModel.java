package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.view.Rulesets;

public interface RulesetsModel extends Rulesets {
    public void reset();

    public void setCurrentRuleset(String id);
    public void set(String k, String v);

    // These do not change the ruleset, just the internal values.
    // Used when loading a save.
    public void setId(String id);
    public void setName(String n);

    public void removeRuleset(String id);
    public RulesetModel getRulesetModel(String id);
    public RulesetModel addRuleset(String name, String parentId);
    public RulesetModel addRuleset(String name, String parentId, String id);

    public static interface RulesetModel extends Rulesets.Ruleset {
        public void setName(String n);
        public void setParentRulesetId(String id);

        // A missing entry means no override for that rule.
        public void setAll(Map<String, String> s);
    }
}
