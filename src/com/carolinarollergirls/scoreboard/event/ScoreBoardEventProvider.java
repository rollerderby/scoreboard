package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;
import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
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

public interface ScoreBoardEventProvider extends ValueWithId{
    public String getProviderName();
    public Class<? extends ScoreBoardEventProvider> getProviderClass();
    public String getProviderId();
    public ScoreBoardEventProvider getParent();
    public List<Class<? extends Property>> getProperties();

    public void addScoreBoardListener(ScoreBoardListener listener);
    public void removeScoreBoardListener(ScoreBoardListener listener);
    
    public Object valueFromString(PermanentProperty prop, String sValue);
    public Object get(PermanentProperty prop);
    //return value indicates if value was changed
    public boolean set(PermanentProperty prop, Object value);
    public boolean set(PermanentProperty prop, Object value, Flag flag);
    
    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue);
    public ValueWithId get(AddRemoveProperty prop, String id);
    public ValueWithId get(AddRemoveProperty prop, String id, boolean add);
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop);
    public boolean add(AddRemoveProperty prop, ValueWithId item);
    public boolean remove(AddRemoveProperty prop, String id);
    public boolean remove(AddRemoveProperty prop, ValueWithId item);
    public void removeAll(AddRemoveProperty prop);
    public ValueWithId create(AddRemoveProperty prop, String id);
    
    public void execute(CommandProperty prop);
    
    public enum Flag {
	CHANGE,
	RESET,
	FORCE,
	CUSTOM;
    }
}
