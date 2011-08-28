package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;

/**
 * Converts a ScoreBoardEvent into a representative XML Document or XML String.
 *
 * This class is not synchronized.  Each event method modifies the same document.
 */
public class ScoreBoardXmlListener implements ScoreBoardListener
{
  public ScoreBoardXmlListener() { }
  public ScoreBoardXmlListener(boolean p) {
    setPersistent(p);
  }
  public ScoreBoardXmlListener(ScoreBoard sb, boolean p) {
    setPersistent(p);
    sb.addScoreBoardListener(this);
  }
  public ScoreBoardXmlListener(ScoreBoard sb) {
    sb.addScoreBoardListener(this);
  }

  public boolean isEmpty() { return empty; }

  public Document getDocument() { return document; }

  public Document resetDocument() {
    Document oldDoc = document;
    empty = true;
    document = editor.createDocument("ScoreBoard");
    return oldDoc;
  }

  public void scoreBoardChange(ScoreBoardEvent event) {
    ScoreBoardEventProvider p = event.getProvider();
    String prop = event.getProperty();
    String v = (event.getValue()==null?null:event.getValue().toString());
    if (p.getProviderName().equals("ScoreBoard")) {
      if (prop.equals("AddClock")) {
        converter.toElement(getScoreBoardElement(), (Clock)event.getValue());
      } else if (prop.equals("RemoveClock")) {
        if (isPersistent())
          editor.removeElement(getScoreBoardElement(), "Clock", ((Clock)event.getValue()).getId());
        else
          converter.toElement(getScoreBoardElement(), (Clock)event.getValue()).setAttribute("remove", "true");
      } else if (prop.equals("AddTeam")) {
        converter.toElement(getScoreBoardElement(), (Team)event.getValue());
      } else if (prop.equals("RemoveTeam")) {
        if (isPersistent())
          editor.removeElement(getScoreBoardElement(), "Team", ((Team)event.getValue()).getId());
        else
          converter.toElement(getScoreBoardElement(), (Team)event.getValue()).setAttribute("remove", "true");
      } else if (prop.equals("AddPolicy")) {
        converter.toElement(getScoreBoardElement(), (Policy)event.getValue());
      } else if (prop.equals("RemovePolicy")) {
        if (isPersistent())
          editor.removeElement(getScoreBoardElement(), "Policy", ((Policy)event.getValue()).getId());
        else
          converter.toElement(getScoreBoardElement(), (Policy)event.getValue()).setAttribute("remove", "true");
      } else {
        editor.setElement(getScoreBoardElement(), prop, null, v);
      }
    } else if (p.getProviderName().equals("Team")) {
      if (prop.equals("AddSkater")) {
        Element e = converter.toElement(getTeamElement((Team)p), (Skater)event.getValue());
      } else if (prop.equals("RemoveSkater")) {
        if (isPersistent()) {
          editor.removeElement(getTeamElement((Team)p), "Skater", ((Skater)event.getValue()).getId());
        } else {
          Element e = converter.toElement(getTeamElement((Team)p), (Skater)event.getValue());
          e.setAttribute("remove", "true");
        }
      } else {
        editor.setElement(getTeamElement((Team)p), prop, null, v);
      }
    } else if (p.getProviderName().equals("Position")) {
      Element e = getPositionElement((Position)p);
      if (prop.equals("Skater")) {
        Skater s = (Skater)event.getValue();
        editor.setElement(e, "Id", null, (s==null?"":s.getId()));
        editor.setElement(e, "Name", null, (s==null?"":s.getName()));
        editor.setElement(e, "Number", null, (s==null?"":s.getNumber()));
      }
    } else if (p.getProviderName().equals("Skater")) {
      editor.setElement(getSkaterElement((Skater)p), prop, null, v);
    } else if (p.getProviderName().equals("Clock")) {
      editor.setElement(getClockElement((Clock)p), prop, null, v);
    } else if (p.getProviderName().equals("Policy")) {
      editor.setElement(getPolicyElement((Policy)p), prop, null, v);
    } else if (p.getProviderName().equals("Parameter")) {
      editor.setElement(getPolicyParameterElement((Policy.Parameter)p), prop, null, v);
    } else {
      return;
    }
    empty = false;
  }

  public boolean isPersistent() { return persistent; }
  public void setPersistent(boolean p) { persistent = p; }

  protected Element getScoreBoardElement() {
    return editor.getElement(document.getRootElement(), "ScoreBoard");
  }

  protected Element getPolicyElement(Policy policy) {
    return editor.getElement(getScoreBoardElement(), "Policy", policy.getId());
  }

  protected Element getPolicyParameterElement(Policy.Parameter parameter) {
    return editor.getElement(getPolicyElement(parameter.getPolicy()), "Parameter", parameter.getName());
  }

  protected Element getClockElement(Clock clock) {
    return editor.getElement(getScoreBoardElement(), "Clock", clock.getId());
  }

  protected Element getTeamElement(Team team) {
    return editor.getElement(getScoreBoardElement(), "Team", team.getId());
  }

  protected Element getPositionElement(Position position) {
    return editor.getElement(getTeamElement(position.getTeam()), "Position", position.getId());
  }

  protected Element getSkaterElement(Skater skater) {
    return editor.getElement(getTeamElement(skater.getTeam()), "Skater", skater.getId());
  }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();
  protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

  protected Document document = editor.createDocument("ScoreBoard");
  protected boolean empty = true;
  protected boolean persistent = false;

}
