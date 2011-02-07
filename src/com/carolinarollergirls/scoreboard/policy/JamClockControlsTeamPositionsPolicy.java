package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class JamClockControlsTeamPositionsPolicy extends AbstractClockRunningChangePolicy
{
	public JamClockControlsTeamPositionsPolicy() {
		super();
		setDescription("This clears all Team Positions (who are not in the Penalty Box) when the Jam clock is stopped, sets all Skaters to Not Lead Jammer, and sets the Team to Not Lead Jammer.");

		addClock(Clock.ID_JAM);
	}

	public void clockRunningChange(Clock clock, boolean running) {
		if (!running) {
			Iterator<TeamModel> teams = getScoreBoardModel().getTeamModels().iterator();
			while (teams.hasNext()) {
				TeamModel teamModel = teams.next();
				Iterator<PositionModel> positions = teamModel.getPositionModels().iterator();
				while (positions.hasNext()) {
					SkaterModel sM = positions.next().getSkaterModel();
					if (sM != null && !sM.isPenaltyBox())
						sM.setPosition(Position.ID_BENCH);
				}
				Iterator<SkaterModel> skaters = teamModel.getSkaterModels().iterator();
				while (skaters.hasNext())
					skaters.next().setLeadJammer(false);
				teamModel.setLeadJammer(false);
			}
		}
	}
}
