package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Rulesets extends ScoreBoardEventProvider {
    public RuleDefinition getRuleDefinition(String id);

    public Ruleset getRuleset(String id);
    public void removeRuleset(String id);
    public Ruleset addRuleset(String name, Ruleset parent);
    public Ruleset addRuleset(String name, Ruleset parent, String id);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Child<RuleDefinition> RULE_DEFINITION =
        new Child<>(RuleDefinition.class, "RuleDefinition", props);
    public static final Child<Ruleset> RULESET = new Child<>(Ruleset.class, "Ruleset", props);

    public static final String ROOT_ID = "WFTDARuleset";

    public static interface Ruleset extends ScoreBoardEventProvider {
        public String get(Rule k);

        public String getName();
        public void setName(String n);
        public Ruleset getParentRuleset();
        public void setParentRuleset(Ruleset rs);
        public boolean isAncestorOf(Ruleset rs);
        public void setRule(String id, String value);

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<Ruleset> PARENT = new Value<>(Ruleset.class, "Parent", null, props);
        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);

        public static final Child<ValWithId> RULE = new Child<>(ValWithId.class, "Rule", props);
    }
}
