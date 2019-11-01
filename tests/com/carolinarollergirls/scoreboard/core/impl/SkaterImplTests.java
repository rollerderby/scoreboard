package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.PreparedTeam.PreparedTeamSkater;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Skater.NChild;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.impl.PreparedTeamImpl.PreparedTeamSkaterImpl;
import com.carolinarollergirls.scoreboard.core.impl.SkaterImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;

public class SkaterImplTests {

    private ScoreBoard sb;
    private SkaterImpl skater;
    private Team team;
    private UUID skaterId;

    @Before
    public void setup() {
        sb = new ScoreBoardImpl();
        team = sb.getTeam(Team.ID_1);
        skaterId = UUID.randomUUID();

        skater = new SkaterImpl(team, skaterId.toString());
        sb.getOrCreatePeriod(1).getOrCreate(Period.NChild.JAM, "2");
        sb.getOrCreatePeriod(1).getOrCreate(Period.NChild.JAM, "3");
        sb.getOrCreatePeriod(1).getOrCreate(Period.NChild.JAM, "4");
        sb.getOrCreatePeriod(2).getOrCreate(Period.NChild.JAM, "1");
        sb.getOrCreatePeriod(2).getOrCreate(Period.NChild.JAM, "2");
        sb.getOrCreatePeriod(2).getOrCreate(Period.NChild.JAM, "3");
    }

    @Test
    public void add_penalty() {
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.Value.CODE, "C");

        assertEquals(1, skater.getAll(Skater.NChild.PENALTY).size());
        assertNull(skater.get(Skater.NChild.PENALTY, Skater.FO_EXP_ID));

        Penalty penalty = (Penalty)skater.get(Skater.NChild.PENALTY, "1");

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());

    }

    @Test
    public void add_penalty_with_id() {
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p.set(Penalty.IValue.ID, "f03d5e2e-e581-4fcb-99c7-7fbd49101a36", Flag.FROM_AUTOSAVE);
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.Value.CODE, "C");

        assertEquals(1, skater.getAll(Skater.NChild.PENALTY).size());
        assertNull(skater.get(Skater.NChild.PENALTY, Skater.FO_EXP_ID));

        Penalty penalty = (Penalty)skater.get(Skater.NChild.PENALTY, "1");

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());

    }

    @Test
    public void add_ooo_penalty() {
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.Value.CODE, "C");

        p = (Penalty)skater.getOrCreate(NChild.PENALTY, "2");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.Value.CODE, "P");

        assertEquals(2, skater.getAll(Skater.NChild.PENALTY).size());

        Penalty penalty = (Penalty)skater.get(Skater.NChild.PENALTY, "1");
        Penalty penaltytwo = (Penalty)skater.get(Skater.NChild.PENALTY, "2");

        assertEquals("P", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(2, penalty.getJamNumber());

        assertEquals("C", penaltytwo.getCode());
        assertEquals(1, penaltytwo.getPeriodNumber());
        assertEquals(3, penaltytwo.getJamNumber());
    }

    @Test
    public void add_ooo_penalty_diff_period() {
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(2).getJam(3));
        p.set(Penalty.Value.CODE, "C");

        p = (Penalty)skater.getOrCreate(NChild.PENALTY, "2");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.Value.CODE, "P");

        assertEquals(2, skater.getAll(Skater.NChild.PENALTY).size());

        Penalty penalty = (Penalty)skater.get(Skater.NChild.PENALTY, "1");
        Penalty penaltytwo = (Penalty)skater.get(Skater.NChild.PENALTY, "2");

        assertEquals("P", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());

        assertEquals("C", penaltytwo.getCode());
        assertEquals(2, penaltytwo.getPeriodNumber());
        assertEquals(3, penaltytwo.getJamNumber());
    }

    @Test
    public void remove_penalty() {
        Penalty p1 = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p1.set(Penalty.Value.JAM, sb.getOrCreatePeriod(2).getJam(1));
        p1.set(Penalty.Value.CODE, "C");
        Penalty p2 = (Penalty)skater.getOrCreate(NChild.PENALTY, "2");
        p2.set(Penalty.Value.JAM, sb.getOrCreatePeriod(2).getJam(2));
        p2.set(Penalty.Value.CODE, "D");
        Penalty p3 = (Penalty)skater.getOrCreate(NChild.PENALTY, "3");
        p3.set(Penalty.Value.JAM, sb.getOrCreatePeriod(2).getJam(3));
        p3.set(Penalty.Value.CODE, "E");

        assertEquals(3, skater.getAll(Skater.NChild.PENALTY).size());
        assertEquals(1, p1.getNumber());
        assertEquals(2, p2.getNumber());
        assertEquals(3, p3.getNumber());
        assertEquals(p1, skater.getFirst(NChild.PENALTY));
        assertEquals(p2, p3.getPrevious());
        assertEquals(p1, p2.getPrevious());
        assertEquals(p3, p2.getNext());
        assertEquals(p2, p1.getNext());
        assertEquals(p3, skater.getLast(NChild.PENALTY));

        p2.unlink();

        assertEquals(2, skater.getAll(Skater.NChild.PENALTY).size());
        assertEquals(1, p1.getNumber());
        assertEquals(2, p3.getNumber());
        assertEquals(p1, skater.getFirst(NChild.PENALTY));
        assertEquals(p1, p3.getPrevious());
        assertEquals(p3, p1.getNext());
        assertEquals(p3, skater.getLast(NChild.PENALTY));
    }

    @Test
    public void update_penalty() {
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.Value.CODE, "C");

        p = (Penalty)skater.getOrCreate(NChild.PENALTY, "2");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.Value.CODE, "P");

        Penalty penalty = (Penalty)skater.get(Skater.NChild.PENALTY, "1");

        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(4));
        p.set(Penalty.Value.CODE, "X");

        assertEquals(2, skater.getAll(Skater.NChild.PENALTY).size());

        Penalty penaltyone = (Penalty)skater.get(Skater.NChild.PENALTY, "1");
        Penalty penaltytwo = (Penalty)skater.get(Skater.NChild.PENALTY, "2");

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
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, Skater.FO_EXP_ID);
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(3));
        p.set(Penalty.Value.CODE, "C");

        assertEquals(1, skater.getAll(Skater.NChild.PENALTY).size());
        assertNotNull(skater.get(Skater.NChild.PENALTY, Skater.FO_EXP_ID));

        Penalty penalty = (Penalty)skater.get(Skater.NChild.PENALTY, Skater.FO_EXP_ID);

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriodNumber());
        assertEquals(3, penalty.getJamNumber());

        assertEquals(Role.INELIGIBLE, skater.getBaseRole());
        penalty.unlink();
        assertEquals(Role.BENCH, skater.getBaseRole());
    }

    @Test
    public void penalty_between_jams_fields_skater() {
        team.field(skater, Role.BLOCKER);
        sb.startJam();
        sb.stopJamTO();
        
        Penalty p = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(1));
        p.set(Penalty.Value.CODE, "C");

        assertEquals(Role.BLOCKER, skater.getRole(team.getRunningOrUpcomingTeamJam()));
    }

    @Test
    public void alt_prepared_skater_not_in_game() {
        PreparedTeamSkater pts = new PreparedTeamSkaterImpl(null, "1234");
        pts.set(PreparedTeamSkater.Value.NUMBER, "1");
        pts.set(PreparedTeamSkater.Value.NAME, "Uno");
        pts.set(PreparedTeamSkater.Value.FLAGS, "ALT");
        skater = new SkaterImpl(team, pts);

        assertEquals(Role.NOT_IN_GAME, skater.getBaseRole());
        assertEquals(Role.NOT_IN_GAME, skater.getRole());
    }
}
