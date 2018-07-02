package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Settings;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PenaltyCodesManager {

	public PenaltyCodesManager(Settings settings) {
		this.settings = settings;
	}
	
	public PenaltyCodesDefinition loadFromJSON() {
		try {
			return mapper.readValue(new File(ScoreBoardManager.getDefaultPath(),settings.get(PenaltiesFileSetting)), PenaltyCodesDefinition.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load Penalty Data from file", e);
		}
	}
	
	public String toJSON(PenaltyCodesDefinition definition) {
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
