package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Settings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PenaltiesManager {

	public PenaltiesManager(Settings settings) {
		this.settings = settings;
	}
	
	public PenaltiesDefinition loadFromJSON() {
		try {
			return mapper.readValue(new File(ScoreBoardManager.getDefaultPath(),settings.get(PenaltiesManager.PenaltiesFileSetting)), PenaltiesDefinition.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load Penalty Data from file", e);
		}
	}
	
	public String toJSON(PenaltiesDefinition definition) {
		try {
			return mapper.writeValueAsString(definition);
		}catch (Exception e) {
			throw new RuntimeException("Failed writing Penalty Definition as JSON", e);
		}
	}

	private final Settings settings;
	
	private static final ObjectMapper mapper = new ObjectMapper();
	public static final String PenaltiesFileSetting = "ScoreBoard.PenaltyDefinitionFile";
}
