package com.carolinarollergirls.scoreboard.core.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam.PreparedSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.prepared.PreparedTeamImpl.PreparedTeamSkaterImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class SkaterImplTests {

    private ScoreBoard sb;
    private Game g;
    private SkaterImpl skater;
    private Team team;
    private UUID skaterId;

    @Before
    public void setup() {
        ScoreBoardClock.getInstance().stop();
        GameImpl.setQuickClockThreshold(0L);
        sb = new ScoreBoardImpl();
        sb.postAutosaveUpdate();
        g = sb.getCurrentGame().get(CurrentGame.GAME);
        team = g.getTeam(Team.ID_1);
        skaterId = UUID.randomUUID();
        skater = new SkaterImpl(team, skaterId.toString());

        fastForwardJams(5);
        fastForwardPeriod();
        fastForwardJams(3);
    }

    private void fastForwardJams(int number) {
        for (int i = 0; i < number; i++) {
            g.startJam();
            g.stopJamTO();
        }
    }

    private void fastForwardPeriod() {
        g.getClock(Clock.ID_PERIOD).setTime(0);
        ScoreBoardClock.getInstance().advance(1000);
        g.getClock(Clock.ID_INTERMISSION).setTime(0);
        ScoreBoardClock.getInstance().advance(1000);
    }

    @Test
    public void add_penalty() {
        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.CODE, "C");

        assertEquals(1, skater.numberOf(Skater.PENALTY));
        assertNull(skater.get(Skater.PENALTY, Skater.FO_EXP_ID));

        Penalty penalty = skater.get(Skater.PENALTY, "1");

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());
    }

    @Test
    public void add_penalty_with_id() {
        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        p.set(Penalty.ID, "f03d5e2e-e581-4fcb-99c7-7fbd49101a36", Source.AUTOSAVE);
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.CODE, "C");

        assertEquals(1, skater.numberOf(Skater.PENALTY));
        assertNull(skater.get(Skater.PENALTY, Skater.FO_EXP_ID));

        Penalty penalty = skater.get(Skater.PENALTY, "1");

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());
    }

    @Test
    public void add_ooo_penalty() {
        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.CODE, "C");

        p = skater.getOrCreate(Skater.PENALTY, "2");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.CODE, "P");

        assertEquals(2, skater.numberOf(Skater.PENALTY));

        Penalty penalty = skater.get(Skater.PENALTY, "1");
        Penalty penaltytwo = skater.get(Skater.PENALTY, "2");

        assertEquals("P", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(2, penalty.getJamNumber());

        assertEquals("C", penaltytwo.getCode());
        assertEquals(1, penaltytwo.getPeriodNumber());
        assertEquals(3, penaltytwo.getJamNumber());
    }

    @Test
    public void add_ooo_penalty_diff_period() {
        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        //        p.set(Penalty.JAM, g.getOrCreatePeriod(2).getJam(3));
        p.set(Penalty.CODE, "C");
        assertEquals("C", p.getCode());
        assertEquals(3, p.getJamNumber());
        assertEquals(2, p.getPeriodNumber());

        p = skater.getOrCreate(Skater.PENALTY, "2");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.CODE, "P");

        assertEquals(2, skater.numberOf(Skater.PENALTY));

        Penalty penalty = skater.get(Skater.PENALTY, "1");
        Penalty penaltytwo = skater.get(Skater.PENALTY, "2");

        assertEquals("P", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());

        assertEquals("C", penaltytwo.getCode());
        assertEquals(2, penaltytwo.getPeriodNumber());
        assertEquals(3, penaltytwo.getJamNumber());
    }

    @Test
    public void remove_penalty() {
        Penalty p1 = skater.getOrCreate(Skater.PENALTY, "1");
        p1.set(Penalty.JAM, g.getOrCreatePeriod(2).getJam(1));
        p1.set(Penalty.CODE, "C");
        Penalty p2 = skater.getOrCreate(Skater.PENALTY, "2");
        p2.set(Penalty.JAM, g.getOrCreatePeriod(2).getJam(2));
        p2.set(Penalty.CODE, "D");
        Penalty p3 = skater.getOrCreate(Skater.PENALTY, "3");
        p3.set(Penalty.JAM, g.getOrCreatePeriod(2).getJam(3));
        p3.set(Penalty.CODE, "E");

        assertEquals(3, skater.numberOf(Skater.PENALTY));
        assertEquals(1, p1.getNumber());
        assertEquals(2, p2.getNumber());
        assertEquals(3, p3.getNumber());
        assertEquals(p1, skater.getFirst(Skater.PENALTY));
        assertEquals(p2, p3.getPrevious());
        assertEquals(p1, p2.getPrevious());
        assertEquals(p3, p2.getNext());
        assertEquals(p2, p1.getNext());
        assertEquals(p3, skater.getLast(Skater.PENALTY));

        p2.delete();

        assertEquals(2, skater.numberOf(Skater.PENALTY));
        assertEquals(1, p1.getNumber());
        assertEquals(2, p3.getNumber());
        assertEquals(p1, skater.getFirst(Skater.PENALTY));
        assertEquals(p1, p3.getPrevious());
        assertEquals(p3, p1.getNext());
        assertEquals(p3, skater.getLast(Skater.PENALTY));
    }

    @Test
    public void update_penalty() {
        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.CODE, "C");

        p = skater.getOrCreate(Skater.PENALTY, "2");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.CODE, "P");

        Penalty penalty = skater.get(Skater.PENALTY, "1");

        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(4));
        p.set(Penalty.CODE, "X");

        assertEquals(2, skater.numberOf(Skater.PENALTY));

        Penalty penaltyone = skater.get(Skater.PENALTY, "1");
        Penalty penaltytwo = skater.get(Skater.PENALTY, "2");

        assertEquals("C", penaltyone.getCode());
        assertEquals(1, penaltyone.getPeriodNumber());
        assertEquals(3, penaltyone.getJamNumber());

        assertEquals("X", penaltytwo.getCode());
        assertEquals(1, penaltytwo.getPeriodNumber());
        assertEquals(4, penaltytwo.getJamNumber());

        assertEquals(penalty.getId(), penaltytwo.getId());
    }

    @Test
    public void add_fo_exp() {
        Penalty p = skater.getOrCreate(Skater.PENALTY, Skater.FO_EXP_ID);
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.CODE, "C");

        assertEquals(1, skater.numberOf(Skater.PENALTY));
        assertNotNull(skater.get(Skater.PENALTY, Skater.FO_EXP_ID));

        Penalty penalty = skater.get(Skater.PENALTY, Skater.FO_EXP_ID);

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());

        assertEquals(Role.INELIGIBLE, skater.getBaseRole());
        penalty.delete();
        assertEquals(Role.BENCH, skater.getBaseRole());
    }

    @Test
    public void auto_added_fo() {
        g.set(Rule.FO_LIMIT, "2");
        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.CODE, "B");

        assertNull(skater.getPenalty(Skater.FO_EXP_ID));

        Penalty p2 = skater.getOrCreate(Skater.PENALTY, "2");
        p2.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p2.set(Penalty.CODE, "C");

        assertEquals(g.getOrCreatePeriod(1).getJam(3), skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.JAM));
        assertEquals("FO", skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.CODE));

        p2.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(4));

        assertEquals(g.getOrCreatePeriod(1).getJam(4), skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.JAM));
        assertEquals("FO", skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.CODE));

        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(5));

        assertEquals(g.getOrCreatePeriod(1).getJam(5), skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.JAM));
        assertEquals("FO", skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.CODE));

        p2.set(Penalty.CODE, null);

        assertNull(skater.get(Skater.PENALTY, g.getInt(Rule.FO_LIMIT)));
        assertNull(skater.getPenalty(Skater.FO_EXP_ID));

        p2 = skater.getOrCreate(Skater.PENALTY, "2");
        p2.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(3));
        p2.set(Penalty.CODE, "C");

        assertEquals(1, p2.getNumber());
        assertEquals(2, p.getNumber());
        assertEquals(g.getOrCreatePeriod(1).getJam(5), skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.JAM));
        assertEquals("FO", skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.CODE));

        skater.getPenalty(Skater.FO_EXP_ID).set(Penalty.CODE, "B");

        p2.set(Penalty.CODE, null);

        assertNull(skater.get(Skater.PENALTY, g.getInt(Rule.FO_LIMIT)));
        assertEquals("B", skater.getPenalty(Skater.FO_EXP_ID).get(Penalty.CODE));
    }

    @Test
    public void penalty_between_jams_fields_skater() {
        sb.getSettings().set(ScoreBoard.SETTING_USE_LT, "true");
        team.execute(Team.ADVANCE_FIELDINGS);
        team.field(skater, Role.BLOCKER);
        g.startJam();
        g.stopJamTO();

        Penalty p = skater.getOrCreate(Skater.PENALTY, "1");
        p.set(Penalty.CODE, "C");

        assertEquals(Role.BLOCKER, skater.getRole(team.getRunningOrUpcomingTeamJam()));
    }

    @Test
    public void alt_prepared_skater_not_in_game() {
        PreparedSkater pts = new PreparedTeamSkaterImpl(null, "1234");
        pts.set(Skater.ROSTER_NUMBER, "1");
        pts.set(Skater.NAME, "Uno");
        pts.set(Skater.FLAGS, "ALT");
        skater = new SkaterImpl(team, pts, null);

        assertEquals(Role.NOT_IN_GAME, skater.getBaseRole());
        assertEquals(Role.NOT_IN_GAME, skater.getRole());
    }
}
