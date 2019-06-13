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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider.IValue;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

/**
 * Bulk set ScoreBoard atttributes with JSON paths.
 */
public class ScoreBoardJSONSetter {

    // Make a list of sets to a scoreboard, with JSON paths to fields.
    public static void set(ScoreBoard sb, List<JSONSet> js) {
        List<ValueSet> postponedSets = new ArrayList<>();
        for(JSONSet s : js) {
            Matcher m = pathElementPattern.matcher(s.path);
            if (m.matches() && m.group("name").equals("ScoreBoard") &&
                    m.group("id") == null && m.group("remainder") != null) {
                set(sb, m.group("remainder"), s.value, s.flag, postponedSets);
            } else {
                ScoreBoardManager.printMessage("Illegal path: " + s.path);
            }
        }
        for (ValueSet vs : postponedSets) {
            vs.process();
        }
    }

    private static void set(ScoreBoardEventProvider p, String path, String value, Flag flag, List<ValueSet> postponedSets) {
        Matcher m = pathElementPattern.matcher(path);
        if (m.matches()) {
            String name = m.group("name");
            String elementId = m.group("id");
            String remainder = m.group("remainder");
            if (elementId == null) { elementId = ""; }
            try {
                Property prop = PropertyConversion.fromFrontend(name, p.getProperties());
                if (prop == null) { throw new IllegalArgumentException("Unknown property"); }

                if (prop == IValue.ID) {
                    p.set((PermanentProperty) prop, p.valueFromString((PermanentProperty) prop, value, flag), flag);
                } else if (prop instanceof PermanentProperty) {
                    // postpone setting PermanentProperties except ID, as they may reference
                    //  elements not yet created when restoring from autosave
                    postponedSets.add(new ValueSet(p, (PermanentProperty) prop, value, flag));
                } else if (prop instanceof CommandProperty) {
                    if (Boolean.parseBoolean(value)) {
                        p.execute((CommandProperty)prop);
                    }
                } else if (remainder != null) {
                    set((ScoreBoardEventProvider)p.getOrCreate((AddRemoveProperty)prop, elementId), remainder, value, flag, postponedSets);
                } else if (value == null) {
                    p.remove((AddRemoveProperty)prop, elementId);
                } else {
                    p.add((AddRemoveProperty)prop, p.childFromString((AddRemoveProperty)prop, elementId, value));
                }
            } catch (Exception e) {
                ScoreBoardManager.printMessage("Exception handling update for " + p.getProviderName() +
                                               "(" + p.getProviderId() + ")." + name + "(" + elementId + ") - " + value + ": " + e.toString());
                e.printStackTrace();
            }
        } else {
            ScoreBoardManager.printMessage("Illegal path element: " + path);
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

    protected static class ValueSet {
        protected ValueSet(ScoreBoardEventProvider sbe, PermanentProperty prop, String value, Flag flag) {
            this.sbe = sbe;
            this.prop = prop;
            this.value = value;
            this.flag = flag;
        }

        public void process() { sbe.set(prop, sbe.valueFromString(prop, value, flag), flag); }

        private ScoreBoardEventProvider sbe;
        private PermanentProperty prop;
        private String value;
        private Flag flag;
    }

    private static final Pattern pathElementPattern = Pattern.compile("^(?<name>\\w+)(\\((?<id>[^\\)]*)\\))?(\\.(?<remainder>.*))?$");

}
