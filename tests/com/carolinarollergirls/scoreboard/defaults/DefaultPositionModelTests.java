package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.Position;
import com.carolinarollergirls.scoreboard.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.model.PositionModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.SkaterModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;

public class DefaultPositionModelTests {
	private final String firstId = "662caf51-17da-4ef2-8f01-a6d7e1c30d56";
	private final String secondId = "91a6c3f5-8258-46c7-a845-62c21b6e37ca";
	private final String thirdId = "5df0a35b-aaa6-4c30-93ef-07ba4f174cdc";
	
	private ScoreBoardModel sbModelMock;
	private Ruleset ruleMock;
	private TeamModel teamModel;
	private SkaterModel first;
	private SkaterModel second;
	private SkaterModel third;
	
	@Before
	public void setup() {
		sbModelMock = Mockito.mock(DefaultScoreBoardModel.class);
		
		ruleMock = Mockito.mock(Ruleset.class);
		
		Mockito
			.when(sbModelMock.getScoreBoard())
			.thenReturn(sbModelMock);
		
		Mockito
			.when(sbModelMock._getRuleset())
			.thenReturn(ruleMock);
		
		teamModel = new DefaultTeamModel(sbModelMock, "A");
		
		first = new DefaultSkaterModel(teamModel, firstId, "First","123", "");
		second = new DefaultSkaterModel(teamModel, secondId, "Second","456", "");
		third = new DefaultSkaterModel(teamModel, thirdId, "Third","789","");
		
		teamModel.addSkaterModel(first);
		teamModel.addSkaterModel(second);
		teamModel.addSkaterModel(third);
	}
	
	@Test
	public void key_values_populated() {
		PositionModel blocker = teamModel.getPositionModel(Position.ID_BLOCKER1);

		assertSame(blocker.getId(),Position.ID_BLOCKER1);
		assertSame(blocker.getProviderName(), "Position");
		assertSame(blocker.getProviderId(), Position.ID_BLOCKER1);
		assertSame(blocker.getProviderClass(), Position.class);
		assertSame(blocker.getTeam(), teamModel);
	}
	
	@Test
	public void make_skater_jammer_via_position() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setSkaterModel(firstId);
		
		assertSame(jammer.getSkaterModel(), first);
		assertSame(first.getPosition(),Position.ID_JAMMER);
	}
	
	@Test
	public void make_skater_jammer_via_skater() {
		first.setPosition(Position.ID_JAMMER);
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		
		assertSame(jammer.getSkaterModel(), first);
		assertSame(first.getPosition(), Position.ID_JAMMER);
	}
	
	@Test
	public void position_knows_skater_penalty() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setSkaterModel(firstId);
		first.setPenaltyBox(true);
		
		assertTrue(jammer.getPenaltyBox());
	}
	
	@Test
	public void skater_knows_position_penalty() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setSkaterModel(firstId);
		jammer.setPenaltyBox(true);
		
		assertTrue(first.isPenaltyBox());
	}
	
	@Test
	public void doesnt_set_penalty_with_no_skater() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setPenaltyBox(true);
		
		assertFalse(jammer.getPenaltyBox());
	}
	
	@Test
	public void sp_works() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		PositionModel pivot = teamModel.getPositionModel(Position.ID_PIVOT);
		jammer.setSkaterModel(firstId);
		pivot.setSkaterModel(firstId);
		
		assertSame(pivot.getSkaterModel(), first);
		assertNull(jammer.getSkaterModel());
	}
	
	@Test
	public void clears_with_empty_model_id() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setSkaterModel(firstId);
		jammer.setSkaterModel("");
		
		assertNull(jammer.getSkaterModel());
	}
	
	@Test
	public void clears_with_null_model_id() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setSkaterModel(firstId);
		jammer.setSkaterModel(null);
		
		assertNull(jammer.getSkaterModel());
	}
	
	
	@Test(expected = SkaterNotFoundException.class)
	public void throws_with_bogus_skater_id() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		jammer.setSkaterModel("bogus");
	}
	
	@Test
	public void position_knows_penalty_after_sp() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		PositionModel pivot = teamModel.getPositionModel(Position.ID_PIVOT);
		pivot.setSkaterModel(firstId);
		pivot.setPenaltyBox(true);
		first.setPosition(Position.ID_JAMMER);
		
		assertTrue(jammer.getPenaltyBox());
		assertFalse(pivot.getPenaltyBox());
		assertTrue(first.isPenaltyBox());
	}
	
	@Test
	public void position_knows_penalty_after_sp_position() {
		PositionModel jammer = teamModel.getPositionModel(Position.ID_JAMMER);
		PositionModel pivot = teamModel.getPositionModel(Position.ID_PIVOT);
		pivot.setSkaterModel(firstId);
		pivot.setPenaltyBox(true);
		jammer.setSkaterModel(firstId);
		
		assertTrue(jammer.getPenaltyBox());
		assertFalse(pivot.getPenaltyBox());
		assertTrue(first.isPenaltyBox());
	}
	
	
	

}
