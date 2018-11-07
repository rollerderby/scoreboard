package com.carolinarollergirls.scoreboard.view;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Team extends ScoreBoardEventProvider {
    public ScoreBoard getScoreBoard();

    public String getId();

    public String getName();

    public List<AlternateName> getAlternateNames();
    public AlternateName getAlternateName(String id);

    public List<Color> getColors();
    public Color getColor(String id);

    public String getLogo();

    public int getScore();
    public int getLastScore();

    public int getTimeouts();
    public int getOfficialReviews();

    public boolean inTimeout();
    public boolean inOfficialReview();
    public boolean retainedOfficialReview();

    public List<Skater> getSkaters();
    public Skater getSkater(String id) throws SkaterNotFoundException;

    public List<Position> getPositions();
    public Position getPosition(String id) throws PositionNotFoundException;

    public String getLeadJammer();
    public boolean isStarPass();

    public static final String ID_1 = "1";
    public static final String ID_2 = "2";

    public static final String LEAD_LEAD = "Lead";
    public static final String LEAD_NO_LEAD = "NoLead";
    public static final String LEAD_LOST_LEAD = "LostLead";

    public static final String RULE_NUMBER_TIMEOUTS = "Team.Timeouts";
    public static final String RULE_TIMEOUTS_PER_PERIOD = "Team.TimeoutsPer";
    public static final String RULE_NUMBER_REVIEWS = "Team.OfficialReviews";
    public static final String RULE_REVIEWS_PER_PERIOD = "Team.OfficialReviewsPer";

    public static final String EVENT_NAME = "Name";
    public static final String EVENT_LOGO = "Logo";
    public static final String EVENT_SCORE = "Score";
    public static final String EVENT_LAST_SCORE = "LastScore";
    public static final String EVENT_TIMEOUTS = "Timeouts";
    public static final String EVENT_OFFICIAL_REVIEWS = "OfficialReviews";
    public static final String EVENT_IN_TIMEOUT = "InTimeout";
    public static final String EVENT_IN_OFFICIAL_REVIEW = "InOfficialReview";
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

        public Team getTeam();

        public static final String EVENT_COLOR = "Color";
    }
}
