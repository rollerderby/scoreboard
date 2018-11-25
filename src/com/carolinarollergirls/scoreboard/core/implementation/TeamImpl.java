package com.carolinarollergirls.scoreboard.core.implementation;
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

import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.PositionNotFoundException;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;

public class TeamImpl extends DefaultScoreBoardEventProvider implements Team {
    public TeamImpl(ScoreBoard sbm, String i) {
        Iterator<String> posIds = Position.FLOOR_POSITIONS.iterator();
        while (posIds.hasNext()) {
            String id = posIds.next();
            Position pM = new PositionImpl(this, id);
            positions.put(id, pM);
            pM.addScoreBoardListener(this);
        }

        scoreBoardModel = sbm;
        id = i;

        reset();
    }

    public String getProviderName() { return "Team"; }
    public Class<Team> getProviderClass() { return Team.class; }
    public String getProviderId() { return getId(); }

    public ScoreBoard getScoreBoard() { return scoreBoardModel; }

    public void reset() {
        synchronized (coreLock) {
            setName(DEFAULT_NAME_PREFIX + id);
            setLogo(DEFAULT_LOGO);
            setScore(DEFAULT_SCORE);
            setLastScore(DEFAULT_SCORE);
            _setLeadJammer(DEFAULT_LEADJAMMER);
            _setStarPass(DEFAULT_STARPASS);

            resetTimeouts(true);

            removeAlternateNames();
            removeColors();
            Iterator<Position> p = getPositionModels().iterator();
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

    public Team getTeam() { return this; }

    public String getName() { return name; }
    public void setName(String n) {
        synchronized (coreLock) {
            String last = name;
            name = n;
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_NAME, name, last));
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

            benchSkaters();
            _setLeadJammer(Team.LEAD_NO_LEAD);
            _setStarPass(false);

            requestBatchEnd();
        }
    }

    public void benchSkaters() {
        synchronized (coreLock) {
            for (Skater sM : skaters.values()) {
                sM.bench();
            }
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
    public List<AlternateName> getAlternateNameModels() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<AlternateName>(alternateNames.values()));
        }
    }
    public AlternateName getAlternateName(String i) { return getAlternateNameModel(i); }
    public AlternateName getAlternateNameModel(String i) { return alternateNames.get(i); }
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) {
            if (alternateNames.containsKey(i)) {
                alternateNames.get(i).setName(n);
            } else {
                AlternateName anm = new AlternateNameImpl(this, i, n);
                alternateNames.put(i, anm);
                anm.addScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_ALTERNATE_NAME, anm, null));
            }
        }
    }
    public void removeAlternateName(String i) {
        synchronized (coreLock) {
            AlternateName anm = alternateNames.remove(i);
            anm.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_ALTERNATE_NAME, anm, null));
        }
    }
    public void removeAlternateNames() {
        synchronized (coreLock) {
            Iterator<AlternateName> i = getAlternateNameModels().iterator();
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
    public List<Color> getColorModels() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<Color>(colors.values()));
        }
    }
    public Color getColor(String i) { return getColorModel(i); }
    public Color getColorModel(String i) { return colors.get(i); }
    public void setColor(String i, String c) {
        synchronized (coreLock) {
            if (colors.containsKey(i)) {
                Color cm = colors.get(i);
                cm.setColor(c);
            } else {
                Color cm = new ColorImpl(this, i, c);
                colors.put(i, cm);
                cm.addScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_COLOR, cm, null));
            }
        }
    }
    public void removeColor(String i) {
        synchronized (coreLock) {
            Color cm = colors.remove(i);
            cm.removeScoreBoardListener(this);
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_COLOR, cm, null));
        }
    }
    public void removeColors() {
        synchronized (coreLock) {
            Iterator<Color> i = getColorModels().iterator();
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_LOGO, logo, last));
        }
    }

    public void timeout() {
        synchronized (coreLock) {
            if (getTimeouts() > 0) {
                getScoreBoard().setTimeoutType(getId(), false);
                changeTimeouts(-1);
            }
        }
    }
    public void officialReview() {
        synchronized (coreLock) {
            if (getOfficialReviews() > 0) {
                getScoreBoard().setTimeoutType(getId(), true);
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_SCORE, new Integer(score), last));
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_LAST_SCORE, new Integer(lastscore), last));
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_TIMEOUT, new Boolean(b), last));
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_OFFICIAL_REVIEW, new Boolean(b), last));
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_RETAINED_OFFICIAL_REVIEW, new Boolean(b), last));
        }
    }

    public int getTimeouts() { return timeouts; }
    public void setTimeouts(int t) {
        synchronized (coreLock) {
            if (0 > t) {
                t = 0;
            }
            if (scoreBoardModel.getRulesets().getInt(RULE_NUMBER_TIMEOUTS) < t) {
                t = scoreBoardModel.getRulesets().getInt(RULE_NUMBER_TIMEOUTS);
            }
            Integer last = new Integer(timeouts);
            timeouts = t;
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_TIMEOUTS, new Integer(timeouts), last));
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
            if (scoreBoardModel.getRulesets().getInt(RULE_NUMBER_REVIEWS) < r) {
                r = scoreBoardModel.getRulesets().getInt(RULE_NUMBER_REVIEWS);
            }
            Integer last = new Integer(officialReviews);
            officialReviews = r;
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_OFFICIAL_REVIEWS, new Integer(officialReviews), last));
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
            if (gameStart || scoreBoardModel.getRulesets().getBoolean(RULE_TIMEOUTS_PER_PERIOD)) {
                setTimeouts(scoreBoardModel.getRulesets().getInt(RULE_NUMBER_TIMEOUTS));
            }
            if (gameStart || scoreBoardModel.getRulesets().getBoolean(RULE_REVIEWS_PER_PERIOD)) {
                setOfficialReviews(scoreBoardModel.getRulesets().getInt(RULE_NUMBER_REVIEWS));
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
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_SKATER, skater, null));
        }
    }
    public void removeSkater(String id) throws SkaterNotFoundException {
        synchronized (coreLock) {
            Skater sm = getSkater(id);
            try { getPositionModel(sm.getPosition()).clear(); }
            catch ( PositionNotFoundException pnfE ) { /* was on BENCH */ }
            sm.removeScoreBoardListener(this);
            skaters.remove(id);
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_SKATER, sm, null));
        }
    }

    public Position getPosition(String id) throws PositionNotFoundException { return getPositionModel(id); }
    public Position getPositionModel(String id) throws PositionNotFoundException {
        synchronized (coreLock) {
            if (positions.containsKey(id)) {
                return positions.get(id);
            } else {
                throw new PositionNotFoundException(id);
            }
        }
    }
    public List<Position> getPositions() { return Collections.unmodifiableList(new ArrayList<Position>(positions.values())); }
    public List<Position> getPositionModels() { return Collections.unmodifiableList(new ArrayList<Position>(positions.values())); }

    public String getLeadJammer() { return leadJammer; }
    public void setLeadJammer(String lead) {
        synchronized (coreLock) {
            _setLeadJammer(lead);
        }
    }
    private void _setLeadJammer(String lead) {
        if ("false".equals(lead.toLowerCase())) {
            lead = Team.LEAD_NO_LEAD;
        } else if ("true".equals(lead.toLowerCase())) {
            lead = Team.LEAD_LEAD;
        }
        requestBatchStart();

        String last = leadJammer;
        leadJammer = lead;
        scoreBoardChange(new ScoreBoardEvent(this, EVENT_LEAD_JAMMER, leadJammer, last));

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
    public void setStarPass(boolean starPass) {
        synchronized (coreLock) {
            _setStarPass(starPass);
        }
    }
    private void _setStarPass(boolean starPass) {
        synchronized (coreLock) {
            requestBatchStart();

            Boolean last = new Boolean(this.starPass);
            this.starPass = starPass;
            scoreBoardChange(new ScoreBoardEvent(this, EVENT_STAR_PASS, new Boolean(starPass), last));

            if (starPass && Team.LEAD_LEAD.equals(leadJammer)) {
                _setLeadJammer(Team.LEAD_LOST_LEAD);
            }

            requestBatchEnd();
        }
    }

    public void penalty(String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code) {
        synchronized(coreLock) {
            getSkater(skaterId).AddPenalty(penaltyId, fo_exp, period, jam, code);
        }
    }



    protected ScoreBoard scoreBoardModel;

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
    protected boolean in_jam = false;
    protected boolean in_timeout = false;
    protected boolean in_official_review = false;
    protected boolean retained_official_review = false;

    protected Map<String,AlternateName> alternateNames = new ConcurrentHashMap<String,AlternateName>();

    protected Map<String,Color> colors = new ConcurrentHashMap<String,Color>();

    protected Map<String,Skater> skaters = new ConcurrentHashMap<String,Skater>();
    protected Map<String,Position> positions = new ConcurrentHashMap<String,Position>();

    public static final String DEFAULT_NAME_PREFIX = "Team ";
    public static final String DEFAULT_LOGO = "";
    public static final int DEFAULT_SCORE = 0;
    public static final int DEFAULT_TIMEOUTS = 3;
    public static final int DEFAULT_OFFICIAL_REVIEWS = 1;
    public static final String DEFAULT_LEADJAMMER = Team.LEAD_NO_LEAD;
    public static final boolean DEFAULT_STARPASS = false;

    public class AlternateNameImpl extends DefaultScoreBoardEventProvider implements AlternateName {
        public AlternateNameImpl(Team t, String i, String n) {
            teamModel = t;
            id = i;
            name = n;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String n) {
            synchronized (coreLock) {
                String last = name;
                name = n;
                scoreBoardChange(new ScoreBoardEvent(this, AlternateName.EVENT_NAME, name, last));
            }
        }

        public Team getTeam() { return getTeamModel(); }
        public Team getTeamModel() { return teamModel; }

        public String getProviderName() { return "AlternateName"; }
        public Class<AlternateName> getProviderClass() { return AlternateName.class; }
        public String getProviderId() { return getId(); }

        protected Team teamModel;
        protected String id;
        protected String name;
    }

    public class ColorImpl extends DefaultScoreBoardEventProvider implements Color {
        public ColorImpl(Team t, String i, String c) {
            teamModel = t;
            id = i;
            color = c;
        }
        public String getId() { return id; }
        public String getColor() { return color; }
        public void setColor(String c) {
            synchronized (coreLock) {
                String last = color;
                color = c;
                scoreBoardChange(new ScoreBoardEvent(this, Color.EVENT_COLOR, color, last));
            }
        }

        public Team getTeam() { return getTeamModel(); }
        public Team getTeamModel() { return teamModel; }

        public String getProviderName() { return "Color"; }
        public Class<Color> getProviderClass() { return Color.class; }
        public String getProviderId() { return getId(); }

        protected Team teamModel;
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
            in_timeout = team.inTimeout();
            in_official_review = team.inOfficialReview();
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
        public boolean inTimeout() { return in_timeout; }
        public boolean inOfficialReview() { return in_official_review; }
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots() { return skaterSnapshots; }
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater) { return skaterSnapshots.get(skater); }

        protected String id;
        protected int score;
        protected int lastscore;
        protected int timeouts;
        protected int officialReviews;
        protected String leadJammer;
        protected boolean starPass;
        protected boolean in_timeout;
        protected boolean in_official_review;
        protected Map<String, Skater.SkaterSnapshot> skaterSnapshots;
    }

}
