package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Settings;
import com.fasterxml.jackson.jr.ob.JSON;

public class PenaltyCodesManager {

	public PenaltyCodesManager(Settings settings) {
		this.settings = settings;
	}
	
	public PenaltyCodesDefinition loadFromJSON() {
		synchronized(lock) {
			File penaltyFile = new File(ScoreBoardManager.getDefaultPath(),settings.get(PenaltiesFileSetting));
			try(Reader reader = new FileReader(penaltyFile)) {
				return JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
			} catch (Exception e) {
				throw new RuntimeException("Failed to load Penalty Data from file", e);
			}
		}
	}
	
	public String toJSON(PenaltyCodesDefinition definition) {
		try {
			return JSON.std.asString(definition);
		}catch (Exception e) {
			throw new RuntimeException("Failed writing Penalty Definition as JSON", e);
		}
	}
	
	private final Settings settings;
	private final Object lock = new Object();
	public static final String PenaltiesFileSetting = "ScoreBoard.PenaltyDefinitionFile";
}
