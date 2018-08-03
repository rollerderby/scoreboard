package com.carolinarollergirls.scoreboard.snapshots;

import com.carolinarollergirls.scoreboard.model.SkaterModel;

public class SkaterSnapshot {
	public SkaterSnapshot(SkaterModel skater) {
		id = skater.getId();
		position = skater.getPosition();
		box = skater.isPenaltyBox();
	}
	
	public String getId( ) { return id; }
	public String getPosition() { return position; }
	public boolean isPenaltyBox() { return box; }
	
	protected String id;
	protected String position;
	protected boolean box;
}
