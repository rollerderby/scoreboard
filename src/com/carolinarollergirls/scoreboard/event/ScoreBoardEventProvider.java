package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard. The CRG
 * ScoreBoard is licensed under either the GNU General Public License version 3
 * (or later), or the Apache License 2.0, at your option. See the file COPYING
 * for details.
 */

public interface ScoreBoardEventProvider extends ValueWithId, Comparable<ScoreBoardEventProvider> {
    /**
     * This should be the frontend string for the Child enum value corresponding to
     * this type in its parent element
     */
    public String getProviderName();
    /**
     * This should return the class or interface that this type will be accessed
     * through by event receivers
     */
    public Class<? extends ScoreBoardEventProvider> getProviderClass();
    /**
     * Id to be used in order to identify this element amongst its siblings. (Could
     * e.g. be a Period/Jam/etc number or a UUID.)
     */
    public String getProviderId();
    /**
     * The parent element.
     */
    public ScoreBoardEventProvider getParent();
    /**
     * remove all references to this element
     */
    public void delete();
    /**
     * remove all references to this element
     */
    public void delete(Source source);
    /**
     * This should return all the enums that contain values, children, or commands
     * that can be accessed from the frontend
     */
    public List<Class<? extends Property>> getProperties();

    public void addScoreBoardListener(ScoreBoardListener listener);
    public void removeScoreBoardListener(ScoreBoardListener listener);

    public Object valueFromString(PermanentProperty prop, String sValue);
    public Object get(PermanentProperty prop);
    // return value indicates if value was changed
    public boolean set(PermanentProperty prop, Object value);
    /*
     * return value indicates if value was changed Change flag for Integer and Long
     * values is implemented to add the given value to the previous one. Other flags
     * need to be implemented in overrides.
     */
    public boolean set(PermanentProperty prop, Object value, Flag flag);
    // return value indicates if value was changed
    public boolean set(PermanentProperty prop, Object value, Source source);
    /*
     * return value indicates if value was changed Change flag for Integer and Long
     * values is implemented to add the given value to the previous one. Other flags
     * need to be implemented in overrides.
     */
    public boolean set(PermanentProperty prop, Object value, Source source, Flag flag);

    /**
     * Run the given function inside a batch, to combine any resultant events.
     */
    public void runInBatch(Runnable r);

    /**
     * If create is implemented for the respective type, this function will resort
     * to that, ignoring sValue. Otherwise it will create a ValWithId from id and
     * sValue.
     */
    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue);
    /*
     * Will return null if no such child is found
     */
    public <T extends ValueWithId> T get(AddRemoveProperty prop, Class<T> t, String id);
    public <T extends OrderedScoreBoardEventProvider<T>> T get(NumberedProperty prop, Class<T> t, Integer num);
    public <T extends ValueWithId> T getOrCreate(AddRemoveProperty prop, Class<T> t, String id);
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedProperty prop, Class<T> t, Integer num);
    public <T extends ValueWithId> T getOrCreate(AddRemoveProperty prop, Class<T> t, String id, Source source);
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedProperty prop, Class<T> t, Integer num,
            Source source);
    public <T extends ValueWithId> Collection<T> getAll(AddRemoveProperty prop, Class<T> t);
    public <T extends OrderedScoreBoardEventProvider<T>> T getFirst(NumberedProperty prop, Class<T> t);
    public <T extends OrderedScoreBoardEventProvider<T>> T getLast(NumberedProperty prop, Class<T> t);
    public int numberOf(AddRemoveProperty prop);
    // returns true, if a value was either changed or added
    public boolean add(AddRemoveProperty prop, ValueWithId item);
    public boolean add(AddRemoveProperty prop, ValueWithId item, Source source);
    // returns true, if a value was removed
    public boolean remove(AddRemoveProperty prop, String id);
    public boolean remove(AddRemoveProperty prop, String id, Source source);
    public boolean remove(AddRemoveProperty prop, ValueWithId item);
    public boolean remove(AddRemoveProperty prop, ValueWithId item, Source source);
    public void removeAll(AddRemoveProperty prop);
    public void removeAll(AddRemoveProperty prop, Source source);
    /**
     * Must call an appropriate constructor for all children that are themselves a
     * ScoreBoardEventProvider and can be created from the frontend or autosave
     */
    public ScoreBoardEventProvider create(AddRemoveProperty prop, String id, Source source);
    public Integer getMinNumber(NumberedProperty prop);
    public Integer getMaxNumber(NumberedProperty prop);

    public void execute(CommandProperty prop);
    /**
     * Defaults to doing nothing. Should be overridden in classes that have frontend
     * commands.
     */
    public void execute(CommandProperty prop, Source source);

    public ScoreBoard getScoreBoard();

    public ScoreBoardEventProvider getElement(Class<?> type, String id);

    public enum IValue implements PermanentProperty {
        ID(String.class, ""),
        READONLY(Boolean.class, false),
        NUMBER(Integer.class, 0),
        PREVIOUS(OrderedScoreBoardEventProvider.class, null),
        NEXT(OrderedScoreBoardEventProvider.class, null);

        private IValue(Class<?> t, Object dv) {
            type = t;
            defaultValue = dv;
        }

        private final Class<?> type;
        private final Object defaultValue;

        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }

    public enum Source {
        WS(false, false),
        AUTOSAVE(false, true),
        JSON(false, true),
        DEFAULTS(false, true),
        INVERSE_REFERENCE(true, false),
        COPY(true, false),
        RECALCULATE(true, false),
        UNLINK(true, false),
        RENUMBER(true, false),
        OTHER(true, false),

        // the following are intended for use as writeProtection Override only;
        ANY_INTERNAL(true, false),
        ANY_FILE(false, true);

        private Source(boolean i, boolean f) {
            internal = i;
            file = f;
        }

        private final boolean internal;
        private final boolean file;

        public boolean isInternal() { return internal; }
        public boolean isFile() { return file; }
    }

    public enum Flag {
        CHANGE,
        RESET,
        SPECIAL_CASE;
    }
}
