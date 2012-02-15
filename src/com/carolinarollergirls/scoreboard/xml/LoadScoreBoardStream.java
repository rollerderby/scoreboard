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

  protected void doStart(File file) throws IOException,FileNotFoundException {
    try {
      firstDocument = true;
      inputStream = new ScoreBoardInputStream(file, new RealtimeXmlScoreBoardListenerFilter(this));
      inputStream.start();
    } catch ( FileNotFoundException fnfE ) {
      doStop();
      throw new FileNotFoundException("File '"+file.getName()+"' not found : "+fnfE.getMessage());
    } catch ( IOException ioE ) {
      doStop();
      throw new IOException("Could not start streaming from file '"+file.getName()+"' : "+ioE.getMessage());
    }
  }

  protected void doStop() {
    if (null != inputStream)
      inputStream.stop();
    inputStream = null;
  }

  protected void doXmlChange(Document d) {
    if (null == d) {
      stop();
    } else if (firstDocument) {
      getXmlScoreBoard().loadDocument(d);
      firstDocument = false;
    } else {
      getXmlScoreBoard().xmlChange(d);
    }
  }

  protected ScoreBoardInputStream inputStream = null;
  protected boolean firstDocument;
}
