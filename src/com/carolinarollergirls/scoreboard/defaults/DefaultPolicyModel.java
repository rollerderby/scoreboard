package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.Policy;
import com.carolinarollergirls.scoreboard.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.model.PolicyModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public class DefaultPolicyModel extends DefaultScoreBoardEventProvider implements PolicyModel
{
	public DefaultPolicyModel(String i, String d) { this(i, i, d); }
	public DefaultPolicyModel(String i, String n, String d) {
		id = i;
		name = n;
		description = d;
		reset();
	}

	public String getProviderName() { return "Policy"; }
	public Class<Policy> getProviderClass() { return Policy.class; }
	public String getProviderId() { return getId(); }

	public ScoreBoard getScoreBoard() { return getScoreBoardModel(); }
	public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }
	public void setScoreBoardModel(ScoreBoardModel sbm) {
		if (scoreBoardModel != null)
			throw new IllegalStateException("This PolicyModel is already associated with a ScoreBoardModel!");

		scoreBoardModel = sbm;
	}

	public Policy getPolicy() { return this; }

	public void reset() {
		setEnabled(DEFAULT_ENABLED);
		Iterator<ParameterModel> p = getParameterModels().iterator();
		while (p.hasNext())
			p.next().reset();
	}

	public String getId() { return id; }

	public String getName() { return name; }

	public String getDescription() { return description; }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean e) {
		synchronized (enabledLock) {
			Boolean last = new Boolean(enabled);
			enabled = e;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_ENABLED, new Boolean(enabled), last));
		}
	}

	public List<PolicyModel.ParameterModel> getParameterModels() { return Collections.unmodifiableList(new ArrayList<PolicyModel.ParameterModel>(parameters.values())); }
	public List<Policy.Parameter> getParameters() {
		List<Policy.Parameter> list = new ArrayList<Policy.Parameter>(parameters.size());
		Iterator<PolicyModel.ParameterModel> i = parameters.values().iterator();
		while (i.hasNext())
			list.add(i.next().getParameter());
		return Collections.unmodifiableList(list);
	}
	public PolicyModel.ParameterModel getParameterModel(String name) { return parameters.get(name); }
	public Policy.Parameter getParameter(String name) { try { return parameters.get(name).getParameter(); } catch ( NullPointerException npE ) { return null; } }

	protected void addParameterModel(PolicyModel.ParameterModel parameterModel) {
		parameters.put(parameterModel.getName(), parameterModel);
		parameterModel.addScoreBoardListener(this);
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_PARAMETER, parameterModel, null));
	}

	protected ScoreBoardModel scoreBoardModel = null;
	protected Map<String,PolicyModel.ParameterModel> parameters = new Hashtable<String,PolicyModel.ParameterModel>();
	protected String id;
	protected String name;
	protected String description;
	protected boolean enabled;
	protected Object enabledLock = new Object();

	public static final boolean DEFAULT_ENABLED = true;

	public class DefaultParameterModel extends DefaultScoreBoardEventProvider implements PolicyModel.ParameterModel
	{
		public DefaultParameterModel(PolicyModel pM, String n, String t, String v) {
			policyModel = pM;
			name = n;
			type = t;
			defaultValue = v;

			try {
				constructor = Class.forName("java.lang."+type).getConstructor(new Class<?>[]{ String.class });
			} catch ( Exception e ) {
				constructor = null;
			}

			reset();
		}

		public String getProviderName() { return "Parameter"; }
		public Class<Parameter> getProviderClass() { return Parameter.class; }
		public String getProviderId() { return getName(); }

		public PolicyModel getPolicyModel() { return policyModel; }
		public Policy getPolicy() { return getPolicyModel().getPolicy(); }

		public void reset() {
			try {
				setValue(defaultValue);
			} catch ( IllegalArgumentException iaE ) {
			}
		}

		public Policy.Parameter getParameter() { return this; }

		public String getName() { return name; }

		public String getValue() { return value; }

		public String getType() { return type; }

		public void setValue(String v) throws IllegalArgumentException {
			synchronized (valueLock) {
				try {
					if (null != constructor)
						constructor.newInstance(new Object[]{ v });
					String last = value;
					value = v;
					scoreBoardChange(new ScoreBoardEvent(this, EVENT_VALUE, value, last));
				} catch ( Exception e ) {
					throw new IllegalArgumentException("Invalid value ("+v+") : "+e.getMessage());
				}
			}
		}

		protected PolicyModel policyModel;
		protected String name;
		protected String type;
		protected String value;
		protected String defaultValue;
		protected Object valueLock = new Object();
		protected Constructor<?> constructor;
	}
}
