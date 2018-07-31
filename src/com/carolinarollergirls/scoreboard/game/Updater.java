package com.carolinarollergirls.scoreboard.game;

import com.carolinarollergirls.scoreboard.Game;

public abstract class Updater {
	protected abstract String getUpdaterBase();

	public Updater(Game g) { game = g; }

	protected void update(String key, Object value) {
	}

	protected void updateState() {
	}

	public Game getGame() { return game; }
	protected Game game;
}
