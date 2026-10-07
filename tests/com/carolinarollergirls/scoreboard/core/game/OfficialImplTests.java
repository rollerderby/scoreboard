package com.carolinarollergirls.scoreboard.core.game;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.OfficialPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class OfficialImplTests {

    private ScoreBoardImpl sb;

    @Before
    public void setUp() {
        ScoreBoardClock.getInstance().stop();
        GameImpl.setQuickClockThreshold(0L);
        sb = new ScoreBoardImpl(false);
        sb.postAutosaveUpdate();
    }

    @After
    public void tearDown() {
        ScoreBoardClock.getInstance().start(false);
        GameImpl.setQuickClockThreshold(1000L);
    }

    @Test
    public void testTeamAssignedRoleSwapAtHalftime() {
        String[] teamAssignedRoles = {
            Official.ROLE_PLT, Official.ROLE_LT, Official.ROLE_SK, Official.ROLE_JR, Official.ROLE_PBT
        };
        int gameNumber = 0;

        for (String role : teamAssignedRoles) {
            for (boolean swap : new boolean[] {false, true}) {
                GameImpl game = new GameImpl(sb, "team-role-swap-" + gameNumber);
                Child<Official> type = Official.ROLE_JR.equals(role) ? Game.REF : Game.NSO;
                OfficialImpl official = new OfficialImpl(game, "official-" + gameNumber, type);
                OfficialImpl partner = new OfficialImpl(game, "partner-" + gameNumber, type);

                game.add(type, official);
                game.add(type, partner);

                official.set(Official.ROLE, role);
                official.set(Official.P1_TEAM, game.getTeam(Team.ID_1));
                partner.set(Official.ROLE, role);
                official.set(Official.SWAP, swap);

                assertEquals(role + " initial position", Team.ID_1,
                             official.get(Official.CURRENT_POSITION).get(OfficialPosition.TEAM).getProviderId());
                assertEquals(role + " partner initial position", Team.ID_2,
                             partner.get(Official.CURRENT_POSITION).get(OfficialPosition.TEAM).getProviderId());

                fastForwardPeriod(game);
                fastForwardPeriod(game);

                assertEquals(2, game.getCurrentPeriodNumber());
                assertEquals(Team.ID_1, official.get(Official.P1_TEAM).getProviderId());
                assertEquals(Team.ID_2, partner.get(Official.P1_TEAM).getProviderId());

                String expectedTeam = swap ? Team.ID_2 : Team.ID_1;
                assertEquals(role + " with Swap=" + swap, expectedTeam,
                             official.get(Official.CURRENT_POSITION).get(OfficialPosition.TEAM).getProviderId());
                String expectedPartnerTeam = swap ? Team.ID_1 : Team.ID_2;
                assertEquals(role + " partner with Swap=" + swap, expectedPartnerTeam,
                             partner.get(Official.CURRENT_POSITION).get(OfficialPosition.TEAM).getProviderId());

                gameNumber++;
            }
        }
    }

    private void fastForwardPeriod(Game game) {
        game.getClock(Clock.ID_INTERMISSION).setTime(0);
        game.startJam();
        game.getClock(Clock.ID_PERIOD).setTime(0);
        game.stopJamTO();
    }
}
