package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.core.admin.ClientsImpl;
import com.carolinarollergirls.scoreboard.core.admin.MediaImpl;
import com.carolinarollergirls.scoreboard.core.admin.SettingsImpl;
import com.carolinarollergirls.scoreboard.core.admin.TwitterImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl;
import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Media;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Settings;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.core.prepared.PreparedTeamImpl;
import com.carolinarollergirls.scoreboard.core.prepared.RulesetsImpl;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.utils.ValWithId;
import com.carolinarollergirls.scoreboard.utils.Version;

public class ScoreBoardImpl extends ScoreBoardEventProviderImpl<ScoreBoard> implements ScoreBoard {
    public ScoreBoardImpl() {
        super(null, "", null);
        addProperties(VERSION, SETTINGS, TWITTER, RULESETS, MEDIA, CLIENTS, GAME, PREPARED_TEAM, CURRENT_GAME);
        setupScoreBoard();
    }
    public ScoreBoardImpl(ScoreBoardImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new ScoreBoardImpl(this, root);
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
        add(TWITTER, new TwitterImpl(this));
        addWriteProtection(TWITTER);
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PREPARED_TEAM) { return new PreparedTeamImpl(this, id); }
            if (prop == GAME) { return new GameImpl(this, id); }
            if (prop == TWITTER) { return new TwitterImpl(this); }
            return null;
        }
    }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            for (Game g : getAll(GAME)) { g.postAutosaveUpdate(); }
            get(CURRENT_GAME, "").postAutosaveUpdate();
            get(CLIENTS, "").postAutosaveUpdate();
            get(TWITTER, "").postAutosaveUpdate();
            if (getAll(PREPARED_TEAM).size() < 2) {
                // fewer than 2 teams - create black and white so an ad hoc game can be started
                PreparedTeam t1 = scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, "Black");
                t1.set(Team.TEAM_NAME, "Black");
                t1.set(Team.UNIFORM_COLOR, "Black");
                PreparedTeam t2 = scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, "White");
                t2.set(Team.TEAM_NAME, "White");
                t1.set(Team.UNIFORM_COLOR, "White");
            }
            initialLoadDone = true;
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
        if (getCurrentGame().get(CurrentGame.TEAM, id) != null) {
            return getCurrentGame().get(CurrentGame.TEAM, id).get(CurrentTeam.TEAM);
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
