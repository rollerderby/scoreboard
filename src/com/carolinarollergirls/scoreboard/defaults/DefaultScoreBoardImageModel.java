package com.carolinarollergirls.scoreboard.defaults;

import java.awt.image.*;

import javax.imageio.*;

import java.io.*;
import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultScoreBoardImageModel extends DefaultScoreBoardEventProvider implements ScoreBoardImageModel
{
	public DefaultScoreBoardImageModel(ScoreBoardModel sbM, String i, String t, String d, String f, String n) {
		scoreBoardModel = sbM;
		id = i;
		type = t;
		directory = d;
		filename = f;
		setName(n);
	}

	public String getProviderName() { return "ScoreBoardImage"; }
	public Class getProviderClass() { return ScoreBoardImage.class; }

	public ScoreBoard getScoreBoard() { return getScoreBoardModel(); }
	public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }

	public String getId() { return id; }

	public ScoreBoardImage getScoreBoardImage() { return this; }

	public String getType() { return type; }

	public String getName() { return name; }
	public void setName(String n) {
		name = n;
		scoreBoardChange(new ScoreBoardEvent(this, "Name", n));
	}

	public String getDirectory() { return directory; }
	public String getFilename() { return filename; }

	public BufferedImage getImage() {
		try {
			return ImageIO.read(new FileInputStream(new File(getDirectory() + "/" + getFilename())));
		} catch ( Exception e ) {
			return null;
		}
	}

	protected ScoreBoardModel scoreBoardModel;

	protected String id;
	protected String type;
	protected String name;
	protected String directory;
	protected String filename;
}
