package com.carolinarollergirls.scoreboard.core.prepared;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.prepared.RulesetsImpl;
import com.carolinarollergirls.scoreboard.core.state.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class RulesetsImplTests {

    private ScoreBoard sb;
    private Rulesets rulesets;
    private Ruleset root;

    private final String id1 = "11111111-1111-1111-1111-111111111";
    private final String id2 = "22222222-2222-2222-2222-222222222";

    @Before
    public void setUp() throws Exception {
        sb = new ScoreBoardImpl();
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
        rulesets = sb.getRulesets();
        root = rulesets.getRuleset(RulesetsImpl.ROOT_ID);
    }

    @Test
    public void testChangingRuleset() {
        Ruleset child = rulesets.addRuleset("child", root, id1);
        assertEquals(root, child.getParentRuleset());
        assertEquals(2, rulesets.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(root, rulesets.getCurrentRuleset());
        assertEquals("WFTDA", rulesets.getCurrentRulesetName());

        child.add(Ruleset.RULE, new ValWithId(Rule.NUMBER_PERIODS.toString(), "5"));
        rulesets.setCurrentRuleset(id1);
        assertEquals(5, rulesets.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(id1, rulesets.getCurrentRuleset().getId());
        assertEquals("child", rulesets.getCurrentRulesetName());

        rulesets.setCurrentRuleset(root.getId());
        assertEquals(2, rulesets.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(root, rulesets.getCurrentRuleset());
        assertEquals("WFTDA", rulesets.getCurrentRulesetName());

        rulesets.set(Rule.NUMBER_PERIODS, "6");
        assertEquals(6, rulesets.getInt(Rule.NUMBER_PERIODS));

        rulesets.set(Rule.NUMBER_PERIODS, "zz");
        assertEquals(6, rulesets.getInt(Rule.NUMBER_PERIODS));
    }

    @Test
    public void testTimeRule() {
        Ruleset child = rulesets.addRuleset("child", root, id1);
        assertEquals(root, child.getParentRuleset());
        assertEquals(1800000, rulesets.getLong(Rule.PERIOD_DURATION));

        child.add(Ruleset.RULE, new ValWithId(Rule.PERIOD_DURATION.toString(), "1:00"));
        rulesets.setCurrentRuleset(id1);
        assertEquals(60000, rulesets.getLong(Rule.PERIOD_DURATION));
        assertEquals(60000, sb.getClock(Clock.ID_PERIOD).getTime());
    }

    @Test
    public void testReparentingToGrandparent() {
        rulesets.addRuleset("child", root, id1);
        Ruleset grandchild = rulesets.addRuleset("grandchild", rulesets.getRuleset(id1), id2);
        assertEquals(id1, grandchild.getParentRuleset().getId());

        rulesets.removeRuleset(id1);
        assertEquals(root, grandchild.getParentRuleset());
    }

}
