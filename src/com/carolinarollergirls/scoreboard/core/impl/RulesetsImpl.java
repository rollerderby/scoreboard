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
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImpl extends ScoreBoardEventProviderImpl<Rulesets> implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.RULESETS);
        addProperties(CURRENT_RULESET, CURRENT_RULESET_ID, CURRENT_RULESET_NAME, CURRENT_RULE, RULE_DEFINITION,
                RULESET);
        initialize();
        reset();
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == CURRENT_RULESET) {
            setCurrentRulesetRecurse(((Ruleset) value).getId());
        }
        return value;
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        if (prop == RULESET) {
            return new RulesetImpl(this, "", "", id);
        }
        return null;
    }
    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == RULESET) {
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            String parentId = ((Ruleset) item).getParentRulesetId();
            for (Ruleset rm : getAll(RULESET)) {
                if (item.getId().equals(rm.getParentRulesetId())) {
                    rm.setParentRulesetId(parentId);
                }
            }
        }
    }

    private void initialize() {
        setCopy(CURRENT_RULESET_ID, this, CURRENT_RULESET, ID, true);
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
    public String getCurrentRulesetId() { return get(CURRENT_RULESET_ID); }
    @Override
    public String getCurrentRulesetName() { return get(CURRENT_RULESET_NAME); }
    @Override
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            set(CURRENT_RULESET, getRuleset(id));
        }
    }
    @Override
    public void refreshRuleset(String id) {
        synchronized (coreLock) {
            for (String tId = getCurrentRulesetId(); !ROOT_ID.equals(tId); tId = getRuleset(tId).getParentRulesetId()) {
                if (tId.equals(id)) {
                    setCurrentRulesetRecurse(getCurrentRulesetId());
                    scoreBoardChange(
                            new ScoreBoardEvent<>(this, CURRENT_RULESET, get(CURRENT_RULESET), get(CURRENT_RULESET)));
                    break;
                }
            }
        }
    }

    private void setCurrentRulesetRecurse(String id) {
        Ruleset rs = getRuleset(id);
        if (!rs.getId().equals(ROOT_ID)) {
            setCurrentRulesetRecurse(rs.getParentRulesetId());
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
    public Ruleset addRuleset(String name, String parentId) {
        return addRuleset(name, parentId, UUID.randomUUID().toString());
    }
    @Override
    public Ruleset addRuleset(String name, String parentId, String id) {
        synchronized (coreLock) {
            Ruleset r = new RulesetImpl(this, name, parentId, id);
            add(RULESET, r);
            return r;
        }
    }
    @Override
    public void removeRuleset(String id) { remove(RULESET, id); }

    public static final String ROOT_ID = "WFTDARuleset";

    public class RulesetImpl extends ScoreBoardEventProviderImpl<Ruleset> implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, String parentId, String id) {
            super(rulesets, id, Rulesets.RULESET);
            addProperties(PARENT_ID, NAME, RULE);
            set(NAME, name);
            set(PARENT_ID, parentId);
        }

        @Override
        protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
            if (prop == NAME && !last.equals("")) {
                ((Rulesets) parent).refreshRuleset(getId());
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
        public String getParentRulesetId() { return get(PARENT_ID); }
        @Override
        public void setParentRulesetId(String p) { set(PARENT_ID, p); }
    }
}
