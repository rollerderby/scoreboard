package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImpl extends DefaultScoreBoardEventProvider implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        parent = s;
        children.put(Child.RULESET, new HashMap<String, ValueWithId>());
        children.put(Child.RULE_DEFINITION, new HashMap<String, ValueWithId>());
        children.put(Child.CURRENT_RULE, new HashMap<String, ValueWithId>());
        initialize();
        reset();
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.RULESETS); }
    public Class<Rulesets> getProviderClass() { return Rulesets.class; }
    public String getId() { return ""; }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
	synchronized (coreLock) {
	    if (prop == Child.RULESET) {
		return new RulesetImpl(this, "", "", id);
	    }
	    return null;
	}
    }
    public boolean add(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    requestBatchStart();
	    if (!(prop instanceof Child) || prop == Child.RULE_DEFINITION) { return false; }
	    if (prop == Child.RULESET && item.getId().equals(ROOT_ID)) { return false; }
	    if (prop == Child.CURRENT_RULE && 
		    !getRuleDefinition(item.getId()).isValueValid(item.getValue())) {
		return false;
	    }
	    boolean result = super.add(prop, item);
	    requestBatchEnd();
	    return result;
	}
    }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    requestBatchStart();
	    if (!(prop instanceof Child) || prop == Child.RULE_DEFINITION) { return false; }
	    if (prop == Child.RULESET && item.getId().equals(ROOT_ID)) { return false; }
	    ValueWithId last = get(prop, item.getId());
	    boolean result = super.remove(prop, item);
	    if (result && prop == Child.RULESET) {
		// Point any rulesets with the deleted one as their parent
		// to their grandparent.
                String parentId = ((Ruleset)last).getParentRulesetId();
                for (ValueWithId rm : getAll(Child.RULESET)) {
                    if (last.getId().equals(((Ruleset)rm).getParentRulesetId())) {
                        ((Ruleset)rm).setParentRulesetId(parentId);
                    }
                }
            }
	    requestBatchEnd();
	    return result;
	}
    }

    private void initialize() {
	Ruleset root = new RulesetImpl(this, "WFTDA Sanctioned", null, ROOT_ID);
	children.get(Child.RULESET).put(ROOT_ID, root);
        for (Rule r : Rule.values()) {
            r.getRuleDefinition().setParent(this);
            r.getRuleDefinition().setIndex(r.ordinal());
            children.get(Child.RULE_DEFINITION).put(r.toString(), r.getRuleDefinition());
            root.add(Ruleset.Child.RULE, new ValWithId(r.toString(), r.getRuleDefinition().getDefaultValue()));
        }
    }

    public void reset() {
        synchronized (coreLock) {
            setCurrentRuleset(ROOT_ID);
        }
    }

    public String getCurrentRulesetId() { return (String)get(Value.CURRENT_RULESET_ID); }
    public String getCurrentRulesetName() { return (String)get(Value.CURRENT_RULESET_NAME); }
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            Ruleset rs = getRuleset(id);
            setCurrentRulesetRecurse(id);
            set(Value.CURRENT_RULESET_ID, rs.getId());
            set(Value.CURRENT_RULESET_NAME, rs.getName());
            for (ValueWithId r : getAll(Child.CURRENT_RULE)) {
        	scoreBoardChange(new ScoreBoardEvent(this, Child.CURRENT_RULE, r, false));
            }
        }
    }

    private void setCurrentRulesetRecurse(String id) {
	Ruleset rs = getRuleset(id);
        if (!rs.getId().equals(ROOT_ID)) {
            setCurrentRulesetRecurse(rs.getParentRulesetId());
        }
        for (ValueWithId r : rs.getAll(Ruleset.Child.RULE)) {
            children.get(Child.CURRENT_RULE).put(r.getId(), r);
        }
    }
    
    public String get(Rule k) { return get(Child.CURRENT_RULE, k.toString()).getValue(); }
    public boolean getBoolean(Rule k) { return Boolean.parseBoolean(get(k)); }
    public int getInt(Rule k) { return Integer.parseInt(get(k)); }
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
    public void set(Rule k, String v) {
        synchronized (coreLock) {
            RuleDefinition r = k.getRuleDefinition();
            if (r == null || !r.isValueValid(v)) {
                return;
            }
            add(Child.CURRENT_RULE, new ValWithId(k.toString(), v));
        }
    }
    
    public RuleDefinition getRuleDefinition(String k) { return (RuleDefinition)get(Child.RULE_DEFINITION, k); }

    public Ruleset getRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = (Ruleset)get(Child.RULESET, id);
            if (r == null) {
                r = (Ruleset)get(Child.RULESET, ROOT_ID);
            }
            return r;
        }
    }
    public Ruleset addRuleset(String name, String parentId) {
        return addRuleset(name, parentId, UUID.randomUUID().toString());
    }
    public Ruleset addRuleset(String name, String parentId, String id) {
        synchronized (coreLock) {
            Ruleset r = new RulesetImpl(this, name, parentId, id);
            add(Child.RULESET, r);
            return r;
        }
    }
    public void removeRuleset(String id) { remove(Child.RULESET, id); }

    private ScoreBoardEventProvider parent = null;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
	add(Child.class);
    }};

    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000000";

    public class RulesetImpl extends DefaultScoreBoardEventProvider implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, String parentId, String id) {
            this.rulesets = rulesets;
            children.put(Child.RULE, new HashMap<String, ValueWithId>());
            values.put(Value.ID, id);
            values.put(Value.NAME, name);
            values.put(Value.PARENT_ID, parentId);
        }

        public String getProviderName() { return PropertyConversion.toFrontend(Rulesets.Child.RULESET); }
        public Class<Ruleset> getProviderClass() { return Ruleset.class; }
        public ScoreBoardEventProvider getParent() { return rulesets; }
        public List<Class<? extends Property>> getProperties() { return properties; }
        
        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            synchronized (coreLock) {
        	if (!(prop instanceof Value) || getId().equals(ROOT_ID)) { return false; }
        	return super.set(prop, value, flag);
	    }
        }
        
        public boolean add(AddRemoveProperty prop, ValueWithId item) {
            synchronized (coreLock) {
		requestBatchStart();
		if (prop == Child.RULE && getId().equals(ROOT_ID) && get(Child.RULE, item.getId()) != null) { return false; }
		boolean result = super.add(prop, item);
		requestBatchEnd();
		return result;
	    }
        }
        
        public boolean remove(AddRemoveProperty prop, ValueWithId item) {
            synchronized (coreLock) {
		requestBatchStart();
		if (prop == Child.RULE && getId().equals(ROOT_ID)) { return false; }
		boolean result = super.add(prop, item);
		requestBatchEnd();
		return result;
	    }
        }

        public String get(Rule r) { return get(Child.RULE, r.toString()).getValue(); }

        public String getId() { return (String)get(Value.ID); }
        public String getName() { return (String)get(Value.NAME); }
        public void setName(String n) { set(Value.NAME, n); }
        public String getParentRulesetId() { return (String)get(Value.PARENT_ID); }
        public void setParentRulesetId(String p) { set(Value.PARENT_ID, p); }

        public void setAll(Collection<ValueWithId> s) {
            synchronized (coreLock) {
        	requestBatchStart();
        	removeAll(Child.RULE);
        	for (ValueWithId r : s) {
        	    add(Child.RULE, r);
        	}
        	requestBatchEnd();
            }
        }

        private Rulesets rulesets;
        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
            add(Child.class);
        }};
    }
}
