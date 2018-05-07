package com.carolinarollergirls.scoreboard.penalties;

import java.util.List;

public class PenaltyCode {
	
	private String code;
	private List<String> verbalCues;
	private boolean expellable;
	
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
	public boolean isExpellable() {
		return expellable;
	}
	public void setExpellable(boolean expellable) {
		this.expellable = expellable;
	}
	
	public String generateWSString() {
		StringBuilder verbal = new StringBuilder();
		
		if(verbalCues != null) {
			for(int i = 0; i < verbalCues.size(); i++) {
				if(i != 0) {
					verbal.append("-");
				}
				verbal.append(verbalCues.get(i));
			}
		}
		
		return verbal.toString();
	}
	
	
}
