package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.impl.SkaterImpl;
import com.carolinarollergirls.scoreboard.core.impl.TeamImpl;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class PositionImplTests {
    private final String firstId = "662caf51-17da-4ef2-8f01-a6d7e1c30d56";
    private final String secondId = "91a6c3f5-8258-46c7-a845-62c21b6e37ca";
    private final String thirdId = "5df0a35b-aaa6-4c30-93ef-07ba4f174cdc";

    private ScoreBoard sbMock;
    private Rulesets rulesetsMock;
    private Team team;
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
        .when(sbMock.getScoreBoard())
        .thenReturn(sbMock);

        Mockito
        .when(rulesetsMock.getInt(Rule.NUMBER_TIMEOUTS))
        .thenReturn(3);
        Mockito
        .when(rulesetsMock.getBoolean(Rule.TIMEOUTS_PER_PERIOD))
        .thenReturn(false);
        Mockito
        .when(rulesetsMock.getInt(Rule.NUMBER_REVIEWS))
        .thenReturn(1);

        Mockito
        .when(rulesetsMock.getBoolean(Rule.REVIEWS_PER_PERIOD))
        .thenReturn(true);

        team = new TeamImpl(sbMock, "A");

        first = new SkaterImpl(team, firstId, "First","123", "");
        second = new SkaterImpl(team, secondId, "Second","456", "");
        third = new SkaterImpl(team, thirdId, "Third","789","");

        team.addSkater(first);
        team.addSkater(second);
        team.addSkater(third);
    }

    @Test
    public void key_values_populated() {
        Position blocker = team.getPosition(FloorPosition.BLOCKER1);

        assertEquals(blocker.getId(), FloorPosition.BLOCKER1.toString());
        assertEquals(blocker.getProviderName(), "Position");
        assertEquals(blocker.getProviderId(), FloorPosition.BLOCKER1.toString());
        assertEquals(blocker.getProviderClass(), Position.class);
        assertEquals(blocker.getTeam(), team);
    }

    @Test
    public void make_skater_jammer_via_position() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        jammer.setSkater(first);

        assertSame(jammer.getSkater(), first);
    }

    @Test
    public void field_skater_as_jammer() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        team.field(first, jammer);

        assertSame(jammer.getSkater(), first);
        assertSame(first.getPosition(), jammer);
    }

    @Test
    public void position_knows_skater_penalty() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        team.field(first, jammer);
        first.setPenaltyBox(true);

        assertTrue(jammer.isPenaltyBox());
    }

    @Test
    public void skater_knows_position_penalty() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        team.field(first, jammer);
        jammer.setPenaltyBox(true);

        assertTrue(first.isPenaltyBox());
    }

    @Test
    public void doesnt_set_penalty_with_no_skater() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        jammer.setPenaltyBox(true);

        assertFalse(jammer.isPenaltyBox());
    }

    @Test
    public void sp_works() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        Position pivot = team.getPosition(FloorPosition.PIVOT);
        team.field(first, jammer);
        team.field(first, pivot);

        assertSame(pivot.getSkater(), first);
        assertNull(jammer.getSkater());
    }

    @Test
    public void clears_with_null_skater_id() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        jammer.setSkater(first);
        jammer.setSkater(null);

        assertNull(jammer.getSkater());
    }


    @Test
    public void position_knows_penalty_after_sp() {
        Position jammer = team.getPosition(FloorPosition.JAMMER);
        Position pivot = team.getPosition(FloorPosition.PIVOT);
        team.field(first, pivot);
        pivot.setPenaltyBox(true);
        team.field(first, jammer);

        assertTrue(jammer.isPenaltyBox());
        assertFalse(pivot.isPenaltyBox());
        assertTrue(first.isPenaltyBox());
    }
}
