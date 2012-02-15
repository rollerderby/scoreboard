package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.util.concurrent.*;

public class FilterScoreBoardListener implements ScoreBoardListener
{
	public FilterScoreBoardListener() { }
	public FilterScoreBoardListener(ScoreBoardEventProvider sbeP) { this(sbeP, ANY_PROPERTY); }
	public FilterScoreBoardListener(ScoreBoardEventProvider sbeP, String p) { this(sbeP, p, ANY_VALUE); }
	public FilterScoreBoardListener(ScoreBoardEventProvider sbeP, String p, Object v) {
		addProperty(sbeP, p, v);
		sbeP.addScoreBoardListener(this);
	}
	public FilterScoreBoardListener(Class c, String i) { this(c, i, ANY_PROPERTY); }
	public FilterScoreBoardListener(Class c, String i, String p) { this(c, i, p, ANY_VALUE); }
	public FilterScoreBoardListener(Class c, String i, String p, Object v) { addProperty(c, i, p, v); }

	public void setScoreBoardListener(ScoreBoardListener l) { listener = l; }

	public void addProperty(ScoreBoardEventProvider sbeP, String p) { addProperty(sbeP, p, ANY_VALUE); }
	public void addProperty(ScoreBoardEventProvider sbeP, String p, Object v) {
		synchronized (propertyLock) {
			if (!providers.containsKey(sbeP))
				providers.put(sbeP, new ConcurrentHashMap<String,Collection<Object>>());

			_addProperty(providers.get(sbeP), p, v);
		}
	}

	public void addProperty(Class c, String p) { addProperty(c, ANY_ID, p, ANY_VALUE); }
	public void addProperty(Class c, String p, Object v) { addProperty(c, ANY_ID, p, v); }
	public void addProperty(Class c, String i, String p) { addProperty(c, i, p, ANY_VALUE); }
	public void addProperty(Class c, String i, String p, Object v) {
		synchronized (propertyLock) {
			if (!providerclasses.containsKey(c))
				providerclasses.put(c, new ConcurrentHashMap<String,Map<String,Collection<Object>>>());

			Map<String,Map<String,Collection<Object>>> idMap = providerclasses.get(c);
			if (!idMap.containsKey(i))
				idMap.put(i, new ConcurrentHashMap<String,Collection<Object>>());

			_addProperty(idMap.get(i), p, v);
		}
	}

	protected void _addProperty(Map<String,Collection<Object>> m, String p, Object v) {
		if (null == p)
			return;

		if (!m.containsKey(p))
			m.put(p, new ConcurrentLinkedQueue<Object>());

		if (null == v)
			return;

		if (!m.get(p).contains(v))
			m.get(p).add(v);
	}

	public void removeProperty(ScoreBoardEventProvider sbeP, String p) { removeProperty(sbeP, p, null); }
	public void removeProperty(ScoreBoardEventProvider sbeP, String p, Object v) {
		synchronized (propertyLock) {
			if (providers.containsKey(sbeP))
				_removeProperty(providers.get(sbeP), p, v);
		}
	}

	public void removeProperty(Class c, String i, String p) { removeProperty(c, i, p, null); }
	public void removeProperty(Class c, String i, String p, Object v) {
		synchronized (propertyLock) {
			if (providerclasses.containsKey(c) && providerclasses.get(c).containsKey(i))
				_removeProperty(providerclasses.get(c).get(i), p, v);
		}
	}

	protected void _removeProperty(Map<String,Collection<Object>> m, String p, Object v) {
		if (m.containsKey(p)) {
			if (null == v)
				m.remove(p);
			else
				m.get(p).remove(v);
		}
	}

	public void scoreBoardChange(ScoreBoardEvent event) {
		String p = event.getProperty();
		Object v = event.getValue();
		boolean match = false;
		Map<String,Map<String,Collection<Object>>> classMap;
		if (_matchProperty(providers.get(event.getProvider()), p, v)) {
			match = true;
		} else if (null != (classMap = providerclasses.get(event.getProvider().getProviderClass()))) {
			if (_matchProperty(classMap.get(ANY_ID), p, v))
				match = true;
			else {
				try {
					java.lang.reflect.Method m = event.getProvider().getClass().getMethod("getId", new Class[]{});
					if (_matchProperty(classMap.get(m.invoke(event.getProvider(), new Object[]{})), p, v))
						match = true;
				} catch ( Exception e ) { }
			}
		}

		if (match)
			filteredScoreBoardChange(event);
	}

	protected boolean _matchProperty(Map<String,Collection<Object>> m, String p, Object v) {
		if (null == m)
			return false;
		if (m.containsKey(ANY_PROPERTY)) {
			Collection<Object> s = m.get(ANY_PROPERTY);
			if (s.contains(ANY_VALUE) || s.contains(v))
				return true;
		}
		if (m.containsKey(p)) {
			Collection<Object> s = m.get(p);
			if (s.contains(ANY_VALUE) || s.contains(v))
				return true;
		}
		return false;
	}

	public void filteredScoreBoardChange(ScoreBoardEvent event) {
		if (null != listener)
			listener.scoreBoardChange(event);
	}

	public static final String ANY_ID = FilterScoreBoardListener.class.getName()+".AnyId";
	public static final String ANY_PROPERTY = FilterScoreBoardListener.class.getName()+".AnyProperty";
	public static final Object ANY_VALUE = new Object();

	protected ScoreBoardListener listener = null;

	protected Map<Class,Map<String,Map<String,Collection<Object>>>> providerclasses = new ConcurrentHashMap<Class,Map<String,Map<String,Collection<Object>>>>();
	protected Map<ScoreBoardEventProvider,Map<String,Collection<Object>>> providers = new ConcurrentHashMap<ScoreBoardEventProvider,Map<String,Collection<Object>>>();

	protected Object propertyLock = new Object();
}

