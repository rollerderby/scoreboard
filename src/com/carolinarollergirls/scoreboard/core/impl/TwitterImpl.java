package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.HashMap;
import java.util.Map;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterMethod;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Twitter;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.FormatSpecifierScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.viewer.FormatSpecifierViewer;

public class TwitterImpl extends ScoreBoardEventProviderImpl implements Twitter {
    public TwitterImpl(ScoreBoard sb) {
        super(sb, null, "", ScoreBoard.Child.TWITTER, Twitter.class, Value.class, Child.class, Command.class);

        formatSpecifierViewer = new FormatSpecifierViewer(sb);
        int i = 0;
        for(Map.Entry<String,String> entry : formatSpecifierViewer.getFormatSpecifierDescriptions().entrySet()) {
            final FormatSpecifier fs = (FormatSpecifier)getOrCreate(Child.FORMAT_SPECIFIER, String.valueOf(i++));
            final String key = entry.getKey();
            final FormatSpecifierViewer.ScoreBoardValue value = formatSpecifierViewer.getFormatSpecifierScoreBoardValue(key);
            fs.set(FormatSpecifier.Value.KEY, key);
            fs.set(FormatSpecifier.Value.DESCRIPTION, entry.getValue());
            // Provide current value of each specifier to the frontend.
            scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(
                                                 formatSpecifierViewer.getScoreBoardCondition(key),
            new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent e) {
                    fs.set(FormatSpecifier.Value.CURRENT_VALUE, value.getValue());
                }
            }
                                             ));
        }
        addWriteProtection(Child.FORMAT_SPECIFIER);

        twitter.addListener(new Listener());
    }
    
    @Override
    public void postAutosaveUpdate() {
        set(Value.MANUAL_TWEET, "");
        set(Value.ERROR, "");
        // If we were authenticated when shut down.
        if (isLoggedIn()) {
            twitter.setOAuthAccessToken(new AccessToken((String)get(Value.ACCESS_TOKEN), (String)get(Value.ACCESS_TOKEN_SECRET)));
            twitter.verifyCredentials();  // This is async, and checks our credentials work.
        }
        initilized = true;
    }

    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (!initilized) { return; }
        if (prop == Value.OAUTH_VERIFIER && value != null && !((String)value).isEmpty()) {
            twitter.getOAuthAccessTokenAsync(requestToken, (String) value);
        } else if (prop == Value.MANUAL_TWEET && value != null && !((String)value).isEmpty()) {
            tweet((String)value);
            set(Value.MANUAL_TWEET, "");
        }
    }

    @Override
    public void execute(CommandProperty prop) {
        switch((Command)prop) {
        case LOGIN:
            twitter.getOAuthRequestTokenAsync((String)get(Value.CALLBACK_URL));
            break;
        case LOGOUT:
            resetTwitter();
            break;
        }
    }

    private void resetTwitter() {
        synchronized(coreLock) {
            set(Value.LOGGED_IN, false);
            set(Value.SCREEN_NAME, "");
            set(Value.AUTH_URL, "");
            set(Value.ERROR, "");
            set(Value.OAUTH_VERIFIER, "");
            set(Value.ACCESS_TOKEN, "");
            set(Value.ACCESS_TOKEN_SECRET, "");
            twitter.setOAuthAccessToken(null);
        }
    }

    @Override
    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            switch((Child)prop) {
            case CONDITIONAL_TWEET:
                return new ConditionalTweetImpl(this, id);
            case FORMAT_SPECIFIER:
                return new FormatSpecifierImpl(this, id);
            default:
                return null;
            }
        }
    }

    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.CONDITIONAL_TWEET) {
            removeConditionalListener(item.getId());
        }
    }

    protected void removeConditionalListener(String id) {
        if (conditionalListeners.containsKey(id)) {
            scoreBoard.removeScoreBoardListener(conditionalListeners.get(id));
            conditionalListeners.remove(id);
        }
    }

    protected void tweet(String tweet) {
        String parsedTweet = formatSpecifierViewer.parse(tweet);
        if (isTestMode()) {
            System.out.println("(TEST MODE) Tweeting '" + parsedTweet + "'");
            set(Value.STATUS, "(TEST MODE) " + parsedTweet);
        } else if (isLoggedIn()) {
            System.out.println("Tweeting '" + parsedTweet + "'");
            set(Value.STATUS, "");  // Clear it so frontend can notice duplicates.
            twitter.updateStatus(parsedTweet);
        }
    }
    protected boolean isTestMode() { return (Boolean)get(Value.TEST_MODE); }
    protected boolean isLoggedIn() { return (Boolean)get(Value.LOGGED_IN); }

    private ConfigurationBuilder getConfigurationBuilder() {
        return new ConfigurationBuilder()
               .setDebugEnabled(false)
               .setUserStreamRepliesAllEnabled(false)
               /* Twitter Account for this - derbyscores */
               .setOAuthConsumerKey("qOzry57P6rp6PBnj2BfpuvzK6")
               .setOAuthConsumerSecret("Yx0bI3M5SinRYDGX1tfxQiP6a1oCVmfu5FXE4o27sNmvYeCY1W");
    }

    protected boolean initilized = false;
    protected AsyncTwitterFactory twitterFactory = new AsyncTwitterFactory(getConfigurationBuilder().build());
    protected AsyncTwitter twitter = twitterFactory.getInstance();
    protected RequestToken requestToken;
    protected FormatSpecifierViewer formatSpecifierViewer;
    protected Map<String,ScoreBoardListener> conditionalListeners = new HashMap<>();

    class Listener extends TwitterAdapter {
        @Override
        public void onException(TwitterException te, TwitterMethod method) {
            te.printStackTrace();
            synchronized(coreLock) {
                set(Value.ERROR, "Twitter Exception for " + method + ": "+ te.getMessage());
            }
        }

        @Override
        public void gotOAuthRequestToken(RequestToken token) {
            synchronized(coreLock) {
                requestToken = token;
                resetTwitter();
                set(Value.AUTH_URL, requestToken.getAuthorizationURL());
            }
        }

        @Override
        public void gotOAuthAccessToken(AccessToken token) {
            synchronized(coreLock) {
                set(Value.ACCESS_TOKEN, token.getToken());
                set(Value.ACCESS_TOKEN_SECRET, token.getTokenSecret());
                set(Value.SCREEN_NAME, token.getScreenName());
                set(Value.LOGGED_IN, true);
                set(Value.OAUTH_VERIFIER, "");
            }
        }

        @Override
        public void updatedStatus(Status status) {
            synchronized(coreLock) {
                set(Value.STATUS, status.getText());
                set(Value.ERROR, "");
            }
        }
    }

    public class ConditionalTweetImpl extends ScoreBoardEventProviderImpl implements ConditionalTweet {
        public ConditionalTweetImpl(Twitter t, String id) {
            super(t, Value.ID, id, Twitter.Child.CONDITIONAL_TWEET, ConditionalTweet.class, Value.class);
        }

        @Override
        protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
            if (prop == Value.TWEET || prop == Value.CONDITION) {
                removeConditionalListener(getId());
                if (!getTweet().isEmpty() && !getCondition().isEmpty()) {
                    // Everything is set, we can create the condition.
                    ScoreBoardListener tweetListener = new TweetScoreBoardListener(getTweet());
                    ScoreBoardListener conditionalListener;
                    try {
                        conditionalListener = new FormatSpecifierScoreBoardListener(formatSpecifierViewer, getCondition(), tweetListener);
                    } catch (IllegalArgumentException e) {
                        // Invalid condition.
                        return;
                    }

                    conditionalListeners.put(getId(), conditionalListener);
                    scoreBoard.addScoreBoardListener(conditionalListener);
                }
            }
        }

        protected String getTweet() {return (String)get(Value.TWEET); }
        protected String getCondition() {return (String)get(Value.CONDITION); }
    }

    public class FormatSpecifierImpl extends ScoreBoardEventProviderImpl implements FormatSpecifier {
        public FormatSpecifierImpl(Twitter t, String id) {
            super(t, Value.ID, id, Twitter.Child.FORMAT_SPECIFIER, FormatSpecifier.class, Value.class);
        }
    }

    protected class TweetScoreBoardListener implements ScoreBoardListener {
        public TweetScoreBoardListener(String t) {
            tweet = t;
        }
        @Override
        public void scoreBoardChange(ScoreBoardEvent e) {
            tweet(tweet);
        }
        protected String tweet;
    }
}
