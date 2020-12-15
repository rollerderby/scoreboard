package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImpl extends ScoreBoardEventProviderImpl<Rulesets> implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.RULESETS);
        addProperties(CURRENT_RULESET, CURRENT_RULESET_NAME, CURRENT_RULE, RULE_DEFINITION, RULESET);
        initialize();
        reset();
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == CURRENT_RULESET) {
            setCurrentRulesetRecurse(((Ruleset) value));
        }
        return value;
    }

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
        setCopy(CURRENT_RULESET_NAME, this, CURRENT_RULESET, Ruleset.NAME, true);
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
        addWriteProtectionOverride(CURRENT_RULE, Source.ANY_INTERNAL);
    }

    @Override
    public void reset() {
        synchronized (coreLock) {
            setCurrentRuleset(ROOT_ID);
        }
    }

    @Override
    public Ruleset getCurrentRuleset() { return get(CURRENT_RULESET); }
    @Override
    public String getCurrentRulesetName() { return get(CURRENT_RULESET_NAME); }
    @Override
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            set(CURRENT_RULESET, getRuleset(id));
        }
    }
    @Override
    public void refreshRuleset(Ruleset rs) {
        synchronized (coreLock) {
            for (Ruleset tRs = getCurrentRuleset(); !ROOT_ID.equals(tRs.getId()); tRs = tRs.getParentRuleset()) {
                if (tRs.equals(rs)) {
                    setCurrentRulesetRecurse(getCurrentRuleset());
                    scoreBoardChange(
                            new ScoreBoardEvent<>(this, CURRENT_RULESET, getCurrentRuleset(), getCurrentRuleset()));
                    break;
                }
            }
        }
    }

    private void setCurrentRulesetRecurse(Ruleset rs) {
        if (!rs.getId().equals(ROOT_ID)) {
            setCurrentRulesetRecurse(rs.getParentRuleset());
        }
        for (ValWithId r : rs.getAll(Ruleset.RULE)) {
            if (getRuleDefinition(r.getId()).isValueValid(r.getValue())) {
                add(CURRENT_RULE, r);
            }
        }
    }

    @Override
    public String get(Rule k) { return get(CURRENT_RULE, k.toString()).getValue(); }
    @Override
    public boolean getBoolean(Rule k) { return Boolean.parseBoolean(get(k)); }
    @Override
    public int getInt(Rule k) { return Integer.parseInt(get(k)); }
    @Override
    public long getLong(Rule k) {
        synchronized (coreLock) {
            switch (k.getRuleDefinition().getType()) {
            case TIME:
                return ClockConversion.fromHumanReadable(get(k));
            default:
                return Long.parseLong(get(k));
            }
        }
    }
    @Override
    public void set(Rule k, String v) {
        synchronized (coreLock) {
            RuleDefinition r = k.getRuleDefinition();
            if (r == null || !r.isValueValid(v)) {
                return;
            }
            add(CURRENT_RULE, new ValWithId(k.toString(), v));
        }
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

    public static final String ROOT_ID = "WFTDARuleset";

    public class RulesetImpl extends ScoreBoardEventProviderImpl<Ruleset> implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, Ruleset parent, String id) {
            super(rulesets, id, Rulesets.RULESET);
            addProperties(PARENT, NAME, RULE);
            set(NAME, name);
            set(PARENT, parent);
        }

        @Override
        protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
            if (prop == NAME && !last.equals("")) {
                ((Rulesets) parent).refreshRuleset(this);
            }
            return value;
        }

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
