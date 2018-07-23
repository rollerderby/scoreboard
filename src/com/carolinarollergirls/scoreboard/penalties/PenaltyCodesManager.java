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
		Reader reader = null;
		try {
			reader = new FileReader(new File(ScoreBoardManager.getDefaultPath(),settings.get(PenaltiesFileSetting)));
			return JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load Penalty Data from file", e);
		} finally {
			try {
				if(reader != null) {
					reader.close();
				}
			} catch(Exception i) {
				//ignored
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
	public static final String PenaltiesFileSetting = "ScoreBoard.PenaltyDefinitionFile";
}
