package com.carolinarollergirls.scoreboard.core.prepared;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public final class RulesetsImpl extends ScoreBoardEventProviderImpl<Rulesets> implements Rulesets {
    public RulesetsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.RULESETS);
        addProperties(props);
        initialize();
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        if (prop == RULESET) { return new RulesetImpl(this, "", get(DEFAULT), id); }
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
                if (removed.equals(rm.getParentRuleset())) { rm.setParentRuleset(grandparent); }
            }
        }
    }

    private void initialize() {
        RulesetImpl root = new RulesetImpl(this, "WFTDA", null, ROOT_ID);
        for (Rule r : Rule.values()) {
            r.getRuleDefinition().setParent(this);
            r.getRuleDefinition().setIndex(r.ordinal());
            add(RULE_DEFINITION, r.getRuleDefinition());
            root.setRule(r.toString(), r.getRuleDefinition().getDefaultValue());
        }
        root.set(READONLY, true);
        add(RULESET, root);
        set(DEFAULT, root);
        addWriteProtection(RULE_DEFINITION);
        addDefaultRulesets(root);
    }

    private void addDefaultRulesets(Ruleset root) {
        RulesetImpl jrda = new RulesetImpl(this, "JRDA", root, "JRDARuleset");
        jrda.setRule(Rule.SUDDEN_SCORING, true).setRule(Rule.INJURY_CONTINUATION, true).set(READONLY, true);
        add(RULESET, jrda);

        RulesetImpl sevens = new RulesetImpl(this, "Sevens", root, "SevensRuleset");
        sevens.setRule(Rule.INTERMISSION_DURATIONS, "60:00")
            .setRule(Rule.FO_LIMIT, 4)
            .setRule(Rule.PERIOD_DURATION, "21:00")
            .setRule(Rule.NUMBER_PERIODS, 1)
            .setRule(Rule.NUMBER_REVIEWS, 0)
            .setRule(Rule.NUMBER_TIMEOUTS, 0)
            .set(READONLY, true);
        add(RULESET, sevens);

        RulesetImpl rdcl = new RulesetImpl(this, "RDCL", root, "RDCLRuleset");
        rdcl.setRule(Rule.INTERMISSION_DURATIONS, "5:00,15:00,5:00,60:00")
            .setRule(Rule.JAM_DURATION, "1:00")
            .setRule(Rule.JAM_NUMBER_PER_PERIOD, false)
            .setRule(Rule.PENALTIES_FILE, "/config/penalties/RDCL.json")
            .setRule(Rule.PERIOD_DURATION, "15:00")
            .setRule(Rule.PERIOD_END_BETWEEN_JAMS, false)
            .setRule(Rule.NUMBER_PERIODS, 4)
            .setRule(Rule.RDCL_PER_HALF_RULES, true)
            .setRule(Rule.WFTDA_LATE_SCORE_RULE, false)
            .set(READONLY, true);
        add(RULESET, rdcl);

        RulesetImpl rdclHalf = new RulesetImpl(this, "RDCL half game", rdcl, "RDCLHalfGameRuleset");
        rdclHalf.setRule(Rule.INTERMISSION_DURATIONS, "5:00,60:00")
            .setRule(Rule.FO_LIMIT, 4)
            .setRule(Rule.NUMBER_PERIODS, 2)
            .setRule(Rule.NUMBER_TIMEOUTS, 1)
            .setRule(Rule.TIMEOUTS_PER_PERIOD, true)
            .set(READONLY, true);
        add(RULESET, rdclHalf);

        RulesetImpl shortTrack = new RulesetImpl(this, "Short Track", root, "ShortTrackRuleset");
        shortTrack.setRule(Rule.JAMS_PER_PERIOD, 10)
            .setRule(Rule.JAM_DURATION, "1:00")
            .setRule(Rule.NUMBER_TIMEOUTS, 1)
            .setRule(Rule.NUMBER_REVIEWS, 0)
            .setRule(Rule.PENALTY_POINTS_DEDUCTION, 2)
            .setRule(Rule.PENALTY_DURATION, 0L)
            .setRule(Rule.OVERTIME_LINEUP_DURATION, "0:30")
            .setRule(Rule.POINTS_ON_INITIAL, true)
            .setRule(Rule.WFTDA_LATE_SCORE_RULE, false)
            .set(READONLY, true);
        add(RULESET, shortTrack);
    }

    @Override
    public RuleDefinition getRuleDefinition(String k) {
        return get(RULE_DEFINITION, k);
    }

    @Override
    public Ruleset getRuleset(String id) {
        synchronized (coreLock) {
            Ruleset r = get(RULESET, id);
            if (r == null) { r = get(RULESET, ROOT_ID); }
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
    public void removeRuleset(String id) {
        remove(RULESET, id);
    }

    public class RulesetImpl extends ScoreBoardEventProviderImpl<Ruleset> implements Ruleset {
        private RulesetImpl(Rulesets rulesets, String name, Ruleset parent, String id) {
            super(rulesets, id, Rulesets.RULESET);
            addProperties(props);
            set(NAME, name);
            set(PARENT, parent);
        }

        @Override
        protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
            if (prop == PARENT && this.isAncestorOf((Ruleset) value)) { return last; }
            return value;
        }

        @Override
        public String get(Rule r) {
            return get(RULE, r.toString()).getValue();
        }

        @Override
        public String getName() {
            return get(NAME);
        }
        @Override
        public void setName(String n) {
            set(NAME, n);
        }
        @Override
        public Ruleset getParentRuleset() {
            return get(PARENT);
        }
        @Override
        public void setParentRuleset(Ruleset rs) {
            set(PARENT, rs);
        }
        @Override
        public boolean isAncestorOf(Ruleset rs) {
            if (rs == null) { return false; }
            Ruleset parentRs = rs.getParentRuleset();
            return this == rs || this.isAncestorOf(parentRs);
        }
        private Ruleset setRule(String id, String value) {
            add(RULE, new ValWithId(id, value));
            return this;
        }
        @Override
        public Ruleset setRule(Rule r, Object value) {
            return setRule(r.toString(), value.toString());
        }
    }
}
