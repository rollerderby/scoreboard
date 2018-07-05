package com.carolinarollergirls.scoreboard;

import static org.junit.Assert.*;

import org.junit.Test;

import com.carolinarollergirls.scoreboard.defaults.DefaultSkaterModel;
import com.carolinarollergirls.scoreboard.model.SkaterModel;

public class ExampleTest {

	@Test
	@SuppressWarnings("all")
	public void test() {
		assertTrue(1==1);
	}
	
	@Test
	public void test_using_a_crg_thing() {
		SkaterModel skater = new DefaultSkaterModel(null, "abc", "A B C","123","");
		assertEquals("Names Don't match","A B C",skater.getName());
	}

}
