package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.stream.*;

public class LoadScoreBoardStream extends AbstractScoreBoardStream
{
  public LoadScoreBoardStream() { super("LoadStream"); }

  protected void doStart(File file) throws IOException {
    try {
      inputStream = new ScoreBoardInputStream(file, this);
      inputStream.start();
    } catch ( IOException ioE ) {
      inputStream.stop();
      inputStream = null;
      throw new IOException("Could not start streaming from file '"+file.getName()+"' : "+ioE.getMessage());
    }
  }

  protected void doStop() {
    inputStream.stop();
  }

  protected void doXmlChange(Document d) {
    System.err.println("got doc");
  }

  protected ScoreBoardInputStream inputStream = null;
}
