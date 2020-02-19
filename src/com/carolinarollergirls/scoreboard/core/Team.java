package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Managemnt of currently playing teams.
public interface Team extends ScoreBoardEventProvider, TimeoutOwner {
    public void reset();

    public String getName();
    public void setName(String name);

    public void startJam();
    public void stopJam();
    public TeamSnapshot snapshot();
    public void restoreSnapshot(TeamSnapshot s);

    public String getAlternateName(String id);
    public String getAlternateName(AlternateNameId id);
    public void setAlternateName(String id, String name);
    public void removeAlternateName(String id);

    public String getColor(String id);
    public void setColor(String id, String color);
    public void removeColor(String id);

    public String getLogo();
    public void setLogo(String logo);

    public void loadPreparedTeam(PreparedTeam pt);

    public void timeout();
    public void officialReview();

    public TeamJam getRunningOrUpcomingTeamJam();
    public TeamJam getRunningOrEndedTeamJam();
    public void updateTeamJams();

    public int getScore();

    public ScoringTrip getCurrentTrip();
    
    //returns true if an advancement was pending
    public boolean cancelTripAdvancement();

    public int getTimeouts();
    public int getOfficialReviews();

    public boolean inTimeout();
    public boolean inOfficialReview();
    public boolean retainedOfficialReview();
    public void setRetainedOfficialReview(boolean retained_official_review);
    public void recountTimeouts();

    public Skater getSkater(String id);
    public void addSkater(Skater skater);
    public void removeSkater(String id);

    public Position getPosition(FloorPosition fp);

    public void field(Skater s, Role r);
    public void field(Skater s, Role r, TeamJam tj);
    public boolean hasFieldingAdvancePending();

    public boolean isLost();
    public boolean isLead();
    public boolean isCalloff();
    public boolean isInjury();
    public boolean isDisplayLead();
    public boolean isStarPass();
    public boolean hasNoPivot();
    
    public Team getOtherTeam();


    public static final String ID_1 = "1";
    public static final String ID_2 = "2";

    public enum Value implements PermanentProperty {
        ID(String.class, ""),
        NAME(String.class, ""),
        LOGO(String.class, ""),
        RUNNING_OR_UPCOMING_TEAM_JAM(TeamJam.class, null),
        RUNNING_OR_ENDED_TEAM_JAM(TeamJam.class, null),
        FIELDING_ADVANCE_PENDING(Boolean.class, false),
        CURRENT_TRIP(ScoringTrip.class, null),
        SCORE(Integer.class, 0),
        JAM_SCORE(Integer.class, 0),
        TRIP_SCORE(Integer.class, 0),
        LAST_SCORE(Integer.class, 0),
        TIMEOUTS(Integer.class, 0),
        OFFICIAL_REVIEWS(Integer.class, 0),
        LAST_REVIEW(Timeout.class, null),
        IN_TIMEOUT(Boolean.class, false),
        IN_OFFICIAL_REVIEW(Boolean.class, false),
        NO_PIVOT(Boolean.class, false),
        RETAINED_OFFICIAL_REVIEW(Boolean.class, false),
        LOST(Boolean.class, false),
        LEAD(Boolean.class, false),
        CALLOFF(Boolean.class, false),
        INJURY(Boolean.class, false),
        NO_INITIAL(Boolean.class, true),
        DISPLAY_LEAD(Boolean.class, false),
        STAR_PASS(Boolean.class, false),
        STAR_PASS_TRIP(ScoringTrip.class, null);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        SKATER(Skater.class),
        POSITION(Position.class),
        TIME_OUT(Timeout.class), // can't be TIMEOUT, as that would lead to a conflict with Command.TIMEOUT
        ALTERNATE_NAME(ValWithId.class),
        COLOR(ValWithId.class),
        BOX_TRIP(BoxTrip.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum Command implements CommandProperty {
        ADD_TRIP,
        REMOVE_TRIP,
        ADVANCE_FIELDINGS,
        TIMEOUT,
        OFFICIAL_REVIEW;
        
        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }

    public enum AlternateNameId {
        OPERATOR("operator"),
        OVERLAY("overlay"),
        TWITTER("twitter");
        
        private AlternateNameId(String i) { id = i; }
        @Override
        public String toString() { return id; }

        private String id;
    }

    public static interface TeamSnapshot {
        public String getId();
        public boolean getFieldingAdvancePending();
    }
}
