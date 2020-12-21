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
import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients;
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
import com.carolinarollergirls.scoreboard.utils.ValWithId;
import com.carolinarollergirls.scoreboard.utils.Version;

public class ScoreBoardImpl extends ScoreBoardEventProviderImpl<ScoreBoard> implements ScoreBoard {
    public ScoreBoardImpl() {
        super(null, "", null);
        addProperties(VERSION, SETTINGS, TWITTER, RULESETS, MEDIA, CLIENTS, GAME, PREPARED_TEAM);
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
        add(GAME, new GameImpl(this, ""));
        add(TWITTER, new TwitterImpl(this));
        addWriteProtection(TWITTER);
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PREPARED_TEAM) { return new PreparedTeamImpl(this, id); }
            if (prop == TWITTER) { return new TwitterImpl(this); }
            return null;
        }
    }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            get(GAME, "").postAutosaveUpdate();
            get(CLIENTS, "").postAutosaveUpdate();
            get(TWITTER, "").postAutosaveUpdate();
        }
    }

    @Override
    public Settings getSettings() { return get(SETTINGS, ""); }

    @Override
    public Rulesets getRulesets() { return get(RULESETS, ""); }

    @Override
    public Media getMedia() { return get(MEDIA, ""); }

    @Override
    public Clients getClients() { return get(CLIENTS, ""); }

    @Override
    public Game getGame() { return get(GAME, ""); }

    @Override
    public PreparedTeam getPreparedTeam(String id) { return get(PREPARED_TEAM, id); }

    @Override
    public TimeoutOwner getTimeoutOwner(String id) {
        if (id == null) { id = ""; }
        for (Timeout.Owners o : Timeout.Owners.values()) {
            if (o.getId().equals(id)) { return o; }
        }
        if (getGame().getTeam(id) != null) {
            return getGame().getTeam(id);
        }
        return Timeout.Owners.NONE;
    }
}
