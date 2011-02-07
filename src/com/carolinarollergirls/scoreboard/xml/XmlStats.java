package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public abstract class XmlStats extends AbstractXmlDocumentManager implements ScoreBoardListener
{
	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		super.setXmlScoreBoard(xsB);
		scoreBoard = xsB.getScoreBoardModel();
		reset();
		scoreBoard.addScoreBoardListener(this);
	}

	public void reset() {
		//FIXME - need to fix the whole "remove attribute" and "persistent" document stuff
		//FIXME - the listener model forces the need for "persistent" or not documents,
		//FIXME - and the "remove" attribute is not really safe - if the node is later added
		//FIXME - back, if that "removed" attribute hasn't been read from the listener yet
		//FIXME - then the new added node will still be marked as "Removed" and the listener
		//FIXME - will have the wrong view of the document.  Need to fix up the listener model.
		//
		//FIxME - once the listener model is fixed up, we should be able to just "remove" the
		//FIXME - top level element, and re-create immediately; instead of having to remove all children.
		try {
			Iterator i = getXPathElement().getChildren().iterator();
			while (i.hasNext()) {
				String name = ((Element)i.next()).getName();
				if (!name.equals("Start"))
					update(createXPathElement().addContent(new Element(name).setAttribute("remove", "true")));
			}
		} catch ( Exception e ) {
			//FIXME - shouldn't happen?  how to handle...
		}

		statsStartTime = new Date().getTime();
		update(createXPathElement().addContent(new Element("Start").setText(Long.toString(statsStartTime))));
	}

	public abstract void scoreBoardChange(ScoreBoardEvent event);

	protected String getStatsTime() { return Long.toString(new Date().getTime() - statsStartTime); }

	protected String getXPathString() { return "/*/Stats"; }

	protected ScoreBoard scoreBoard;
	protected long statsStartTime;
}
