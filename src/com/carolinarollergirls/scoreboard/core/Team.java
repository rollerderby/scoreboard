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

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Team extends ScoreBoardEventProvider {
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
    public void setInTimeout(boolean in_timeouts);
    public boolean inOfficialReview();
    public void setInOfficialReview(boolean in_official_review);
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

    public static final String EVENT_NAME = "Name";
    public static final String EVENT_LOGO = "Logo";
    public static final String EVENT_SCORE = "Score";
    public static final String EVENT_LAST_SCORE = "LastScore";
    public static final String EVENT_TIMEOUTS = "Timeouts";
    public static final String EVENT_OFFICIAL_REVIEWS = "OfficialReviews";
    public static final String EVENT_IN_TIMEOUT = "InTimeout";
    public static final String EVENT_IN_OFFICIAL_REVIEW = "InOfficialReview";
    public static final String EVENT_NO_PIVOT = "NoPivot";
    public static final String EVENT_RETAINED_OFFICIAL_REVIEW = "RetainedOfficialReview";
    public static final String EVENT_ADD_SKATER = "AddSkater";
    public static final String EVENT_REMOVE_SKATER = "RemoveSkater";
    public static final String EVENT_LEAD_JAMMER = "LeadJammer";
    public static final String EVENT_STAR_PASS = "StarPass";
    public static final String EVENT_ADD_ALTERNATE_NAME = "AddAlternateName";
    public static final String EVENT_REMOVE_ALTERNATE_NAME = "RemoveAlternateName";
    public static final String EVENT_ADD_COLOR = "AddColor";
    public static final String EVENT_REMOVE_COLOR = "RemoveColor";

    public static interface AlternateName extends ScoreBoardEventProvider {
        public String getId();
        public String getName();
        public void setName(String n);

        public Team getTeam();

        public static final String EVENT_NAME = "Name";

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

        public static final String EVENT_COLOR = "Color";
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
