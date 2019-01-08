package com.carolinarollergirls.scoreboard.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class PeriodImpl extends DefaultScoreBoardEventProvider implements Period {
    public PeriodImpl(Stats s, int p) {
        children.put(Child.JAM, new HashMap<String, ValueWithId>());
        stats = s;
        period = p;
    }

    public String getProviderName() { return PropertyConversion.toFrontend(Stats.Child.PERIOD); }
    public Class<Period> getProviderClass() { return Period.class; }
    public String getId() { return String.valueOf(period); }
    public ScoreBoardEventProvider getParent() { return stats; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
        return new JamImpl(this, Integer.valueOf(id));
    }

    public int getPeriodNumber() { return period; }

    public void ensureAtLeastNJams(int n) {
        synchronized (coreLock) {
    	requestBatchStart();
    	for (int i = getAll(Child.JAM).size(); i < n; i++) {
    	    get(Child.JAM, String.valueOf(i+1), true);
    	}
    	requestBatchEnd();
        }
    }

    public void truncateAfterNJams(int n) {
        synchronized (coreLock) {
            requestBatchStart();
            for (int i = getAll(Child.JAM).size(); i > n; i--) {
                remove(Child.JAM, getJam(i));
            }
            requestBatchEnd();
        }
    }

    public Jam getJam(int j) { return (Jam)get(Child.JAM, String.valueOf(j)); }

    private Stats stats;
    private int period;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
        add(Child.class);
    }};
}
