package com.carolinarollergirls.scoreboard.viewer;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.io.*;

import twitter4j.*;
import twitter4j.conf.*;
import twitter4j.auth.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;

public class TwitterViewer implements ScoreBoardViewer
{
  public TwitterViewer() {
    reset();
  }

  public void setScoreBoard(ScoreBoard sB) {
    scoreBoard = sB;
    ScoreBoardEvent onJamStart = new ScoreBoardEvent(sB.getClock(Clock.ID_JAM), "Running", Boolean.TRUE);
    ScoreBoardEvent onTimeoutStart = new ScoreBoardEvent(sB.getClock(Clock.ID_TIMEOUT), "Running", Boolean.TRUE);
    ScoreBoardEvent onIntermissionStart = new ScoreBoardEvent(sB.getClock(Clock.ID_INTERMISSION), "Running", Boolean.TRUE);
    addConditionalTweet(onJamStart, "Start Jam. %cjn, %t1n %t1s - %t2n %t2s");
    addConditionalTweet(onTimeoutStart, "Timeout. %t1n %t1s - %t2n %t2s");
    addConditionalTweet(onIntermissionStart, "End of Period %cpn. %t1n %t1s - %t2n %t2s");
  }

  public void addConditionalTweet(ScoreBoardEvent event, String tweet) {
    ScoreBoardListener l = new TweetScoreBoardListener(tweet);
    // FIXME - need to put cond listener in list/map so we can remove it later
    scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(event, l));
  }

  public String getAuthorizationURL(String callbackURL) throws TwitterException,IllegalStateException {
    synchronized (twitterLock) {
      if (null != requestToken)
        reset();
      requestToken = twitter.getOAuthRequestToken(callbackURL);
      return requestToken.getAuthorizationURL();
    }
  }

  public void setOAuthVerifier(String verifier) throws TwitterException {
    synchronized (twitterLock) {
      /* should throw exception if no requestToken or already logged in */
      AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
      twitterStream = twitterStreamFactory.getInstance(accessToken);
      requestToken = null;
      loggedIn = true;
    }
  }

  public void addUserStreamListener(UserStreamListener listener) throws TwitterException {
    synchronized (twitterLock) {
      try {
        twitterStream.addListener(listener);
        twitterStream.user();
      } catch ( NullPointerException npE ) {
        throw new TwitterException("Not Logged In");
      }
    }
  }

  public String getScreenName() throws TwitterException {
    synchronized (twitterLock) {
      return twitter.getScreenName();
    }
  }

  public void logout() {
    synchronized (twitterLock) {
      reset();
    }
  }

  protected void reset() {
    if (null != twitter)
      twitter.shutdown();
    twitter = twitterFactory.getInstance();
    requestToken = null;
    if (null != twitterStream)
      twitterStream.cleanUp();
    twitterStream = null;
    loggedIn = false;
  }

  public void tweet(String tweet) throws TwitterException {
    synchronized (twitterLock) {
      /* will throw exception if not logged in */
      twitter.updateStatus(parseTweet(tweet));
    }
  }

  /**
   * Parse a tweet string, replacing special sequences
   *
   * Current special sequences are:
   * %t1n = Team 1 Name
   * %t1s = Team 1 Score
   * %t1t = Team 1 Timeouts
   * %t1jn = Team 1 Jammer Name
   * %t1jN = Team 1 Jammer Number
   * %t1pn = Team 1 Pivot Name
   * %t1pN = Team 1 Pivot Number
   * %t1b1n = Team 1 Blocker1 Name
   * %t1b1N = Team 1 Blocker1 Number
   * %t1b2n = Team 1 Blocker2 Name
   * %t1b2N = Team 1 Blocker2 Number
   * %t1b3n = Team 1 Blocker3 Name
   * %t1b3N = Team 1 Blocker3 Number
   *   repeat above for Team 2 with %t2*
   * %cpn = Clock Period Number
   * %cpts = Clock Period Time, in seconds
   * %cptms = Clock Period Time, in min:sec
   *   repeat above for Clock Jam with %cj*,
   *     Clock Lineup with %cl*,
   *     Clock Intermission with %ci*,
   *     Clock Timeout with %ct*
   */
  protected String parseTweet(String tweet) {
    tweet = tweetReplaceTeams(tweet);
    tweet = tweetReplaceClocks(tweet);
    return tweet;
  }

  protected String tweetReplaceTeams(String tweet) {
    if (!tweet.contains("%t"))
      return tweet;
    tweet = tweetReplaceTeam(tweet, "1", Team.ID_1);
    tweet = tweetReplaceTeam(tweet, "2", Team.ID_2);
    return tweet;
  }

  protected String tweetReplaceTeam(String tweet, String t, String id) {
    if (!tweet.contains("%t"+t))
      return tweet;
    Team team = scoreBoard.getTeam(id);
    Skater jammer = team.getPosition(Position.ID_JAMMER).getSkater();
    Skater pivot = team.getPosition(Position.ID_PIVOT).getSkater();
    Skater b1 = team.getPosition(Position.ID_BLOCKER1).getSkater();
    Skater b2 = team.getPosition(Position.ID_BLOCKER2).getSkater();
    Skater b3 = team.getPosition(Position.ID_BLOCKER3).getSkater();
    return tweet
      .replaceAll("%t"+t+"n", team.getName())
      .replaceAll("%t"+t+"s", String.valueOf(team.getScore()))
      .replaceAll("%t"+t+"t", String.valueOf(team.getTimeouts()))
      .replaceAll("%t"+t+"jn", getSkaterName(jammer))
      .replaceAll("%t"+t+"jN", getSkaterNumber(jammer))
      .replaceAll("%t"+t+"pn", getSkaterName(pivot))
      .replaceAll("%t"+t+"pN", getSkaterNumber(pivot))
      .replaceAll("%t"+t+"b1n", getSkaterName(b1))
      .replaceAll("%t"+t+"b1N", getSkaterNumber(b1))
      .replaceAll("%t"+t+"b2n", getSkaterName(b2))
      .replaceAll("%t"+t+"b2N", getSkaterNumber(b2))
      .replaceAll("%t"+t+"b3n", getSkaterName(b3))
      .replaceAll("%t"+t+"b3N", getSkaterNumber(b3));
  }

  protected String getSkaterName(Skater s) { return (null==s?"":s.getName()); }
  protected String getSkaterNumber(Skater s) { return (null==s?"":s.getNumber()); }

  protected String tweetReplaceClocks(String tweet) {
    if (!tweet.contains("%c"))
      return tweet;
    tweet = tweetReplaceClock(tweet, "p", Clock.ID_PERIOD);
    tweet = tweetReplaceClock(tweet, "j", Clock.ID_JAM);
    tweet = tweetReplaceClock(tweet, "t", Clock.ID_TIMEOUT);
    tweet = tweetReplaceClock(tweet, "l", Clock.ID_LINEUP);
    tweet = tweetReplaceClock(tweet, "i", Clock.ID_INTERMISSION);
    return tweet;
  }

  protected String tweetReplaceClock(String tweet, String c, String id) {
    if (!tweet.contains("%c"+c))
      return tweet;
    Clock clock = scoreBoard.getClock(id);
    return tweet
      .replaceAll("%c"+c+"n", String.valueOf(clock.getNumber()))
      .replaceAll("%c"+c+"ts", getClockSeconds(clock.getTime()))
      .replaceAll("%c"+c+"tms", getClockMinSec(clock.getTime()));
  }

  protected String getClockSeconds(long time) { return String.valueOf(time/1000); }
  protected String getClockMinSec(long time) {
    time = (time/1000);
    return (time/60)+":"+(time%60);
  }

  public boolean isLoggedIn() { return loggedIn; }

  private ConfigurationBuilder getConfigurationBuilder() {
    return new ConfigurationBuilder()
      .setOAuthConsumerKey("LcSklLv7gic519YE5ylK1g")
      .setOAuthConsumerSecret("BXjvuTrbl6rTIgybxqCTIfZS7obv2OdUYiM1n8V3Q");
  }

  protected TwitterFactory twitterFactory = new TwitterFactory(getConfigurationBuilder().build());
  protected TwitterStreamFactory twitterStreamFactory = new TwitterStreamFactory(getConfigurationBuilder().build());

  protected ScoreBoard scoreBoard;

  protected Twitter twitter;
  protected Object twitterLock = new Object();

  protected boolean loggedIn = false;

  protected RequestToken requestToken = null;
  protected TwitterStream twitterStream = null;

  protected class TweetScoreBoardListener implements ScoreBoardListener
  {
    public TweetScoreBoardListener(String t) { tweet = t; }
    public void scoreBoardChange(ScoreBoardEvent e) {
      try {
        if (loggedIn)
          tweet(tweet);
      } catch ( TwitterException tE ) {
        ScoreBoardManager.printMessage("Error trying to tweet : "+tE.getMessage());
      }
    }
    protected String tweet;
  }
}
