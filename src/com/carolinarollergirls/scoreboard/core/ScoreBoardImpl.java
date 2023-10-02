package com.carolinarollergirls.scoreboard.core;

import java.util.Map;

import com.carolinarollergirls.scoreboard.core.admin.ClientsImpl;
import com.carolinarollergirls.scoreboard.core.admin.MediaImpl;
import com.carolinarollergirls.scoreboard.core.admin.SettingsImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl;
import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Media;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Settings;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.core.prepared.PreparedTeamImpl;
import com.carolinarollergirls.scoreboard.core.prepared.RulesetsImpl;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.utils.StatsbookExporter;
import com.carolinarollergirls.scoreboard.utils.ValWithId;
import com.carolinarollergirls.scoreboard.utils.Version;

public class ScoreBoardImpl extends ScoreBoardEventProviderImpl<ScoreBoard> implements ScoreBoard {
    public ScoreBoardImpl() {
        super(null, "", null);
        addProperties(props);
        setupScoreBoard();
    }

    protected void setupScoreBoard() {
        removeAll(VERSION);
        for (Map.Entry<String, String> entry : Version.getAll().entrySet()) {
            add(VERSION, new ValWithId(entry.getKey(), entry.getValue()));
        }
        addWriteProtection(VERSION);
        add(SETTINGS, new SettingsImpl(this));
        addWriteProtection(SETTINGS);
        add(RULESETS, new RulesetsImpl(this));
        addWriteProtection(RULESETS);
        add(MEDIA, new MediaImpl(this));
        addWriteProtection(MEDIA);
        add(CLIENTS, new ClientsImpl(this));
        addWriteProtection(CLIENTS);
        add(CURRENT_GAME, new CurrentGameImpl(this));
        addWriteProtection(CURRENT_GAME);
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == GAME && item == getCurrentGame().getSourceElement()) {
            getCurrentGame().set(CurrentGame.GAME, null);
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PREPARED_TEAM) { return new PreparedTeamImpl(this, id); }
            if (prop == GAME) { return new GameImpl(this, id); }
            return null;
        }
    }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            for (Game g : getAll(GAME)) { g.postAutosaveUpdate(); }
            get(CURRENT_GAME, "").postAutosaveUpdate();
            get(CLIENTS, "").postAutosaveUpdate();
            initialLoadDone = true;
            StatsbookExporter.preload(getSettings().get(SETTING_STATSBOOK_INPUT), this);
        }
    }

    @Override
    public Settings getSettings() {
        return get(SETTINGS, "");
    }

    @Override
    public Rulesets getRulesets() {
        return get(RULESETS, "");
    }

    @Override
    public Media getMedia() {
        return get(MEDIA, "");
    }

    @Override
    public Clients getClients() {
        return get(CLIENTS, "");
    }

    @Override
    public Game getGame(String id) {
        return get(GAME, id);
    }

    @Override
    public PreparedTeam getPreparedTeam(String id) {
        return get(PREPARED_TEAM, id);
    }

    @Override
    public CurrentGame getCurrentGame() {
        return get(CURRENT_GAME, "");
    }

    @Override
    public TimeoutOwner getTimeoutOwner(String id) {
        if (id == null) { id = ""; }
        for (Timeout.Owners o : Timeout.Owners.values()) {
            if (o.getId().equals(id)) { return o; }
        }
        if (id.contains("_")) { // gameId_teamId
            String[] parts = id.split("_");
            Game g = get(GAME, parts[0]);
            if (g != null && g.getTeam(parts[1]) != null) { return g.getTeam(parts[1]); }
        }
        return Timeout.Owners.NONE;
    }

    @Override
    public JSONStateManager getJsm() {
        return jsm;
    }

    @Override
    public boolean isInitialLoadDone() {
        return initialLoadDone;
    }

    private JSONStateManager jsm = new JSONStateManager();
    private boolean initialLoadDone = false;
}
