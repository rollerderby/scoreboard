package com.carolinarollergirls.scoreboard.json;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.game.SkaterImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.prepared.RulesetsImpl;
import com.carolinarollergirls.scoreboard.json.JSONStateListener.StateTrie;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class ScoreBoardJSONListenerTests {

    private ScoreBoardImpl sb;
    private Game g;
    private String gameId;
    private JSONStateManager jsm;

    @org.junit.Rule
    public TemporaryFolder dir = new TemporaryFolder();
    private File oldDir;

    private StateTrie state;
    private JSONStateListener jsonListener = new JSONStateListener() {
        @Override
        public void sendUpdates(StateTrie s, StateTrie changed) {
            state = s;
        }
    };

    @Before
    public void setUp() throws Exception {
        oldDir = BasePath.get();
        BasePath.set(dir.getRoot());
        dir.newFolder("config", "penalties");
        Files.copy(oldDir.toPath().resolve("config/penalties/wftda2018.json"),
                   dir.getRoot().toPath().resolve("config/penalties/wftda2018.json"));
        dir.newFolder("html", "images", "teamlogo");
        dir.newFile("html/images/teamlogo/init.png");

        ScoreBoardClock.getInstance().stop();
        GameImpl.setQuickClockThreshold(0L);
        sb = new ScoreBoardImpl();
        sb.postAutosaveUpdate();
        g = sb.getCurrentGame().get(CurrentGame.GAME);
        gameId = g.getId();
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, Clock.ID_LINEUP);
        sb.getSettings().set(Clock.SETTING_SYNC, String.valueOf(false));

        jsm = new JSONStateManager();
        jsm.register(jsonListener);
        new ScoreBoardJSONListener(sb, jsm);
        advance(0);
    }

    @After
    public void tearDown() throws Exception {
        // Make sure events are still flowing through the ScoreBoardJSONListener.
        sb.getSettings().set("teardownTest", "foo");
        advance(0);
        assertEquals("foo", state.get("ScoreBoard.Settings.Setting(teardownTest)"));
        ScoreBoardClock.getInstance().start(false);
        GameImpl.setQuickClockThreshold(1000L);
        BasePath.set(oldDir);
    }

    private void advance(long time_ms) {
        ScoreBoardClock.getInstance().advance(time_ms);
        jsm.waitForSent();
    }

    @Test
    public void testScoreBoardEvents() {
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").InPeriod"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").InOvertime"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").OfficialScore"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").OfficialReview"));

        g.timeout();
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").InPeriod"));

        g.setInOvertime(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").InOvertime"));

        g.setOfficialReview(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").OfficialReview"));

        g.startJam();
        g.getClock(Clock.ID_PERIOD).setTime(0);
        g.stopJamTO();
        g.getClock(Clock.ID_INTERMISSION).setTime(0);
        g.startJam();
        g.getClock(Clock.ID_PERIOD).setTime(0);
        g.stopJamTO();
        g.setOfficialScore(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").OfficialScore"));
    }

    @Test
    public void testTeamEvents() {
        g.startJam();
        advance(0);

        g.getTeam("1").set(Team.TRIP_SCORE, 5);
        advance(0);
        assertEquals(0, state.get("ScoreBoard.Game(" + gameId + ").Team(1).LastScore"));
        assertEquals(5, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Score"));
        assertEquals(5, state.get("ScoreBoard.Game(" + gameId + ").Team(1).JamScore"));

        g.getTeam("1").set(Team.STAR_PASS, true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).StarPass"));

        g.getTeam("1").set(Team.LEAD, true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Lead"));

        g.timeout();
        g.setTimeoutType(g.getTeam(Team.ID_1), false);
        advance(1500);
        g.timeout();
        g.setTimeoutType(g.getTeam(Team.ID_1), true);
        g.getTeam(Team.ID_1).setRetainedOfficialReview(true);
        advance(0);
        assertEquals(2, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Timeouts"));
        assertEquals(1, state.get("ScoreBoard.Game(" + gameId + ").Team(1).OfficialReviews"));
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).RetainedOfficialReview"));
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).InOfficialReview"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").Team(1).InTimeout"));

        g.setTimeoutType(g.getTeam(Team.ID_1), false);
        advance(0);
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").Team(1).InOfficialReview"));
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).InTimeout"));

        g.getTeam("1").set(Team.TEAM_NAME, "ATeam");
        g.getTeam("1").set(Team.LEAGUE_NAME, "ALeague");
        g.getTeam("1").setLogo("ATeamLogo");
        advance(0);
        assertEquals("ALeague", state.get("ScoreBoard.Game(" + gameId + ").Team(1).Name"));
        assertEquals("ALeague - ATeam", state.get("ScoreBoard.Game(" + gameId + ").Team(1).FullName"));
        assertEquals("ATeam", state.get("ScoreBoard.Game(" + gameId + ").Team(1).TeamName"));
        assertEquals("ALeague", state.get("ScoreBoard.Game(" + gameId + ").Team(1).LeagueName"));
        assertEquals("ATeamLogo", state.get("ScoreBoard.Game(" + gameId + ").Team(1).Logo"));

        g.getTeam("1").setAlternateName("overlay", "AT");
        advance(0);
        assertEquals("AT", state.get("ScoreBoard.Game(" + gameId + ").Team(1).AlternateName(overlay)"));

        g.getTeam("1").setAlternateName("overlay", "AT");
        advance(0);
        assertEquals("AT", state.get("ScoreBoard.Game(" + gameId + ").Team(1).AlternateName(overlay)"));
        g.getTeam("1").removeAlternateName("overlay");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId + ").Team(1).AlternateName(overlay)"));

        g.getTeam("1").setColor("overlay", "red");
        advance(0);
        assertEquals("red", state.get("ScoreBoard.Game(" + gameId + ").Team(1).Color(overlay)"));
        g.getTeam("1").removeColor("overlay");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Color(overlay)"));
    }

    @Test
    public void testSkaterAndPositionEvents() {
        g.startJam();
        advance(0);

        String id = "00000000-0000-0000-0000-000000000001";

        Team t = g.getTeam("1");
        Skater s = t.getOrCreate(Team.SKATER, id);
        s.setName("Uno");
        s.setRosterNumber("01");
        advance(0);
        assertEquals("Uno", state.get("ScoreBoard.Game(" + gameId +
                                      ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
        assertEquals("01", state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).RosterNumber"));
        assertEquals("", state.get("ScoreBoard.Game(" + gameId +
                                   ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Flags"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
        assertEquals("Bench", state.get("ScoreBoard.Game(" + gameId +
                                        ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Role"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId +
                                      ").Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));

        t.removeSkater(id);
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).RosterNumber"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Flags"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Role"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));

        s = t.getOrCreate(Team.SKATER, id);
        s.setName("Uno");
        s.setRosterNumber("01");
        t.field(s, Role.JAMMER);
        s.setPenaltyBox(true);
        advance(0);
        assertEquals(
            gameId + "_1_Jammer",
            state.get("ScoreBoard.Game(" + gameId + ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
        assertEquals("Jammer", state.get("ScoreBoard.Game(" + gameId +
                                         ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Role"));
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
        assertEquals(id, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Position(Jammer).Skater"));
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Position(Jammer).PenaltyBox"));

        t.removeSkater(id); // should be inhibited after fielding them
        advance(0);
        assertEquals("Uno", state.get("ScoreBoard.Game(" + gameId +
                                      ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
        assertEquals("01", state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).RosterNumber"));
        assertEquals(id, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Position(Jammer).Skater"));
        assertEquals(true, state.get("ScoreBoard.Game(" + gameId + ").Team(1).Position(Jammer).PenaltyBox"));
    }

    @Test
    public void testPenaltyEvents() {
        String sid = "00000000-0000-0000-0000-000000000001";
        String pid;

        Team t = g.getTeam("1");
        Skater s = new SkaterImpl(t, sid);
        s.setName("Uno");
        s.setRosterNumber("01");
        t.addSkater(s);
        Penalty p = s.getOrCreate(Skater.PENALTY, "1");
        pid = p.getId();
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getOrCreate(Period.JAM, "2"));
        p.set(Penalty.CODE, "X");
        advance(0);
        assertEquals(pid, state.get("ScoreBoard.Game(" + gameId +
                                    ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Id"));
        assertEquals(1, state.get("ScoreBoard.Game(" + gameId +
                                  ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).PeriodNumber"));
        assertEquals(2, state.get("ScoreBoard.Game(" + gameId +
                                  ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).JamNumber"));
        assertEquals("X", state.get("ScoreBoard.Game(" + gameId +
                                    ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Code"));

        s.remove(Skater.PENALTY, "1");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Id"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).PeriodNumber"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).JamNumber"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Code"));

        p = s.getOrCreate(Skater.PENALTY, "0");
        p.set(Penalty.JAM, g.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.CODE, "B");
        advance(0);
        assertEquals(1, state.get("ScoreBoard.Game(" + gameId +
                                  ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).PeriodNumber"));
        assertEquals(2, state.get("ScoreBoard.Game(" + gameId +
                                  ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).JamNumber"));
        assertEquals("B", state.get("ScoreBoard.Game(" + gameId +
                                    ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).Code"));

        s.remove(Skater.PENALTY, "0");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).PeriodNumber"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).JamNumber"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId +
                                     ").Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).Code"));
    }

    @Test
    public void testStatsEvents() {
        String id = "00000000-0000-0000-0000-000000000001";

        Team t = g.getTeam("1");
        Skater s = new SkaterImpl(t, id);
        s.setName("Uno");
        s.setRosterNumber("01");
        t.addSkater(s);
        t.field(s, Role.JAMMER);
        g.startJam();
        advance(2000);

        assertEquals(0L, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).PeriodClockElapsedStart"));
        assertEquals(0, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).JamScore"));
        assertEquals(0, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).TotalScore"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).DisplayLead"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).StarPass"));
        assertEquals("00000000-0000-0000-0000-000000000001",
                     state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId +
                                      ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).PenaltyBox"));
        assertEquals(gameId + "_1_Jammer", state.get("ScoreBoard.Game(" + gameId +
                                                     ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Position"));

        t.field(s, Role.BENCH);
        advance(0);
        assertEquals(null,
                     state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId +
                                      ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).PenaltyBox"));
        assertEquals(gameId + "_1_Jammer", state.get("ScoreBoard.Game(" + gameId +
                                                     ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Position"));

        t.field(s, Role.JAMMER);
        advance(0);
        assertEquals("00000000-0000-0000-0000-000000000001",
                     state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        t.field(s, Role.BENCH);
        advance(0);
        assertEquals(null,
                     state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        assertEquals(false, state.get("ScoreBoard.Game(" + gameId +
                                      ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).PenaltyBox"));
        assertEquals(gameId + "_1_Jammer", state.get("ScoreBoard.Game(" + gameId +
                                                     ").Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Position"));

        g.stopJamTO();
        advance(1000);
        assertEquals(2000L, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).Duration"));
        assertEquals(2000L, state.get("ScoreBoard.Game(" + gameId + ").Period(1).Jam(1).PeriodClockElapsedEnd"));

        g.getClock(Clock.ID_PERIOD).setTime(0);
        g.getClock(Clock.ID_INTERMISSION).setTime(0);
        g.startJam(); // 1
        advance(1000);
        g.stopJamTO();
        g.startJam(); // 2
        advance(1000);
        g.stopJamTO();
        g.startJam(); // 3
        advance(1000);
        g.stopJamTO();
        g.startJam(); // 4
        advance(1000);
        g.stopJamTO();
        advance(1000);
        g.startJam(); // 5
        advance(1000);
        g.stopJamTO();
        advance(1000);
        assertEquals(3000L, state.get("ScoreBoard.Game(" + gameId + ").Period(2).Jam(4).PeriodClockElapsedStart"));
        assertEquals(5000L, state.get("ScoreBoard.Game(" + gameId + ").Period(2).Jam(5).PeriodClockElapsedStart"));
        // Remove a jam.
        g.getCurrentPeriod().remove(Period.JAM, "5");
        advance(0);
        assertEquals(3000L, state.get("ScoreBoard.Game(" + gameId + ").Period(2).Jam(4).PeriodClockElapsedStart"));
        assertEquals(null, state.get("ScoreBoard.Game(" + gameId + ").Period(2).Jam(5).PeriodClockElapsedStart"));
    }

    @Test
    public void testRulesetsEvents() {
        String rootId = RulesetsImpl.ROOT_ID;
        Rulesets.Ruleset rootRs = sb.getRulesets().getRuleset(rootId);
        String cid = "11111111-1111-1111-1111-111111111111";
        assertEquals(rootId, state.get("ScoreBoard.Game(" + gameId + ").Ruleset"));
        assertEquals("WFTDA", state.get("ScoreBoard.Game(" + gameId + ").RulesetName"));

        assertEquals("2", state.get("ScoreBoard.Game(" + gameId + ").Rule(Period.Number)"));
        assertEquals("Period.Number", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Name"));
        assertEquals("Number of periods", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Description"));
        assertEquals("Integer", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Type"));
        assertEquals(0, state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Index"));

        assertEquals("Boolean", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).Type"));
        assertEquals(2, state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).Index"));
        assertEquals("Count Down", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).TrueValue"));
        assertEquals("Count Up", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).FalseValue"));

        sb.getRulesets().addRuleset("child", rootRs, cid);
        advance(0);
        assertEquals(cid, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Id"));
        assertEquals(rootId, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Parent"));
        assertEquals("child", state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Name"));
        assertEquals(
            null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Rule(Period.Number)"));
        sb.getRulesets().getRuleset(cid).add(Ruleset.RULE, new ValWithId(Rule.NUMBER_PERIODS.toString(), "3"));
        advance(0);
        assertEquals(
            "3", state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Rule(Period.Number)"));

        g.setRuleset(sb.getRulesets().getRuleset(cid));
        advance(0);
        assertEquals(cid, state.get("ScoreBoard.Game(" + gameId + ").Ruleset"));
        assertEquals("child", state.get("ScoreBoard.Game(" + gameId + ").RulesetName"));
        assertEquals("3", state.get("ScoreBoard.Game(" + gameId + ").Rule(Period.Number)"));

        sb.getRulesets().removeRuleset(cid);
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Id"));
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).ParentId"));
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Name"));
        assertEquals(
            null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Rule(Period.Number)"));
    }

    @Test
    public void testMediaEvents() throws Exception {
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(fullscreen)"));
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo)"));
        assertEquals("init.png", state.get("ScoreBoard.Media.Format(images).Type(teamlogo).File(init.png).Id"));
        assertEquals("init", state.get("ScoreBoard.Media.Format(images).Type(teamlogo).File(init.png).Name"));
        assertEquals("/images/teamlogo/init.png",
                     state.get("ScoreBoard.Media.Format(images).Type(teamlogo).File(init.png).Src"));

        sb.getMedia().removeMediaFile("images", "teamlogo", "init.png");
        dir.newFile("html/images/fullscreen/new.png");

        Thread.sleep(100);
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo.File(init.png).Id"));
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo.File(init.png).Name"));
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo.File(init.png).Src"));
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo)"));
        assertEquals("new.png", state.get("ScoreBoard.Media.Format(images).Type(fullscreen).File(new.png).Id"));
        assertEquals("new", state.get("ScoreBoard.Media.Format(images).Type(fullscreen).File(new.png).Name"));
        assertEquals("/images/fullscreen/new.png",
                     state.get("ScoreBoard.Media.Format(images).Type(fullscreen).File(new.png).Src"));
    }
}
