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

  protected void doStart(File file) throws IOException,FileNotFoundException {
    try {
      outputStream = new ScoreBoardOutputStream(file);
      outputStream.start();
      outputStream.write(editor.filterNoSavePI(getXmlScoreBoard().getDocument()));
      getXmlScoreBoard().addXmlScoreBoardListener(this);
    } catch ( FileNotFoundException fnfE ) {
      doStop();
      throw new FileNotFoundException("File '"+file.getName()+"' could not be created : "+fnfE.getMessage());
    } catch ( IOException ioE ) {
      doStop();
      throw new IOException("Could not start streaming to file '"+file.getName()+"' : "+ioE.getMessage());
    }
  }

  protected void doStop() {
    getXmlScoreBoard().removeXmlScoreBoardListener(this);
    if (null != outputStream)
      outputStream.stop();
    outputStream = null;
  }

  protected void doXmlChange(Document d) throws IOException {
    try {
      outputStream.write(editor.filterNoSavePI(d));
    } catch ( IOException ioE ) {
      outputStream.stop();
      throw new IOException("Error while streaming to file : "+ioE.getMessage());
    }
  }

  protected ScoreBoardOutputStream outputStream = null;
}
