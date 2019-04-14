package com.carolinarollergirls.scoreboard.json;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Skater.NChild;
import com.carolinarollergirls.scoreboard.core.Team.Value;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.impl.RulesetsImpl;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class ScoreBoardJSONListenerTests {

    private ScoreBoardImpl sb;
    private JSONStateManager jsm;

    @org.junit.Rule
    public TemporaryFolder dir = new TemporaryFolder();
    private File oldDir;

    private Map<String, Object> state;
    private JSONStateListener jsonListener = new JSONStateListener() {
        @Override
        public void sendUpdates(Map<String, Object> s, Set<String> changed) {
            state = s;
        }
    };

    @Before
    public void setUp() throws Exception {
        oldDir = ScoreBoardManager.getDefaultPath();
        ScoreBoardManager.setDefaultPath(dir.getRoot());
        dir.newFolder("config", "penalties");
        Files.copy(oldDir.toPath().resolve("config/penalties/wftda2018.json"),
                dir.getRoot().toPath().resolve("config/penalties/wftda2018.json"));
        dir.newFolder("html", "images", "teamlogo");
        dir.newFile("html/images/teamlogo/init.png");

        ScoreBoardClock.getInstance().stop();
        sb = new ScoreBoardImpl();

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
        ScoreBoardManager.setDefaultPath(oldDir);
    }

    private void advance(long time_ms) {
        ScoreBoardClock.getInstance().advance(time_ms);
        jsm.waitForSent();
    }

    @Test
    public void testScoreBoardEvents() {
        assertEquals(false, state.get("ScoreBoard.InPeriod"));
        assertEquals(false, state.get("ScoreBoard.InOvertime"));
        assertEquals(false, state.get("ScoreBoard.OfficialScore"));
        assertEquals(false, state.get("ScoreBoard.OfficialReview"));

        sb.setInPeriod(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.InPeriod"));

        sb.setInOvertime(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.InOvertime"));

        sb.setOfficialReview(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.OfficialReview"));

        sb.startJam();
        sb.getClock(Clock.ID_PERIOD).setTime(0);
        sb.stopJamTO();
        sb.getClock(Clock.ID_INTERMISSION).setTime(0);
        sb.startJam();
        sb.getClock(Clock.ID_PERIOD).setTime(0);
        sb.stopJamTO();
        sb.setOfficialScore(true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.OfficialScore"));
    }

    @Test
    public void testTeamEvents() {
        sb.startJam();
        advance(0);

        sb.getTeam("1").set(Value.TRIP_SCORE, 5);
        advance(0);
        assertEquals(0, state.get("ScoreBoard.Team(1).LastScore"));
        assertEquals(5, state.get("ScoreBoard.Team(1).Score"));
        assertEquals(5, state.get("ScoreBoard.Team(1).JamScore"));

        sb.getTeam("1").set(Value.STAR_PASS, true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Team(1).StarPass"));

        sb.getTeam("1").set(Value.LEAD, true);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Team(1).Lead"));

        sb.getTeam("1").setTimeouts(2);
        sb.getTeam("1").setOfficialReviews(1);
        sb.getTeam("1").setRetainedOfficialReview(true);
        advance(0);
        assertEquals(2, state.get("ScoreBoard.Team(1).Timeouts"));
        assertEquals(1, state.get("ScoreBoard.Team(1).OfficialReviews"));
        assertEquals(true, state.get("ScoreBoard.Team(1).RetainedOfficialReview"));

        sb.getTeam("1").setInOfficialReview(true);
        sb.getTeam("1").setInTimeout(false);
        advance(0);
        assertEquals(true, state.get("ScoreBoard.Team(1).InOfficialReview"));
        assertEquals(false, state.get("ScoreBoard.Team(1).InTimeout"));

        sb.getTeam("1").setInOfficialReview(false);
        sb.getTeam("1").setInTimeout(true);
        advance(0);
        assertEquals(false, state.get("ScoreBoard.Team(1).InOfficialReview"));
        assertEquals(true, state.get("ScoreBoard.Team(1).InTimeout"));

        sb.getTeam("1").setName("ATeam");
        sb.getTeam("1").setLogo("ATeamLogo");
        advance(0);
        assertEquals("ATeam", state.get("ScoreBoard.Team(1).Name"));
        assertEquals("ATeamLogo", state.get("ScoreBoard.Team(1).Logo"));

        sb.getTeam("1").setAlternateName("overlay", "AT");
        advance(0);
        assertEquals("AT", state.get("ScoreBoard.Team(1).AlternateName(overlay).Name"));

        sb.getTeam("1").setAlternateName("overlay", "AT");
        advance(0);
        assertEquals("AT", state.get("ScoreBoard.Team(1).AlternateName(overlay).Name"));
        sb.getTeam("1").removeAlternateName("overlay");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Team(1).AlternateName(overlay).Name"));

        sb.getTeam("1").setColor("overlay", "red");
        advance(0);
        assertEquals("red", state.get("ScoreBoard.Team(1).Color(overlay).Color"));
        sb.getTeam("1").removeColor("overlay");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Team(1).Color(overlay).Color"));
    }

    @Test
    public void testSkaterAndPositionEvents() {
        sb.startJam();
        advance(0);

        String id = "00000000-0000-0000-0000-000000000001";

        sb.getTeam("1").addSkater(id, "Uno", "01", "");
        advance(0);
        assertEquals("Uno", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
        assertEquals("01", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Number"));
        assertEquals("", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Flags"));
        assertEquals("", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
        assertEquals("Bench", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Role"));
        assertEquals(false, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));

        sb.getTeam("1").field(sb.getTeam("1").getSkater(id), Role.JAMMER);
        sb.getTeam("1").getSkater(id).setPenaltyBox(true);
        advance(0);
        assertEquals("1_Jammer", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
        assertEquals("Jammer", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Role"));
        assertEquals(true, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
        assertEquals("00000000-0000-0000-0000-000000000001", state.get("ScoreBoard.Team(1).Position(Jammer).Skater"));
        assertEquals(true, state.get("ScoreBoard.Team(1).Position(Jammer).PenaltyBox"));

        sb.getTeam("1").removeSkater(id);
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Number"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Flags"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Role"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Position(Jammer).Skater"));
        assertEquals(true, state.get("ScoreBoard.Team(1).Position(Jammer).PenaltyBox"));
    }

    @Test
    public void testPenaltyEvents() {
        String sid = "00000000-0000-0000-0000-000000000001";
        String pid;

        sb.getTeam("1").addSkater(sid, "Uno", "01", "");
        Skater s = sb.getTeam("1").getSkater(sid);
        Penalty p = (Penalty)s.getOrCreate(NChild.PENALTY, "1");
        pid = p.getId();
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getOrCreate(Period.NChild.JAM, "2"));
        p.set(Penalty.Value.CODE, "X");
        advance(0);
        assertEquals(pid, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Id"));
        assertEquals(1, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).PeriodNumber"));
        assertEquals(2, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).JamNumber"));
        assertEquals("X", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Code"));

        s.remove(NChild.PENALTY, "1");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Id"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).PeriodNumber"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).JamNumber"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Code"));

        p = (Penalty)s.getOrCreate(NChild.PENALTY, "0");
        p.set(Penalty.Value.JAM, sb.getOrCreatePeriod(1).getJam(2));
        p.set(Penalty.Value.CODE, "B");
        advance(0);
        assertEquals(1, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).PeriodNumber"));
        assertEquals(2, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).JamNumber"));
        assertEquals("B", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).Code"));

        s.remove(NChild.PENALTY, "0");
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).PeriodNumber"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).JamNumber"));
        assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(0).Code"));
    }

    @Test
    public void testStatsEvents() {
        String id = "00000000-0000-0000-0000-000000000001";

        sb.getTeam("1").addSkater(id, "Uno", "01", "");
        sb.getTeam("1").field(sb.getTeam("1").getSkater(id), Role.JAMMER);
        sb.startJam();
        advance(2000);

        assertEquals(0L, state.get("ScoreBoard.Period(1).Jam(1).PeriodClockElapsedStart"));
        assertEquals(0, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).JamScore"));
        assertEquals(0, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).TotalScore"));
        assertEquals(false, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).DisplayLead"));
        assertEquals(false, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).StarPass"));
        assertEquals("00000000-0000-0000-0000-000000000001", state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        assertEquals(false, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).PenaltyBox"));
        assertEquals("1_Jammer", state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Position"));

        sb.getTeam("1").field(sb.getTeam("1").getSkater(id), Role.BENCH);
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        assertEquals(false, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).PenaltyBox"));
        assertEquals("1_Jammer", state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Position"));

        sb.getTeam("1").field(sb.getTeam("1").getSkater(id), Role.JAMMER);
        advance(0);
        assertEquals("00000000-0000-0000-0000-000000000001", state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        sb.getTeam("1").removeSkater(id);
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Skater"));
        assertEquals(false, state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).PenaltyBox"));
        assertEquals("1_Jammer", state.get("ScoreBoard.Period(1).Jam(1).TeamJam(1).Fielding(Jammer).Position"));

        sb.stopJamTO();
        advance(1000);
        assertEquals(2000L, state.get("ScoreBoard.Period(1).Jam(1).Duration"));
        assertEquals(2000L, state.get("ScoreBoard.Period(1).Jam(1).PeriodClockElapsedEnd"));

        sb.getClock(Clock.ID_PERIOD).setTime(0);
        sb.getClock(Clock.ID_INTERMISSION).setTime(0);
        sb.startJam(); //1
        advance(1000);
        sb.stopJamTO();
        sb.startJam(); //2
        advance(1000);
        sb.stopJamTO();
        sb.startJam(); //3
        advance(1000);
        sb.stopJamTO();
        sb.startJam(); //4
        advance(1000);
        sb.stopJamTO();
        advance(1000);
        sb.startJam(); //5
        advance(1000);
        sb.stopJamTO();
        advance(1000);
        assertEquals(3000L, state.get("ScoreBoard.Period(2).Jam(4).PeriodClockElapsedStart"));
        assertEquals(5000L, state.get("ScoreBoard.Period(2).Jam(5).PeriodClockElapsedStart"));
        // Remove a jam.
        sb.getCurrentPeriod().remove(Period.NChild.JAM, "5");
        advance(0);
        assertEquals(3000L, state.get("ScoreBoard.Period(2).Jam(4).PeriodClockElapsedStart"));
        assertEquals(null, state.get("ScoreBoard.Period(2).Jam(5).PeriodClockElapsedStart"));
    }

    @Test
    public void testRulesetsEvents() {
        String rootId = RulesetsImpl.ROOT_ID;
        String cid = "11111111-1111-1111-1111-111111111111";
        assertEquals(rootId, state.get("ScoreBoard.Rulesets.CurrentRulesetId"));
        assertEquals("WFTDA Sanctioned", state.get("ScoreBoard.Rulesets.CurrentRulesetName"));

        assertEquals("2", state.get("ScoreBoard.Rulesets.CurrentRule(Period.Number)"));
        assertEquals("Period.Number", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Name"));
        assertEquals("Number of periods", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Description"));
        assertEquals("Integer", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Type"));
        assertEquals(0, state.get("ScoreBoard.Rulesets.RuleDefinition(Period.Number).Index"));

        assertEquals("Boolean", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).Type"));
        assertEquals(2, state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).Index"));
        assertEquals("Count Down", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).TrueValue"));
        assertEquals("Count Up", state.get("ScoreBoard.Rulesets.RuleDefinition(Period.ClockDirection).FalseValue"));

        sb.getRulesets().addRuleset("child", rootId, cid);
        advance(0);
        assertEquals(cid, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Id"));
        assertEquals(rootId, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).ParentId"));
        assertEquals("child", state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Name"));
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Rule(Period.Number)"));
        Set<ValueWithId> s = new HashSet<>();
        s.add(new ValWithId(Rule.NUMBER_PERIODS.toString(), "3"));
        sb.getRulesets().getRuleset(cid).setAll(s);
        advance(0);
        assertEquals("3", state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Rule(Period.Number)"));

        sb.getRulesets().setCurrentRuleset(cid);
        advance(0);
        assertEquals(cid, state.get("ScoreBoard.Rulesets.CurrentRulesetId"));
        assertEquals("child", state.get("ScoreBoard.Rulesets.CurrentRulesetName"));
        assertEquals("3", state.get("ScoreBoard.Rulesets.CurrentRule(Period.Number)"));

        sb.getRulesets().removeRuleset(cid);
        advance(0);
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Id"));
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).ParentId"));
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Name"));
        assertEquals(null, state.get("ScoreBoard.Rulesets.Ruleset(11111111-1111-1111-1111-111111111111).Rule(Period.Number)"));
    }

    @Test
    public void testMediaEvents() throws Exception {
        assertEquals("", state.get("ScoreBoard.Media.Format(images).Type(fullscreen)"));
        assertEquals("", state.get("ScoreBoard.Media.Format(images).Type(teamlogo)"));
        assertEquals("init.png", state.get("ScoreBoard.Media.Format(images).Type(teamlogo).File(init.png).Id"));
        assertEquals("init", state.get("ScoreBoard.Media.Format(images).Type(teamlogo).File(init.png).Name"));
        assertEquals("/images/teamlogo/init.png", state.get("ScoreBoard.Media.Format(images).Type(teamlogo).File(init.png).Src"));

        sb.getMedia().removeMediaFile("images", "teamlogo", "init.png");
        dir.newFile("html/images/fullscreen/new.png");

        Thread.sleep(100);
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo.File(init.png).Id"));
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo.File(init.png).Name"));
        assertEquals(null, state.get("ScoreBoard.Media.Format(images).Type(teamlogo.File(init.png).Src"));
        assertEquals("", state.get("ScoreBoard.Media.Format(images).Type(teamlogo)"));
        assertEquals("new.png", state.get("ScoreBoard.Media.Format(images).Type(fullscreen).File(new.png).Id"));
        assertEquals("new", state.get("ScoreBoard.Media.Format(images).Type(fullscreen).File(new.png).Name"));
        assertEquals("/images/fullscreen/new.png", state.get("ScoreBoard.Media.Format(images).Type(fullscreen).File(new.png).Src"));

    }

}
