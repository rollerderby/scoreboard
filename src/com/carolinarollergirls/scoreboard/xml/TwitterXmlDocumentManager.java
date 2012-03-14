package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import org.jdom.*;

import twitter4j.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.viewer.*;

public class TwitterXmlDocumentManager extends SegmentedXmlDocumentManager
{
  public TwitterXmlDocumentManager() { super("Viewers", "Twitter"); }

  public void reset() {
    try { logout(); } catch ( Exception e ) { }
  }

  protected void processChildElement(Element e) {
    String text = editor.getText(e);
    boolean nullText = (null == text);
    try {
      if (e.getName().equals("Start") && !nullText && !"".equals(text))
        startOAuth(e);
      else if (e.getName().equals("Stop") && editor.isTrue(e))
        logout();
      else if (e.getName().equals("SetOAuthVerifier") && !nullText)
        setOAuthVerifier(e);
      else if (e.getName().equals("Tweet") && !nullText)
        tweet(e);
      else if (e.getName().equals("AddConditionalTweet"))
        addConditionalTweet(e);
      else if (e.getName().equals("ConditionalTweet") && editor.hasPI(e, "Remove"))
        removeConditionalTweet(e);
      else if (e.getName().equals("AuthorizationURL") && !nullText && "".equals(text))
        clearAuthorizationURL();
      else if (e.getName().equals("Denied") && editor.isTrue(e))
        denied();
    } catch ( NoTwitterViewerException ntvE ) {
      Element updateE = createXPathElement();
      updateE.addContent(editor.setText(new Element("Error"), "Twitter Viewer not loaded"));
      update(updateE);
    } catch ( TwitterException tE ) {
      Element updateE = createXPathElement();
      updateE.addContent(editor.setText(new Element("Error"), "Twitter Exception : "+tE.getMessage()));
      update(updateE);
    } catch ( IllegalArgumentException iaE ) {
      Element updateE = createXPathElement();
      updateE.addContent(editor.setText(new Element("Error"), iaE.getMessage()));
      update(updateE);
    }
  }

  protected void startOAuth(Element e) throws NoTwitterViewerException,TwitterException {
    Element updateE = createXPathElement();
    try {
      String authURL = getTwitterViewer().getAuthorizationURL(editor.getText(e));
      updateE.addContent(editor.setText(new Element("AuthorizationURL"), authURL));
      updateE.addContent(editor.setText(new Element("Error"), ""));
    } catch ( IllegalStateException isE ) {
      updateE.addContent(editor.setText(new Element("Error"), "Already logged in"));
    }
    update(updateE);
  }

  protected void logout() throws NoTwitterViewerException,TwitterException {
    getTwitterViewer().logout();
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Authorized"), "false"));
    updateE.addContent(editor.setText(new Element("ScreenName"), ""));
    updateE.addContent(editor.setText(new Element("Error"), ""));
    updateE.addContent(editor.setText(new Element("Status"), ""));
    update(updateE);
  }

  protected void denied() throws NoTwitterViewerException,TwitterException {
    logout();
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Error"), "You denied access..."));
    update(updateE);
  }

  protected void clearAuthorizationURL() {
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("AuthorizationURL"), ""));
    updateE.addContent(editor.setText(new Element("Error"), ""));
    update(updateE);
  }

  protected void setOAuthVerifier(Element e) throws NoTwitterViewerException,TwitterException {
    String verifier = editor.getText(e);
    if (null == verifier || "".equals(verifier))
      return;
    getTwitterViewer().setOAuthVerifier(verifier);
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("AuthorizationURL"), ""));
    updateE.addContent(editor.setText(new Element("Authorized"), "true"));
    updateE.addContent(editor.setText(new Element("ScreenName"), getTwitterViewer().getScreenName()));
    updateE.addContent(editor.setText(new Element("Status"), ""));
    updateE.addContent(editor.setText(new Element("Error"), ""));
    update(updateE);
    getTwitterViewer().addUserStreamListener(userStreamListener);
  }

  protected void updateStatus(Status status) {
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Status"), status.getText()));
    update(updateE);
  }

  protected void tweet(Element e) throws NoTwitterViewerException,TwitterException {
    String tweet = editor.getText(e);
    if (null == tweet || "".equals(tweet))
      return;
    getTwitterViewer().tweet(tweet);
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Error"), ""));
    update(updateE);
  }

  protected void addConditionalTweet(Element e) throws NoTwitterViewerException,TwitterException,IllegalArgumentException {
    String condition = getElementCondition(e);
    String tweet = getElementTweet(e);
    try {
      getTwitterViewer().addConditionalTweet(condition, tweet);
    } catch ( TooManyListenersException tmlE ) {
      throw new IllegalArgumentException(tmlE.getMessage());
    }
    Element updateE = createXPathElement();
    Element conditionalTweet = editor.addElement(updateE, "ConditionalTweet", UUID.randomUUID().toString());
    conditionalTweet.addContent(editor.setText(new Element("Condition"), condition));
    conditionalTweet.addContent(editor.setText(new Element("Tweet"), tweet));
    updateE.addContent(editor.setText(new Element("Error"), ""));
    update(updateE);
  }
  protected void removeConditionalTweet(Element e) throws NoTwitterViewerException,TwitterException,IllegalArgumentException {
    Element thisElement;
    try { thisElement = getXPathElement(); }
    catch ( JDOMException jE ) { return; /* FIXME: What to do? */ }
    Element conditionalTweet = editor.getElement(thisElement, "ConditionalTweet", editor.getId(e), false);
    if (null == conditionalTweet)
      return;
    getTwitterViewer().removeConditionalTweet(getElementCondition(conditionalTweet), getElementTweet(conditionalTweet));
    Element updateE = createXPathElement();
    editor.setPI(editor.addElement(updateE, "ConditionalTweet", editor.getId(e)), "Remove");
    update(updateE);
  }
  protected String getElementCondition(Element e) throws IllegalArgumentException {
    String condition = editor.getText(editor.getElement(e, "Condition"));
    if (null == condition || "".equals(condition))
      throw new IllegalArgumentException("No condition specified");
    return condition;
  }
  protected String getElementTweet(Element e) throws IllegalArgumentException {
    String tweet = editor.getText(editor.getElement(e, "Tweet"));
    if (null == tweet || "".equals(tweet))
      throw new IllegalArgumentException("No tweet specified");
    return tweet;
  }

  protected TwitterViewer getTwitterViewer() throws NoTwitterViewerException {
    synchronized (twitterViewerLock) {
      if (null == twitterViewer) {
        twitterViewer = (TwitterViewer)ScoreBoardManager.getScoreBoardViewer(twitterKey);
        if (null == twitterViewer)
          throw new NoTwitterViewerException("No TwitterViewer found");
      }
      return twitterViewer;
    }
  }

  protected Element createXPathElement() {
    return editor.setNoSavePI(super.createXPathElement());
  }

  protected TwitterViewer twitterViewer = null;
  protected Object twitterViewerLock = new Object();
  protected UserStreamListener userStreamListener = new UserStreamAdapter() {
      public void onStatus(Status status) { updateStatus(status); }
    };

  protected static final String twitterKey = TwitterViewer.class.getName();

  protected static class NoTwitterViewerException extends Exception
  {
    public NoTwitterViewerException(String msg) { super(msg); }
  }
}
