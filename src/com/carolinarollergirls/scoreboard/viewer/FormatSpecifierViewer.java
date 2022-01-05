package com.carolinarollergirls.scoreboard.viewer;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame.CurrentSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.MirrorScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardCondition;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class FormatSpecifierViewer {
    public FormatSpecifierViewer(ScoreBoard sb) { setScoreBoard(sb); }

    public Map<String, String> getFormatSpecifierDescriptions() {
        Map<String, String> m = new LinkedHashMap<>();
        Iterator<String> keys = scoreBoardValues.keySet().iterator();
        while (keys.hasNext()) {
            String k = keys.next();
            m.put(k, scoreBoardValues.get(k).getDescription());
        }
        return m;
    }

    public void setScoreBoard(ScoreBoard sb) {
        scoreBoard = sb;
        setupScoreBoardValues();
    }

    public String parse(String format) {
        StringBuffer buffer = new StringBuffer();
        Matcher m = formatPattern.matcher(format);
        while (m.find()) { m.appendReplacement(buffer, getFormatSpecifierValue(m.group())); }
        m.appendTail(buffer);
        return buffer.toString();
    }

    public boolean checkCondition(String format, ScoreBoardEvent<?> event) {
        boolean triggerCondition = true;
        Matcher m = conditionPattern.matcher(format);
        if (!m.find()) { throw new IllegalArgumentException("No conditions in format : " + format); }
        do {
            String specifier = m.group(1);
            String comparator = m.group(2);
            String targetValue = m.group(3);
            if (null == comparator || null == targetValue) { continue; }
            String value = scoreBoardValues.get(specifier).getValue();
            if (triggerCondition) {
                triggerCondition = false;
                // If current trigger event value == previous value after processing
                // (e.g. conversion to min:sec) then ignore, to prevent multiple consecutive
                // identical triggers
                if (value.equals(scoreBoardValues.get(specifier).getPreviousValue(event.getPreviousValue()))) {
                    return false;
                }
            }
            try {
                if (!checkConditionValue(value, comparator, targetValue)) { return false; }
            } catch (IllegalArgumentException iaE) { return false; }
        } while (m.find());
        return true;
    }

    protected boolean checkConditionValue(String value, String comparator, String target)
        throws IllegalArgumentException {
        // Check to see if we're talking about times, and if so, de-time them.
        if (comparator.contains("<") || comparator.contains(">") || comparator.contains("%")) {
            // We're doing maths. Are they times? Times have colons.
            if (value.contains(":")) {
                String[] s = value.split(":");
                if (s.length == 2) try {
                        value = String.valueOf(Long.parseLong(s[0]) * 60 + Long.parseLong(s[1]));
                    } catch (NumberFormatException nfE) {}
            }
            if (target.contains(":")) {
                String[] s = target.split(":");
                if (s.length == 2) try {
                        target = String.valueOf(Long.parseLong(s[0]) * 60 + Long.parseLong(s[1]));
                    } catch (NumberFormatException nfE) {}
            }
        }
        if ("=".equals(comparator)) {
            return value.equals(target);
        } else if ("!=".equals(comparator)) {
            return !value.equals(target);
        } else if ("<".equals(comparator)) {
            try {
                return (Long.parseLong(value) < Long.parseLong(target));
            } catch (NumberFormatException nfE) {}
            try {
                return (Double.parseDouble(value) < Double.parseDouble(target));
            } catch (NumberFormatException nfE) {}
            return (value.compareTo(target) < 0);
        } else if ("<=".equals(comparator)) {
            try {
                return (Long.parseLong(value) <= Long.parseLong(target));
            } catch (NumberFormatException nfE) {}
            try {
                return (Double.parseDouble(value) <= Double.parseDouble(target));
            } catch (NumberFormatException nfE) {}
            return (value.compareTo(target) <= 0);
        } else if (">".equals(comparator)) {
            try {
                return (Long.parseLong(value) > Long.parseLong(target));
            } catch (NumberFormatException nfE) {}
            try {
                return (Double.parseDouble(value) > Double.parseDouble(target));
            } catch (NumberFormatException nfE) {}
            return (value.compareTo(target) > 0);
        } else if (">=".equals(comparator)) {
            try {
                return (Long.parseLong(value) >= Long.parseLong(target));
            } catch (NumberFormatException nfE) {}
            try {
                return (Double.parseDouble(value) >= Double.parseDouble(target));
            } catch (NumberFormatException nfE) {}
            return (value.compareTo(target) >= 0);
        } else if ("%".equals(comparator)) {
            try {
                return (0 == (Long.parseLong(value) % Long.parseLong(target)));
            } catch (NumberFormatException nfE) {
                return false;
            } catch (ArithmeticException aE) { // most likely target == 0, % by 0 is invalid
                return false;
            }
        }
        throw new IllegalArgumentException("Invalid comparator : " + comparator);
    }

    public ScoreBoardCondition<?> getScoreBoardCondition(String format) throws IllegalArgumentException {
        Matcher m = eventPattern.matcher(format);
        if (!m.find()) { throw new IllegalArgumentException("No valid event specified"); }
        return getFormatSpecifierScoreBoardCondition(m.group(1));
    }

    protected ScoreBoardCondition<?> getFormatSpecifierScoreBoardCondition(String formatSpecifier)
        throws IllegalArgumentException {
        ScoreBoardValue<?> value = scoreBoardValues.get(formatSpecifier);
        if (null == value) { throw new IllegalArgumentException("Not a valid format specifier : " + formatSpecifier); }
        return value.getScoreBoardCondition();
    }

    protected String getFormatSpecifierValue(String formatSpecifier) {
        ScoreBoardValue<?> value = scoreBoardValues.get(formatSpecifier);
        if (null == value) { return formatSpecifier; }
        return value.getValue();
    }

    public ScoreBoardValue<?> getFormatSpecifierScoreBoardValue(String formatSpecifier) {
        return scoreBoardValues.get(formatSpecifier);
    }

    protected void setupScoreBoardValues() {
        new ScoreBoardValue<TimeoutOwner>("%sbto", "ScoreBoard Timeout Owner", Game.TIMEOUT_OWNER) {
            @Override
            public String getValue() {
                TimeoutOwner to = getGame().get(Game.TIMEOUT_OWNER);
                return to == null ? "" : to.getId();
            }
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getGame();
            }
        };
        new ScoreBoardValue<Boolean>("%sbip", "ScoreBoard Is In Period", Game.IN_PERIOD) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getGame();
            }
        };
        new ScoreBoardValue<Boolean>("%sbio", "ScoreBoard Is In Overtime", Game.IN_OVERTIME) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getGame();
            }
        };
        new ScoreBoardValue<Boolean>("%sbos", "ScoreBoard Is Score Official", Game.OFFICIAL_SCORE) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getGame();
            }
        };

        setupTeamValues("1", Team.ID_1);
        setupTeamValues("2", Team.ID_2);
        setupClockValues("p", Clock.ID_PERIOD);
        setupClockValues("j", Clock.ID_JAM);
        setupClockValues("l", Clock.ID_LINEUP);
        setupClockValues("t", Clock.ID_TIMEOUT);
        setupClockValues("i", Clock.ID_INTERMISSION);

        StringBuffer patternBuffer = new StringBuffer();
        Iterator<String> patterns = scoreBoardValues.keySet().iterator();
        while (patterns.hasNext()) { patternBuffer.append(patterns.next() + "|"); }
        String specifiersRegex = patternBuffer.toString().replaceAll("[|]$", "");
        formatPattern = Pattern.compile(specifiersRegex);
        eventPattern = Pattern.compile("^\\s*(" + specifiersRegex + ")");
        conditionPattern = Pattern.compile("(" + specifiersRegex + ")(?:(" + comparatorRegex + ")(\\S+))?");
    }

    protected void setupTeamValues(String t, final String id) {
        new ScoreBoardValue<String>("%t" + t + "n", "Team " + t + " Name", Team.DISPLAY_NAME) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getTeam(id);
            }
        };
        new ScoreBoardValue<ValWithId>("%t" + t + "Nt", "Team " + t + " Twitter Name", Team.ALTERNATE_NAME) {
            @Override
            public String getValue() {
                try {
                    return getTeam(id).get(Team.ALTERNATE_NAME, Team.AlternateNameId.TWITTER.toString()).getValue();
                } catch (NullPointerException npE) {
                    MirrorScoreBoardEventProvider<Team> team = getTeam(id);
                    return team == null ? "" : team.get(Team.INITIALS);
                }
            }
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getTeam(id);
            }
        };
        new ScoreBoardValue<Integer>("%t" + t + "s", "Team " + t + " Score", Team.SCORE) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getTeam(id);
            }
        };
        new ScoreBoardValue<Integer>("%t" + t + "t", "Team " + t + " Timeouts", Team.TIMEOUTS) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getTeam(id);
            }
        };
        new ScoreBoardValue<Integer>("%t" + t + "or", "Team " + t + " Official Reviews", Team.OFFICIAL_REVIEWS) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getTeam(id);
            }
        };
        new ScoreBoardValue<Boolean>("%t" + t + "l", "Team " + t + " is Lead Jammer", Team.DISPLAY_LEAD) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getTeam(id);
            }
        };
        setupPositionValues(t, "j", id, FloorPosition.JAMMER);
        setupPositionValues(t, "p", id, FloorPosition.PIVOT);
        setupPositionValues(t, "b1", id, FloorPosition.BLOCKER1);
        setupPositionValues(t, "b2", id, FloorPosition.BLOCKER2);
        setupPositionValues(t, "b3", id, FloorPosition.BLOCKER3);
    }

    protected void setupPositionValues(String t, String p, final String id, final FloorPosition fp) {
        new ScoreBoardValue<String>("%t" + t + p + "n", "Team " + t + " " + fp.toString() + " Name", Position.NAME) {
            @Override
            public String getPreviousValue(Object o) {
                return getSkaterName((CurrentSkater) o);
            }
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getPosition(id, fp);
            }
        };
        new ScoreBoardValue<String>("%t" + t + p + "N", "Team " + t + " " + fp.toString() + " Number",
                                    Position.ROSTER_NUMBER) {
            @Override
            public String getPreviousValue(Object o) {
                return getSkaterNumber((CurrentSkater) o);
            }
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getPosition(id, fp);
            }
        };
    }

    protected MirrorScoreBoardEventProvider<Team> getTeam(String id) { return getGame().getMirror(Game.TEAM, id); }

    protected MirrorScoreBoardEventProvider<Position> getPosition(String id, FloorPosition position) {
        MirrorScoreBoardEventProvider<Team> team = getTeam(id);
        return team == null ? null : team.getMirror(Team.POSITION, position.toString());
    }

    protected String getSkaterName(MirrorScoreBoardEventProvider<Skater> s) {
        return (null == s ? NO_SKATER_NAME_VALUE : s.get(Skater.NAME));
    }
    protected String getSkaterNumber(MirrorScoreBoardEventProvider<Skater> s) {
        return (null == s ? NO_SKATER_NUMBER_VALUE : s.get(Skater.ROSTER_NUMBER));
    }

    protected void setupClockValues(String c, final String id) {
        new ScoreBoardValue<String>("%c" + c + "n", "Clock " + id + " Name", Clock.NAME) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getClock(id);
            }
        };
        new ScoreBoardValue<Integer>("%c" + c + "N", "Clock " + id + " Number", Clock.NUMBER) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getClock(id);
            }
        };
        new ScoreBoardValue<Boolean>("%c" + c + "r", "Clock " + id + " is Running", Clock.RUNNING) {
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getClock(id);
            }
        };
        new ScoreBoardValue<Long>("%c" + c + "ts", "Clock " + id + " Time (seconds)", Clock.TIME) {
            @Override
            public String getValue() {
                return getClockSecs(id);
            }
            @Override
            public String getPreviousValue(Object o) {
                return getClockSecs(((Long) o), getClock(id).get(Clock.DIRECTION));
            }
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getClock(id);
            }
        };
        new ScoreBoardValue<Long>("%c" + c + "tms", "Clock " + id + " Time (min:sec)", Clock.TIME) {
            @Override
            public String getValue() {
                return getClockMinSecs(id);
            }
            @Override
            public String getPreviousValue(Object o) {
                return getClockMinSecs(((Long) o), getClock(id).get(Clock.DIRECTION));
            }
            @Override
            public ScoreBoardEventProvider getProvider() {
                return getClock(id);
            }
        };
    }

    protected MirrorScoreBoardEventProvider<Clock> getClock(String id) { return getGame().getMirror(Game.CLOCK, id); }

    protected String getClockSecs(String id) {
        MirrorScoreBoardEventProvider<Clock> clock = getClock(id);
        return clock == null ? "0" : getClockSecs(clock.get(Clock.TIME), clock.get(Clock.DIRECTION));
    }
    protected String getClockSecs(long time, boolean roundUp) {
        long roundedTime = time / 1000;
        if (roundUp && time % 1000 != 0) { roundedTime++; }
        return String.valueOf(roundedTime);
    }
    protected String getClockMinSecs(String id) {
        MirrorScoreBoardEventProvider<Clock> clock = getClock(id);
        return clock == null ? "0" : getClockMinSecs(clock.get(Clock.TIME), clock.get(Clock.DIRECTION));
    }
    protected String getClockMinSecs(long time, boolean roundUp) {
        long roundedTime = time / 1000;
        if (roundUp && time % 1000 != 0) { roundedTime++; }
        String min = String.valueOf(roundedTime / 60);
        String sec = String.valueOf(roundedTime % 60);
        if (sec.length() == 1) { sec = "0" + sec; }
        return min + ":" + sec;
    }

    protected CurrentGame getGame() { return scoreBoard.getCurrentGame(); }

    protected ScoreBoard scoreBoard = null;

    protected Pattern formatPattern;
    protected Pattern eventPattern;
    protected Pattern conditionPattern;

    protected String comparatorRegex = "=|!=|<|<=|>|>=|%";

    protected Map<String, ScoreBoardValue<?>> scoreBoardValues = new LinkedHashMap<>();

    public abstract class ScoreBoardValue<T> {
        protected ScoreBoardValue(String f, String d, Property<T> prop) {
            format = f;
            description = d;
            property = prop;
            updateCondition();
            scoreBoardValues.put(format, this);
        }
        public String getValue() {
            ScoreBoardEventProvider provider = getProvider();
            return provider == null ? "" : String.valueOf(provider.get((Value<T>) property));
        }
        public abstract ScoreBoardEventProvider getProvider();
        public String getPreviousValue(Object value) { return String.valueOf(value); }
        public String getDescription() { return description; }
        public ScoreBoardCondition<T> getScoreBoardCondition() { return scoreBoardCondition; }
        public void setListener(ConditionalScoreBoardListener<T> listener) { this.listener = listener; }
        public void updateCondition() {
            ScoreBoardEventProvider provider = getProvider();
            scoreBoardCondition = new ScoreBoardCondition<>(provider == null ? scoreBoard : provider, property);
            if (listener != null) { listener.setCondition(scoreBoardCondition); }
        }

        protected String format;
        protected String description;
        Property<T> property;
        protected ScoreBoardCondition<T> scoreBoardCondition;
        protected ConditionalScoreBoardListener<T> listener;
    }

    public static final String NO_SKATER_NAME_VALUE = "";
    public static final String NO_SKATER_NUMBER_VALUE = "";
}
