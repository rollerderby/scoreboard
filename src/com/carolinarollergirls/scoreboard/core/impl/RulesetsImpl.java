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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImpl extends DefaultScoreBoardEventProvider implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        parent = s;
        initialize();
        reset();
    }

    public String getProviderName() { return "Rulesets"; }
    public Class<Rulesets> getProviderClass() { return Rulesets.class; }
    public String getProviderId() { return ""; }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    private void initialize() {
        Map<Rule, String> rootSettings = new HashMap<Rule, String>();
        for (Rule r : Rule.values()) {
            r.getRuleDefinition().setParent(this);
            r.getRuleDefinition().setIndex(r.ordinal());
            rules.put(r.toString(), r);
            rootSettings.put(r, r.getRuleDefinition().getDefaultValue());
        }
        // The root ruleset is always created from the above list,
        // so that as rules are added over time there will always
        // be a value for them.
        Ruleset root = addRuleset("WFTDA Sanctioned", null, ROOT_ID);
        root.setAll(rootSettings);
    }

    public void reset() {
        synchronized (coreLock) {
            setCurrentRuleset(ROOT_ID);
        }
    }

    public String getId() { return (String)get(Value.CURRENT_RULESET_ID); }
    public String getName() { return (String)get(Value.CURRENT_RULESET_NAME); }
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            current.clear();
            Ruleset rs = getRuleset(id);
            setCurrentRulesetRecurse(id);
            set(Value.CURRENT_RULESET_ID, rs.getId());
            set(Value.CURRENT_RULESET_NAME, rs.getName());
            for (Rule k : current.keySet()) {
        	scoreBoardChange(new ScoreBoardEvent(this, Child.CURRENT_RULE, new ValWithId(k.toString(), current.get(k)), false));
            }
        }
    }

    private void setCurrentRulesetRecurse(String id) {
	Ruleset rs = getRuleset(id);
        if (!rs.getId().equals(ROOT_ID)) {
            setCurrentRulesetRecurse(rs.getParentRulesetId());
        }
        current.putAll(rs.getAll());
    }
    
    public Rule getRule(String k) {
        synchronized (coreLock) {
            return rules.get(k);
        }
    }
    public Collection<RuleDefinition> getRuleDefinitions() {
	Collection<RuleDefinition> defs = new HashSet<RuleDefinition>();
	for (Rule r : Rule.values()) {
	    defs.add(r.getRuleDefinition());
	}
	return defs;
    }


    public Map<Rule, String> getAll() {
        synchronized (coreLock) {
            return Collections.unmodifiableMap(current);
        }
    }
    public String get(Rule k) {
        synchronized (coreLock) {
            return current.get(k);
        }
    }
    public boolean getBoolean(Rule k) {
        synchronized (coreLock) {
            return Boolean.parseBoolean(get(k));
        }
    }
    public int getInt(Rule k) {
        synchronized (coreLock) {
            return Integer.parseInt(get(k));
        }
    }
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
            current.put(k, v);
            scoreBoardChange(new ScoreBoardEvent(this, Child.CURRENT_RULE, new ValWithId(k.toString(), v), false));
        }
    }

    public Ruleset getRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = rulesets.get(id);
            if (r == null) {
                r = rulesets.get(ROOT_ID);
            }
            return r;
        }
    }
    public Map<String, Ruleset> getRulesets() {
        synchronized (coreLock) {
            return Collections.unmodifiableMap(new HashMap<String, Ruleset>(rulesets));
        }
    }
    public Ruleset addRuleset(String name, String parentId) {
        return addRuleset(name, parentId, UUID.randomUUID().toString());
    }
    public Ruleset addRuleset(String name, String parentId, String id) {
        synchronized (coreLock) {
            Ruleset r = new RulesetImpl(this, name, parentId, id);
            rulesets.put(id, r);
            r.addScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, Child.RULESET, r, false));
            return r;
        }
    }
    public void removeRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = getRuleset(id);
            if (r.getId().equals(ROOT_ID)) {
                return;
            }
            requestBatchStart();
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            String parentId = r.getParentRulesetId();
            for (Ruleset rm : rulesets.values()) {
                if (id.equals(rm.getParentRulesetId())) {
                    rm.setParentRulesetId(parentId);
                }
            }

            rulesets.remove(id);
            r.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, Child.RULESET, r, true));
            requestBatchEnd();
        }
    }

    private Map<Rule, String> current = new HashMap<Rule, String>();
    private Map<String, Ruleset> rulesets = new HashMap<String, Ruleset>();
    // Ordering is preserved for the UI.
    private Map<String, Rule> rules = new LinkedHashMap<String, Rule>();
    private ScoreBoardEventProvider parent = null;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
    }};

    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000000";

    public class RulesetImpl extends DefaultScoreBoardEventProvider implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, String parentId, String id) {
            this.rulesets = rulesets;
            values.put(Value.ID, id);
            values.put(Value.NAME, name);
            values.put(Value.PARENT_ID, parentId);
        }

        public Map<Rule, String> getAll() {
            synchronized (coreLock) {
                return Collections.unmodifiableMap(settings);
            }
        }
        public String get(Rule r) {
            synchronized (coreLock) {
                return settings.get(r);
            }
        }

        public String getId() { return (String)get(Value.ID); }
        public String getName() { return (String)get(Value.NAME); }
        public void setName(String n) { set(Value.NAME, n); }
        public String getParentRulesetId() { return (String)get(Value.PARENT_ID); }
        public void setParentRulesetId(String p) { set(Value.PARENT_ID, p); }

        public void setAll(Map<Rule, String> s) {
            synchronized (coreLock) {
                if (getId().equals(ROOT_ID) && !settings.isEmpty()) {
                    return;  // Don't allow changing root after initial setup.
                }
                Set<Rule> oldKeys = settings.keySet();
                // Check all values are valid.
                for (Iterator<Rule> it = s.keySet().iterator(); it.hasNext();) {
                    Rule k = it.next();
                    RuleDefinition r = k.getRuleDefinition();
                    if (r == null || !r.isValueValid(s.get(k))) {
                        it.remove();
                        oldKeys.add(k);  // Allow the XML to remove this.
                    } else {
                	oldKeys.remove(k);
                    }
                }
                for (Rule k : oldKeys) {
                    scoreBoardChange(new ScoreBoardEvent(this, Child.RULE, new ValWithId(k.toString(), settings.get(k)), true));
                }
                settings = s;
                for (Rule k : settings.keySet()) {
                    scoreBoardChange(new ScoreBoardEvent(this, Child.RULE, new ValWithId(k.toString(), settings.get(k)), false));
                }
            }
        }

        public String getProviderName() { return PropertyConversion.toFrontend(Rulesets.Child.RULESET); }
        public Class<Ruleset> getProviderClass() { return Ruleset.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return rulesets; }
        public List<Class<? extends Property>> getProperties() { return properties; }
        
        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            synchronized (coreLock) {
        	if (!(prop instanceof Value) || getId().equals(ROOT_ID)) { return false; }
        	return super.set(prop, value, flag);
	    }
        }

        private Map<Rule, String> settings = new HashMap<Rule, String>();

        private Rulesets rulesets;
        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
            add(Child.class);
        }};
    }
}
