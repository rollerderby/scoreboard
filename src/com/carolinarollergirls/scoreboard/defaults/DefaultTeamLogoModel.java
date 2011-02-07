package com.carolinarollergirls.scoreboard.defaults;

import java.awt.image.*;

import javax.imageio.*;

import java.io.*;
import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultTeamLogoModel extends DefaultScoreBoardEventProvider implements TeamLogoModel
{
	public DefaultTeamLogoModel(TeamModel tM) {
		teamModel = tM;
		reset();
		getTeamModel().getScoreBoard().addScoreBoardListener(removeScoreBoardImageListener);
	}

	public String getProviderName() { return "TeamLogo"; }
	public Class getProviderClass() { return TeamLogo.class; }

	public Team getTeam() { return getTeamModel().getTeam(); }
	public TeamModel getTeamModel() { return teamModel; }

	public TeamLogo getTeamLogo() { return this; }

	public void reset() {
		setId(DEFAULT_ID);
	}

	public String getId() { try { return scoreBoardImage.getId(); } catch ( NullPointerException npE ) { return ""; } }
	public void setId(String i) {
		scoreBoardImage = getTeam().getScoreBoard().getScoreBoardImage(i);

		scoreBoardChange(new ScoreBoardEvent(getTeamLogo(), "Id", getId()));
		scoreBoardChange(new ScoreBoardEvent(getTeamLogo(), "Type", getType()));
		scoreBoardChange(new ScoreBoardEvent(getTeamLogo(), "Name", getName()));
		scoreBoardChange(new ScoreBoardEvent(getTeamLogo(), "Directory", getDirectory()));
		scoreBoardChange(new ScoreBoardEvent(getTeamLogo(), "Filename", getFilename()));
	}

	public String getType() { try { return scoreBoardImage.getType(); } catch ( NullPointerException npE ) { return ""; } }

	public String getName() { try { return scoreBoardImage.getName(); } catch ( NullPointerException npE ) { return ""; } }

	public String getDirectory() { try { return scoreBoardImage.getDirectory(); } catch ( NullPointerException npE ) { return ""; } }
	public String getFilename() { try { return scoreBoardImage.getFilename(); } catch ( NullPointerException npE ) { return ""; } }

	public BufferedImage getImage() { try {	return scoreBoardImage.getImage(); } catch ( NullPointerException npE ) { return null; } }

	protected TeamModel teamModel;

	protected ScoreBoardImage scoreBoardImage = null;

	protected ScoreBoardListener removeScoreBoardImageListener = new ScoreBoardListener() {
			public void scoreBoardChange(ScoreBoardEvent event) {
				if (event.getProperty().equals("RemoveScoreBoardImage")) {
					ScoreBoardImage sbI = (ScoreBoardImage)event.getValue();
					if (scoreBoardImage.getId().equals(sbI.getId()))
						setId(null);
				}
			}
		};

	public static final String DEFAULT_ID = "";
}
