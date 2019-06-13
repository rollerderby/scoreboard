package com.carolinarollergirls.scoreboard.jetty;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

public class WSTest {
    private WS.PathTrie pt;

    @Before
    public void setup() {
        pt = new WS.PathTrie();
    }

    @Test
    public void path_trie_simple() {
        pt.add("ScoreBoard.Period");

        assertTrue(pt.covers("ScoreBoard.Period"));
        assertTrue(pt.covers("ScoreBoard.Period(1)"));
        assertTrue(pt.covers("ScoreBoard.Period.Jam"));

        assertFalse(pt.covers("ScoreBoard.PeriodFoo"));
        assertFalse(pt.covers("ScoreBoard.Perioc"));
        assertFalse(pt.covers("ScoreBoard.Perioe"));
        assertFalse(pt.covers("ScoreBoard(1).Perioe"));
        assertFalse(pt.covers("ScoreBoard.Perio"));
        assertFalse(pt.covers("Scoreboard.Period"));
    }

    @Test
    public void path_trie_overlap() {
        pt.add("ScoreBoard.Period(1).Jam(1).StarPass");
        pt.add("ScoreBoard.Period");

        assertTrue(pt.covers("ScoreBoard.Period"));
        assertTrue(pt.covers("ScoreBoard.Period(1)"));
        assertTrue(pt.covers("ScoreBoard.Period(1).Jam(1).StarPass"));
    }

    @Test
    public void path_trie_overlap_glob() {
        pt.add("ScoreBoard.Period(1).Foo");
        pt.add("ScoreBoard.Period(*).Bar");

        assertTrue(pt.covers("ScoreBoard.Period(1).Foo"));
        assertTrue(pt.covers("ScoreBoard.Period(1).Bar"));
        assertTrue(pt.covers("ScoreBoard.Period(2).Bar"));

        assertFalse(pt.covers("ScoreBoard.Period(2).Foo"));
    }

    @Test
    public void path_trie_id_glob() {
        pt.add("ScoreBoard.Period(*).Jam(1).Foo(*).Bar");

        assertTrue(pt.covers("ScoreBoard.Period(1).Jam(1).Foo(2).Bar"));
        assertTrue(pt.covers("ScoreBoard.Period(1).Jam(1).Foo(2).Bar.Baz"));
        assertTrue(pt.covers("ScoreBoard.Period(1).Jam(1).Foo(2).Bar(zzz)"));

        assertFalse(pt.covers("ScoreBoard.Period"));
        assertFalse(pt.covers("ScoreBoard.Period("));
        assertFalse(pt.covers("ScoreBoard.Period(1)"));
        assertFalse(pt.covers("ScoreBoard.Period(1).Jam(2).Foo(2).Bar"));
        assertFalse(pt.covers("ScoreBoard.Period(1).Jam(1).Foo(2)"));
        assertFalse(pt.covers("ScoreBoard.Period(1).Jam(1).TeamJam(1).Foo(2)"));
    }

    @Test
    public void path_trie_non_id_glob() {
        pt.add("ScoreBoard.Period*");

        assertTrue(pt.covers("ScoreBoard.Period*"));

        assertFalse(pt.covers("ScoreBoard.Period*a"));
        assertFalse(pt.covers("ScoreBoard.Period"));
        assertFalse(pt.covers("ScoreBoard.Period("));
        assertFalse(pt.covers("ScoreBoard.Period(1).Jam(2).Foo(2).Bar"));
    }

    @Test
    public void path_trie_dot_in_id() {
        pt.add("ScoreBoard.Rulesets.Rule(Period.Duration)");
        pt.add("ScoreBoard.Rulesets.Rule(Jam.*)");
        pt.add("ScoreBoard.Rulesets.Rule(Intermission*)");
        
        assertTrue(pt.covers("ScoreBoard.Rulesets.Rule(Period.Duration)"));
        assertTrue(pt.covers("ScoreBoard.Rulesets.Rule(Jam.Foo)"));
        assertTrue(pt.covers("ScoreBoard.Rulesets.Rule(Jam.Foo.Bar)"));
        
        assertFalse(pt.covers("ScoreBoard.Rulesets.Rule(Period.Direction)"));
        assertFalse(pt.covers("ScoreBoard.Rulesets.Rule(Jam)"));
        assertFalse(pt.covers("ScoreBoard.Rulesets.Rule(Intermission.Direction)"));
    }
}
