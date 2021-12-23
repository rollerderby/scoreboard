package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
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

    Child<ValWithId> VERSION = new Child<>(ValWithId.class, "Version");
    Child<Settings> SETTINGS = new Child<>(Settings.class, "Settings");
    Child<Twitter> TWITTER = new Child<>(Twitter.class, "Twitter");
    Child<Media> MEDIA = new Child<>(Media.class, "Media");
    Child<Clients> CLIENTS = new Child<>(Clients.class, "Clients");
    Child<Rulesets> RULESETS = new Child<>(Rulesets.class, "Rulesets");
    Child<Game> GAME = new Child<>(Game.class, "Game");
    Child<PreparedTeam> PREPARED_TEAM = new Child<>(PreparedTeam.class, "PreparedTeam");
    Child<CurrentGame> CURRENT_GAME = new Child<>(CurrentGame.class, "CurrentGame");
}
