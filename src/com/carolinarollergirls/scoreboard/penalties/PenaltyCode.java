package com.carolinarollergirls.scoreboard.penalties;

import java.util.Arrays;
import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

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
    public String getId() {
        return code;
    }

    @Override
    public String getValue() {
        return cuesForWS();
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public List<String> getVerbalCues() {
        return verbalCues;
    }
    public void setVerbalCues(List<String> verbalCues) {
        this.verbalCues = verbalCues;
    }

    public String cuesForWS() {
        //TODO: replace by String.join() when we move to Java 1.8
        if (verbalCues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(String cue : verbalCues) {
            sb.append(",").append(cue);
        }
        sb.deleteCharAt(0);
        return sb.toString();
    }
}
