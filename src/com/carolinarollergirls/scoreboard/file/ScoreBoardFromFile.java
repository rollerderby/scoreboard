package com.carolinarollergirls.scoreboard.file;

import java.io.*;
import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public abstract class ScoreBoardFromFile extends ScoreBoardFileIO
{
  public ScoreBoardFromFile() { }
  public ScoreBoardFromFile(String d) { super(d); }
  public ScoreBoardFromFile(String d, String f) { super(d, f); }

  public List<File> getFiles() {
    File[] fileArray = getDirectory().listFiles();
    if (fileArray == null)
      return new ArrayList<File>();
    List<File> files = Arrays.asList(fileArray);
    Collections.sort(files);
    return files;
  }

  /**
   * Load the file into the current ScoreBoard.
   *
   * This replaces the entire contents of the
   * current scoreboad.
   */
  public abstract void load(ScoreBoardModel sbM) throws Exception;

  /**
   * Merge the file into the current ScoreBoard.
   *
   * This is the same as load(), but it doesn't
   * reset the scoreboard before loading.
   */
  public abstract void merge(ScoreBoardModel sbM) throws Exception;
}
