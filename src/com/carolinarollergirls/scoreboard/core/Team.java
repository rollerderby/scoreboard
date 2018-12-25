package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.MultiProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.SingleProperty;
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

    public List<AlternateName> getAlternateNames();
    public AlternateName getAlternateName(String id);
    public void setAlternateName(String id, String name);
    public void removeAlternateName(String id);
    public void removeAlternateNames();

    public List<Color> getColors();
    public Color getColor(String id);
    public void setColor(String id, String color);
    public void removeColor(String id);
    public void removeColors();

    public String getLogo();
    public void setLogo(String logo);

    public void timeout();
    public void officialReview();

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

    public List<Skater> getSkaters();
    public Skater getSkater(String id) throws SkaterNotFoundException;
    public void addSkater(Skater skater);
    public Skater addSkater(String id);
    public Skater addSkater(String id, String name, String number, String flag);
    public void removeSkater(String id) throws SkaterNotFoundException;

    public List<Position> getPositions();
    public Position getPosition(FloorPosition fp);

    public void field(Skater s, Position p);
    public void field(Skater s, Role r);
    
    public String getLeadJammer();
    public void setLeadJammer(String lead);
    public boolean isStarPass();
    public void setStarPass(boolean starPass);
    public boolean hasNoPivot();
    

    public void penalty(String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code);

    public static final String ID_1 = "1";
    public static final String ID_2 = "2";

    public static final String LEAD_LEAD = "Lead";
    public static final String LEAD_NO_LEAD = "NoLead";
    public static final String LEAD_LOST_LEAD = "LostLead";

    public enum Value implements SingleProperty {
	NAME,
	LOGO,
	SCORE,
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
    public enum Child implements MultiProperty {
	SKATER,
	POSITION,
	ALTERNATE_NAME,
	COLOR;
    }

    public static interface AlternateName extends ScoreBoardEventProvider {
        public String getId();
        public String getName();
        public void setName(String n);

        public Team getTeam();

        public enum Value implements SingleProperty {
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

        public enum Value implements SingleProperty {
            COLOR;
        }
    }

    public static interface TeamSnapshot {
        public String getId();
        public int getScore();
        public int getLastScore();
        public int getTimeouts();
        public int getOfficialReviews();
        public String getLeadJammer();
        public boolean getStarPass();
        public boolean inTimeout();
        public boolean inOfficialReview();
        public boolean hasNoPivot();
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots();
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater);
    }
}