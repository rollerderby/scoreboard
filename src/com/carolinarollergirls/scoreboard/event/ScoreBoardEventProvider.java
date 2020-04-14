package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;

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
     * This should return all the values, children, or commands that can be accessed
     * from the frontend
     */
    public Collection<Property<?>> getProperties();
    public Property<?> getProperty(String jsonName);

    public void addScoreBoardListener(ScoreBoardListener listener);
    public void removeScoreBoardListener(ScoreBoardListener listener);

    public <T> T valueFromString(Value<T> prop, String sValue);
    public <T> T get(Value<T> prop);
    // return value indicates if value was changed
    public <T> boolean set(Value<T> prop, T value);
    /*
     * return value indicates if value was changed Change flag for Integer and Long
     * values is implemented to add the given value to the previous one. Other flags
     * need to be implemented in overrides.
     */
    public <T> boolean set(Value<T> prop, T value, Flag flag);
    // return value indicates if value was changed
    public <T> boolean set(Value<T> prop, T value, Source source);
    /*
     * return value indicates if value was changed Change flag for Integer and Long
     * values is implemented to add the given value to the previous one. Other flags
     * need to be implemented in overrides.
     */
    public <T> boolean set(Value<T> prop, T value, Source source, Flag flag);

    /**
     * Run the given function inside a batch, to combine any resultant events.
     */
    public void runInBatch(Runnable r);

    /**
     * If create is implemented for the respective type, this function will resort
     * to that, ignoring sValue. Otherwise it will create a ValWithId from id and
     * sValue.
     */
    public <T extends ValueWithId> T childFromString(Child<T> prop, String id, String sValue);
    /*
     * Will return null if no such child is found
     */
    public <T extends ValueWithId> T get(Child<T> prop, String id);
    public <T extends OrderedScoreBoardEventProvider<T>> T get(NumberedChild<T> prop, Integer num);
    public <T extends ScoreBoardEventProvider> T getOrCreate(Child<T> prop, String id);
    public <T extends ScoreBoardEventProvider> T getOrCreate(Child<T> prop, String id, Source source);
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedChild<T> prop, Integer num);
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedChild<T> prop, Integer num,
            Source source);
    public <T extends ValueWithId> Collection<T> getAll(Child<T> prop);
    public <T extends OrderedScoreBoardEventProvider<T>> T getFirst(NumberedChild<T> prop);
    public <T extends OrderedScoreBoardEventProvider<T>> T getLast(NumberedChild<T> prop);
    public int numberOf(Child<?> prop);
    // returns true, if a value was either changed or added
    public <T extends ValueWithId> boolean add(Child<T> prop, T item);
    public <T extends ValueWithId> boolean add(Child<T> prop, T item, Source source);
    // returns true, if a value was removed
    public <T extends ValueWithId> boolean remove(Child<T> prop, String id);
    public <T extends ValueWithId> boolean remove(Child<T> prop, String id, Source source);
    public <T extends ValueWithId> boolean remove(Child<T> prop, T item);
    public <T extends ValueWithId> boolean remove(Child<T> prop, T item, Source source);
    public <T extends ValueWithId> void removeAll(Child<T> prop);
    public <T extends ValueWithId> void removeAll(Child<T> prop, Source source);
    /**
     * Must call an appropriate constructor for all children that are themselves a
     * ScoreBoardEventProvider and can be created from the frontend or autosave
     */
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source);
    public Integer getMinNumber(NumberedChild<?> prop);
    public Integer getMaxNumber(NumberedChild<?> prop);

    public void execute(Command prop);
    /**
     * Defaults to doing nothing. Should be overridden in classes that have frontend
     * commands.
     */
    public void execute(Command prop, Source source);

    public ScoreBoard getScoreBoard();

    public <T extends ValueWithId> T getElement(Class<T> type, String id);

    public void checkProperty(Property<?> prop);

    public static final Value<String> ID = new Value<>(String.class, "Id", "");
    public static final Value<Boolean> READONLY = new Value<>(Boolean.class, "Readonly", false);

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
