package com.carolinarollergirls.scoreboard.file;

import java.io.*;
import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public class ScoreBoardFromFile extends ScoreBoardFileIO
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
}
