package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.Skater;
import com.carolinarollergirls.scoreboard.model.TeamModel;

public class DefaultSkaterModelTests {

	private DefaultSkaterModel model;
	private TeamModel teamMock;
	private UUID skaterId;
	
	@Before
	public void setup() {
		teamMock = Mockito.mock(TeamModel.class);
		skaterId = UUID.randomUUID();
		
		model = new DefaultSkaterModel(teamMock, skaterId.toString(), "Test Skater","123","");
	}
	
	@Test
	public void add_penalty() {
		model.AddPenaltyModel(null, false, 1, 3, "C");
		
		assertEquals(1, model.getPenalties().size());
		assertNull(model.getFOEXPPenalty());
		
		Skater.Penalty penalty = model.getPenalties().get(0);
		
		assertEquals("C", penalty.getCode());
		assertEquals(1, penalty.getPeriod());
		assertEquals(3, penalty.getJam());
		
	}
	
	@Test
	public void add_ooo_penalty() {
		model.AddPenaltyModel(null, false, 1, 3, "C");
		model.AddPenaltyModel(null, false, 1, 2, "P");
		
		assertEquals(2, model.getPenalties().size());
		
		Skater.Penalty penalty = model.getPenalties().get(0);
		Skater.Penalty penaltytwo = model.getPenalties().get(1);
		
		assertEquals("P", penalty.getCode());
		assertEquals(1, penalty.getPeriod());
		assertEquals(2, penalty.getJam());
		
		assertEquals("C", penaltytwo.getCode());
		assertEquals(1, penaltytwo.getPeriod());
		assertEquals(3, penaltytwo.getJam());
	}
	
	@Test
	public void add_ooo_penalty_diff_period() {
		model.AddPenaltyModel(null, false, 2, 3, "C");
		model.AddPenaltyModel(null, false, 1, 3, "P");
		
		assertEquals(2, model.getPenalties().size());
		
		Skater.Penalty penalty = model.getPenalties().get(0);
		Skater.Penalty penaltytwo = model.getPenalties().get(1);
		
		assertEquals("P", penalty.getCode());
		assertEquals(1, penalty.getPeriod());
		assertEquals(3, penalty.getJam());
		
		assertEquals("C", penaltytwo.getCode());
		assertEquals(2, penaltytwo.getPeriod());
		assertEquals(3, penaltytwo.getJam());
	}
	
	
	
	@Test
	public void remove_penalty() {
		model.AddPenaltyModel(null, false, 2, 3, "C");
		Skater.Penalty penalty = model.getPenalties().get(0);
		
		model.AddPenaltyModel(penalty.getId(), false, 0, 0, null);
		
		
		assertEquals(0, model.getPenalties().size());
	}
	
	@Test
	public void update_penalty() {
		model.AddPenaltyModel(null, false, 1, 3, "C");
		model.AddPenaltyModel(null, false, 1, 2, "P");
		
		Skater.Penalty penalty = model.getPenalties().get(0);
		
		model.AddPenaltyModel(penalty.getId(), false, 1, 4, "X");
		
		assertEquals(2, model.getPenalties().size());
		
		Skater.Penalty penaltyone = model.getPenalties().get(0);
		Skater.Penalty penaltytwo = model.getPenalties().get(1);
		
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
		model.AddPenaltyModel(null, true, 1, 3, "C");
		
		assertEquals(0, model.getPenalties().size());
		assertNotNull(model.getFOEXPPenalty());
		
		Skater.Penalty penalty = model.getFOEXPPenalty();
		
		assertEquals("C", penalty.getCode());
		assertEquals(1, penalty.getPeriod());
		assertEquals(3, penalty.getJam());
	}
}
