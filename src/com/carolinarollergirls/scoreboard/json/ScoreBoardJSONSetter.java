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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.Logger;

/**
 * Bulk set ScoreBoard atttributes with JSON paths.
 */
public class ScoreBoardJSONSetter {

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
        List<ValueSet<?>> postponedSets = new ArrayList<>();
        for (JSONSet s : jsl) {
            Matcher m = pathElementPattern.matcher(s.path);
            if (m.matches() && m.group("name").equals("ScoreBoard") && m.group("id") == null
                    && m.group("remainder") != null) {
                set(sb, m.group("remainder"), s.value, source, s.flag, postponedSets);
            } else {
                Logger.printMessage("Illegal path: " + s.path);
            }
        }
        for (ValueSet<?> vs : postponedSets) {
            vs.process();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void set(ScoreBoardEventProvider p, String path, String value, Source source, Flag flag,
            List<ValueSet<?>> postponedSets) {
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
                    if (Boolean.parseBoolean(value)) {
                        p.execute((Command) prop, source);
                    }
                } else if (remainder != null) {
                    @SuppressWarnings("unchecked")
                    ScoreBoardEventProvider o = p.getOrCreate((Child<? extends ScoreBoardEventProvider>) prop,
                            elementId, source);
                    if (o == null) {
                        Logger.printMessage("Could not get or create property " + readable);
                        return;
                    }
                    set(o, remainder, value, source, flag, postponedSets);
                } else if (value == null) {
                    p.remove((Child<?>) prop, elementId, source);
                } else {
                    Child aprop = (Child) prop;
                    p.add(aprop, p.childFromString(aprop, elementId, value), source);
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

    protected static class ValueSet<T> {
        protected ValueSet(ScoreBoardEventProvider sbe, Value<T> prop, String value, Source source, Flag flag) {
            this.sbe = sbe;
            this.prop = prop;
            this.value = value;
            this.source = source;
            this.flag = flag;
        }

        public void process() { sbe.set(prop, sbe.valueFromString(prop, value), source, flag); }

        private ScoreBoardEventProvider sbe;
        private Value<T> prop;
        private String value;
        private Source source;
        private Flag flag;
    }

    private static final Pattern pathElementPattern = Pattern
            .compile("^(?<name>\\w+)(\\((?<id>[^\\)]*)\\))?(\\.(?<remainder>.*))?$");

}
