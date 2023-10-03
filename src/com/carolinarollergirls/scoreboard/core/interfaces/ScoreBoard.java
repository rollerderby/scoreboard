package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface ScoreBoard extends ScoreBoardEventProvider {
    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    // convert the id into a timeoutOwner object
    public TimeoutOwner getTimeoutOwner(String id);

    public Settings getSettings();

    public Rulesets getRulesets();

    public Media getMedia();

    public Clients getClients();

    public Game getGame(String id);

    public PreparedTeam getPreparedTeam(String id);

    public CurrentGame getCurrentGame();

    public JSONStateManager getJsm();

    public boolean isInitialLoadDone();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> BLANK_STATSBOOK_FOUND =
        new Value<>(String.class, "BlankStatsbookFound", "false", props);
    public static final Value<Integer> IMPORTS_IN_PROGRESS = new Value<>(Integer.class, "ImportsInProgress", 0, props);

    public static final Child<ValWithId> VERSION = new Child<>(ValWithId.class, "Version", props);
    public static final Child<Settings> SETTINGS = new Child<>(Settings.class, "Settings", props);
    public static final Child<Media> MEDIA = new Child<>(Media.class, "Media", props);
    public static final Child<Clients> CLIENTS = new Child<>(Clients.class, "Clients", props);
    public static final Child<Rulesets> RULESETS = new Child<>(Rulesets.class, "Rulesets", props);
    public static final Child<Game> GAME = new Child<>(Game.class, "Game", props);
    public static final Child<PreparedTeam> PREPARED_TEAM = new Child<>(PreparedTeam.class, "PreparedTeam", props);
    public static final Child<CurrentGame> CURRENT_GAME = new Child<>(CurrentGame.class, "CurrentGame", props);

    public static final String SETTING_CLOCK_AFTER_TIMEOUT = "ScoreBoard.ClockAfterTimeout";
    public static final String SETTING_AUTO_START = "ScoreBoard.AutoStart";
    public static final String SETTING_AUTO_START_BUFFER = "ScoreBoard.AutoStartBuffer";
    public static final String SETTING_AUTO_END_JAM = "ScoreBoard.AutoEndJam";
    public static final String SETTING_AUTO_END_TTO = "ScoreBoard.AutoEndTTO";
    public static final String SETTING_USE_LT = "ScoreBoard.Penalties.UseLT";
    public static final String SETTING_STATSBOOK_INPUT = "ScoreBoard.Stats.InputFile";
}
