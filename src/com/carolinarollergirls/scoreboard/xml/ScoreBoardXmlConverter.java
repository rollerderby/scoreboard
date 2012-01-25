package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class ScoreBoardXmlConverter
{
  /*****************************/
  /* ScoreBoard to XML methods */

  public String toString(ScoreBoard scoreBoard) {
    return editor.toString(toDocument(scoreBoard));
  }

  public Document toDocument(ScoreBoard scoreBoard) {
    Element sb = new Element("ScoreBoard");
    Document d = new Document(new Element("document").addContent(sb));

    editor.setElement(sb, "StartJam", null, "");
    editor.setElement(sb, "UnStartJam", null, "");
    editor.setElement(sb, "StopJam", null, "");
    editor.setElement(sb, "UnStopJam", null, "");
    editor.setElement(sb, "Timeout", null, "");
    editor.setElement(sb, "UnTimeout", null, "");
    editor.setElement(sb, "StartOvertime", null, "");
    editor.setElement(sb, "UpdateImages", null, "");

    editor.setElement(sb, "TimeoutOwner", null, scoreBoard.getTimeoutOwner());
    editor.setElement(sb, "Overtime", null, String.valueOf(scoreBoard.getOvertime()));

    Iterator<Clock> clocks = scoreBoard.getClocks().iterator();
    while (clocks.hasNext())
      toElement(sb, clocks.next());

    Iterator<Team> teams = scoreBoard.getTeams().iterator();
    while (teams.hasNext())
      toElement(sb, teams.next());

    Iterator<Policy> policies = scoreBoard.getPolicies().iterator();
    while (policies.hasNext())
      toElement(sb, policies.next());

    return d;
  }

  public Element toElement(Element sb, Clock c) {
    Element e = editor.setElement(sb, "Clock", c.getId());

    editor.setElement(e, "Start", null, "");
    editor.setElement(e, "UnStart", null, "");
    editor.setElement(e, "Stop", null, "");
    editor.setElement(e, "UnStop", null, "");
    editor.setElement(e, "ResetTime", null, "");

    editor.setElement(e, "Name", null, c.getName());
    editor.setElement(e, "Number", null, String.valueOf(c.getNumber()));
    editor.setElement(e, "MinimumNumber", null, String.valueOf(c.getMinimumNumber()));
    editor.setElement(e, "MaximumNumber", null, String.valueOf(c.getMaximumNumber()));
    editor.setElement(e, "Time", null, String.valueOf(c.getTime()));
    editor.setElement(e, "MinimumTime", null, String.valueOf(c.getMinimumTime()));
    editor.setElement(e, "MaximumTime", null, String.valueOf(c.getMaximumTime()));
    editor.setElement(e, "Running", null, String.valueOf(c.isRunning()));
    editor.setElement(e, "Direction", null, String.valueOf(c.isCountDirectionDown()));
    return e;
  }

  public Element toElement(Element sb, Team t) {
    Element e = editor.setElement(sb, "Team", t.getId());

    editor.setElement(e, "Timeout", null, "");

    editor.setElement(e, "Name", null, t.getName());
    editor.setElement(e, "Logo", null, t.getLogo());
    editor.setElement(e, "Score", null, String.valueOf(t.getScore()));
    editor.setElement(e, "Timeouts", null, String.valueOf(t.getTimeouts()));
    editor.setElement(e, "LeadJammer", null, String.valueOf(t.isLeadJammer()));
    editor.setElement(e, "Pass", null, String.valueOf(t.getPass()));

    Iterator<Position> positions = t.getPositions().iterator();
    while (positions.hasNext())
      toElement(e, positions.next());

    Iterator<Skater> skaters = t.getSkaters().iterator();
    while (skaters.hasNext())
      toElement(e, skaters.next());

    return e;
  }

  public Element toElement(Element team, Position p) {
    Element e = editor.setElement(team, "Position", p.getId());

    editor.setElement(e, "Clear", null, "");

    Skater s = p.getSkater();
    editor.setElement(e, "Id", null, (s==null?"":s.getId()));
    editor.setElement(e, "Name", null, (s==null?"":s.getName()));
    editor.setElement(e, "Number", null, (s==null?"":s.getNumber()));

    return e;
  }

  public Element toElement(Element sb, Policy p) {
    Element e = editor.setElement(sb, "Policy", p.getId());
    editor.setElement(e, "Name", null, p.getName());
    editor.setElement(e, "Description", null, p.getDescription());
    editor.setElement(e, "Enabled", null, String.valueOf(p.isEnabled()));

    Iterator<Policy.Parameter> parameters = p.getParameters().iterator();
    while (parameters.hasNext())
      toElement(e, parameters.next());

    return e;
  }

  public Element toElement(Element p, Policy.Parameter pp) {
    Element e = editor.setElement(p, "Parameter", pp.getName());
    editor.setElement(e, "Name", null, pp.getName());
    editor.setElement(e, "Type", null, pp.getType());
    editor.setElement(e, "Value", null, pp.getValue());
    return e;
  }

  public Element toElement(Element t, Skater s) {
    Element e = editor.setElement(t, "Skater", s.getId());
    editor.setElement(e, "Name", null, s.getName());
    editor.setElement(e, "Number", null, s.getNumber());
    editor.setElement(e, "Position", null, s.getPosition());
    editor.setElement(e, "LeadJammer", null, String.valueOf(s.isLeadJammer()));
    editor.setElement(e, "PenaltyBox", null, String.valueOf(s.isPenaltyBox()));
    editor.setElement(e, "Pass", null, String.valueOf(s.getPass()));

    return e;
  }

  /*****************************/
  /* XML to ScoreBoard methods */

  public void processDocument(ScoreBoardModel scoreBoardModel, Document document) {
    Iterator children = document.getRootElement().getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      if (element.getName().equals("ScoreBoard"))
        processScoreBoard(scoreBoardModel, element);
    }
  }

  public void processScoreBoard(ScoreBoardModel scoreBoardModel, Element scoreBoard) {
/* DEPRECATED - all this "call" functionality is deprecated; use actual XML elements instead! */
    String call = scoreBoard.getAttributeValue("call");
    if (call == null)
      call = "";

    if (call.equalsIgnoreCase("startJam"))
      scoreBoardModel.startJam();
    else if (call.equalsIgnoreCase("stopJam"))
      scoreBoardModel.stopJam();
    else if (call.equalsIgnoreCase("timeout"))
      scoreBoardModel.timeout();
    else if (call.equalsIgnoreCase("unStartJam"))
      scoreBoardModel.unStartJam();
    else if (call.equalsIgnoreCase("unStopJam"))
      scoreBoardModel.unStopJam();
    else if (call.equalsIgnoreCase("unTimeout"))
      scoreBoardModel.unTimeout();
/* END DEPRECATED */

    Iterator children = scoreBoard.getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      try {
        String name = element.getName();
        String value = editor.getText(element);
        boolean bVal = Boolean.parseBoolean(value);

        if (name.equals("StartJam") && bVal)
          scoreBoardModel.startJam();
        else if (name.equals("StopJam") && bVal)
          scoreBoardModel.stopJam();
        else if (name.equals("Timeout") && bVal)
          scoreBoardModel.timeout();
        else if (name.equals("UnStartJam") && bVal)
          scoreBoardModel.unStartJam();
        else if (name.equals("UnStopJam") && bVal)
          scoreBoardModel.unStopJam();
        else if (name.equals("UnTimeout") && bVal)
          scoreBoardModel.unTimeout();
        else if (name.equals("StartOvertime") && bVal)
          scoreBoardModel.startOvertime();
        else if (name.equals("TimeoutOwner"))
          scoreBoardModel.setTimeoutOwner(value);
        else if (name.equals("Overtime"))
          scoreBoardModel.setOvertime(bVal);
        else if (name.equals("Clock"))
          processClock(scoreBoardModel, element);
        else if (name.equals("Team"))
          processTeam(scoreBoardModel, element);
        else if (name.equals("Policy"))
          processPolicy(scoreBoardModel, element);
        else
          continue;
      } catch ( Exception e ) {
      }
    }

    removeScoreBoardChildren(scoreBoard);
  }

  // FIXME - this removes all children of ScoreBoard except the "Page" children
  // one the legacy HTML code is replaced (or fixed), we should just remove ScoreBoard entirely.
  @SuppressWarnings("unchecked")
  protected void removeScoreBoardChildren(Element sb) {
    sb.getChildren().retainAll(sb.getChildren("Page"));
  }

  public void processClock(ScoreBoardModel scoreBoardModel, Element clock) {
    String id = clock.getAttributeValue("Id");
    ClockModel clockModel = scoreBoardModel.getClockModel(id);

/* DEPRECATED - all this "call" functionality is deprecated; use actual XML elements instead! */
    String call = clock.getAttributeValue("call");
    if (call == null)
      call = "";

    if (call.equalsIgnoreCase("start"))
      clockModel.start();
    else if (call.equalsIgnoreCase("stop"))
      clockModel.stop();
    else if (call.equalsIgnoreCase("unStart"))
      clockModel.unstart();
    else if (call.equalsIgnoreCase("unStop"))
      clockModel.unstop();
    else if (call.equalsIgnoreCase("resetTime"))
      clockModel.resetTime();
/* END DEPRECATED */

    Iterator children = clock.getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      try {
        String name = element.getName();
        String value = editor.getText(element);

        boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));
        boolean isReset = Boolean.parseBoolean(element.getAttributeValue("reset"));

//FIXME - might be better way to handle changes/resets than an attribute...
        if ((null == value) && !isReset)
          continue;
        else if (name.equals("Start") && Boolean.parseBoolean(value))
          clockModel.start();
        else if (name.equals("Stop") && Boolean.parseBoolean(value))
          clockModel.stop();
        else if (name.equals("UnStart") && Boolean.parseBoolean(value))
          clockModel.unstart();
        else if (name.equals("UnStop") && Boolean.parseBoolean(value))
          clockModel.unstop();
        else if (name.equals("ResetTime") && Boolean.parseBoolean(value))
          clockModel.resetTime();
        else if (name.equals("Name"))
          clockModel.setName(value);
        else if (name.equals("Number") && isChange)
          clockModel.changeNumber(Integer.parseInt(value));
        else if (name.equals("Number") && !isChange)
          clockModel.setNumber(Integer.parseInt(value));
        else if (name.equals("MinimumNumber"))
          clockModel.setMinimumNumber(Integer.parseInt(value));
        else if (name.equals("MaximumNumber"))
          clockModel.setMaximumNumber(Integer.parseInt(value));
        else if (name.equals("Time") && isChange)
          clockModel.changeTime(Long.parseLong(value));
        else if (name.equals("Time") && isReset)
          clockModel.resetTime();
        else if (name.equals("Time") && !isChange && !isReset)
          clockModel.setTime(Long.parseLong(value));
        else if (name.equals("MinimumTime") && isChange)
          clockModel.changeMinimumTime(Long.parseLong(value));
        else if (name.equals("MinimumTime"))
          clockModel.setMinimumTime(Long.parseLong(value));
        else if (name.equals("MaximumTime") && isChange)
          clockModel.changeMaximumTime(Long.parseLong(value));
        else if (name.equals("MaximumTime"))
          clockModel.setMaximumTime(Long.parseLong(value));
        else if (name.equals("Running") && Boolean.parseBoolean(value))
          clockModel.start();
        else if (name.equals("Running") && !Boolean.parseBoolean(value))
          clockModel.stop();
        else if (name.equals("Direction"))
          clockModel.setCountDirectionDown(Boolean.parseBoolean(value));
      } catch ( Exception e ) {
      }
    }
  }

  public void processTeam(ScoreBoardModel scoreBoardModel, Element team) {
    String id = team.getAttributeValue("Id");
    TeamModel teamModel = scoreBoardModel.getTeamModel(id);

/* DEPRECATED - all this "call" functionality is deprecated; use actual XML elements instead! */
    String call = team.getAttributeValue("call");
    if (call == null)
      call = "";

    if (call.equalsIgnoreCase("timeout"))
      teamModel.timeout();
/* END DEPRECATED */

    Iterator children = team.getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      try {
        String name = element.getName();
        String eId = element.getAttributeValue("Id");
        String value = editor.getText(element);

        boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));

        if (name.equals("Skater"))
          processSkater(teamModel, element);
        else if (name.equals("Position"))
          processPosition(teamModel, element);
        else if (null == value)
          continue;
        else if (name.equals("Timeout") && Boolean.parseBoolean(value))
          teamModel.timeout();
        else if (name.equals("Name"))
          teamModel.setName(value);
        else if (name.equals("Logo"))
          teamModel.setLogo(value);
        else if (name.equals("Score") && isChange)
          teamModel.changeScore(Integer.parseInt(value));
        else if (name.equals("Score") && !isChange)
          teamModel.setScore(Integer.parseInt(value));
        else if (name.equals("Timeouts") && isChange)
          teamModel.changeTimeouts(Integer.parseInt(value));
        else if (name.equals("Timeouts") && !isChange)
          teamModel.setTimeouts(Integer.parseInt(value));
        else if (name.equals("LeadJammer"))
          teamModel.setLeadJammer(Boolean.parseBoolean(value));
        else if (name.equals("Pass") && isChange)
          teamModel.changePass(Integer.parseInt(value));
        else if (name.equals("Pass") && !isChange)
          teamModel.setPass(Integer.parseInt(value));
      } catch ( Exception e ) {
      }
    }
  }

  public void processPosition(TeamModel teamModel, Element position) {
    String id = position.getAttributeValue("Id");
    PositionModel positionModel = teamModel.getPositionModel(id);

/* DEPRECATED - all this "call" functionality is deprecated; use actual XML elements instead! */
    String call = position.getAttributeValue("call");
    if ("clear".equalsIgnoreCase(call))
      positionModel.clear();
/* END DEPRECATED */

    Iterator children = position.getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      try {
        String name = element.getName();
        String value = editor.getText(element);

        if (null == value)
          continue;
        else if (name.equals("Clear") && Boolean.parseBoolean(value))
          positionModel.clear();
        else if (name.equals("Id"))
          positionModel.setSkaterModel(value);
      } catch ( Exception e ) {
      }
    }
  }

  public void processPolicy(ScoreBoardModel scoreBoardModel, Element policy) throws NoSuchElementException {
    String id = policy.getAttributeValue("Id");
    PolicyModel policyModel = scoreBoardModel.getPolicyModel(id);

    Iterator children = policy.getChildren().iterator();
    while (children.hasNext()) {
      Element element= (Element)children.next();
      try {
        String name = element.getName();
        String value = editor.getText(element);

        if (name.equals("Parameter"))
          processPolicyParameter(policyModel, element);
        else if (null == value)
          continue;
        else if (name.equals("Name"))
          policyModel.setName(value);
        else if (name.equals("Description"))
          policyModel.setDescription(value);
        else if (name.equals("Enabled"))
          policyModel.setEnabled(Boolean.parseBoolean(value));
      } catch ( Exception e ) {
      }
    }
  }

  public void processPolicyParameter(PolicyModel policyModel, Element parameter) throws NoSuchElementException {
    String id = parameter.getAttributeValue("Id");
    PolicyModel.ParameterModel parameterModel = policyModel.getParameterModel(id);

    Iterator children = parameter.getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      try {
        String name = element.getName();
        String value = editor.getText(element);

        if (null == value)
          continue;
        else if (name.equals("Value"))
          parameterModel.setValue(value);
      } catch ( Exception e ) {
      }
    }
  }

  public void processSkater(TeamModel teamModel, Element skater) {
    String id = skater.getAttributeValue("Id");
    SkaterModel skaterModel;

    if (Boolean.parseBoolean(skater.getAttributeValue("remove"))) {
      teamModel.removeSkaterModel(id);
      return;
    }

    try {
      skaterModel = teamModel.getSkaterModel(id);
    } catch ( SkaterNotFoundException snfE ) {
      Element nameE = skater.getChild("Name");
      String name = (nameE == null ? id : editor.getText(nameE));
      Element numberE = skater.getChild("Number");
      String number = (numberE == null ? "" : editor.getText(numberE));
      teamModel.addSkaterModel(id, name, number);
      skaterModel = teamModel.getSkaterModel(id);
    }

    Iterator children = skater.getChildren().iterator();
    while (children.hasNext()) {
      Element element = (Element)children.next();
      try {
        String name = element.getName();
        String value = editor.getText(element);

        boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));

        if (name.equals("Name"))
          skaterModel.setName(value);
        else if (name.equals("Number"))
          skaterModel.setNumber(value);
        else if (name.equals("Position"))
          skaterModel.setPosition(value);
        else if (name.equals("LeadJammer"))
          skaterModel.setLeadJammer(Boolean.parseBoolean(value));
        else if (name.equals("PenaltyBox"))
          skaterModel.setPenaltyBox(Boolean.parseBoolean(value));
        else if (name.equals("Pass") && isChange)
          skaterModel.changePass(Integer.parseInt(value));
        else if (name.equals("Pass") && !isChange)
          skaterModel.setPass(Integer.parseInt(value));
      } catch ( Exception e ) {
      }
    }
  }

  public static ScoreBoardXmlConverter getInstance() { return scoreBoardXmlConverter; }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();

  private static ScoreBoardXmlConverter scoreBoardXmlConverter = new ScoreBoardXmlConverter();
}
