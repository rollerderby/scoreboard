package com.carolinarollergirls.scoreboard.core.implementation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.implementation.RulesetsImpl;
import com.carolinarollergirls.scoreboard.core.implementation.ScoreBoardImpl;

import java.util.HashMap;
import java.util.Map;

public class RulesetsImplTests {

    private ScoreBoard sb;
    private Rulesets rulesets;

    private final String rootId = RulesetsImpl.rootId;
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
        assertEquals(2, rulesets.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(ScoreBoard.RULE_PERIOD_DURATION));
        assertEquals(rootId, rulesets.getId());
        assertEquals("WFTDA Sanctioned", rulesets.getName());

        Map<String,String> s = new HashMap<String,String>();
        s.put(ScoreBoard.RULE_NUMBER_PERIODS, "5");
        child.setAll(s);
        rulesets.setCurrentRuleset(id1);
        assertEquals(5, rulesets.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(ScoreBoard.RULE_PERIOD_DURATION));
        assertEquals(id1, rulesets.getId());
        assertEquals("child", rulesets.getName());

        rulesets.setCurrentRuleset(rootId);
        assertEquals(2, rulesets.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
        assertEquals(1800000, rulesets.getLong(ScoreBoard.RULE_PERIOD_DURATION));
        assertEquals(rootId, rulesets.getId());
        assertEquals("WFTDA Sanctioned", rulesets.getName());

        rulesets.set(ScoreBoard.RULE_NUMBER_PERIODS, "6");
        assertEquals(6, rulesets.getInt(ScoreBoard.RULE_NUMBER_PERIODS));

        rulesets.set(ScoreBoard.RULE_NUMBER_PERIODS, "zz");
        assertEquals(6, rulesets.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
    }

    public void testTimeRule() {
        Ruleset child = rulesets.addRuleset("child", rootId, id1);
        assertEquals(rootId, child.getParentRulesetId());
        assertEquals(1800000, rulesets.getLong(ScoreBoard.RULE_PERIOD_DURATION));

        Map<String,String> s = new HashMap<String,String>();
        s.put(ScoreBoard.RULE_PERIOD_DURATION, "1:00");
        child.setAll(s);
        rulesets.setCurrentRuleset(id1);
        assertEquals(60000, rulesets.getLong(ScoreBoard.RULE_PERIOD_DURATION));
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
