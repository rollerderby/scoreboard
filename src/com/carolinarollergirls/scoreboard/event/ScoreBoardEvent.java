package com.carolinarollergirls.scoreboard.event;

import java.util.*;

import java.lang.reflect.*;

public class ScoreBoardEvent extends EventObject implements Cloneable
{
	public ScoreBoardEvent(ScoreBoardEventProvider sbeP, String p, Object v) {
		super(sbeP);
		provider = sbeP;
		property = p;
		value = v;
	}

	public ScoreBoardEventProvider getProvider() { return provider; }
	public String getProperty() { return property; }
	public Object getValue() { return value; }

	public Object clone() { return new ScoreBoardEvent(getProvider(), getProperty(), getValue()); }

	public boolean reflect(Object o) {
		try {
			reflectWithException(o);
			return true;
		} catch ( Exception e ) {
			return false;
		}
	}
	public void reflectWithException(Object o) throws Exception {
		Method m = o.getClass().getMethod("scoreBoardChange", new Class[]{getProvider().getProviderClass(), ScoreBoardEvent.class});
		m.invoke(o, new Object[]{getProvider(), this});
	}

	protected ScoreBoardEventProvider provider;
	protected String property;
	protected Object value;
}
