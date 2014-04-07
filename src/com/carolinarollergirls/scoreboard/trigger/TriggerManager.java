package com.carolinarollergirls.scoreboard.trigger;
/**
  * Copyright (C) 2014 Michael Mitton <mmitton@gmail.com>
  *
  * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
  * The CRG ScoreBoard is licensed under either the GNU General Public
  * License version 3 (or later), or the Apache License 2.0, at your option.
  * See the file COPYING for details.
  */

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

import java.util.*;
import org.jdom.*;
import org.jdom.output.*;

public class TriggerManager {
	public TriggerManager(ScoreBoard sb) {
		scoreBoard = sb;
		listener.reset(sb);
		sb.getXmlScoreBoard().addXmlScoreBoardListener(listener);

		setupTest();
	}

	private void setupTest() {
		Trigger t1 = new TriggerString("ScoreBoard.Team(1).Name", TriggerString.Operator.Equals, "Team 1", 
			new TriggerHandler() {
				public void Trigger(Trigger t, TriggerManager.State s) { 
					System.out.printf("T1 Triggered: %s\n", t.toString());
				} 
			}
		);
		Trigger t2 = new TriggerNot(t1, 
			new TriggerHandler() {
				public void Trigger(Trigger t, TriggerManager.State s) { 
					System.out.printf("T2 Triggered: %s\n", t.toString());
				} 
			}
		);
		Trigger t3 = new TriggerComposite(Arrays.asList(new Trigger[]{ t1, t2 }), TriggerComposite.Operator.Or,
			new TriggerHandler() {
				public void Trigger(Trigger t, TriggerManager.State s) { 
					System.out.printf("T3 Triggered: %s\n", t.toString());
				} 
			}
		);
		Trigger t4 = new TriggerString("ScoreBoard.Team(2).Name", TriggerString.Operator.Equals, "Team 2", 
			new TriggerHandler() {
				public void Trigger(Trigger t, TriggerManager.State s) { 
					System.out.printf("T4 Triggered: %s\n", t.toString());
				} 
			}
		);
		Trigger t5 = new TriggerComposite(new Trigger[]{ t1, t4 }, TriggerComposite.Operator.And,
			new TriggerHandler() {
				public void Trigger(Trigger t, TriggerManager.State s) { 
					System.out.printf("T5 Triggered: %s\n", t.toString());
				} 
			}
		);

		// registerTrigger(t1);
		// registerTrigger(t2);
		registerTriggerAndRun(t3);
		registerTriggerAndRun(t5);
	}

	public void registerTriggerAndRun(Trigger t) {
		listener.registerTrigger(t, true);
	}

	public void registerTrigger(Trigger t) {
		listener.registerTrigger(t, false);
	}

	public void unregisterTrigger(Trigger t) {
		listener.unregisterTrigger(t);
	}

	protected Map<String, String> currentState = new Hashtable<String, String>();
	protected Map<String, String> shortcuts = new Hashtable<String, String>();
	private TriggerListener listener = new TriggerListener();
	private ScoreBoard scoreBoard = null;

	public class State {
		protected State(Map<String, String> cs, Map<String, Field> c) {
			currentState = cs;
			if (c != null)
				changes = c;
			else
				changes = new Hashtable<String, Field>();
		}

		public final Field getField(String name) {
			if (changes.containsKey(name))
				return changes.get(name);
			else {
				String v = currentState.get(name);
				if (v == null)
					return null;
				Field nf = new Field(name, v, v, Field.Action.Unchanged);
				changes.put(name, nf);
				return nf;
			}
		}

		private final Map<String, String> currentState;
		private Map<String, Field> changes;
	}

	protected class TriggerListener implements XmlScoreBoardListener {
		protected void reset(ScoreBoard sb) {
			Map<String, Field> changes = new Hashtable<String, Field>();
			for (String f : currentState.keySet()) {
				changes.put(f, new Field(f, null, currentState.get(f), Field.Action.Remove));
			}
			currentState.clear();
			shortcuts.clear();
			System.out.printf("Processing changes for reset\n");
			processChanges(changes);
			xmlChange(sb.getXmlScoreBoard().getDocument());
		}

		public void xmlChange(Document d) {
			// Process Changes
			Map<String, Field> changes = processElement(d.getRootElement(), null, null);
			processChanges(changes);
		}

		private void processChanges(Map<String, Field> changes) {
			// Look for interested triggers
			Map<Trigger, Boolean> triggers = null;
			for (String name : changes.keySet()) {
				if (triggerMap.containsKey(name)) {
					if (triggers == null)
						triggers = new Hashtable<Trigger, Boolean>();
					for (Trigger trigger : triggerMap.get(name))
						triggers.put(trigger, true);
				}
			}

			// Process interested triggers
			if (triggers != null) {
				State s = new State(currentState, changes);
				for (Trigger t : triggers.keySet()) {
					t.checkTrigger(s);
				}
			}
		}

		private Map<String, Field> processElement(Element e, String parent, Map<String, Field> changes) {
			if (changes == null) {
				changes = new Hashtable<String, Field>();
			}
			String path = "";
			if (parent != null) {
				path = e.getName();
				if (!parent.equals(""))
					path = parent + "." + path;
				Attribute id = e.getAttribute("Id");
				if (id != null)
					path = path + "(" + id.getValue() + ")";

				String oldValue = currentState.get(path);
				if (editor.hasRemovePI(e)) {
					changes.put(path, new Field(path, null, oldValue, Field.Action.Remove));
					// Remove all children as well
					path = path + ".";
					for (String key : currentState.keySet()) {
						if (key.startsWith(path)) {
							changes.put(path, new Field(key, null, currentState.get(path), Field.Action.Remove));
						}
					}
					return changes;
				} else {
					String value = e.getTextTrim();
					if (!value.equals(oldValue)) {
						changes.put(path, new Field(path, value, oldValue, oldValue == null ? Field.Action.Add : Field.Action.Change));
						currentState.put(path, value);
					}

					// Immediately process any shortcut changes
					if (parent.equals("Trigger.Shortcut")) {
						if (value.equals(""))
							shortcuts.remove(e.getName());
						else
							shortcuts.put(e.getName(), value);
					}
				}
			}
			for (Object child : e.getChildren()) {
				processElement((Element)child, path, changes);
			}
			return changes;
		}

		protected void registerTrigger(Trigger t, Boolean andRun) {
			for (String f : t.getFields()) {
				if (!triggerMap.containsKey(f)) {
					triggerMap.put(f, new ArrayList<Trigger>());
				}
				List<Trigger> triggers = triggerMap.get(f);
				if (!triggers.contains(t))
					triggers.add(t);
			}
			if (andRun) {
				State s = new State(currentState, null);
				t.checkTrigger(s);
			}
		}

		protected void unregisterTrigger(Trigger t) {
			for (String f : t.getFields()) {
				if (triggerMap.containsKey(f)) {
					List<Trigger> triggers = triggerMap.get(f);
					triggers.remove(t);
					if (triggers.size() == 0)
						triggerMap.remove(f);
				}
			}
		}

		private XMLOutputter printer = XmlDocumentEditor.getRawXmlOutputter();
		private Map<String, List<Trigger>> triggerMap = new Hashtable<String, List<Trigger>>();

		private XmlDocumentEditor editor = XmlDocumentEditor.getInstance();
	}
}
