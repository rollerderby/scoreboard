package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class TeamImpl extends ScoreBoardEventProviderImpl implements Team {
    public TeamImpl(ScoreBoard sb, String i) {
        scoreBoard = sb;
        id = i;
        for (Child c : Child.values()) {
            children.put(c, new HashMap<String, ValueWithId>());
        }
	for (FloorPosition fp : FloorPosition.values()) {
            Position p = new PositionImpl(this, fp);
            add(Child.POSITION, p);
        }
        values.put(Value.LAST_SCORE, 0);

        reset();
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.TEAM); }
    public Class<Team> getProviderClass() { return Team.class; }
    public String getProviderId() { return getId(); }
    public ScoreBoardEventProvider getParent() { return scoreBoard; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public Object get(PermanentProperty prop) {
	if (prop == Value.JAM_SCORE) { return getScore() - getLastScore(); }
	return super.get(prop);
    }

    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (!(prop instanceof Value) || prop == Value.JAM_SCORE) { return false; }
	    requestBatchStart();
	    if (prop == Value.LEAD_JAMMER) {
	        if ("false".equals(((String)value).toLowerCase())) {
	            value = Team.LEAD_NO_LEAD;
	        } else if ("true".equals(((String)value).toLowerCase())) {
	            value = Team.LEAD_LEAD;
	        }
	    }
	    Number min = (value instanceof Integer) ? 0 : null;
	    Number max = null;
	    if (prop == Value.LAST_SCORE) { max = getScore(); }
	    if (prop == Value.TIMEOUTS) { max =  scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS); }
	    if (prop == Value.OFFICIAL_REVIEWS) { max = scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS); }
	    boolean result = super.set(prop, value, flag, min, max, 0);
	    if (result) {
		if (prop == Value.SCORE) {
		    setLastScore(getLastScore()); //check boundary
		}
		if (prop == Value.SCORE || prop == Value.LAST_SCORE) {
	            scoreBoardChange(new ScoreBoardEvent(this, Value.JAM_SCORE, getScore() - getLastScore(), null));
		}
		if (prop == Value.RETAINED_OFFICIAL_REVIEW && (Boolean)value && getOfficialReviews() == 0) {
		    setOfficialReviews(1);
		}
		if (prop == Value.LEAD_JAMMER && Team.LEAD_LEAD.equals((String)value)) {
		    String otherId = id.equals(Team.ID_1) ? Team.ID_2 : Team.ID_1;
		    Team otherTeam = getScoreBoard().getTeam(otherId);
		    if (Team.LEAD_LEAD.equals(otherTeam.getLeadJammer())) {
			otherTeam.setLeadJammer(Team.LEAD_NO_LEAD);
		    }
		}
		if (prop == Value.STAR_PASS) {
	            if ((Boolean)value && Team.LEAD_LEAD.equals(getLeadJammer())) {
	                setLeadJammer(Team.LEAD_LOST_LEAD);
	            }
	            if (getPosition(FloorPosition.JAMMER).getSkater() != null) {
	        	getPosition(FloorPosition.JAMMER).getSkater().setRole(FloorPosition.JAMMER.getRole(this));
	            }
	            if (getPosition(FloorPosition.PIVOT).getSkater() != null) {
	        	getPosition(FloorPosition.PIVOT).getSkater().setRole(FloorPosition.PIVOT.getRole(this));
	            }
		}
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    
    public void execute(CommandProperty prop) {
	switch((Command)prop) {
	case OFFICIAL_REVIEW:
	    officialReview();
	    break;
	case TIMEOUT:
	    timeout();
	    break;
	}
    }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
	synchronized (coreLock) {
	    switch((Child)prop) {
	    case ALTERNATE_NAME:
		return new AlternateNameImpl(this, id, "");
	    case COLOR:
		return new ColorImpl(this, id, "");
	    case SKATER:
		return new SkaterImpl(this, id, "", "", "");
	    case POSITION:
		return null;
	    }
	    return null;
	}
    }
    
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    boolean result = super.remove(prop, item);
	    if (result && prop == Child.SKATER && ((Skater)item).getPosition() != null) {
		((Skater)item).getPosition().setSkater(null);
	    }
	    return result;
	}
    }
    
    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public void reset() {
        synchronized (coreLock) {
            setName(DEFAULT_NAME_PREFIX + id);
            setLogo(DEFAULT_LOGO);
            setScore(DEFAULT_SCORE);
            setLastScore(DEFAULT_SCORE);
            setLeadJammer(DEFAULT_LEADJAMMER);
            setStarPass(DEFAULT_STARPASS);
            setNoPivot(true);

            resetTimeouts(true);

            removeAll(Child.ALTERNATE_NAME);
            removeAll(Child.COLOR);
            removeAll(Child.SKATER);
            Iterator<?> p = getAll(Child.POSITION).iterator();
            while (p.hasNext()) {
                ((Position)p.next()).reset();
            }
        }
    }

    public String getId() { return id; }

    public String toString() { return id; }

    public String getName() { return (String)get(Value.NAME); }
    public void setName(String n) { set(Value.NAME, n); }

    public void startJam() {
        synchronized (coreLock) {
            setLastScore(getScore());
        }
    }

    public void stopJam() {
        synchronized (coreLock) {
            requestBatchStart();

            Map<Skater, Role> boxedSkaters = new HashMap<Skater, Role>();
            for (FloorPosition fp : FloorPosition.values()) {
        	Skater s = getPosition(fp).getSkater();
        	if (s != null) {
        	    if (s.isPenaltyBox()) {
        		boxedSkaters.put(s, s.getRole());
        	    } else {
        		field(s, (Position)null);
        	    }
        	}
            }            
            setLeadJammer(Team.LEAD_NO_LEAD);
            setStarPass(false);
            nextReplacedBlocker = FloorPosition.PIVOT;
            for (Skater s : boxedSkaters.keySet()) {
        	field(s, boxedSkaters.get(s));
            }
            requestBatchEnd();
        }
    }

    public TeamSnapshot snapshot() {
        synchronized (coreLock) {
            return new TeamSnapshotImpl(this);
        }
    }
    public void restoreSnapshot(TeamSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) {	return; }
            //don't reset score
            setLastScore(s.getLastScore());
            setTimeouts(s.getTimeouts());
            setOfficialReviews(s.getOfficialReviews());
            setLeadJammer(s.getLeadJammer());
            setStarPass(s.getStarPass());
            setInTimeout(s.inTimeout());
            setInOfficialReview(s.inOfficialReview());
            for (ValueWithId skater : getAll(Child.SKATER)) {
                ((Skater)skater).restoreSnapshot(s.getSkaterSnapshot(skater.getId()));
            }
        }
    }

    public AlternateName getAlternateName(String i) { return (AlternateName)get(Child.ALTERNATE_NAME, i); }
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) {
            requestBatchStart();
            ((AlternateName)get(Child.ALTERNATE_NAME, i, true)).setName(n);
            requestBatchEnd();
        }
    }
    public void removeAlternateName(String i) { remove(Child.ALTERNATE_NAME, getAlternateName(i)); }

    public Color getColor(String i) { return (Color)get(Child.COLOR, i); }
    public void setColor(String i, String c) {
        synchronized (coreLock) {
            requestBatchStart();
            ((Color)get(Child.COLOR, i, true)).setColor(c);
            requestBatchEnd();
        }
    }
    public void removeColor(String i) { remove(Child.COLOR, getColor(i)); }

    public String getLogo() { return (String)get(Value.LOGO); }
    public void setLogo(String l) { set(Value.LOGO, l); }

    public void timeout() {
        synchronized (coreLock) {
            if (getTimeouts() > 0) {
                getScoreBoard().setTimeoutType(this, false);
                changeTimeouts(-1);
            }
        }
    }
    public void officialReview() {
        synchronized (coreLock) {
            if (getOfficialReviews() > 0) {
                getScoreBoard().setTimeoutType(this, true);
                changeOfficialReviews(-1);
            }
        }
    }

    public int getScore() { return (Integer)get(Value.SCORE); }
    public void setScore(int s) { set(Value.SCORE, s); }
    public void changeScore(int c) { set(Value.SCORE, c, Flag.CHANGE); }

    public int getLastScore() { return (Integer)get(Value.LAST_SCORE); }
    public void setLastScore(int s) { set(Value.LAST_SCORE, s); }
    public void changeLastScore(int c) { set(Value.LAST_SCORE, c, Flag.CHANGE); }

    public boolean inTimeout() { return (Boolean)get(Value.IN_TIMEOUT); }
    public void setInTimeout(boolean b) { set(Value.IN_TIMEOUT, b); }

    public boolean inOfficialReview() { return (Boolean)get(Value.IN_OFFICIAL_REVIEW); }
    public void setInOfficialReview(boolean b) { set(Value.IN_OFFICIAL_REVIEW, b); }

    public boolean retainedOfficialReview() { return (Boolean)get(Value.RETAINED_OFFICIAL_REVIEW); }
    public void setRetainedOfficialReview(boolean b) { set(Value.RETAINED_OFFICIAL_REVIEW, b); }

    public int getTimeouts() { return (Integer)get(Value.TIMEOUTS); }
    public void setTimeouts(int t) { set(Value.TIMEOUTS, t); }
    public void changeTimeouts(int c) { set(Value.TIMEOUTS, c, Flag.CHANGE); } 
    public int getOfficialReviews() { return (Integer)get(Value.OFFICIAL_REVIEWS); }
    public void setOfficialReviews(int r) { set(Value.OFFICIAL_REVIEWS, r); }
    public void changeOfficialReviews(int c) { set(Value.OFFICIAL_REVIEWS, c, Flag.CHANGE); }
    public void resetTimeouts(boolean gameStart) {
        synchronized (coreLock) {
            setInTimeout(false);
            setInOfficialReview(false);
            if (gameStart || scoreBoard.getRulesets().getBoolean(Rule.TIMEOUTS_PER_PERIOD)) {
                setTimeouts(scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS));
            }
            if (gameStart || scoreBoard.getRulesets().getBoolean(Rule.REVIEWS_PER_PERIOD)) {
                setOfficialReviews(scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS));
                setRetainedOfficialReview(false);
            }
        }
    }

    public static Comparator<Skater> SkaterComparator = new Comparator<Skater>() {
        public int compare(Skater s1, Skater s2) {
            if (s2 == null) {
                return 1;
            }
            String n1 = s1.getNumber();
            String n2 = s2.getNumber();
            if (n1 == null) { return -1; }
            if (n2 == null) { return 1; }

            return n1.compareTo(n2);
        }
    };

    public Skater getSkater(String id) { return (Skater)get(Child.SKATER, id); }
    public Skater addSkater(String id) { return (Skater)get(Child.SKATER, id, true); }
    public Skater addSkater(String id, String n, String num, String flags) {
        synchronized (coreLock) {
            Skater s = new SkaterImpl(this, id, n, num, flags);
            addSkater(s);
            return s;
        }
    }
    public void addSkater(Skater skater) { add(Child.SKATER, skater); }
    public void removeSkater(String id) { remove(Child.SKATER, id); }

    public Position getPosition(FloorPosition fp) { return fp == null ? null : (Position)get(Child.POSITION, fp.toString()); }

    public void field(Skater s, Position p) {
	synchronized (coreLock) {
	    requestBatchStart();
	    if (s == null) {
		if (p != null) {
		    if (p.getSkater() != null) {
			p.getSkater().setRoleToBase();
			p.getSkater().setPosition(null);
		    }
		    p.setSkater(null);
		}
	    } else if (p == null) {
		if (s.getPosition() != null) {
		    s.getPosition().setSkater(null);
		}
		s.setRoleToBase();
		s.setPosition(null);
	    } else if (s.getPosition() != p) {
		if (s.getPosition() != null) {
		    s.getPosition().setSkater(null);
		}
		if (p.getSkater() != null) {
		    p.getSkater().setPosition(null);
		    p.getSkater().setRoleToBase();
		}
		s.setPosition(p);
		s.setRole(p.getFloorPosition().getRole(this));
		p.setSkater(s);
	    }
	    requestBatchEnd();
	}
    }
    public void field(Skater s, Role r) {
	synchronized (coreLock) {
	    if (s == null) { return; }
	    requestBatchStart();
	    if (s.getPosition() == getPosition(FloorPosition.PIVOT)) {
		setNoPivot(r != Role.PIVOT);
		if (r == Role.BLOCKER || r == Role.PIVOT) {
		    s.setRole(r);
		}
	    }
	    if (s.getRole() != r) {
		Position p = getAvailablePosition(r);
		if (r == Role.PIVOT) {
		    if (p.getSkater() != null && (hasNoPivot() || s.getRole() == Role.BLOCKER)) {
			// If we are moving a blocker to pivot, move the previous pivot to blocker
			// If we are replacing a blocker from the pivot spot,
			//  see if we have a blocker spot available for them instead
			Position p2;
			if (s.getRole() == Role.BLOCKER) {
			    p2 = s.getPosition();
			} else {
			    p2 = getAvailablePosition(Role.BLOCKER);
			}
			field(p.getSkater(), p2);
		    }
		    setNoPivot(false);
		}
		field (s, p);
	    }
	    requestBatchEnd();
	}
    }
    private Position getAvailablePosition(Role r) {
	switch (r) {
	case JAMMER:
	    if (isStarPass()) {
		return getPosition(FloorPosition.PIVOT);
	    } else {
		return getPosition(FloorPosition.JAMMER);
	    }
	case PIVOT:
	    if (isStarPass()) {
		return null; 
	    } else {
		return getPosition(FloorPosition.PIVOT);
	    }
	case BLOCKER:
	    Position[] ps = {getPosition(FloorPosition.BLOCKER1),
		    getPosition(FloorPosition.BLOCKER2),
		    getPosition(FloorPosition.BLOCKER3)};
	    for (Position p : ps) {
		if (p.getSkater() == null) { 
		    return p; 
		}
	    }
	    Position fourth = getPosition(isStarPass() ? FloorPosition.JAMMER : FloorPosition.PIVOT);
	    if (fourth.getSkater() == null) {
		return fourth;
	    }
	    int tries = 0;
	    do {
		if (++tries > 4) { return null; }
		switch (nextReplacedBlocker) {
		case BLOCKER1:
		    nextReplacedBlocker = FloorPosition.BLOCKER2;
		    break;
		case BLOCKER2:
		    nextReplacedBlocker = FloorPosition.BLOCKER3;
		    break;
		case BLOCKER3:
		    nextReplacedBlocker = (hasNoPivot() && !isStarPass()) ? FloorPosition.PIVOT : FloorPosition.BLOCKER1;
		    break;
		case PIVOT:
		    nextReplacedBlocker = FloorPosition.BLOCKER1;
		    break;
		default:
		    break;
		}
	    } while(getPosition(nextReplacedBlocker).isPenaltyBox());
	    return getPosition(nextReplacedBlocker);
	default:
	    return null;
	}
    }
    
    public String getLeadJammer() { return (String)get(Value.LEAD_JAMMER); }
    public void setLeadJammer(String lead) { set(Value.LEAD_JAMMER, lead); }

    public boolean isStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

    public boolean hasNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
    private void setNoPivot(boolean noPivot) { set(Value.NO_PIVOT, noPivot); }


    protected ScoreBoard scoreBoard;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
        add(Child.class);
        add(Command.class);
    }};

    protected String id;

    protected Map<String,AlternateName> alternateNames = new ConcurrentHashMap<String,AlternateName>();

    protected Map<String,Color> colors = new ConcurrentHashMap<String,Color>();

    protected Map<String,Skater> skaters = new ConcurrentHashMap<String,Skater>();
    protected Map<FloorPosition,Position> positions = new ConcurrentHashMap<FloorPosition,Position>();

    FloorPosition nextReplacedBlocker = FloorPosition.PIVOT;
    
    public static final String DEFAULT_NAME_PREFIX = "Team ";
    public static final String DEFAULT_LOGO = "";
    public static final int DEFAULT_SCORE = 0;
    public static final int DEFAULT_TIMEOUTS = 3;
    public static final int DEFAULT_OFFICIAL_REVIEWS = 1;
    public static final String DEFAULT_LEADJAMMER = Team.LEAD_NO_LEAD;
    public static final boolean DEFAULT_STARPASS = false;

    public class AlternateNameImpl extends ScoreBoardEventProviderImpl implements AlternateName {
        public AlternateNameImpl(Team t, String i, String n) {
            team = t;
            id = i;
            setName(n);
        }
        public String getId() { return id; }
        public String getName() { return (String)get(Value.NAME); }
        public void setName(String n) { set(Value.NAME, n); }

        public Team getTeam() { return team; }

        public String getProviderName() { return PropertyConversion.toFrontend(Team.Child.ALTERNATE_NAME); }
        public Class<AlternateName> getProviderClass() { return AlternateName.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return team; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
        }};

        protected Team team;
        protected String id;
    }

    public class ColorImpl extends ScoreBoardEventProviderImpl implements Color {
        public ColorImpl(Team t, String i, String c) {
            team = t;
            id = i;
            setColor(c);
        }
        public String getId() { return id; }
        public String getColor() { return (String)get(Value.COLOR); }
        public void setColor(String c) { set(Value.COLOR, c); }

        public Team getTeam() { return team; }

        public String getProviderName() { return PropertyConversion.toFrontend(Team.Child.COLOR); }
        public Class<Color> getProviderClass() { return Color.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return team; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
        }};

        protected Team team;
        protected String id;
        protected String color;
    }

    public static class TeamSnapshotImpl implements TeamSnapshot {
        private TeamSnapshotImpl(Team team) {
            id = team.getId();
            score = team.getScore();
            lastscore = team.getLastScore();
            timeouts = team.getTimeouts();
            officialReviews = team.getOfficialReviews();
            leadJammer = team.getLeadJammer();
            starPass = team.isStarPass();
            inTimeout = team.inTimeout();
            inOfficialReview = team.inOfficialReview();
            hasNoPivot = team.hasNoPivot();
            skaterSnapshots = new HashMap<String, Skater.SkaterSnapshot>();
            for (ValueWithId skater : team.getAll(Child.SKATER)) {
                skaterSnapshots.put(skater.getId(), ((Skater)skater).snapshot());
            }
        }

        public String getId() { return id; }
        public int getScore() { return score;}
        public int getLastScore() { return lastscore; }
        public int getTimeouts() { return timeouts; }
        public int getOfficialReviews() { return officialReviews; }
        public String getLeadJammer() { return leadJammer; }
        public boolean getStarPass() { return starPass; }
        public boolean inTimeout() { return inTimeout; }
        public boolean inOfficialReview() { return inOfficialReview; }
        public boolean hasNoPivot() { return hasNoPivot; }
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots() { return skaterSnapshots; }
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater) { return skaterSnapshots.get(skater); }

        protected String id;
        protected int score;
        protected int lastscore;
        protected int timeouts;
        protected int officialReviews;
        protected String leadJammer;
        protected boolean starPass;
        protected boolean inTimeout;
        protected boolean inOfficialReview;
        protected boolean hasNoPivot;
        protected Map<String, Skater.SkaterSnapshot> skaterSnapshots;
    }
}
