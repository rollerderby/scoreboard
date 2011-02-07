package com.carolinarollergirls.scoreboard.file;

import java.io.*;
import java.util.*;
import java.text.*;

import com.carolinarollergirls.scoreboard.*;

public abstract class ScoreBoardToFile extends ScoreBoardFileIO
{
	public ScoreBoardToFile() {
		super();
		countFormat.setMinimumIntegerDigits(3);
	}
	public ScoreBoardToFile(String d) {
		super(d);
		countFormat.setMinimumIntegerDigits(3);
	}
	public ScoreBoardToFile(String d, String f) {
		super(d, f);
		countFormat.setMinimumIntegerDigits(3);
	}

	public abstract void save(ScoreBoard sB) throws Exception;

	public void setFile(String n) { setFile(n,null); }
	public void setFile(String n, String e) { setFile(n, e, true); }
	public void setFile(String n, String e, boolean f) {
		if (null == e)
			e = "";
		if (e.trim().equals("")) {
			int i = n.lastIndexOf(".");
			if (0 < i) {
				e = n.substring(i);
				n = n.substring(0,i);
			}
		}
 		if (e.length() > 0 && !e.startsWith("."))
			e = "."+e;
		name = n;
		ext = e;
		if (f) {
			try {
				file = findNextAvailableFile();
			} catch ( IOException ioE ) {
				file = null;
			}
		} else {
			super.setFile(name+ext);
		}
	}

	protected File findNextAvailableFile() throws IOException {
		if (!getDirectory().isDirectory() && !getDirectory().mkdirs())
			throw new IOException("Could not create directory '"+getDirectory().getName()+"'");
		int count = 0;
		File f = new File(getDirectory(), name+ext);
		while (!f.createNewFile() && ++count<1000)
			f = new File(getDirectory(), getNumName(count));
		if (count<1000)
			return f;
		else
			throw new IOException("Could not find any available file for base name '"+name+ext+"'");
	}
	protected String getNumName(int n) { return name+"_"+countFormat.format(n)+ext; }

	protected String name;
	protected String ext;

	protected NumberFormat countFormat = NumberFormat.getInstance();
}
