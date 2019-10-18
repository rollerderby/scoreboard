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
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

public interface ScoreBoardEventProvider extends ValueWithId, Comparable<ScoreBoardEventProvider> {
    /**
     * This should be the frontend string for the Child enum value corresponding to this type
     * in its parent element
     */
    public String getProviderName();
    /**
     * This should return the class or interface that this type will be accessed through
     * by event receivers
     */
    public Class<? extends ScoreBoardEventProvider> getProviderClass();
    /**
     * Id to be used in order to identify this element amongst its siblings.
     *  (Could e.g. be a Period/Jam/etc number or a UUID.)
     */
    public String getProviderId();
    /**
     * The parent element.
     */
    public ScoreBoardEventProvider getParent();
    /**
     * remove all references to this element
     */
    public void unlink();
    /**
     * This should return all the enums that contain values, children, or commands
     * that can be accessed from the frontend
     */
    public List<Class<? extends Property>> getProperties();

    public void addScoreBoardListener(ScoreBoardListener listener);
    public void removeScoreBoardListener(ScoreBoardListener listener);

    public Object valueFromString(PermanentProperty prop, String sValue, Flag flag);
    public Object get(PermanentProperty prop);
    //return value indicates if value was changed
    public boolean set(PermanentProperty prop, Object value);
    /*
     * return value indicates if value was changed
     * Change flag for Integer and Long values is implemented to add the given
     * value to the previous one. Other flags need to be implemented in overrides.
     */
    public boolean set(PermanentProperty prop, Object value, Flag flag);

    /**
     * Run the given function inside a batch, to combine any resultant events.
     */
    public void runInBatch(Runnable r);

    /**
     * If create is implemented for the respective type, this function will resort to that,
     * ignoring sValue.
     * Otherwise it will create a ValWithId from id and sValue.
     */
    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue);
    /*
     * Will return null if no such child is found
     */
    public ValueWithId get(AddRemoveProperty prop, String id);
    public ValueWithId get(NumberedProperty prop, Integer num);
    public ValueWithId getOrCreate(AddRemoveProperty prop, String id);
    public ValueWithId getOrCreate(NumberedProperty prop, Integer num);
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop);
    public OrderedScoreBoardEventProvider<?> getFirst(NumberedProperty prop);
    public OrderedScoreBoardEventProvider<?> getLast(NumberedProperty prop);
    //returns true, if a value was either changed or added
    public boolean add(AddRemoveProperty prop, ValueWithId item);
    //returns true, if a value was removed
    public boolean remove(AddRemoveProperty prop, String id);
    //returns true, if a value was removed
    public boolean remove(AddRemoveProperty prop, ValueWithId item);
    public void removeAll(AddRemoveProperty prop);
    /**
     * Must call an appropriate constructor for all children that are themselves a
     * ScoreBoardEventProvider and can be created from the frontend or autosave
     */
    public ValueWithId create(AddRemoveProperty prop, String id);
    public Integer getMinNumber(NumberedProperty prop);
    public Integer getMaxNumber(NumberedProperty prop);

    /**
     * Defaults to doing nothing. Should be overridden in classes that have
     * frontend commands.
     */
    public void execute(CommandProperty prop);

    public ScoreBoard getScoreBoard();

    public ScoreBoardEventProvider getElement(Class<?> type, String id);

    public enum Flag {
        CHANGE,
        RESET,
        FROM_AUTOSAVE,
        INVERSE_REFERENCE,
        COPY,
        RECALCULATE,
        INTERNAL;
    }
}
