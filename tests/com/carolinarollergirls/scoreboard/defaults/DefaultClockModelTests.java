package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.Settings;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public class DefaultClockModelTests {

	private ScoreBoardModel sbModelMock;
	private Ruleset ruleMock;
	private Settings settingsMock;
	
	
	private DefaultClockModel clock;
	private static String ID = "TEST";
	
	private boolean syncStatus = false;
	
	@Before
	public void setUp() throws Exception {
		syncStatus = false;
		sbModelMock = Mockito.mock(DefaultScoreBoardModel.class);
		
		ruleMock = Mockito.mock(Ruleset.class);
		settingsMock = Mockito.mock(Settings.class);
		
		Mockito
			.when(sbModelMock.getScoreBoard())
			.thenReturn(sbModelMock);
		
		Mockito
			.when(sbModelMock._getRuleset())
			.thenReturn(ruleMock);
		
		Mockito
			.when(sbModelMock.getSettings())
			.thenReturn(settingsMock);
		
		// makes it easier to test both sync and non-sync paths through clock model
		Mockito
			.when(settingsMock.getBoolean("Scoreboard.Clock.Sync"))
			.thenAnswer(new Answer<Boolean>() {
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					return syncStatus;
				}
			});
		
		clock = new DefaultClockModel(sbModelMock, ID);
	}
	
	@Test
	public void test_defaults() {
		assertEquals(0, clock.getMinimumNumber());
		assertEquals(0, clock.getMaximumNumber());
		assertEquals(0, clock.getNumber());
		
		assertEquals(ID, clock.getId());
		assertEquals(null, clock.getName());
		assertFalse(clock.isMasterClock());
		assertFalse(clock.isCountDirectionDown());
		assertFalse(clock.isRunning());

		assertEquals(clock, clock.getClock());
		assertEquals(sbModelMock, clock.getScoreBoard());
		assertEquals(sbModelMock, clock.getScoreBoardModel());
		
		assertEquals("Clock", clock.getProviderName());
		assertEquals(ID, clock.getProviderId());
		assertEquals(Clock.class, clock.getProviderClass());
	}
	
	@Test
	public void syncTimeTest() {
		
	}

	
	@Test
	public void test_name() {
		clock.setName("Test Clock");
		
		assertEquals("Test Clock", clock.getName());
		//TODO: Test Event system
	}
	
	@Test
	public void test_min_number_setter() {
		clock.setMinimumNumber(1);
		
		// validate constraint: max > min
		assertEquals(1, clock.getMinimumNumber());
		assertEquals(1, clock.getMaximumNumber());
		assertEquals(1, clock.getNumber());

	}
	
	@Test
	public void test_min_number_setter2() {
		clock.setMinimumNumber(10);
		clock.setMinimumNumber(5);
		
		// validate constraint: number is automatically set to max
		assertEquals(5, clock.getMinimumNumber());
		assertEquals(10, clock.getMaximumNumber());
		assertEquals(10, clock.getNumber());

	}
	
	@Test
	public void test_max_number_setter() {
		clock.setMaximumNumber(5);
		
		
		assertEquals(0, clock.getMinimumNumber());
		assertEquals(5, clock.getMaximumNumber());
		assertEquals(0, clock.getNumber());
	}
	
	@Test
	public void test_max_number_setter2() {
		clock.setMinimumNumber(10);
		clock.setMaximumNumber(5);
		
		// validate constraint: cannot set a max that is < min
		assertEquals(10, clock.getMinimumNumber());
		assertEquals(10, clock.getMaximumNumber());
		assertEquals(10, clock.getNumber());
	}
	
	@Test
	public void test_max_number_change() {
		clock.setMaximumNumber(5);
		clock.changeMaximumNumber(2);
		
		assertEquals(0, clock.getMinimumNumber());
		assertEquals(7, clock.getMaximumNumber());
		assertEquals(0, clock.getNumber());
	}
	
	@Test
	public void test_min_number_change() {
		clock.setMinimumNumber(5);
		clock.changeMinimumNumber(2);
		
		assertEquals(7, clock.getMinimumNumber());
		assertEquals(7, clock.getMaximumNumber());
		assertEquals(7, clock.getNumber());
	}
	
	@Test
	public void test_number_changing() {
		clock.setMaximumNumber(12);
		clock.setMinimumNumber(3);
		
		clock.setNumber(5);
		assertEquals(5, clock.getNumber());
		
		clock.changeNumber(3);
		assertEquals(8, clock.getNumber());
		
		// validate constraint: cannot set number above maximum
		clock.setNumber(23);
		assertEquals(12, clock.getNumber());
		
		// validate constraint: cannot set number below minimum
		clock.setNumber(-2);
		assertEquals(3, clock.getNumber());
		
		// ...and check that constraint is not a >0 type constraint
		clock.setNumber(1);
		assertEquals(3, clock.getNumber());
		
		clock.changeNumber(6);
		assertEquals(9, clock.getNumber());
		
		// validate constraint: cannot changeNumber above maximum
		clock.changeNumber(6);
		assertEquals(12, clock.getNumber());
		
		
		clock.setNumber(5);
		clock.changeNumber(-1);
		assertEquals(4, clock.getNumber());
		
		// validate constraint: cannot changeNumber below minimum
		clock.changeNumber(-4);
		assertEquals(3, clock.getNumber());
	}
	
	@Test
	public void test_min_time_setter() {
		clock.setMinimumTime(1000);
		
		// validate constraint: max > min
		assertEquals(1000, clock.getMinimumTime());
		assertEquals(1000, clock.getMaximumTime());
		assertEquals(1000, clock.getTime());
		
		clock.setMinimumTime(500);
		

	}
	
	@Test
	public void test_min_time_setter2() {
		clock.setMinimumTime(2000);
		clock.setMinimumTime(1000);
		
		// validate constraint: reducing min time doesn't reset max or current time
		assertEquals(1000, clock.getMinimumTime());
		assertEquals(2000, clock.getMaximumTime());
		assertEquals(2000, clock.getTime());

	}
	
	@Test
	public void test_min_time_setter3() {
		clock.setMaximumTime(2000);
		clock.setMinimumTime(1000);
		
		// validate constraint: time cannot be less than min time
		assertEquals(1000, clock.getMinimumTime());
		assertEquals(2000, clock.getMaximumTime());
		assertEquals(1000, clock.getTime());

	}
	
	@Test
	public void test_max_time_setter() {
		clock.setMaximumTime(5000);
		
		// validate constraint: increase max time doesn't reset min or current time
		assertEquals(0, clock.getMinimumTime());
		assertEquals(5000, clock.getMaximumTime());
		assertEquals(0, clock.getTime());
	}
	
	@Test
	public void test_max_time_setter2() {
		clock.setMinimumTime(2000);
		clock.setMaximumTime(1000);
		
		// validate constraint: cannot set a max that is < min
		assertEquals(2000, clock.getMinimumTime());
		assertEquals(2000, clock.getMaximumTime());
		assertEquals(2000, clock.getTime());
	} 
	
	@Test
	public void test_max_time_change() {
		clock.setMaximumTime(1000);
		clock.changeMaximumTime(2000);
		
		assertEquals(0, clock.getMinimumTime());
		assertEquals(3000, clock.getMaximumTime());
		assertEquals(0, clock.getTime());
	}
	
	@Test
	public void test_min_time_change() {
		clock.setMinimumTime(5000);
		clock.changeMinimumTime(2000);
		
		assertEquals(7000, clock.getMinimumTime());
		assertEquals(7000, clock.getMaximumTime());
		assertEquals(7000, clock.getTime());
	}
	
	@Test
	public void test_time_changing() {
		clock.setMaximumTime(5000);
		clock.setMinimumTime(1000);
		
		clock.setTime(2000);
		assertEquals(2000, clock.getTime());
		assertEquals(3000, clock.getInvertedTime());
		
		clock.setTime(6000);
		assertEquals(5000, clock.getTime());
		assertEquals(0, clock.getInvertedTime());
		
		clock.setTime(400);
		assertEquals(1000, clock.getTime());
		assertEquals(4000, clock.getInvertedTime());
		
		clock.setTime(600);
		assertEquals(600, clock.getTime());
		assertEquals(4400, clock.getInvertedTime());
		//TODO: validate that event DOES NOT fire in this case
		
		clock.setTime(2000);
		clock.changeTime(1200);
		assertEquals(3200, clock.getTime());
		assertEquals(1800, clock.getInvertedTime());
		//TODO: validate that events fire in this case
		
		clock.changeTime(-5000);
		assertEquals(1000, clock.getTime());
		assertEquals(4000, clock.getInvertedTime());
		
		clock.changeTime(4100);
		assertEquals(5100, clock.getTime());
		assertEquals(-100, clock.getInvertedTime());
	}
	
	
	@Test
	public void test_time_elapse_count_up()
	{
		clock.setMaximumTime(5000);
		
		clock.setTime(2000);
		assertEquals(2000, clock.getTimeElapsed());
		assertEquals(3000, clock.getTimeRemaining());

		clock.elapseTime(1000);
		assertEquals(3000, clock.getTime());
		assertEquals(3000, clock.getTimeElapsed());
		assertEquals(2000, clock.getTimeRemaining());
	}

	@Test
	public void test_time_elapse_count_down()
	{
		clock.setCountDirectionDown(true);
		clock.setMaximumTime(5000);
		
		clock.setTime(2000);
		assertEquals(3000, clock.getTimeElapsed());
		assertEquals(2000, clock.getTimeRemaining());

		clock.elapseTime(1000);
		assertEquals(1000, clock.getTime());
		assertEquals(4000, clock.getTimeElapsed());
		assertEquals(1000, clock.getTimeRemaining());
	}
	
	@Test
	public void test_time_at_start_count_up()
	{
		clock.setMaximumTime(5000);
		
		assertTrue(clock.isTimeAtStart());
		assertFalse(clock.isTimeAtEnd());
		
		clock.setTime(2000);
		
		assertFalse(clock.isTimeAtStart());
		assertFalse(clock.isTimeAtEnd());
		
		clock.setTime(5000);
		
		assertFalse(clock.isTimeAtStart());
		assertTrue(clock.isTimeAtEnd());
	}
	
	@Test
	public void test_time_at_start_count_down()
	{
		clock.setCountDirectionDown(true);
		clock.setMaximumTime(5000);
		clock.setTime(5000);
		
		assertTrue(clock.isTimeAtStart());
		assertFalse(clock.isTimeAtEnd());
		
		clock.setTime(2000);
		
		assertFalse(clock.isTimeAtStart());
		assertFalse(clock.isTimeAtEnd());
		
		clock.setTime(0);
		
		assertFalse(clock.isTimeAtStart());
		assertTrue(clock.isTimeAtEnd());
	}
	
	@Test
	public void test_reset_time()
	{
		clock.setMaximumTime(5000);
		clock.setMinimumTime(1000);
		
		clock.setTime(3000);
		
		clock.resetTime();
		
		assertEquals(1000, clock.getTime());
		
		clock.setTime(3000);
		clock.setCountDirectionDown(true);
		
		clock.resetTime();
		
		assertEquals(5000, clock.getTime());
	}
	
	@Test
	public void test_apply_rules_name()
	{
		clock.applyRule("Clock.TEST.Name", "New Name");
		assertEquals("New Name", clock.getName());
		
		clock.applyRule("Clock.OTHER.Name", "Shouldn't Change");
		assertEquals("New Name", clock.getName());
	}
	
	@Test
	public void test_apply_rules_direction()
	{
		clock.applyRule("Clock." + ID + ".Direction", true);
		assertTrue(clock.isCountDirectionDown());
		
		clock.applyRule("Clock.OTHER.Direction", false);
		assertTrue(clock.isCountDirectionDown());
	}
	
	@Test
	public void test_apply_rules_min_number()
	{
		clock.applyRule("Clock." + ID + ".MinimumNumber", 10);
		assertEquals(10, clock.getMinimumNumber());
		assertEquals(10, clock.getMaximumNumber());
		assertEquals(10, clock.getNumber());

		
		clock.applyRule("Clock.OTHER.MaximumNumber", 20);
		assertEquals(10, clock.getMinimumNumber());
		assertEquals(10, clock.getMaximumNumber());
		assertEquals(10, clock.getNumber());
	}
	
	@Test
	public void test_apply_rules_max_number()
	{
		clock.applyRule("Clock." + ID + ".MaximumNumber", 10);
		assertEquals(0, clock.getMinimumNumber());
		assertEquals(10, clock.getMaximumNumber());
		assertEquals(0, clock.getNumber());

		
		clock.applyRule("Clock.OTHER.MaximumNumber", 20);
		assertEquals(0, clock.getMinimumNumber());
		assertEquals(10, clock.getMaximumNumber());
		assertEquals(0, clock.getNumber());
	}
	
	@Test
	public void test_apply_rules_min_time()
	{
		clock.applyRule("Clock." + ID + ".MinimumTime", (long)10000);
		assertEquals(10000, clock.getMinimumTime());
		assertEquals(10000, clock.getMaximumTime());
		assertEquals(10000, clock.getTime());

		
		clock.applyRule("Clock.OTHER.MinimumTime", (long)20000);
		assertEquals(10000, clock.getMinimumTime());
		assertEquals(10000, clock.getMaximumTime());
		assertEquals(10000, clock.getTime());
	}
	
	@Test
	public void test_apply_rules_max_time()
	{
		clock.applyRule("Clock." + ID + ".MaximumTime", (long)10000);
		assertEquals(0, clock.getMinimumTime());
		assertEquals(10000, clock.getMaximumTime());
		assertEquals(0, clock.getTime());

		
		clock.applyRule("Clock.OTHER.MaximumTime", (long)20000);
		assertEquals(0, clock.getMinimumTime());
		assertEquals(10000, clock.getMaximumTime());
		assertEquals(0, clock.getTime());
	}
}
