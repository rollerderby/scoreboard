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
    Element updateE = createXPathElement();
    editor.setPI(editor.addElement(updateE, "AuthorizationURL"), "NoSave");
    editor.setPI(editor.addElement(updateE, "Authorized"), "NoSave");
    editor.setPI(editor.addElement(updateE, "Error"), "NoSave");
    editor.setPI(editor.addElement(updateE, "ScreenName"), "NoSave");
    editor.setPI(editor.addElement(updateE, "Status"), "NoSave");
    update(updateE);
    try { logout(); } catch ( Exception e ) { }
    removeAllConditionalTweets();
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
      else if (e.getName().equals("ConditionalTweet") && editor.hasPI(e, "Remove"))
        removeConditionalTweet(e);
      else if (e.getName().equals("ConditionalTweet"))
        addConditionalTweet(e);
      else if (e.getName().equals("AuthorizationURL") && !nullText && "".equals(text))
        clearAuthorizationURL();
      else if (e.getName().equals("Denied") && editor.isTrue(e))
        denied();
    } catch ( NoTwitterViewerException ntvE ) {
      setError("Twitter Viewer not loaded");
    } catch ( TwitterException tE ) {
      setError("Twitter Exception : "+tE.getMessage());
    } catch ( IllegalArgumentException iaE ) {
      setError(iaE.getMessage());
    }
  }

  protected void startOAuth(Element e) throws NoTwitterViewerException,TwitterException {
    Element updateE = createXPathElement();
    try {
      String authURL = getTwitterViewer().getAuthorizationURL(editor.getText(e));
      editor.addElement(updateE, "AuthorizationURL", null, authURL);
      editor.addElement(updateE, "Error", null, "");
    } catch ( IllegalStateException isE ) {
      editor.addElement(updateE, "Error", null, "Already logged in");
    }
    update(updateE);
  }

  protected void logout() throws NoTwitterViewerException,TwitterException {
    getTwitterViewer().logout();
    getTwitterViewer().removeTweetListener(tweetListener);
    Element updateE = createXPathElement();
    editor.addElement(updateE, "Authorized", null, "false");
    editor.addElement(updateE, "ScreenName", null, "");
    editor.addElement(updateE, "Error", null, "");
    editor.addElement(updateE, "Status", null, "");
    update(updateE);
  }

  protected void denied() throws NoTwitterViewerException,TwitterException {
    logout();
    setError("You denied access...");
  }

  protected void clearAuthorizationURL() {
    Element updateE = createXPathElement();
    editor.addElement(updateE, "AuthorizationURL", null, "");
    editor.addElement(updateE, "Error", null, "");
    update(updateE);
  }

  protected void setOAuthVerifier(Element e) throws NoTwitterViewerException,TwitterException {
    String verifier = editor.getText(e);
    if (null == verifier || "".equals(verifier))
      return;
    getTwitterViewer().setOAuthVerifier(verifier);
    Element updateE = createXPathElement();
    editor.addElement(updateE, "AuthorizationURL", null, "");
    editor.addElement(updateE, "Authorized", null, "true");
    editor.addElement(updateE, "ScreenName", null, getTwitterViewer().getScreenName());
    editor.addElement(updateE, "Status", null, "");
    editor.addElement(updateE, "Error", null, "");
    update(updateE);
    getTwitterViewer().addTweetListener(tweetListener);
  }

  protected void updateStatus(String status) {
    Element updateE = createXPathElement();
    editor.addElement(updateE, "Status", null, status);
    update(updateE);
  }

  protected void tweet(Element e) throws NoTwitterViewerException,TwitterException {
    String tweet = editor.getText(e);
    if (null == tweet || "".equals(tweet))
      return;
    getTwitterViewer().tweet(tweet);
    setError("");
  }

  protected void addConditionalTweet(Element e) throws NoTwitterViewerException,TwitterException,IllegalArgumentException {
    if (null == editor.getElement(e, "Condition", null, false) && null == editor.getElement(e, "Tweet", null, false))
      return; /* Ignore if no subelements specified */
    String condition = getElementCondition(e);
    String tweet = getElementTweet(e);
    try {
      getTwitterViewer().addConditionalTweet(condition, tweet);
    } catch ( TooManyListenersException tmlE ) {
      throw new IllegalArgumentException(tmlE.getMessage());
    }
    Element updateE = createXPathElement();
    Element conditionalTweet = editor.addElement(updateE, "ConditionalTweet", UUID.randomUUID().toString());
    editor.addElement(conditionalTweet, "Condition", null, condition);
    editor.addElement(conditionalTweet, "Tweet", null, tweet);
    update(updateE);
    setError("");
  }
  protected void removeConditionalTweet(Element e) throws NoTwitterViewerException,TwitterException,IllegalArgumentException {
    Element thisElement;
    try { thisElement = getXPathElement(); }
    catch ( JDOMException jE ) { return; /* FIXME: really need to fix getXPathElement to not need to throw jE */ }
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
  protected void removeAllConditionalTweets() {
    Element thisElement;
    try { thisElement = getXPathElement(); }
    catch ( JDOMException jE ) { return; }
    Iterator conditionalTweets = thisElement.getChildren("ConditionalTweet").iterator();
    while (conditionalTweets.hasNext()) {
      try { removeConditionalTweet((Element)conditionalTweets.next()); }
      catch ( Exception e ) { /* Since we're removing existing ones, this shouldn't happen */ }
    }
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

  protected void setError(String error) {
    update(createXPathElement().addContent(editor.setText(new Element("Error"), error)));
  }

  protected TwitterViewer twitterViewer = null;
  protected Object twitterViewerLock = new Object();
  protected TwitterViewer.TweetListener tweetListener = new TwitterViewer.TweetListener() {
      public void tweet(String tweet) { updateStatus(tweet); }
    };

  protected static final String twitterKey = TwitterViewer.class.getName();

  protected static class NoTwitterViewerException extends Exception
  {
    public NoTwitterViewerException(String msg) { super(msg); }
  }
}
