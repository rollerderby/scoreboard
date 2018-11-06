package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.RulesetsModel;
import com.carolinarollergirls.scoreboard.model.RulesetsModel.RulesetModel;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;

public class DefaultRulesetsModelTests {

    private ScoreBoardModel sbm;
    private RulesetsModel rm;

    private final String rootId = DefaultRulesetsModel.rootId;
    private final String id1 = "11111111-1111-1111-1111-111111111";
    private final String id2 = "22222222-2222-2222-2222-222222222";

    @Before
    public void setUp() throws Exception {
        ScoreBoardManager.setPropertyOverride(JettyServletScoreBoardController.class.getName() + ".html.dir", "html");
        sbm = new DefaultScoreBoardModel();
        sbm.getSettingsModel().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
        rm = sbm.getRulesetsModel();
    }

    @Test
    public void testChangingRuleset() {
        RulesetModel child = rm.addRuleset("child", rootId, id1);
        assertEquals(rootId, child.getParentRulesetId());
        assertEquals(2, rm.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
        assertEquals(1800000, rm.getLong(ScoreBoard.RULE_PERIOD_DURATION));
        assertEquals(rootId, rm.getId());
        assertEquals("WFTDA Sanctioned", rm.getName());

        Map<String,String> s = new HashMap<String,String>();
        s.put(ScoreBoard.RULE_NUMBER_PERIODS, "5");
        child.setAll(s);
        rm.setCurrentRuleset(id1);
        assertEquals(5, rm.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
        assertEquals(1800000, rm.getLong(ScoreBoard.RULE_PERIOD_DURATION));
        assertEquals(id1, rm.getId());
        assertEquals("child", rm.getName());

        rm.setCurrentRuleset(rootId);
        assertEquals(2, rm.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
        assertEquals(1800000, rm.getLong(ScoreBoard.RULE_PERIOD_DURATION));
        assertEquals(rootId, rm.getId());
        assertEquals("WFTDA Sanctioned", rm.getName());

        rm.set(ScoreBoard.RULE_NUMBER_PERIODS, "6");
        assertEquals(6, rm.getInt(ScoreBoard.RULE_NUMBER_PERIODS));

        rm.set(ScoreBoard.RULE_NUMBER_PERIODS, "zz");
        assertEquals(6, rm.getInt(ScoreBoard.RULE_NUMBER_PERIODS));
    }

    public void testTimeRule() {
        RulesetModel child = rm.addRuleset("child", rootId, id1);
        assertEquals(rootId, child.getParentRulesetId());
        assertEquals(1800000, rm.getLong(ScoreBoard.RULE_PERIOD_DURATION));

        Map<String,String> s = new HashMap<String,String>();
        s.put(ScoreBoard.RULE_PERIOD_DURATION, "1:00");
        child.setAll(s);
        rm.setCurrentRuleset(id1);
        assertEquals(60000, rm.getLong(ScoreBoard.RULE_PERIOD_DURATION));
    }

    @Test
    public void testReparentingToGrandparent() {
        rm.addRuleset("child", rootId, id1);
        RulesetModel grandchild = rm.addRuleset("grandchild", id1, id2);
        assertEquals(id1, grandchild.getParentRulesetId());

        rm.removeRuleset(id1);
        assertEquals(rootId, grandchild.getParentRulesetId());
    }


}
