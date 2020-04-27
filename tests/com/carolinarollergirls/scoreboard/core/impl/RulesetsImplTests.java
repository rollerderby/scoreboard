package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImplTests {

    private ScoreBoard sb;
    private Rulesets rulesets;

    private final String rootId = RulesetsImpl.ROOT_ID;
    private final String id1 = "11111111-1111-1111-1111-111111111";
    private final String id2 = "22222222-2222-2222-2222-222222222";

    @Before
    public void setUp() throws Exception {
        sb = new ScoreBoardImpl();
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
        rulesets = sb.getRulesets();
    }

    @Test
    public void testChangingRuleset() {
        Ruleset child = rulesets.addRuleset("child", rootId, id1);
        assertEquals(rootId, child.getParentRulesetId());
        assertEquals(2, rulesets.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(rootId, rulesets.getCurrentRulesetId());
        assertEquals("WFTDA", rulesets.getCurrentRulesetName());

        child.add(Ruleset.RULE, new ValWithId(Rule.NUMBER_PERIODS.toString(), "5"));
        rulesets.setCurrentRuleset(id1);
        assertEquals(5, rulesets.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(id1, rulesets.getCurrentRulesetId());
        assertEquals("child", rulesets.getCurrentRulesetName());

        rulesets.setCurrentRuleset(rootId);
        assertEquals(2, rulesets.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(rootId, rulesets.getCurrentRulesetId());
        assertEquals("WFTDA", rulesets.getCurrentRulesetName());

        rulesets.set(Rule.NUMBER_PERIODS, "6");
        assertEquals(6, rulesets.getInt(Rule.NUMBER_PERIODS));

        rulesets.set(Rule.NUMBER_PERIODS, "zz");
        assertEquals(6, rulesets.getInt(Rule.NUMBER_PERIODS));
    }

    @Test
    public void testTimeRule() {
        Ruleset child = rulesets.addRuleset("child", rootId, id1);
        assertEquals(rootId, child.getParentRulesetId());
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));

        child.add(Ruleset.RULE, new ValWithId(Rule.PERIOD_DURATION.toString(), "1:00"));
        rulesets.setCurrentRuleset(id1);
        assertEquals(60000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(60000, sb.getClock(Clock.ID_PERIOD).getTime());
    }

    @Test
    public void testReparentingToGrandparent() {
        rulesets.addRuleset("child", rootId, id1);
        Ruleset grandchild = rulesets.addRuleset("grandchild", id1, id2);
        assertEquals(id1, grandchild.getParentRulesetId());

        rulesets.removeRuleset(id1);
        assertEquals(rootId, grandchild.getParentRulesetId());
    }

}
