package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.fasterxml.jackson.jr.ob.JSON;

public class PenaltyCodesManager extends ScoreBoardEventProviderImpl {

    public PenaltyCodesManager(ScoreBoard parent) {
        super(parent, null, "", ScoreBoard.Child.PENALTY_CODES, PenaltyCodesManager.class, Child.class);
        this.parent = parent;
        definitions = loadFromJSON(parent.getRulesets().get(Rule.PENALTIES_FILE));
        parent.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Child.CURRENT_RULE, rulesetChangeListener));
    }

    public ValueWithId _get(AddRemoveProperty prop, String id, boolean add) {
        for (PenaltyCode c : getDefinitions()) {
            if (c.getId().equals(id)) { return c; }
        }
        return null;
    }
    @Override
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop) { return getDefinitions(); }
    @Override
    public boolean add(AddRemoveProperty prop, ValueWithId item) { return false; }
    @Override
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
        File penaltyFile = new File(BasePath.get(), file);
        try(Reader reader = new FileReader(penaltyFile)) {
            return JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Penalty Data from file", e);
        }
    }

    private PenaltyCodesDefinition definitions;

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            ValueWithId v = (ValueWithId)event.getValue();
            if (v.getId() == Rule.PENALTIES_FILE.toString()) {
                setDefinitions(loadFromJSON(v.getValue()));
            }
        }
    };

    public enum Child implements AddRemoveProperty {
        CODE(PenaltyCode.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
}
