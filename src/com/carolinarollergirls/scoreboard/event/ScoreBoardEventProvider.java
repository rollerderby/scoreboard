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

public interface ScoreBoardEventProvider extends ValueWithId {
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
     * Id to be used in order to identify this element amongst its siblings in XML and JSON.
     *  (Could e.g. be a Period/Jam/etc number or a UUID.)
     */
    public String getProviderId();
    /**
     * The parent element in the XML and JSON representation
     */
    public ScoreBoardEventProvider getParent();
    /**
     * This should return all the enums that contain values, children, or commands
     * that can be accessed from the frintend
     */
    public List<Class<? extends Property>> getProperties();

    public void addScoreBoardListener(ScoreBoardListener listener);
    public void removeScoreBoardListener(ScoreBoardListener listener);
    
    /**
     * Will automagically work for Boolean, Integer, and Long if there is a previous
     * value of the correct type. For all other cases this must be implemented in
     * derived classes.
     */
    public Object valueFromString(PermanentProperty prop, String sValue);
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
     * If create is implemented for the respective type, this function will resort to that,
     * ignoring sValue.
     * Otherwise it will create a ValWithId from id and sValue.
     */
    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue);
    /*
     * Will return null if no such child is found
     */
    public ValueWithId get(AddRemoveProperty prop, String id);
    /*
     * If no such child is found and add is true, call create(prop, id) to create one.
     * If creation is not implemented for this type of child or add is false, null is returned.
     */
    public ValueWithId get(AddRemoveProperty prop, String id, boolean add);
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop);
    public NumberedScoreBoardEventProvider<?> getFirst(NumberedProperty prop);
    public NumberedScoreBoardEventProvider<?> getLast(NumberedProperty prop);
    //returns true, if a value was either changed or added
    public boolean add(AddRemoveProperty prop, ValueWithId item);
    //returns true, if a value was added
    public boolean insert(NumberedProperty prop, NumberedScoreBoardEventProvider<?> item);
    //returns true, if a value was removed
    public boolean remove(AddRemoveProperty prop, String id);
    //returns true, if a value was removed
    public boolean remove(AddRemoveProperty prop, ValueWithId item);
    //returns true, if a value was removed
    public boolean remove(NumberedProperty prop, String id, boolean renumber);
    //returns true, if a value was removed
    public boolean remove(NumberedProperty prop, NumberedScoreBoardEventProvider<?> item, boolean renumber);
    //returns true, if a value was removed
    public boolean removeSilent(AddRemoveProperty prop, ValueWithId item);
    public void removeAll(AddRemoveProperty prop);
    /**
     * Must call an appropriate constructor for all children that are themselves a
     * ScoreBoardEventProvider and can be created from the frontend or autosave 
     */
    public ValueWithId create(AddRemoveProperty prop, String id);
    public int getMinNumber(NumberedProperty prop);
    public int getMaxNumber(NumberedProperty prop);
    
    /**
     * Defaults to doing nothing. Should be overridden in classes that have
     * frontend commands.
     */
    public void execute(CommandProperty prop);
    
    public ScoreBoard getScoreBoard();
    
    public <T extends ScoreBoardEventProvider> T getElement(Class<T> type, String id);
    
    public enum Flag {
	CHANGE,
	RESET,
	FROM_AUTOSAVE,
	INTERNAL;
    }
    
    public class PropertyReference {
	public PropertyReference(ScoreBoardEventProvider sourceElement, PermanentProperty sourceProperty,
		ScoreBoardEventProvider targetElement, PermanentProperty targetProperty, boolean readonly,
		Object defaultValue) {
	    this.sourceElement = sourceElement;
	    this.sourceProperty = sourceProperty;
	    this.targetElement = targetElement;
	    this.targetProperty = targetProperty;
	    this.readonly = readonly;
	    this.defaultValue = defaultValue;
	}
	
	public ScoreBoardEventProvider getSourceElement() { return sourceElement; }
	public PermanentProperty getSourceProperty() { return sourceProperty; }
	public ScoreBoardEventProvider getTargetElement() { return targetElement; }
	public PermanentProperty getTargetProperty() { return targetProperty; }
	public boolean isReadOnly() { return readonly; }
	public Object getDefaultValue() { return defaultValue; }
	
	private ScoreBoardEventProvider sourceElement;
	private PermanentProperty sourceProperty;
	private ScoreBoardEventProvider targetElement;
	private PermanentProperty targetProperty;
	private boolean readonly;
	private Object defaultValue;
    }
    
    public class IndirectPropertyReference extends PropertyReference {
	public IndirectPropertyReference(ScoreBoardEventProvider sourceElement, PermanentProperty sourceProperty,
		ScoreBoardEventProvider referenceElement, PermanentProperty referenceProperty,
		PermanentProperty targetProperty, boolean readonly, Object defaultValue) {
	    super(sourceElement, sourceProperty, null, targetProperty, readonly, defaultValue);
	    this.referenceElement = referenceElement;
	    this.referenceProperty = referenceProperty;
	}
	
	public ScoreBoardEventProvider getTargetElement() {
	    return (ScoreBoardEventProvider)referenceElement.get(referenceProperty);
	}
	public ScoreBoardEventProvider getReferenceElement() { return referenceElement; }
	public PermanentProperty getReferenceProperty() { return referenceProperty; }
	
	private ScoreBoardEventProvider referenceElement;
	private PermanentProperty referenceProperty;
    }
    
    public class ElementReference {
	public ElementReference(Property localProperty, Class<? extends ScoreBoardEventProvider> remoteClass,
		Property remoteProperty) {
	    this.localProperty = localProperty;
	    this.remoteClass = remoteClass;
	    this.remoteProperty = remoteProperty;
	}
	
	public Property getLocalProperty() { return localProperty; }
	public Class<? extends ScoreBoardEventProvider> getRemoteClass() { return remoteClass; }
	public Property getRemoteProperty() { return remoteProperty; }
	    
	private Property localProperty;
	private Class<? extends ScoreBoardEventProvider> remoteClass;
	private Property remoteProperty;
    }
}
