package com.carolinarollergirls.scoreboard.core.admin;

import java.nio.file.Paths;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Settings;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.StatsbookExporter;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class SettingsImpl extends ScoreBoardEventProviderImpl<Settings> implements Settings {
    public SettingsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.SETTINGS);
        addProperties(props);
        setDefaults();
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (item != null && ScoreBoard.SETTING_STATSBOOK_INPUT.equals(item.getId())) {
            boolean found = Paths.get(item.getValue()).toFile().canRead();
            if (found && scoreBoard.isInitialLoadDone()) {
                StatsbookExporter.preload(item.getValue(), getScoreBoard());
            } else if (!found) {
                getScoreBoard().set(ScoreBoard.BLANK_STATSBOOK_FOUND, "none");
            }
        }
    }

    private void setDefaults() {
        set("Overlay.Interactive.Clock", "On");
        set("Overlay.Interactive.Scaling", "100");
        set("Overlay.Interactive.Score", "On");
        set("Overlay.Interactive.ShowJammers", "On");
        set("Overlay.Interactive.ShowLineups", "On");
        set("Overlay.Interactive.ShowAllNames", "Off");
        set("ScoreBoard.Operator_Default.StartStopButtons", "false");
        set("ScoreBoard.Operator_Default.TabBar", "true");
        set("ScoreBoard.Operator_Default.ReplaceButton", "false");
        set("ScoreBoard.Operator_Default.ScoreAdjustments", "false");
        set(ScoreBoard.SETTING_USE_LT, "false");
        set(ScoreBoard.SETTING_STATSBOOK_INPUT, "");
        set(ScoreBoard.SETTING_AUTO_START, "");
        set(ScoreBoard.SETTING_AUTO_START_BUFFER, "0:02");
        set(ScoreBoard.SETTING_AUTO_END_JAM, "true");
        set(ScoreBoard.SETTING_AUTO_END_TTO, "false");
        set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, Clock.ID_LINEUP);
        set(Clock.SETTING_SYNC, "true");
        set(Team.SETTING_DISPLAY_NAME, Team.OPTION_LEAGUE_NAME);
        set(Game.SETTING_DEFAULT_NAME_FORMAT, "%d %G %1 vs. %2 (%s: %S)");
        set("ScoreBoard.Intermission.PreGame", "Time To Derby");
        set("ScoreBoard.Intermission.Intermission", "Intermission");
        set("ScoreBoard.Intermission.Unofficial", "Unofficial Score");
        set("ScoreBoard.Intermission.Official", "Final Score");
        set("ScoreBoard.Intermission.OfficialWithClock", "Final Score");

        setBothViews("BoxStyle", "box_flat_bright");
        setBothViews("CurrentView", "scoreboard");
        setBothViews("CustomHtml", "/customhtml/fullscreen/example.html");
        setBothViews("Image", "/images/fullscreen/test-image.png");
        setBothViews("ImageScaling", "contain");
        setBothViews("HideLogos", "false");
        setBothViews("SidePadding", "");
        setBothViews("SwapTeams", "false");
        setBothViews("Video", "/videos/fullscreen/test-video.webm");
        setBothViews("VideoScaling", "contain");
    }

    @Override
    public String get(String k) {
        synchronized (coreLock) {
            if (get(SETTING, k) == null) { return null; }
            return get(SETTING, k).getValue();
        }
    }
    @Override
    public void set(String k, String v) {
        synchronized (coreLock) {
            if (v == null) {
                remove(SETTING, k);
            } else {
                add(SETTING, new ValWithId(k, v));
            }
        }
    }

    private void setBothViews(String key, String value) {
        set("ScoreBoard.Preview_" + key, value);
        set("ScoreBoard.View_" + key, value);
    }
}
