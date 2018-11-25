package com.carolinarollergirls.scoreboard.core.implementation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.implementation.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.implementation.SkaterImpl;
import com.carolinarollergirls.scoreboard.core.implementation.TeamImpl;

public class PositionImplTests {
    private final String firstId = "662caf51-17da-4ef2-8f01-a6d7e1c30d56";
    private final String secondId = "91a6c3f5-8258-46c7-a845-62c21b6e37ca";
    private final String thirdId = "5df0a35b-aaa6-4c30-93ef-07ba4f174cdc";

    private ScoreBoard sbMock;
    private Rulesets rulesetsMock;
    private Team teamModel;
    private Skater first;
    private Skater second;
    private Skater third;

    @Before
    public void setup() {
        sbMock = Mockito.mock(ScoreBoardImpl.class);

        rulesetsMock = Mockito.mock(Rulesets.class);

        Mockito
        .when(sbMock.getRulesets())
        .thenReturn(rulesetsMock);

        Mockito
        .when(rulesetsMock.getInt(Team.RULE_NUMBER_TIMEOUTS))
        .thenReturn(3);
        Mockito
        .when(rulesetsMock.getBoolean(Team.RULE_TIMEOUTS_PER_PERIOD))
        .thenReturn(false);
        Mockito
        .when(rulesetsMock.getInt(Team.RULE_NUMBER_REVIEWS))
        .thenReturn(1);

        Mockito
        .when(rulesetsMock.getBoolean(Team.RULE_REVIEWS_PER_PERIOD))
        .thenReturn(true);

        teamModel = new TeamImpl(sbMock, "A");

        first = new SkaterImpl(teamModel, firstId, "First","123", "");
        second = new SkaterImpl(teamModel, secondId, "Second","456", "");
        third = new SkaterImpl(teamModel, thirdId, "Third","789","");

        teamModel.addSkater(first);
        teamModel.addSkater(second);
        teamModel.addSkater(third);
    }

    @Test
    public void key_values_populated() {
        Position blocker = teamModel.getPosition(Position.ID_BLOCKER1);

        assertSame(blocker.getId(),Position.ID_BLOCKER1);
        assertSame(blocker.getProviderName(), "Position");
        assertSame(blocker.getProviderId(), Position.ID_BLOCKER1);
        assertSame(blocker.getProviderClass(), Position.class);
        assertSame(blocker.getTeam(), teamModel);
    }

    @Test
    public void make_skater_jammer_via_position() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setSkater(firstId);

        assertSame(jammer.getSkater(), first);
        assertSame(first.getPosition(),Position.ID_JAMMER);
    }

    @Test
    public void make_skater_jammer_via_skater() {
        first.setPosition(Position.ID_JAMMER);
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);

        assertSame(jammer.getSkater(), first);
        assertSame(first.getPosition(), Position.ID_JAMMER);
    }

    @Test
    public void position_knows_skater_penalty() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setSkater(firstId);
        first.setPenaltyBox(true);

        assertTrue(jammer.getPenaltyBox());
    }

    @Test
    public void skater_knows_position_penalty() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setSkater(firstId);
        jammer.setPenaltyBox(true);

        assertTrue(first.isPenaltyBox());
    }

    @Test
    public void doesnt_set_penalty_with_no_skater() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setPenaltyBox(true);

        assertFalse(jammer.getPenaltyBox());
    }

    @Test
    public void sp_works() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        Position pivot = teamModel.getPosition(Position.ID_PIVOT);
        jammer.setSkater(firstId);
        pivot.setSkater(firstId);

        assertSame(pivot.getSkater(), first);
        assertNull(jammer.getSkater());
    }

    @Test
    public void clears_with_empty_model_id() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setSkater(firstId);
        jammer.setSkater("");

        assertNull(jammer.getSkater());
    }

    @Test
    public void clears_with_null_model_id() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setSkater(firstId);
        jammer.setSkater(null);

        assertNull(jammer.getSkater());
    }


    @Test(expected = SkaterNotFoundException.class)
    public void throws_with_bogus_skater_id() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        jammer.setSkater("bogus");
    }

    @Test
    public void position_knows_penalty_after_sp() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        Position pivot = teamModel.getPosition(Position.ID_PIVOT);
        pivot.setSkater(firstId);
        pivot.setPenaltyBox(true);
        first.setPosition(Position.ID_JAMMER);

        assertTrue(jammer.getPenaltyBox());
        assertFalse(pivot.getPenaltyBox());
        assertTrue(first.isPenaltyBox());
    }

    @Test
    public void position_knows_penalty_after_sp_position() {
        Position jammer = teamModel.getPosition(Position.ID_JAMMER);
        Position pivot = teamModel.getPosition(Position.ID_PIVOT);
        pivot.setSkater(firstId);
        pivot.setPenaltyBox(true);
        jammer.setSkater(firstId);

        assertTrue(jammer.getPenaltyBox());
        assertFalse(pivot.getPenaltyBox());
        assertTrue(first.isPenaltyBox());
    }




}
