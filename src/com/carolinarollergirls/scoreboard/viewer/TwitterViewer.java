package com.carolinarollergirls.scoreboard.viewer;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.FormatSpecifierScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class TwitterViewer {
    public TwitterViewer(ScoreBoard sB) {
        reset();
        scoreBoard = sB;
        formatSpecifier = new FormatSpecifierViewer(scoreBoard);
    }

    public void addConditionalTweet(String conditionFormat, String tweet, TwitterExceptionListener exceptionListener) throws TooManyListenersException {
        synchronized (conditionalListeners) {
            String key = getConditionalListenerKey(conditionFormat, tweet);
            if (conditionalListeners.containsKey(key)) {
                throw new TooManyListenersException("Conditional tweet with given parameters already exists");
            }
            ScoreBoardListener tweetListener = new TweetScoreBoardListener(tweet, exceptionListener);
            ScoreBoardListener asyncListener = new AsyncScoreBoardListener(tweetListener);
            ScoreBoardListener conditionalListener =
                    new FormatSpecifierScoreBoardListener(formatSpecifier, conditionFormat, asyncListener);
            conditionalListeners.put(key, conditionalListener);
            scoreBoard.addScoreBoardListener(conditionalListener);
        }
    }
    public void removeConditionalTweet(String conditionFormat, String tweet) {
        synchronized (conditionalListeners) {
            ScoreBoardListener listener = conditionalListeners.remove(getConditionalListenerKey(conditionFormat, tweet));
            if (null != listener) {
                scoreBoard.removeScoreBoardListener(listener);
            }
        }
    }
    protected String getConditionalListenerKey(String format, String tweet) {
        return "CONDITIONFORMAT:"+format+"TWEET:"+tweet;
    }

    public String getAuthURL(String callbackURL) throws TwitterException,IllegalStateException {
        synchronized (twitterLock) {
            if (null != requestToken) {
                reset();
            }
            requestToken = twitter.getOAuthRequestToken(callbackURL);
            return requestToken.getAuthorizationURL();
        }
    }

    public void setOAuthVerifier(String verifier) throws TwitterException {
        synchronized (twitterLock) {
            /* should throw exception if no requestToken or already logged in */
            AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
            userId = twitter.getId();
            twitterStream = twitterStreamFactory.getInstance(accessToken);
            twitterStream.addListener(userStreamListener);
            twitterStream.user();
            requestToken = null;
            loggedIn = true;
        }
    }

    public void addTweetListener(TweetListener listener) {
        synchronized (twitterLock) {
            if (!tweetListeners.containsKey(listener)) {
                tweetListeners.put(listener, Executors.newSingleThreadExecutor());
            }
        }
    }
    public void removeTweetListener(TweetListener listener) {
        synchronized (twitterLock) {
            if (tweetListeners.containsKey(listener)) {
                tweetListeners.remove(listener).shutdown();
            }
        }
    }
    protected void notifyTweetListeners(final String tweetText) {
        synchronized (twitterLock) {
            Iterator<TweetListener> listeners = tweetListeners.keySet().iterator();
            while (listeners.hasNext()) {
                final TweetListener listener = listeners.next();
                Runnable r = new Runnable() { public void run() { listener.tweet(tweetText); } };
                tweetListeners.get(listener).submit(r);
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
        if (null != twitter) {
            twitter.shutdown();
        }
        twitter = twitterFactory.getInstance();
        userId = 0;
        requestToken = null;
        if (null != twitterStream) {
            // cleanUp has a bug where it blocks until its internal thread
            // blocks until a new status comes in or it times out.
            // We don't need or want to wait so let's do the cleanup
            // in a separate thread.
            final TwitterStream tS = twitterStream;
            Runnable r = new Runnable() { public void run() { tS.cleanUp(); } };
            new Thread(r).start();
        }
        twitterStream = null;
        loggedIn = false;
    }

    public void tweet(String tweet) throws TwitterException {
        String parsedTweet = formatSpecifier.parse(tweet);
        synchronized (twitterLock) {
            if (isTestMode()) {
                notifyTweetListeners(parsedTweet);
            } else {
                /* will throw exception if not logged in */
                twitter.updateStatus(parsedTweet);
            }
        }
    }

    public boolean isLoggedIn() { return loggedIn; }

    public void setTestMode(boolean t) { testMode = t; }
    public boolean isTestMode() { return testMode; }

    private ConfigurationBuilder getConfigurationBuilder() {
        return new ConfigurationBuilder()
                .setDebugEnabled(false)
                .setUserStreamRepliesAllEnabled(false)
                .setOAuthConsumerKey("LcSklLv7gic519YE5ylK1g")
                .setOAuthConsumerSecret("BXjvuTrbl6rTIgybxqCTIfZS7obv2OdUYiM1n8V3Q");
    }

    protected TwitterFactory twitterFactory = new TwitterFactory(getConfigurationBuilder().build());
    protected TwitterStreamFactory twitterStreamFactory = new TwitterStreamFactory(getConfigurationBuilder().build());

    protected ScoreBoard scoreBoard;
    protected FormatSpecifierViewer formatSpecifier;

    protected Map<String,ScoreBoardListener> conditionalListeners = new HashMap<String,ScoreBoardListener>();

    protected Twitter twitter;
    protected Object twitterLock = new Object();
    protected long userId;

    protected boolean loggedIn = false;
    protected boolean testMode = false;

    protected RequestToken requestToken = null;
    protected TwitterStream twitterStream = null;

    protected Map<TweetListener,ExecutorService> tweetListeners = new HashMap<TweetListener,ExecutorService>();

    protected UserStreamListener userStreamListener = new UserStreamAdapter() {
        public void onStatus(Status status) {
            if (status.getUser().getId() == userId) {
                notifyTweetListeners(status.getText());
            }
        }
    };

    protected class TweetScoreBoardListener implements ScoreBoardListener {
        public TweetScoreBoardListener(String t, TwitterExceptionListener l) {
            tweet = t;
            exceptionListener = l;
        }
        public void scoreBoardChange(ScoreBoardEvent e) {
            try {
                if (isLoggedIn() || isTestMode()) {
                    tweet(tweet);
                }
            } catch ( TwitterException tE ) {
                exceptionListener.twitterException(tweet, tE);
            }
        }
        protected String tweet;
        protected TwitterExceptionListener exceptionListener;
    }

    public static interface TweetListener {
        public void tweet(String tweet);
    }
    public static interface TwitterExceptionListener {
        public void twitterException(String tweet, TwitterException exception);
    }
}
