package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.fasterxml.jackson.jr.ob.JSON;

public class PenaltyCodesManager extends ScoreBoardEventProviderImpl<PenaltyCodesManager> {

    public PenaltyCodesManager(ScoreBoard parent) {
        super(parent, "", ScoreBoard.PENALTY_CODES);
        addProperties(CODE);
        this.parent = parent;
        setDefinitions(loadFromJSON(parent.getRulesets().get(Rule.PENALTIES_FILE)));
        parent.addScoreBoardListener(
                new ConditionalScoreBoardListener<>(Rulesets.class, Rulesets.CURRENT_RULE, rulesetChangeListener));
    }

    public void setDefinitions(PenaltyCodesDefinition def) {
        removeAll(CODE);
        def.add(new PenaltyCode("?", "Unknown"));
        for (PenaltyCode p : def.getPenalties()) {
            add(CODE, p);
        }
    }

    public PenaltyCodesDefinition loadFromJSON(String file) {
        File penaltyFile = new File(BasePath.get(), file);
        try (Reader reader = new FileReader(penaltyFile)) {
            return JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Penalty Data from file", e);
        }
    }

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            ValueWithId v = (ValueWithId) event.getValue();
            if (Rule.PENALTIES_FILE.toString().equals(v.getId())) {
                setDefinitions(loadFromJSON(v.getValue()));
            }
        }
    };

    Child<PenaltyCode> CODE = new Child<>(PenaltyCode.class, "Code");
}
