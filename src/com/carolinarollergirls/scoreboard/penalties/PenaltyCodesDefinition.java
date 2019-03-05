package com.carolinarollergirls.scoreboard.penalties;

import java.util.List;

public class PenaltyCodesDefinition {

    private List<PenaltyCode> penalties;

    public List<PenaltyCode> getPenalties() {
        return penalties;
    }

    public void setPenalties(List<PenaltyCode> penalties) {
        this.penalties = penalties;
    }

    public void add(PenaltyCode code) {
        penalties.add(code);
    }
}
