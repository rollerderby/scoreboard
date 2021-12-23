package com.carolinarollergirls.scoreboard.core.prepared;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImpl extends ScoreBoardEventProviderImpl<Rulesets> implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.RULESETS);
        addProperties(RULE_DEFINITION, RULESET);
        initialize();
    }
    public RulesetsImpl(RulesetsImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new RulesetsImpl(this, root); }


    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        if (prop == RULESET) {
            return new RulesetImpl(this, "", null, id);
        }
        return null;
    }
    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == RULESET) {
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            Ruleset removed = (Ruleset) item;
            Ruleset grandparent = removed.getParentRuleset();
            for (Ruleset rm : getAll(RULESET)) {
                if (removed.equals(rm.getParentRuleset())) {
                    rm.setParentRuleset(grandparent);
                }
            }
        }
    }

    private void initialize() {
        RulesetImpl root = new RulesetImpl(this, "WFTDA", null, ROOT_ID);
        for (Rule r : Rule.values()) {
            r.getRuleDefinition().setParent(this);
            r.getRuleDefinition().setIndex(r.ordinal());
            add(RULE_DEFINITION, r.getRuleDefinition());
            root.add(Ruleset.RULE, new ValWithId(r.toString(), r.getRuleDefinition().getDefaultValue()));
        }
        root.set(READONLY, true);
        add(RULESET, root);
        addWriteProtection(RULE_DEFINITION);
    }

    @Override
    public RuleDefinition getRuleDefinition(String k) { return get(RULE_DEFINITION, k); }

    @Override
    public Ruleset getRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = get(RULESET, id);
            if (r == null) {
                r = get(RULESET, ROOT_ID);
            }
            return r;
        }
    }
    @Override
    public Ruleset addRuleset(String name, Ruleset parentRs) {
        return addRuleset(name, parentRs, UUID.randomUUID().toString());
    }
    @Override
    public Ruleset addRuleset(String name, Ruleset parentRs, String id) {
        synchronized (coreLock) {
            Ruleset r = new RulesetImpl(this, name, parentRs, id);
            add(RULESET, r);
            return r;
        }
    }
    @Override
    public void removeRuleset(String id) { remove(RULESET, id); }

    public class RulesetImpl extends ScoreBoardEventProviderImpl<Ruleset> implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, Ruleset parent, String id) {
            super(rulesets, id, Rulesets.RULESET);
            addProperties(PARENT, NAME, RULE);
            set(NAME, name);
            set(PARENT, parent);
        }
        public RulesetImpl(RulesetImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

        @Override
        public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new RulesetImpl(this, root); }
        
        @Override
        public String get(Rule r) { return get(RULE, r.toString()).getValue(); }

        @Override
        public String getName() { return get(NAME); }
        @Override
        public void setName(String n) { set(NAME, n); }
        @Override
        public Ruleset getParentRuleset() { return get(PARENT); }
        @Override
        public void setParentRuleset(Ruleset rs) { set(PARENT, rs); }
    }
}
