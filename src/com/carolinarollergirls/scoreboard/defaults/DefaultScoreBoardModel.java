package com.carolinarollergirls.scoreboard.defaults;

import java.util.*;

import java.io.*;

import java.awt.image.*;

import javax.imageio.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultScoreBoardModel extends DefaultScoreBoardEventProvider implements ScoreBoardModel
{
	public DefaultScoreBoardModel() {
		reset();

		loadPolicies();

		loadImages();

		xmlScoreBoard = new XmlScoreBoard(this);
	}

	public String getProviderName() { return "ScoreBoard"; }
	public Class getProviderClass() { return ScoreBoard.class; }

	public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

	protected void loadPolicies() {
		Enumeration keys = ScoreBoardManager.getProperties().propertyNames();

		while (keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			if (!key.startsWith(POLICY_KEY+"."))
				continue;

			String name = ScoreBoardManager.getProperties().getProperty(key);

			try {
				PolicyModel policyModel = (PolicyModel)Class.forName(name).newInstance();
				addPolicyModel(policyModel);
				ScoreBoardManager.printMessage("Loaded Policy : "+name);
			} catch ( Exception e ) {
				ScoreBoardManager.printMessage("Could not load ScoreBoard policy : " + e.getMessage());
			}
		}
	}

	protected void loadImages() {
		Properties p = ScoreBoardManager.getProperties();
		String imagesTopDir = p.getProperty(IMAGES_DIR);
		if (null == imagesTopDir)
			return;

		Enumeration keys = p.propertyNames();

		while (keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			if (!key.startsWith(IMAGES_DIR+"."))
				continue;

			String type = key.replaceFirst(IMAGES_DIR+".", "");
			String dir = p.getProperty(key);

			scoreBoardImageAddUpdaters.add(new ScoreBoardImageAddUpdater(new File(imagesTopDir + "/" + dir), imagesTopDir, dir, type));
		}

		scoreBoardImageRemoveUpdaters.add(new ScoreBoardImageRemoveUpdater());

		updateScoreBoardImageModels();
	}

	public void updateScoreBoardImageModels() {
		Iterator<Runnable> updaters = scoreBoardImageRemoveUpdaters.iterator();
		while (updaters.hasNext())
			updaters.next().run();

		updaters = scoreBoardImageAddUpdaters.iterator();
		while (updaters.hasNext())
			updaters.next().run();
	}

	public ScoreBoard getScoreBoard() { return this; }

	public void reset() {
		Iterator<ClockModel> c = getClockModels().iterator();
		while (c.hasNext())
			c.next().reset();
		Iterator<TeamModel> t = getTeamModels().iterator();
		while (t.hasNext())
			t.next().reset();

		periodClockWasRunning = false;
		jamClockWasRunning = false;
		lineupClockWasRunning = false;
		timeoutClockWasRunning = false;
		setTimeoutOwner(DEFAULT_TIMEOUT_OWNER);
	}

	public void startJam() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_JAM).isRunning()) {
				ClockModel pc = getClockModel(Clock.ID_PERIOD);
				ClockModel jc = getClockModel(Clock.ID_JAM);
				ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
				lineupClockWasRunning = getClockModel(Clock.ID_LINEUP).isRunning();

//FIXME - change to policies
				// If Period Clock is at end, increment number and reset time
				if (pc.getTime() == (pc.isCountDirectionDown() ? pc.getMinimumTime() : pc.getMaximumTime())) {
					pc.changeNumber(1);
					pc.resetTime();
				}
				periodClockWasRunning = pc.isRunning();
				pc.start();

				// If Jam Clock is not at start (2:00), increment number and reset time
				if (jc.getTime() != (jc.isCountDirectionDown() ? jc.getMaximumTime() : jc.getMinimumTime()))
					jc.changeNumber(1);
				jc.resetTime();
				jc.start();

				timeoutClockWasRunning = tc.isRunning();
				tc.stop();
			}
		}
	}
	public void stopJam() {
		synchronized (runLock) {
			if (getClockModel(Clock.ID_JAM).isRunning()) {
				getClockModel(Clock.ID_JAM).stop();
			}
		}
	}

	public void timeout() { timeout(null); }
	public void timeout(TeamModel team) {
		synchronized (runLock) {
			setTimeoutOwner(null==team?"":team.getId());
			if (!getClockModel(Clock.ID_TIMEOUT).isRunning()) {
//FIXME - change to policy?
				getClockModel(Clock.ID_PERIOD).stop();
				jamClockWasRunning = getClockModel(Clock.ID_JAM).isRunning();
				lineupClockWasRunning = getClockModel(Clock.ID_LINEUP).isRunning();
				getClockModel(Clock.ID_JAM).stop();
				getClockModel(Clock.ID_TIMEOUT).resetTime();
				getClockModel(Clock.ID_TIMEOUT).start();
			}
		}
	}

	public void unStartJam() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_JAM).isRunning())
				return;

			if (lineupClockWasRunning)
				getClockModel(Clock.ID_LINEUP).unstop();
			if (timeoutClockWasRunning)
				getClockModel(Clock.ID_TIMEOUT).unstop();
			if (!periodClockWasRunning)
				getClockModel(Clock.ID_PERIOD).unstart();
			getClockModel(Clock.ID_JAM).unstart();
		}
	}
	public void unStopJam() {
		synchronized (runLock) {
			if (getClock(Clock.ID_JAM).isRunning())
				return;

			getClockModel(Clock.ID_LINEUP).stop();
			getClockModel(Clock.ID_JAM).unstop();
		}
	}
	public void unTimeout() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_TIMEOUT).isRunning())
				return;

			if (lineupClockWasRunning)
				getClockModel(Clock.ID_LINEUP).unstop();
			if (jamClockWasRunning)
				getClockModel(Clock.ID_JAM).unstop();
			getClockModel(Clock.ID_PERIOD).unstop();
			getClockModel(Clock.ID_TIMEOUT).unstart();
		}
	}

	public List<ClockModel> getClockModels() { return new ArrayList<ClockModel>(clocks.values()); }
	public List<TeamModel> getTeamModels() { return new ArrayList<TeamModel>(teams.values()); }
	public List<ScoreBoardImageModel> getScoreBoardImageModels() { return new ArrayList<ScoreBoardImageModel>(scoreBoardImages.values()); }
	public List<PolicyModel> getPolicyModels() { return new ArrayList<PolicyModel>(policies.values()); }

	public List<Clock> getClocks() { return new ArrayList<Clock>(getClockModels()); }
	public List<Team> getTeams() { return new ArrayList<Team>(getTeamModels()); }
	public List<ScoreBoardImage> getScoreBoardImages() { return new ArrayList<ScoreBoardImage>(getScoreBoardImageModels()); }
	public List<Policy> getPolicies() { return new ArrayList<Policy>(getPolicyModels()); }

	public Clock getClock(String id) { return getClockModel(id).getClock(); }
	public Team getTeam(String id) { return getTeamModel(id).getTeam(); }
	public ScoreBoardImage getScoreBoardImage(String id) { try { return getScoreBoardImageModel(id).getScoreBoardImage(); } catch ( NullPointerException npE ) { return null; } }
	public Policy getPolicy(String id) { try { return getPolicyModel(id).getPolicy(); } catch ( NullPointerException npE ) { return null; } }

	public List<ScoreBoardImage> getScoreBoardImages(String type) { return new ArrayList<ScoreBoardImage>(getScoreBoardImageModels(type)); }
	public List<ScoreBoardImageModel> getScoreBoardImageModels(String type) {
		List<ScoreBoardImageModel> l = new LinkedList<ScoreBoardImageModel>(scoreBoardImages.values());
		for (int i=0; i<l.size(); i++) {
			ScoreBoardImageModel sbim = l.get(i);
			if (!type.equals(sbim.getType())) {
				l.remove(sbim);
				i--;
			}
		}
		return l;
	}

	public ClockModel getClockModel(String id) {
		synchronized (clocks) {
// FIXME - don't auto-create!  return null instead - or throw exception.  Need to update all callers to handle first.
			if (!clocks.containsKey(id))
				createClockModel(id);

			return clocks.get(id);
		}
	}

	public TeamModel getTeamModel(String id) {
		synchronized (teams) {
// FIXME - don't auto-create!  return null instead - or throw exception.  Need to update all callers to handle first.
			if (!teams.containsKey(id))
				createTeamModel(id);

			return teams.get(id);
		}
	}

	public ScoreBoardImageModel getScoreBoardImageModel(String id) {
		synchronized (scoreBoardImages) {
			return scoreBoardImages.get(id);
		}
	}

	public void addScoreBoardImageModel(ScoreBoardImageModel model) throws IllegalArgumentException {
		if ((model.getId() == null) || (model.getId().equals("")))
			throw new IllegalArgumentException("ScoreBoardImageModel has null or empty Id");

		synchronized (scoreBoardImages) {
			scoreBoardImages.put(model.getId(), model);
			model.addScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, "AddScoreBoardImage", model));
		}
	}

	public void removeScoreBoardImageModel(String id) {
		synchronized (scoreBoardImages) {
			ScoreBoardImageModel model = scoreBoardImages.remove(id);
			if (null != model) {
				model.removeScoreBoardListener(this);
				scoreBoardChange(new ScoreBoardEvent(this, "RemoveScoreBoardImage", model));
			}
		}
	}

	public PolicyModel getPolicyModel(String id) {
		synchronized (policies) {
			return policies.get(id);
		}
	}
	public void addPolicyModel(PolicyModel model) throws IllegalArgumentException {
		if ((model.getId() == null) || (model.getId().equals("")))
			throw new IllegalArgumentException("PolicyModel has null or empty Id");

		try {
			model.setScoreBoardModel(this);
		} catch ( Exception e ) {
			e.printStackTrace();
			throw new IllegalArgumentException("Exception while setting ScoreBoardModel on PolicyModel : "+e.getMessage());
		}

		synchronized (policies) {
			policies.put(model.getId(), model);
			model.addScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, "AddPolicy", model));
		}
	}
	public void removePolicyModel(PolicyModel model) {
		synchronized (policies) {
			policies.remove(model.getId());
			model.removeScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, "RemovePolicy", model));
		}
	}

	public String getTimeoutOwner() { return timeoutOwner; }
	public void setTimeoutOwner(String owner) {
		synchronized (timeoutOwnerLock) {
			timeoutOwner = owner;
			scoreBoardChange(new ScoreBoardEvent(this, "TimeoutOwner", owner));
		}
	}

	protected void createClockModel(String id) {
		if ((id == null) || (id.equals("")))
			return;

		ClockModel model = new DefaultClockModel(this, id);
		model.addScoreBoardListener(this);
		clocks.put(id, model);
		scoreBoardChange(new ScoreBoardEvent(this, "AddClock", model));
	}

	protected void createTeamModel(String id) {
		if ((id == null) || (id.equals("")))
			return;

		TeamModel model = new DefaultTeamModel(this, id);
		model.addScoreBoardListener(this);
		teams.put(id, model);
		scoreBoardChange(new ScoreBoardEvent(this, "AddTeam", model));
	}

	protected HashMap<String,ClockModel> clocks = new HashMap<String,ClockModel>();
	protected HashMap<String,TeamModel> teams = new HashMap<String,TeamModel>();
	protected HashMap<String,ScoreBoardImageModel> scoreBoardImages = new HashMap<String,ScoreBoardImageModel>();
	protected HashMap<String,PolicyModel> policies = new HashMap<String,PolicyModel>();

	protected Object runLock = new Object();

	protected String timeoutOwner;
	protected Object timeoutOwnerLock = new Object();

	protected boolean periodClockWasRunning = false;
	protected boolean jamClockWasRunning = false;
	protected boolean lineupClockWasRunning = false;
	protected boolean timeoutClockWasRunning = false;

	protected List<Runnable> scoreBoardImageAddUpdaters = new ArrayList<Runnable>();
	protected List<Runnable> scoreBoardImageRemoveUpdaters = new ArrayList<Runnable>();

	protected XmlScoreBoard xmlScoreBoard;

	public static final String DEFAULT_TIMEOUT_OWNER = "";

	public static final String IMAGES_DIR = DefaultScoreBoardModel.class.getName() + ".images.directory";

	public static final String POLICY_KEY = DefaultScoreBoardModel.class.getName() + ".policy";



	protected class ScoreBoardImageRemoveUpdater implements Runnable
	{
		public void run() {
			synchronized (scoreBoardImages) {
				Iterator<ScoreBoardImageModel> imgs = getScoreBoardImageModels().iterator();
				List<ScoreBoardImageModel> removeImages = new LinkedList<ScoreBoardImageModel>();
				while (imgs.hasNext()) {
					ScoreBoardImageModel sbiM = imgs.next();
					if (!(new File(sbiM.getDirectory() + "/" + sbiM.getFilename()).exists()))
						removeImages.add(sbiM);
				}
				Iterator<ScoreBoardImageModel> removeIterator = removeImages.iterator();
				while (removeIterator.hasNext())
					removeScoreBoardImageModel(removeIterator.next().getId());
			}
		}
	}

	protected class ScoreBoardImageAddUpdater implements Runnable
	{
		public ScoreBoardImageAddUpdater(File d, String topD, String dName, String t) {
			directory = d;
			topDirectory = topD;
			directoryName = dName;
			type = t;
		}

		public void run() {
			File[] files = directory.listFiles();
			for (int i=0; i<files.length; i++) {
				try {
					File f = files[i];
					if (f.isDirectory())
						continue;
					String name = f.getName();
					String id = type + "/" + name;
					String filename = "/" + directoryName + "/" + name;
					synchronized (scoreBoardImages) {
						if (!scoreBoardImages.containsKey(id))
							addScoreBoardImageModel(new DefaultScoreBoardImageModel(DefaultScoreBoardModel.this, id, type, topDirectory, filename, name));
					}
				} catch ( Exception e ) {
					ScoreBoardManager.printMessage("Could not add image type "+type+" file "+files[i].getName());
				}
			}
		}

		public boolean running = true;

		protected File directory;
		protected String topDirectory;
		protected String type;
		protected String directoryName;
	}
}

