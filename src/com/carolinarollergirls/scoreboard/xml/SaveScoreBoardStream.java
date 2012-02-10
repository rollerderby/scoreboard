package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.stream.*;

public class SaveScoreBoardStream extends AbstractScoreBoardStream
{
  public SaveScoreBoardStream() { super("SaveStream"); }

  protected void doStart(File file) throws IOException {
    try {
      outputStream = new ScoreBoardOutputStream(file);
      outputStream.start();
      outputStream.write(getXmlScoreBoard().getDocument());
      getXmlScoreBoard().addXmlScoreBoardListener(this);
    } catch ( IOException ioE ) {
      outputStream.stop();
      outputStream = null;
      throw new IOException("Could not start streaming to file '"+file.getName()+"' : "+ioE.getMessage());
    }
  }

  protected void doStop() {
    getXmlScoreBoard().removeXmlScoreBoardListener(this);
    outputStream.stop();
  }

  protected void doXmlChange(Document d) throws IOException {
    try {
      outputStream.write(d);
    } catch ( IOException ioE ) {
      outputStream.stop();
      throw new IOException("Error while streaming to file : "+ioE.getMessage());
    }
  }

  protected ScoreBoardOutputStream outputStream = null;
}
