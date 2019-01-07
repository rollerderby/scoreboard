package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Skater.Child;
import com.carolinarollergirls.scoreboard.core.Skater.Penalty;
import com.carolinarollergirls.scoreboard.core.Skater.Value;
import com.carolinarollergirls.scoreboard.core.impl.SkaterImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;

public class SkaterImplTests {

    private SkaterImpl skater;
    private Team teamMock;
    private UUID skaterId;

    @Before
    public void setup() {
        teamMock = Mockito.mock(Team.class);
        skaterId = UUID.randomUUID();

        skater = new SkaterImpl(teamMock, skaterId.toString(), "Test Skater", "123", "");
    }

    @Test
    public void add_penalty() {
	skater.set(Value.SORT_PENALTIES, false);
	Penalty p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");
	skater.set(Value.SORT_PENALTIES, true);

        assertEquals(1, skater.getAll(Skater.Child.PENALTY).size());
        assertNull(skater.get(Skater.Child.PENALTY, Skater.FO_EXP_ID));

        Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriod());
        assertEquals(3, penalty.getJam());

    }

    @Test
    public void add_penalty_with_id() {
	skater.set(Value.SORT_PENALTIES, false);
	Penalty p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.ID, "f03d5e2e-e581-4fcb-99c7-7fbd49101a36", Flag.FORCE);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");
	skater.set(Value.SORT_PENALTIES, true);

        assertEquals(1, skater.getAll(Skater.Child.PENALTY).size());
        assertNull(skater.get(Skater.Child.PENALTY, Skater.FO_EXP_ID));

        Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriod());
        assertEquals(3, penalty.getJam());

    }

    @Test
    public void add_ooo_penalty() {
	Penalty p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");

	p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 2);
	p.set(Penalty.Value.CODE, "P");

        assertEquals(2, skater.getAll(Skater.Child.PENALTY).size());

        Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");
        Skater.Penalty penaltytwo = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "2");

        assertEquals("P", penalty.getCode());
        assertEquals(1, penalty.getPeriod());
        assertEquals(2, penalty.getJam());

        assertEquals("C", penaltytwo.getCode());
        assertEquals(1, penaltytwo.getPeriod());
        assertEquals(3, penaltytwo.getJam());
    }

    @Test
    public void add_ooo_penalty_diff_period() {
	Penalty p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 2);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");

	p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "P");

        assertEquals(2, skater.getAll(Skater.Child.PENALTY).size());

        Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");
        Skater.Penalty penaltytwo = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "2");

        assertEquals("P", penalty.getCode());
        assertEquals(1, penalty.getPeriod());
        assertEquals(3, penalty.getJam());

        assertEquals("C", penaltytwo.getCode());
        assertEquals(2, penaltytwo.getPeriod());
        assertEquals(3, penaltytwo.getJam());
    }



    @Test
    public void remove_penalty() {
	Penalty p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 2);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");

	Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");

	skater.remove(Child.PENALTY, penalty);

        assertEquals(0, skater.getAll(Skater.Child.PENALTY).size());
    }

    @Test
    public void update_penalty() {
	Penalty p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");

	p = (Penalty)skater.get(Child.PENALTY, "0", true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 2);
	p.set(Penalty.Value.CODE, "P");

        Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");

	p.set(Penalty.Value.JAM, 4);
	p.set(Penalty.Value.CODE, "X");

        assertEquals(2, skater.getAll(Skater.Child.PENALTY).size());

        Skater.Penalty penaltyone = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "1");
        Skater.Penalty penaltytwo = (Skater.Penalty)skater.get(Skater.Child.PENALTY, "2");

        assertEquals("C", penaltyone.getCode());
        assertEquals(1, penaltyone.getPeriod());
        assertEquals(3, penaltyone.getJam());

        assertEquals("X", penaltytwo.getCode());
        assertEquals(1, penaltytwo.getPeriod());
        assertEquals(4, penaltytwo.getJam());

        assertEquals(penalty.getId(), penaltytwo.getId());
    }

    @Test
    public void add_fo_exp() {
	Penalty p = (Penalty)skater.get(Child.PENALTY, Skater.FO_EXP_ID, true);
	p.set(Penalty.Value.PERIOD, 1);
	p.set(Penalty.Value.JAM, 3);
	p.set(Penalty.Value.CODE, "C");

        assertEquals(1, skater.getAll(Skater.Child.PENALTY).size());
        assertNotNull(skater.get(Skater.Child.PENALTY, Skater.FO_EXP_ID));

        Skater.Penalty penalty = (Skater.Penalty)skater.get(Skater.Child.PENALTY, Skater.FO_EXP_ID);

        assertEquals("C", penalty.getCode());
        assertEquals(1, penalty.getPeriod());
        assertEquals(3, penalty.getJam());
    }
}
