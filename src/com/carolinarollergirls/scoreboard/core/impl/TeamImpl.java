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
import java.util.Collections;
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
import com.carolinarollergirls.scoreboard.core.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class TeamImpl extends DefaultScoreBoardEventProvider implements Team {
    public TeamImpl(ScoreBoard sb, String i) {
	for (FloorPosition fp : FloorPosition.values()) {
            Position p = new PositionImpl(this, fp);
            positions.put(fp, p);
            p.addScoreBoardListener(this);
        }
	
        scoreBoard = sb;
        id = i;

        reset();
    }

    public String getProviderName() { return "Team"; }
    public Class<Team> getProviderClass() { return Team.class; }
    public String getProviderId() { return getId(); }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public void reset() {
        synchronized (coreLock) {
            setName(DEFAULT_NAME_PREFIX + id);
            setLogo(DEFAULT_LOGO);
            setScore(DEFAULT_SCORE);
            setLastScore(DEFAULT_SCORE);
            setLeadJammer(DEFAULT_LEADJAMMER);
            setStarPass(DEFAULT_STARPASS);

            resetTimeouts(true);

            removeAlternateNames();
            removeColors();
            Iterator<Position> p = getPositions().iterator();
            Iterator<Skater> s = getSkaters().iterator();
            while (s.hasNext()) {
                try { removeSkater(s.next().getId()); }
                catch ( SkaterNotFoundException snfE ) { }
            }
            while (p.hasNext()) {
                p.next().reset();
            }
        }
    }

    public String getId() { return id; }

    public String toString() { return id; }

    public String getName() { return name; }
    public void setName(String n) {
        synchronized (coreLock) {
            String last = name;
            name = n;
            scoreBoardChange(new ScoreBoardEvent(this, Value.NAME, name, last));
        }
    }

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
            for (Skater skater : getSkaters()) {
                skater.restoreSnapshot(s.getSkaterSnapshot(skater.getId()));
            }
        }
    }

    public List<AlternateName> getAlternateNames() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<AlternateName>(alternateNames.values()));
        }
    }
    public AlternateName getAlternateName(String i) { return alternateNames.get(i); }
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) {
            if (alternateNames.containsKey(i)) {
                alternateNames.get(i).setName(n);
            } else {
                AlternateName an = new AlternateNameImpl(this, i, n);
                alternateNames.put(i, an);
                an.addScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, Child.ALTERNATE_NAME, an, null));
            }
        }
    }
    public void removeAlternateName(String i) {
        synchronized (coreLock) {
            AlternateName an = alternateNames.remove(i);
            an.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, Child.ALTERNATE_NAME, null, an));
        }
    }
    public void removeAlternateNames() {
        synchronized (coreLock) {
            Iterator<AlternateName> i = getAlternateNames().iterator();
            while (i.hasNext()) {
                removeAlternateName(i.next().getId());
            }
        }
    }

    public List<Color> getColors() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<Color>(colors.values()));
        }
    }
    public Color getColor(String i) { return colors.get(i); }
    public void setColor(String i, String c) {
        synchronized (coreLock) {
            if (colors.containsKey(i)) {
                Color cm = colors.get(i);
                cm.setColor(c);
            } else {
                Color cm = new ColorImpl(this, i, c);
                colors.put(i, cm);
                cm.addScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, Child.COLOR, cm, null));
            }
        }
    }
    public void removeColor(String i) {
        synchronized (coreLock) {
            Color cm = colors.remove(i);
            cm.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, Child.COLOR, null, cm));
        }
    }
    public void removeColors() {
        synchronized (coreLock) {
            Iterator<Color> i = getColors().iterator();
            while (i.hasNext()) {
                removeColor(i.next().getId());
            }
        }
    }

    public String getLogo() { return logo; }
    public void setLogo(String l) {
        synchronized (coreLock) {
            String last = logo;
            logo = l;
            scoreBoardChange(new ScoreBoardEvent(this, Value.LOGO, logo, last));
        }
    }

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

    public int getScore() { return score; }
    public void setScore(int s) {
        synchronized (coreLock) {
            if (0 > s) {
                s = 0;
            }
            Integer last = new Integer(score);
            score = s;
            if (s < getLastScore()) {
                setLastScore(s);
            }
            scoreBoardChange(new ScoreBoardEvent(this, Value.SCORE, new Integer(score), last));
        }
    }
    public void changeScore(int c) {
        synchronized (coreLock) {
            setScore(getScore() + c);
        }
    }

    public int getLastScore() { return lastscore; }
    public void setLastScore(int s) {
        synchronized (coreLock) {
            if (0 > s) {
                s = 0;
            }
            if (getScore() < s) {
                s = getScore();
            }
            Integer last = new Integer(lastscore);
            lastscore = s;
            scoreBoardChange(new ScoreBoardEvent(this, Value.LAST_SCORE, new Integer(lastscore), last));
        }
    }
    public void changeLastScore(int c) {
        synchronized (coreLock) {
            setLastScore(getLastScore() + c);
        }
    }

    public boolean inTimeout() { return in_timeout; }
    public void setInTimeout(boolean b) {
        synchronized (coreLock) {
            if (b==in_timeout) {
                return;
            }
            Boolean last = new Boolean(in_timeout);
            in_timeout = b;
            scoreBoardChange(new ScoreBoardEvent(this, Value.IN_TIMEOUT, new Boolean(b), last));
        }
    }

    public boolean inOfficialReview() { return in_official_review; }
    public void setInOfficialReview(boolean b) {
        synchronized (coreLock) {
            if (b==in_official_review) {
                return;
            }
            Boolean last = new Boolean(in_official_review);
            in_official_review = b;
            scoreBoardChange(new ScoreBoardEvent(this, Value.IN_OFFICIAL_REVIEW, new Boolean(b), last));
        }
    }

    public boolean retainedOfficialReview() { return retained_official_review; }
    public void setRetainedOfficialReview(boolean b) {
        synchronized (coreLock) {
            if (b==retained_official_review) {
                return;
            }

            if (b && officialReviews == 0) {
                setOfficialReviews(1);
            }

            Boolean last = new Boolean(retained_official_review);
            retained_official_review = b;
            scoreBoardChange(new ScoreBoardEvent(this, Value.RETAINED_OFFICIAL_REVIEW, new Boolean(b), last));
        }
    }

    public int getTimeouts() { return timeouts; }
    public void setTimeouts(int t) {
        synchronized (coreLock) {
            if (0 > t) {
                t = 0;
            }
            if (scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS) < t) {
                t = scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS);
            }
            Integer last = new Integer(timeouts);
            timeouts = t;
            scoreBoardChange(new ScoreBoardEvent(this, Value.TIMEOUTS, new Integer(timeouts), last));
        }
    }
    public void changeTimeouts(int c) {
        synchronized (coreLock) {
            setTimeouts(getTimeouts() + c);
        }
    }
    public int getOfficialReviews() { return officialReviews; }
    public void setOfficialReviews(int r) {
        synchronized (coreLock) {
            if (0 > r) {
                r = 0;
            }
            if (scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS) < r) {
                r = scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS);
            }
            Integer last = new Integer(officialReviews);
            officialReviews = r;
            scoreBoardChange(new ScoreBoardEvent(this, Value.OFFICIAL_REVIEWS, new Integer(officialReviews), last));
        }
    }
    public void changeOfficialReviews(int c) {
        synchronized (coreLock) {
            setOfficialReviews(getOfficialReviews() + c);
        }
    }
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

    public List<Skater> getSkaters() {
        synchronized (coreLock) {
            ArrayList<Skater> s = new ArrayList<Skater>(skaters.values());
            Collections.sort(s, SkaterComparator);
            return Collections.unmodifiableList(s);
        }
    }
    public Skater getSkater(String id) throws SkaterNotFoundException {
        synchronized (coreLock) {
            if (skaters.containsKey(id)) {
                return skaters.get(id);
            } else {
                throw new SkaterNotFoundException(id);
            }
        }
    }
    public Skater addSkater(String id) { return addSkater(id, "", "", ""); }
    public Skater addSkater(String id, String n, String num, String flags) {
        synchronized (coreLock) {
            Skater sM = new SkaterImpl(this, id, n, num, flags);
            addSkater(sM);
            return sM;
        }
    }
    public void addSkater(Skater skater) {
        synchronized (coreLock) {
            if (null == skater.getId() || "".equals(skater.getId()) || skaters.containsKey(skater.getId())) {
                return;
            }

            skaters.put(skater.getId(), skater);
            skater.addScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, Child.SKATER, skater, null));
        }
    }
    public void removeSkater(String id) throws SkaterNotFoundException {
        synchronized (coreLock) {
            Skater s = getSkater(id);
            Position p = s.getPosition();
            if (p != null) {
        	p.setSkater(null);
            }
            s.removeScoreBoardListener(this);
            skaters.remove(id);
            scoreBoardChange(new ScoreBoardEvent(this, Child.SKATER, null, s));
        }
    }

    public Position getPosition(FloorPosition fp) {
        synchronized (coreLock) {
            return positions.get(fp);
        }
    }
    public List<Position> getPositions() { return Collections.unmodifiableList(new ArrayList<Position>(positions.values())); }

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
    
    public String getLeadJammer() { return leadJammer; }
    public void setLeadJammer(String lead) {
        if ("false".equals(lead.toLowerCase())) {
            lead = Team.LEAD_NO_LEAD;
        } else if ("true".equals(lead.toLowerCase())) {
            lead = Team.LEAD_LEAD;
        }
        requestBatchStart();

        String last = leadJammer;
        leadJammer = lead;
        scoreBoardChange(new ScoreBoardEvent(this, Value.LEAD_JAMMER, leadJammer, last));

        if (Team.LEAD_LEAD.equals(lead)) {
            String otherId = id.equals(Team.ID_1) ? Team.ID_2 : Team.ID_1;
            Team otherTeam = getScoreBoard().getTeam(otherId);
            if (Team.LEAD_LEAD.equals(otherTeam.getLeadJammer())) {
                otherTeam.setLeadJammer(Team.LEAD_NO_LEAD);
            }
        }

        requestBatchEnd();
    }

    public boolean isStarPass() { return starPass; }
    public void setStarPass(boolean sp) {
        synchronized (coreLock) {
            if (sp == starPass) { return; }
            requestBatchStart();

            Boolean last = new Boolean(starPass);
            starPass = sp;
            scoreBoardChange(new ScoreBoardEvent(this, Value.STAR_PASS, new Boolean(sp), last));

            if (sp && Team.LEAD_LEAD.equals(leadJammer)) {
                setLeadJammer(Team.LEAD_LOST_LEAD);
            }
            
            if (getPosition(FloorPosition.JAMMER).getSkater() != null) {
        	getPosition(FloorPosition.JAMMER).getSkater().setRole(FloorPosition.JAMMER.getRole(this));
            }
            if (getPosition(FloorPosition.PIVOT).getSkater() != null) {
        	getPosition(FloorPosition.PIVOT).getSkater().setRole(FloorPosition.PIVOT.getRole(this));
            }

            requestBatchEnd();
        }
    }

    public boolean hasNoPivot() { return hasNoPivot; }
    private void setNoPivot(boolean noPivot) {
        synchronized (coreLock) {
            if (noPivot == hasNoPivot) { return; }
            requestBatchStart();
            Boolean last = new Boolean(hasNoPivot);
            hasNoPivot = noPivot;
            scoreBoardChange(new ScoreBoardEvent(this, Value.NO_PIVOT, new Boolean(noPivot), last));
            requestBatchEnd();
        }
    }

    public void penalty(String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code) {
        synchronized(coreLock) {
            getSkater(skaterId).AddPenalty(penaltyId, fo_exp, period, jam, code);
        }
    }



    protected ScoreBoard scoreBoard;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
        add(Child.class);
    }};

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();

    protected String id;
    protected String name;
    protected String logo = DEFAULT_LOGO;
    protected int score = DEFAULT_SCORE;
    protected int lastscore = DEFAULT_SCORE;
    protected int timeouts = DEFAULT_TIMEOUTS;
    protected int officialReviews = DEFAULT_OFFICIAL_REVIEWS;
    protected String leadJammer = DEFAULT_LEADJAMMER;
    protected boolean starPass = DEFAULT_STARPASS;
    protected boolean hasNoPivot = true;
    protected boolean in_jam = false;
    protected boolean in_timeout = false;
    protected boolean in_official_review = false;
    protected boolean retained_official_review = false;

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

    public class AlternateNameImpl extends DefaultScoreBoardEventProvider implements AlternateName {
        public AlternateNameImpl(Team t, String i, String n) {
            team = t;
            id = i;
            name = n;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String n) {
            synchronized (coreLock) {
                String last = name;
                name = n;
                scoreBoardChange(new ScoreBoardEvent(this, AlternateName.Value.NAME, name, last));
            }
        }

        public Team getTeam() { return team; }

        public String getProviderName() { return "AlternateName"; }
        public Class<AlternateName> getProviderClass() { return AlternateName.class; }
        public String getProviderId() { return getId(); }
        public List<Class<? extends Property>> getProperties() { return properties; }

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
        }};

        protected Team team;
        protected String id;
        protected String name;
    }

    public class ColorImpl extends DefaultScoreBoardEventProvider implements Color {
        public ColorImpl(Team t, String i, String c) {
            team = t;
            id = i;
            color = c;
        }
        public String getId() { return id; }
        public String getColor() { return color; }
        public void setColor(String c) {
            synchronized (coreLock) {
                String last = color;
                color = c;
                scoreBoardChange(new ScoreBoardEvent(this, Color.Value.COLOR, color, last));
            }
        }

        public Team getTeam() { return team; }

        public String getProviderName() { return "Color"; }
        public Class<Color> getProviderClass() { return Color.class; }
        public String getProviderId() { return getId(); }
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
            for (Skater skater : team.getSkaters()) {
                skaterSnapshots.put(skater.getId(), skater.snapshot());
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
