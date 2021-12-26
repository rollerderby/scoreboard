package com.carolinarollergirls.scoreboard.core.prepared;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
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
        sb.postAutosaveUpdate();
        sb.getSettings().set(Game.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
        rulesets = sb.getRulesets();
        root = rulesets.getRuleset(RulesetsImpl.ROOT_ID);
    }

    @Test
    public void testTimeRule() {
        Ruleset child = rulesets.addRuleset("child", root, id1);
        assertEquals(root, child.getParentRuleset());
        assertEquals(1800000, sb.getCurrentGame().get(CurrentGame.GAME).getLong(Rule.PERIOD_DURATION));

        child.add(Ruleset.RULE, new ValWithId(Rule.PERIOD_DURATION.toString(), "1:00"));
        sb.getCurrentGame().get(CurrentGame.GAME).setRuleset(child);
        assertEquals(60000, sb.getCurrentGame().get(CurrentGame.GAME).getLong(Rule.PERIOD_DURATION));
        assertEquals(60000, sb.getCurrentGame().get(CurrentGame.GAME).getClock(Clock.ID_PERIOD).getTime());
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
