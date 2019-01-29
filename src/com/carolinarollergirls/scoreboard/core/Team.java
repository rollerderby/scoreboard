package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Team extends ScoreBoardEventProvider, TimeoutOwner {
    public ScoreBoard getScoreBoard();

    public void reset();

    public String getId();

    public String getName();
    public void setName(String name);

    public void startJam();
    public void stopJam();
    public TeamSnapshot snapshot();
    public void restoreSnapshot(TeamSnapshot s);

    public AlternateName getAlternateName(String id);
    public void setAlternateName(String id, String name);
    public void removeAlternateName(String id);

    public Color getColor(String id);
    public void setColor(String id, String color);
    public void removeColor(String id);

    public String getLogo();
    public void setLogo(String logo);

    public void timeout();
    public void officialReview();
    
    public TeamJam getRunningOrUpcomingTeamJam();
    public TeamJam getRunningOrEndedTeamJam();
    public TeamJam getLastEndedTeamJam();
    public void updateTeamJams();

    public int getScore();
    public void setScore(int score);
    public void changeScore(int change);
    public int getLastScore();
    public void setLastScore(int score);
    public void changeLastScore(int change);

    public int getTimeouts();
    public void setTimeouts(int timeouts);
    public void changeTimeouts(int change);
    public int getOfficialReviews();
    public void setOfficialReviews(int reviews);
    public void changeOfficialReviews(int reviews);
    public void resetTimeouts(boolean gameStart);

    public boolean inTimeout();
    public boolean inOfficialReview();
    public boolean retainedOfficialReview();
    public void setRetainedOfficialReview(boolean retained_official_review);

    public Skater getSkater(String id);
    public void addSkater(Skater skater);
    public Skater addSkater(String id, String name, String number, String flag);
    public void removeSkater(String id);

    public Position getPosition(FloorPosition fp);

    public void field(Skater s, Role r);
    
    public String getLeadJammer();
    public void setLeadJammer(String lead);
    public boolean isStarPass();
    public void setStarPass(boolean starPass);
    public boolean hasNoPivot();
    

    public static final String ID_1 = "1";
    public static final String ID_2 = "2";

    public static final String LEAD_LEAD = "Lead";
    public static final String LEAD_NO_LEAD = "NoLead";
    public static final String LEAD_LOST_LEAD = "LostLead";

    public enum Value implements PermanentProperty {
	NAME,
	LOGO,
	RUNNING_OR_UPCOMING_TEAM_JAM,
	RUNNING_OR_ENDED_TEAM_JAM,
	LAST_ENDED_TEAM_JAM,
	SCORE,
	JAM_SCORE,
	LAST_SCORE,
	TIMEOUTS,
	OFFICIAL_REVIEWS,
	IN_TIMEOUT,
	IN_OFFICIAL_REVIEW,
	NO_PIVOT,
	RETAINED_OFFICIAL_REVIEW,
	LEAD_JAMMER,
	STAR_PASS,
    }
    public enum Child implements AddRemoveProperty {
	SKATER,
	POSITION,
	ALTERNATE_NAME,
	COLOR;
    }
    public enum Command implements CommandProperty {
	TIMEOUT,
	OFFICIAL_REVIEW;
    }

    public static interface AlternateName extends ScoreBoardEventProvider {
        public String getId();
        public String getName();
        public void setName(String n);

        public Team getTeam();

        public enum Value implements PermanentProperty {
            NAME;
        }

        public static final String ID_OPERATOR = "operator";
        public static final String ID_MOBILE = "mobile";
        public static final String ID_OVERLAY = "overlay";
        public static final String ID_TWITTER = "twitter";
    };

    public static interface Color extends ScoreBoardEventProvider {
        public String getId();
        public String getColor();
        public void setColor(String c);

        public Team getTeam();

        public enum Value implements PermanentProperty {
            COLOR;
        }
    }

    public static interface TeamSnapshot {
        public String getId();
        public int getTimeouts();
        public int getOfficialReviews();
        public boolean inTimeout();
        public boolean inOfficialReview();
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots();
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater);
    }
}
