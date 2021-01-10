package com.carolinarollergirls.scoreboard.penalties;

import java.util.Arrays;
import java.util.List;

import com.carolinarollergirls.scoreboard.event.ValueWithId;

public class PenaltyCode implements ValueWithId {

    private String code;
    private List<String> verbalCues;

    public PenaltyCode() {

    }

    public PenaltyCode(String code, List<String> verbalCues) {
        this.code = code;
        this.verbalCues = verbalCues;
    }

    public PenaltyCode(String code, String... verbalCues) {
        this.code = code;
        this.verbalCues = Arrays.asList(verbalCues);
    }

    @Override
    public String getId() { return code; }

    @Override
    public String getValue() { return String.join(",", verbalCues); }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public List<String> getVerbalCues() { return verbalCues; }
    public void setVerbalCues(List<String> verbalCues) { this.verbalCues = verbalCues; }
}
