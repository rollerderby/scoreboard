package com.carolinarollergirls.scoreboard.json;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carolinarollergirls.scoreboard.core.interfaces.Clients;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

/**
 * Bulk set ScoreBoard atttributes with JSON paths.
 */
public class ScoreBoardJSONSetter {

    // check the version of the incoming update and update if necessary
    public static void updateToCurrentVersion(Map<String, Object> state) {
        String version = (String) state.get("ScoreBoard.Version(release)");
        if (version == null) { version = getVersionFromKeys(state.keySet()); }

        if (version.equals("v5")) { return; } // no update needed

        // When updating to v5 we need to move stuff to a newly created game which needs an id
        String newGameId = UUID.randomUUID().toString();

        for (String oldKey : new HashSet<>(state.keySet())) {
            String newKey = oldKey;
            String keyVersion = version;

            if (keyVersion.equals("v4")) {
                if ((newKey.startsWith("ScoreBoard.Clock(") && newKey.endsWith(".MinimumTime"))) {
                    newKey = "";
                } else if (newKey.startsWith("ScoreBoard.Team(") && newKey.endsWith(".LastEndedTeamJam")) {
                    newKey = "";
                } else if (newKey.endsWith(".Number") &&
                           (newKey.contains(".Position(") || newKey.contains(".Skater(")) &&
                           !newKey.contains(".Penalty(")) {
                    newKey = newKey.replace(".Number", ".RosterNumber");
                }
                keyVersion = "v4.1";
            }

            if (keyVersion.equals("v4.1")) {
                if (newKey.startsWith("ScoreBoard.Rulesets.CurrentRule(")) {
                    newKey =
                        newKey.replace("ScoreBoard.Rulesets.CurrentRule(", "ScoreBoard.Game(" + newGameId + ").Rule(");
                } else if (newKey.startsWith("ScoreBoard.Rulesets.Current") ||
                           newKey.startsWith("ScoreBoard.Rulesets.RuleDefinition(")) {
                    newKey = "";
                } else if (newKey.startsWith("ScoreBoard.Rulesets.Ruleset(") && newKey.endsWith(".ParentId")) {
                    newKey = newKey.replace(".ParentId", ".Parent");
                } else if (newKey.startsWith("ScoreBoard.PenaltyCodes.Code(")) {
                    newKey = newKey.replace("ScoreBoard.PenaltyCodes.Code(",
                                            "ScoreBoard.Game(" + newGameId + ").PenaltyCode(");
                } else if (newKey.equals("ScoreBoard.Team(1).Name") || newKey.equals("ScoreBoard.Team(2).Name")) {
                    newKey = newKey.replace("ScoreBoard.", "ScoreBoard.Game(" + newGameId + ").")
                                 .replace(".Name", ".TeamName");
                } else if (newKey.startsWith("ScoreBoard.Clock(") || newKey.startsWith("ScoreBoard.Period(") ||
                           newKey.startsWith("ScoreBoard.Jam(") || newKey.startsWith("ScoreBoard.Team(") ||
                           newKey.equals("ScoreBoard.CurrentPeriodNumber") ||
                           newKey.equals("ScoreBoard.CurrentPeriod") || newKey.equals("ScoreBoard.UpcomingJam") ||
                           newKey.equals("ScoreBoard.UpcomingJamNumber") || newKey.equals("ScoreBoard.InPeriod") ||
                           newKey.equals("ScoreBoard.InJam") || newKey.equals("ScoreBoard.InOvertime") ||
                           newKey.equals("ScoreBoard.OfficialScore") || newKey.equals("ScoreBoard.CurrentTimeout") ||
                           newKey.equals("ScoreBoard.TimeoutOwner") || newKey.equals("ScoreBoard.OfficialReview") ||
                           newKey.equals("ScoreBoard.NoMoreJam")) {
                    newKey = newKey.replace("ScoreBoard.", "ScoreBoard.Game(" + newGameId + ").");
                }
                keyVersion = "v5";
            }

            if (!newKey.equals(oldKey)) {
                if (!newKey.equals("")) { state.put(newKey, state.get(oldKey)); }
                state.remove(oldKey);
            }
        }
    }

    private static String getVersionFromKeys(Set<String> keys) {
        String minVersion = "v4"; // lowest version possible from the keys seen so far
        String maxVersion = "v5"; // highest version possible from the keys seen so far

        for (String key : keys) {
            minVersion = minVersionWith(key, minVersion);
            maxVersion = maxVersionWith(key, maxVersion);
            if (minVersion.equals(maxVersion)) return minVersion;
        }
        // return highest possible version so unaplicable updates are skippped
        return maxVersion;
    }

    private static String minVersionWith(String key, String priorLimit) {
        if (priorLimit.equals("v5") || key.startsWith("ScoreBoard.Game(") ||
            key.startsWith("ScoreBoard.CurrentGame.")) {
            return "v5";
        }
        if (priorLimit.equals("v4.1") || key.startsWith("ScoreBoard.Clients.") || key.endsWith(".RosterNumber") ||
            key.endsWith(".ReadOnly") || (key.endsWith(".Annotation") && key.contains(".ScoringTrip("))) {
            return "v4.1";
        }
        return priorLimit;
    }
    private static String maxVersionWith(String key, String priorLimit) {
        if (priorLimit.equals("v4") || (key.startsWith("ScoreBoard.Clock(") && key.endsWith(".MinimumTime")) ||
            (key.startsWith("ScoreBoard.Team(") && key.endsWith(".LastEndedTeamJam")) ||
            (key.endsWith(".Number") && (key.contains(".Position(") || key.contains(".Skater(")))) {
            return "v4";
        }
        if (priorLimit.equals("v4.1") || key.startsWith("ScoreBoard.Clock(") || key.startsWith("ScoreBoard.Period(") ||
            key.startsWith("ScoreBoard.Jam(") || key.startsWith("ScoreBoard.Team(") ||
            key.startsWith("ScoreBoard.PenaltyCodes.") || key.startsWith("ScoreBoard.Rulesets.Current") ||
            (key.startsWith("ScoreBoard.Rulesets.Ruleset(") && key.endsWith(".ParentId")) ||
            key.equals("ScoreBoard.CurrentPeriodNumber") || key.equals("ScoreBoard.CurrentPeriod") ||
            key.equals("ScoreBoard.UpcomingJam") || key.equals("ScoreBoard.UpcomingJamNumber") ||
            key.equals("ScoreBoard.InPeriod") || key.equals("ScoreBoard.InJam") ||
            key.equals("ScoreBoard.InOvertime") || key.equals("ScoreBoard.OfficialScore") ||
            key.equals("ScoreBoard.CurrentTimeout") || key.equals("ScoreBoard.TimeoutOwner") ||
            key.equals("ScoreBoard.OfficialReview") || key.equals("ScoreBoard.NoMoreJam")) {
            return "v4.1";
        }
        return priorLimit;
    }

    // Make a list of sets to a scoreboard, with JSON paths to fields.
    public static void set(ScoreBoard sb, Map<String, Object> state, Source source) {
        List<JSONSet> jsl = new ArrayList<>();
        for (String key : state.keySet()) {
            Object value = state.get(key);
            String v;
            if (value == null) {
                v = null;
            } else {
                v = value.toString();
            }
            jsl.add(new JSONSet(key, v, null));
        }
        ScoreBoardJSONSetter.set(sb, jsl, source);
    }

    public static void set(ScoreBoard sb, List<JSONSet> jsl, Source source) {
        List<PropertySet> postponedSets = new ArrayList<>();
        for (JSONSet s : jsl) {
            Matcher m = pathElementPattern.matcher(s.path);
            if (m.matches() && m.group("name").equals("ScoreBoard") && m.group("id") == null &&
                m.group("remainder") != null) {
                set(sb, m.group("remainder"), s.value, source, s.flag, postponedSets);
            } else {
                Logger.printMessage("Illegal path: " + s.path);
            }
        }
        for (PropertySet vs : postponedSets) { vs.process(); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(ScoreBoardEventProvider p, String path, String value, Source source, Flag flag,
                            List<PropertySet> postponedSets) {
        Matcher m = pathElementPattern.matcher(path);
        if (m.matches()) {
            String name = m.group("name");
            String elementId = m.group("id");
            String remainder = m.group("remainder");
            if (elementId == null) { elementId = ""; }
            String readable = p.getProviderName() + "(" + p.getProviderId() + ")." + name + "(" + elementId + ")";
            try {
                Property prop = p.getProperty(name);
                if (prop == null) {
                    Logger.printMessage("Unknown property " + readable);
                    return;
                }

                if (prop == ScoreBoardEventProvider.ID) {
                    p.set((Value) prop, p.valueFromString((Value) prop, value), source, flag);
                } else if (prop instanceof Value) {
                    // postpone setting PermanentProperties except ID, as they may reference
                    // elements not yet created when restoring from autosave
                    postponedSets.add(new ValueSet(p, (Value) prop, value, source, flag));
                } else if (prop instanceof Command) {
                    if (Boolean.parseBoolean(value)) { p.execute((Command) prop, source); }
                } else if (remainder != null) {
                    @SuppressWarnings("unchecked")
                    ScoreBoardEventProvider o =
                        p.getOrCreate((Child<? extends ScoreBoardEventProvider>) prop, elementId, source);
                    if (o == null) {
                        if (source.isFile()) {
                            // filter out elements that we expect to fail on each startup
                            if (p.getProviderClass() == CurrentGame.class) { return; }
                            if (prop == Clients.CLIENT) { return; }
                            if (prop == Game.EXPULSION) { return; }
                        }
                        Logger.printMessage("Could not get or create property " + readable);
                        return;
                    }
                    set(o, remainder, value, source, flag, postponedSets);
                } else if (value == null) {
                    p.remove((Child<?>) prop, elementId, source);
                } else if (prop.getType() == ValWithId.class) {
                    Child aprop = (Child) prop;
                    p.add(aprop, p.childFromString(aprop, elementId, value), source);
                } else {
                    postponedSets.add(new ChildSet(p, (Child) prop, elementId, value, source));
                }
            } catch (Exception e) {
                Logger.printMessage("Exception handling update for " + readable + " - " + value + ": " + e.toString());
                e.printStackTrace();
            }
        } else {
            Logger.printMessage("Illegal path element: " + path);
        }
    }

    public static class JSONSet {
        public JSONSet(String path, String value, Flag flag) {
            this.path = path;
            this.value = value;
            this.flag = flag;
        }

        public final String path;
        public final String value;
        public final Flag flag;
    }

    protected static interface PropertySet { public void process(); }

    protected static class ValueSet<T> implements PropertySet {
        protected ValueSet(ScoreBoardEventProvider sbe, Value<T> prop, String value, Source source, Flag flag) {
            this.sbe = sbe;
            this.prop = prop;
            this.value = value;
            this.source = source;
            this.flag = flag;
        }

        @Override
        public void process() {
            sbe.set(prop, sbe.valueFromString(prop, value), source, flag);
        }

        private ScoreBoardEventProvider sbe;
        private Value<T> prop;
        private String value;
        private Source source;
        private Flag flag;
    }

    protected static class ChildSet<T extends ScoreBoardEventProvider> implements PropertySet {
        protected ChildSet(ScoreBoardEventProvider sbe, Child<T> prop, String id, String value, Source source) {
            this.sbe = sbe;
            this.prop = prop;
            this.id = id;
            this.value = value;
            this.source = source;
        }

        @Override
        public void process() {
            sbe.add(prop, sbe.childFromString(prop, id, value), source);
        }

        private ScoreBoardEventProvider sbe;
        private Child<T> prop;
        private String id;
        private String value;
        private Source source;
    }

    private static final Pattern pathElementPattern =
        Pattern.compile("^(?<name>\\w+)(\\((?<id>[^\\)]*)\\))?(\\.(?<remainder>.*))?$");
}
