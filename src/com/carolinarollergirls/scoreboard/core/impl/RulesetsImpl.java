package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Collection;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImpl extends ScoreBoardEventProviderImpl implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        super(s, null, "", ScoreBoard.Child.RULESETS, Rulesets.class, Value.class, Child.class);
        initialize();
        addWriteProtection(Child.RULE_DEFINITION);
        reset();
    }

    @Override
    public ValueWithId create(AddRemoveProperty prop, String id) {
        if (prop == Child.RULESET) {
            return new RulesetImpl(this, "", "", id);
        }
        return null;
    }
    @Override
    public boolean add(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.RULESET && item.getId().equals(ROOT_ID)) { return false; }
        if (prop == Child.CURRENT_RULE && 
                !getRuleDefinition(item.getId()).isValueValid(item.getValue())) {
            return false;
        }
        return super.add(prop, item);
    }
    @Override
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.RULESET && item.getId().equals(ROOT_ID)) { return false; }
        if (prop == Child.CURRENT_RULE) { return false; }
        return super.remove(prop, item);
    }
    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.RULESET) {
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            String parentId = ((Ruleset)item).getParentRulesetId();
            for (ValueWithId rm : getAll(Child.RULESET)) {
                if (item.getId().equals(((Ruleset)rm).getParentRulesetId())) {
                    ((Ruleset)rm).setParentRulesetId(parentId);
                }
            }
        }
    }

    private void initialize() {
        RulesetImpl root = new RulesetImpl(this, "WFTDA Sanctioned", null, ROOT_ID);
        for (Rule r : Rule.values()) {
            r.getRuleDefinition().setParent(this);
            r.getRuleDefinition().setIndex(r.ordinal());
            add(Child.RULE_DEFINITION, r.getRuleDefinition());
            root.add(Ruleset.Child.RULE, new ValWithId(r.toString(), r.getRuleDefinition().getDefaultValue()));
        }
        root.addWriteProtection(Ruleset.Child.RULE);
        super.add(Child.RULESET, root);
    }

    @Override
    public void reset() {
        synchronized (coreLock) {
            setCurrentRuleset(ROOT_ID);
        }
    }

    @Override
    public String getCurrentRulesetId() { return (String)get(Value.CURRENT_RULESET_ID); }
    @Override
    public String getCurrentRulesetName() { return (String)get(Value.CURRENT_RULESET_NAME); }
    @Override
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            Ruleset rs = getRuleset(id);
            setCurrentRulesetRecurse(id);
            set(Value.CURRENT_RULESET_ID, rs.getId());
            set(Value.CURRENT_RULESET_NAME, rs.getName());
        }
    }

    private void setCurrentRulesetRecurse(String id) {
        Ruleset rs = getRuleset(id);
        if (!rs.getId().equals(ROOT_ID)) {
            setCurrentRulesetRecurse(rs.getParentRulesetId());
        }
        for (ValueWithId r : rs.getAll(Ruleset.Child.RULE)) {
            add(Child.CURRENT_RULE, r);
        }
    }

    @Override
    public String get(Rule k) { return get(Child.CURRENT_RULE, k.toString()).getValue(); }
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
            add(Child.CURRENT_RULE, new ValWithId(k.toString(), v));
        }
    }

    @Override
    public RuleDefinition getRuleDefinition(String k) { return (RuleDefinition)get(Child.RULE_DEFINITION, k); }

    @Override
    public Ruleset getRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = (Ruleset)get(Child.RULESET, id);
            if (r == null) {
                r = (Ruleset)get(Child.RULESET, ROOT_ID);
            }
            return r;
        }
    }
    @Override
    public Ruleset addRuleset(String name, String parentId) {
        return addRuleset(name, parentId, UUID.randomUUID().toString());
    }
    @Override
    public Ruleset addRuleset(String name, String parentId, String id) {
        synchronized (coreLock) {
            Ruleset r = new RulesetImpl(this, name, parentId, id);
            add(Child.RULESET, r);
            return r;
        }
    }
    @Override
    public void removeRuleset(String id) { remove(Child.RULESET, id); }

    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000000";

    public class RulesetImpl extends ScoreBoardEventProviderImpl implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, String parentId, String id) {
            super(rulesets, Value.ID, id, Rulesets.Child.RULESET, Ruleset.class, Value.class, Child.class);
            set(Value.NAME, name);
            set(Value.PARENT_ID, parentId);
            if (ROOT_ID.equals(id)) {
                for (Value prop : Value.values()) {
                    addWriteProtection(prop);
                }
            }
        }

        @Override
        public String get(Rule r) { return get(Child.RULE, r.toString()).getValue(); }

        @Override
        public String getName() { return (String)get(Value.NAME); }
        @Override
        public void setName(String n) { set(Value.NAME, n); }
        @Override
        public String getParentRulesetId() { return (String)get(Value.PARENT_ID); }
        @Override
        public void setParentRulesetId(String p) { set(Value.PARENT_ID, p); }

        @Override
        public void setAll(Collection<ValueWithId> s) {
            synchronized (coreLock) {
                removeAll(Child.RULE);
                for (ValueWithId r : s) {
                    add(Child.RULE, r);
                }
            }
        }
    }
}
