package com.carolinarollergirls.scoreboard.file;

import java.io.*;

public class ScoreBoardFileIO
{
  public ScoreBoardFileIO() { }
  public ScoreBoardFileIO(String d) {
    setDirectory(d);
  }
  public ScoreBoardFileIO(String d, String f) {
    setDirectory(d);
    setFile(f);
  }

  public File getDirectory() { return directory; }
  public void setDirectory(String dir) { directory = new File(dir); }

  public File getFile() { return file; }
  public void setFile(String name) { file = new File(getDirectory(), name); }

  protected File file = null;
  protected File directory = null;
}
