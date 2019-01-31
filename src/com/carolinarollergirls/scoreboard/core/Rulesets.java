package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Collection;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;

public interface Rulesets extends ScoreBoardEventProvider {
    public void reset();

    public void setCurrentRuleset(String id);

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

    public enum Value implements PermanentProperty {
	CURRENT_RULESET_ID,
	CURRENT_RULESET_NAME;
    }
    public enum Child implements AddRemoveProperty {
	CURRENT_RULE,
	RULE_DEFINITION,
	RULESET;
    }

    public static interface Ruleset extends ScoreBoardEventProvider {
        public String get(Rule k);

        public String getName();
        public void setName(String n);
        public String getParentRulesetId();
        public void setParentRulesetId(String id);

        // A missing entry means no override for that rule.
        public void setAll(Collection<ValueWithId> s);

        public enum Value implements PermanentProperty {
            ID,
            PARENT_ID,
            NAME;
        }
        public enum Child implements AddRemoveProperty {
            RULE;
        }
    }
}
