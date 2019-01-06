package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.fasterxml.jackson.jr.ob.JSON;

public class PenaltyCodesManager extends DefaultScoreBoardEventProvider {
    
    public PenaltyCodesManager(ScoreBoard parent) {
	this.parent = parent;
	definitions = loadFromJSON(parent.getRulesets().get(Rule.PENALTIES_FILE));
        parent.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Child.CURRENT_RULE, rulesetChangeListener));
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.PENALTY_CODES); }
    public Class<PenaltyCodesManager> getProviderClass() { return PenaltyCodesManager.class; }
    public String getId() { return ""; }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public ValueWithId get(AddRemoveProperty prop, String id, boolean add) {
	for (PenaltyCode c : getDefinitions()) {
	    if (c.getId().equals(id)) { return c; }
	}
	return null;
    }
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop) { return getDefinitions(); }
    public boolean add(AddRemoveProperty prop, ValueWithId item) { return false; }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) { return false; }

    public List<PenaltyCode> getDefinitions() {
	return definitions.getPenalties();
    }
    public void setDefinitions(PenaltyCodesDefinition def) {
	definitions = def;
	definitions.add(new PenaltyCode("?", "Unknown"));
        for (PenaltyCode p : getDefinitions()) {
            scoreBoardChange(new ScoreBoardEvent(this, Child.CODE, p, false));
        }
    }
    
    public PenaltyCodesDefinition loadFromJSON(String file) {
        File folder = ScoreBoardManager.getDefaultPath();
        File penaltyFile = new File(folder,file);
        try(Reader reader = new FileReader(penaltyFile)) {
            return JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Penalty Data from file", e);
        }
    }
    
    private ScoreBoard parent;
    private PenaltyCodesDefinition definitions;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            ValueWithId v = (ValueWithId)event.getValue();
            if (v.getId() == Rule.PENALTIES_FILE.toString()) {
        	setDefinitions(loadFromJSON(v.getValue()));
            }
        }
    };

    public enum Child implements AddRemoveProperty {
        CODE;
    }
}
