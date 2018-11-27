package com.carolinarollergirls.scoreboard.penalties;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.fasterxml.jackson.jr.ob.JSON;

public class PenaltyCodesManager {

    public PenaltyCodesManager() {

    }

    public PenaltyCodesDefinition loadFromJSON(String file) {
        File folder = ScoreBoardManager.getDefaultPath();
        File penaltyFile = new File(folder,file);
        try(Reader reader = new FileReader(penaltyFile)) {
            return JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Penalty Data from file", e);
        }
    }

    public String toJSON(PenaltyCodesDefinition definition) {
        try {
            return JSON.std.asString(definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed writing Penalty Definition as JSON", e);
        }
    }
}
