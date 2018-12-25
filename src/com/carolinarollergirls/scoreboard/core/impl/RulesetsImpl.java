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
import java.util.Collections;
import java.util.HashMap;
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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.rules.AbstractRule;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;

public class RulesetsImpl extends DefaultScoreBoardEventProvider implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        parent = s;
        initialize();
        reset();
    }

    public String getProviderName() { return "KnownRulesets"; }
    public Class<Rulesets> getProviderClass() { return Rulesets.class; }
    public String getProviderId() { return ""; }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    private void initialize() {
        Map<Rule, String> rootSettings = new HashMap<Rule, String>();
        for (Rule r : Rule.values()) {
            rules.put(r.toString(), r);
            rootSettings.put(r, r.getRule().getDefaultValue());
        }
        // The root ruleset is always created from the above list,
        // so that as rules are added over time there will always
        // be a value for them.
        Ruleset root = addRuleset("WFTDA Sanctioned", "", rootId);
        root.setAll(rootSettings);
    }

    public void reset() {
        synchronized (coreLock) {
            setCurrentRuleset(rootId);
        }
    }

    public String getId() {
        synchronized (coreLock) {
            return id;
        }
    }
    public void setId(String i) {
        synchronized (coreLock) {
            id = i;
            scoreBoardChange(new ScoreBoardEvent(this, Value.RULESET, "", ""));
        }
    }
    public String getName() {
        synchronized (coreLock) {
            return name;
        }
    }
    public void setName(String n) {
        synchronized (coreLock) {
            name = n;
            scoreBoardChange(new ScoreBoardEvent(this, Value.RULESET, "", ""));
        }
    }
    public void setCurrentRuleset(String id) {
        synchronized (coreLock) {
            current.clear();
            setCurrentRulesetRecurse(id);
            scoreBoardChange(new ScoreBoardEvent(this, Value.RULESET, "", ""));
        }
    }

    private void setCurrentRulesetRecurse(String i) {
        Ruleset rs = getRuleset(i);
        if (!rs.getId().equals(rootId)) {
            setCurrentRulesetRecurse(rs.getParentRulesetId());
        }
        current.putAll(rs.getAll());
        name = rs.getName();
        id = rs.getId();
    }
    
    public Rule getRule(String k) {
        synchronized (coreLock) {
            return rules.get(k);
        }
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
            switch (k.getRule().getType()) {
            case TIME:
        	return ClockConversion.fromHumanReadable(get(k));
            default:
        	return Long.parseLong(get(k));
            }
        }
    }
    public void set(Rule k, String v) {
        synchronized (coreLock) {
            AbstractRule r = k.getRule();
            if (r == null || !r.isValueValid(v)) {
                return;
            }
            current.put(k, v);
            scoreBoardChange(new ScoreBoardEvent(this, Value.RULESET, "", ""));
        }
    }

    public Ruleset getRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = rulesets.get(id);
            if (r == null) {
                r = rulesets.get(rootId);
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
            scoreBoardChange(new ScoreBoardEvent(r, Child.KNOWN_RULESETS, r, null));
            return r;
        }
    }
    public void removeRuleset(String id) {
        synchronized (coreLock) {
            if (id.equals(rootId)) {
                return;
            }
            Ruleset r = rulesets.get(id);
            if (r == null) {
                return;
            }
            requestBatchStart();
            // Point any rulesets with the deleted one as their parent
            // to their grandparent.
            String parent = r.getParentRulesetId();
            for (Ruleset rm : rulesets.values()) {
                if (id.equals(rm.getParentRulesetId())) {
                    rm.setParentRulesetId(parent);
                }
            }

            rulesets.remove(id);
            r.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, Child.KNOWN_RULESETS, null, r));
            requestBatchEnd();
        }
    }

    private Map<Rule, String> current = new HashMap<Rule, String>();
    private Map<String, Ruleset> rulesets = new HashMap<String, Ruleset>();
    private String id = null;
    private String name = null;
    // Ordering is preserved for the UI.
    private Map<String, Rule> rules = new LinkedHashMap<String, Rule>();
    private ScoreBoardEventProvider parent = null;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
    }};

    private static Object coreLock = ScoreBoardImpl.getCoreLock();
    public static final String rootId = "00000000-0000-0000-0000-000000000000";

    public class RulesetImpl extends DefaultScoreBoardEventProvider implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, String parentId, String id) {
            this.rulesets = rulesets;
            this.id = id;
            this.name = name;
            this.parentId = parentId;
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

        public String getId() {
            synchronized (coreLock) {
                return id;
            }
        }
        public String getName() {
            synchronized (coreLock) {
                return name;
            }
        }
        public String getParentRulesetId() {
            synchronized (coreLock) {
                return parentId;
            }
        }
        public void setName(String n) {
            synchronized (coreLock) {
                if (id.equals(rootId)) {
                    return;
                }
                name = n;
                scoreBoardChange(new ScoreBoardEvent(this, Child.KNOWN_RULESETS, this, null));
            }
        }
        public void setParentRulesetId(String pi) {
            synchronized (coreLock) {
                if (id.equals(rootId)) {
                    return;
                }
                parentId = pi;
                scoreBoardChange(new ScoreBoardEvent(this, Child.KNOWN_RULESETS, this, null));
            }
        }


        public void setAll(Map<Rule, String> s) {
            synchronized (coreLock) {
                if (id.equals(rootId) && !settings.isEmpty()) {
                    return;  // Don't allow changing root after initial setup.
                }
                Set<Rule> oldKeys = settings.keySet();
                // Check all values are valid.
                for (Iterator<Rule> it = s.keySet().iterator(); it.hasNext();) {
                    Rule k = it.next();
                    AbstractRule r = k.getRule();
                    if (r == null || !r.isValueValid(s.get(k))) {
                        it.remove();
                        oldKeys.add(k);  // Allow the XML to remove this.
                    }
                }
                settings = s;
                scoreBoardChange(new ScoreBoardEvent(this, Child.KNOWN_RULESETS, this, oldKeys));
            }
        }

        public String getProviderName() { return "Ruleset"; }
        public Class<Ruleset> getProviderClass() { return Ruleset.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return rulesets; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        private String id;
        private String name;
        private String parentId;
        private Map<Rule, String> settings = new HashMap<Rule, String>();

        private Rulesets rulesets;
        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
            add(Child.class);
        }};
    }
}
