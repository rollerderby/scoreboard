package com.carolinarollergirls.scoreboard.core.admin;
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

import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Twitter;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.FormatSpecifierScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.viewer.FormatSpecifierViewer;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterImpl extends ScoreBoardEventProviderImpl<Twitter> implements Twitter {
    public TwitterImpl(ScoreBoard sb) {
        super(sb, "", ScoreBoard.TWITTER);
        addProperties(AUTH_URL, CALLBACK_URL, OAUTH_VERIFIER, ACCESS_TOKEN, ACCESS_TOKEN_SECRET, MANUAL_TWEET, STATUS,
                LOGGED_IN, ERROR, SCREEN_NAME, TEST_MODE, CONDITIONAL_TWEET, FORMAT_SPECIFIER, LOGIN, LOGOUT);

        formatSpecifierViewer = new FormatSpecifierViewer(sb);
        int i = 0;
        for (Map.Entry<String, String> entry : formatSpecifierViewer.getFormatSpecifierDescriptions().entrySet()) {
            final FormatSpecifier fs = getOrCreate(FORMAT_SPECIFIER, String.valueOf(i++));
            final String key = entry.getKey();
            final FormatSpecifierViewer.ScoreBoardValue<?> value = formatSpecifierViewer
                    .getFormatSpecifierScoreBoardValue(key);
            fs.set(FormatSpecifier.KEY, key);
            fs.set(FormatSpecifier.DESCRIPTION, entry.getValue());
            fs.set(FormatSpecifier.CURRENT_VALUE,
                    formatSpecifierViewer.getFormatSpecifierScoreBoardValue(key).getValue());
            // Provide current value of each specifier to the frontend.
            scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener<>(
                    formatSpecifierViewer.getScoreBoardCondition(key), new ScoreBoardListener() {
                        @Override
                        public void scoreBoardChange(ScoreBoardEvent<?> e) {
                            fs.set(FormatSpecifier.CURRENT_VALUE, value.getValue());
                        }
                    }));
        }
        addWriteProtection(FORMAT_SPECIFIER);

        twitter.addListener(new Listener());
    }
    public TwitterImpl(TwitterImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new TwitterImpl(this, root); }

    @Override
    public void postAutosaveUpdate() {
        set(MANUAL_TWEET, "");
        set(ERROR, "");
        // If we were authenticated when shut down.
        if (isLoggedIn()) {
            twitter.setOAuthAccessToken(new AccessToken(get(ACCESS_TOKEN), get(ACCESS_TOKEN_SECRET)));
            twitter.verifyCredentials(); // This is async, and checks our credentials work.
        }
        initilized = true;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (!initilized) { return; }
        if (prop == OAUTH_VERIFIER && value != null && !((String) value).isEmpty()) {
            twitter.getOAuthAccessTokenAsync(requestToken, (String) value);
        } else if (prop == MANUAL_TWEET && value != null && !((String) value).isEmpty()) {
            tweet((String) value);
            set(MANUAL_TWEET, "");
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == LOGIN) {
            twitter.getOAuthRequestTokenAsync(get(CALLBACK_URL));
        } else if (prop == LOGOUT) {
            resetTwitter();
        }
    }

    private void resetTwitter() {
        synchronized (coreLock) {
            set(LOGGED_IN, false);
            set(SCREEN_NAME, "");
            set(AUTH_URL, "");
            set(ERROR, "");
            set(OAUTH_VERIFIER, "");
            set(ACCESS_TOKEN, "");
            set(ACCESS_TOKEN_SECRET, "");
            twitter.setOAuthAccessToken(null);
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == CONDITIONAL_TWEET) {
                return new ConditionalTweetImpl(this, id);
            } else if (prop == FORMAT_SPECIFIER) {
                return new FormatSpecifierImpl(this, id);
            } else {
                return null;
            }
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == CONDITIONAL_TWEET) {
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
            set(STATUS, "(TEST MODE) " + parsedTweet);
        } else if (isLoggedIn()) {
            System.out.println("Tweeting '" + parsedTweet + "'");
            set(STATUS, ""); // Clear it so frontend can notice duplicates.
            twitter.updateStatus(parsedTweet);
        }
    }
    protected boolean isTestMode() { return get(TEST_MODE); }
    protected boolean isLoggedIn() { return get(LOGGED_IN); }

    private ConfigurationBuilder getConfigurationBuilder() {
        return new ConfigurationBuilder().setDebugEnabled(false).setUserStreamRepliesAllEnabled(false)
                /* Twitter Account for this - derbyscores */
                .setOAuthConsumerKey("qOzry57P6rp6PBnj2BfpuvzK6")
                .setOAuthConsumerSecret("Yx0bI3M5SinRYDGX1tfxQiP6a1oCVmfu5FXE4o27sNmvYeCY1W");
    }

    protected boolean initilized = false;
    protected AsyncTwitterFactory twitterFactory = new AsyncTwitterFactory(getConfigurationBuilder().build());
    protected AsyncTwitter twitter = twitterFactory.getInstance();
    protected RequestToken requestToken;
    protected FormatSpecifierViewer formatSpecifierViewer;
    protected Map<String, ScoreBoardListener> conditionalListeners = new HashMap<>();

    class Listener extends TwitterAdapter {
        @Override
        public void onException(TwitterException te, TwitterMethod method) {
            te.printStackTrace();
            synchronized (coreLock) {
                set(ERROR, "Twitter Exception for " + method + ": " + te.getMessage());
            }
        }

        @Override
        public void gotOAuthRequestToken(RequestToken token) {
            synchronized (coreLock) {
                requestToken = token;
                resetTwitter();
                set(AUTH_URL, requestToken.getAuthorizationURL());
            }
        }

        @Override
        public void gotOAuthAccessToken(AccessToken token) {
            synchronized (coreLock) {
                set(ACCESS_TOKEN, token.getToken());
                set(ACCESS_TOKEN_SECRET, token.getTokenSecret());
                set(SCREEN_NAME, token.getScreenName());
                set(LOGGED_IN, true);
                set(OAUTH_VERIFIER, "");
            }
        }

        @Override
        public void updatedStatus(Status status) {
            synchronized (coreLock) {
                set(STATUS, status.getText());
                set(ERROR, "");
            }
        }
    }

    public class ConditionalTweetImpl extends ScoreBoardEventProviderImpl<ConditionalTweet>
            implements ConditionalTweet {
        public ConditionalTweetImpl(Twitter t, String id) {
            super(t, id, Twitter.CONDITIONAL_TWEET);
            addProperties(CONDITION, TWEET);
        }
        public ConditionalTweetImpl(ConditionalTweetImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

        @Override
        public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new ConditionalTweetImpl(this, root); }
    
        @Override
        protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
            if (prop == TWEET || prop == CONDITION) {
                removeConditionalListener(getId());
                if (!getTweet().isEmpty() && !getCondition().isEmpty()) {
                    // Everything is set, we can create the condition.
                    ScoreBoardListener tweetListener = new TweetScoreBoardListener(getTweet());
                    ScoreBoardListener conditionalListener;
                    try {
                        conditionalListener = new FormatSpecifierScoreBoardListener<>(formatSpecifierViewer,
                                getCondition(), tweetListener);
                    } catch (IllegalArgumentException e) {
                        // Invalid condition.
                        return;
                    }

                    conditionalListeners.put(getId(), conditionalListener);
                    scoreBoard.addScoreBoardListener(conditionalListener);
                }
            }
        }

        protected String getTweet() { return get(TWEET); }
        protected String getCondition() { return get(CONDITION); }
    }

    public class FormatSpecifierImpl extends ScoreBoardEventProviderImpl<FormatSpecifier> implements FormatSpecifier {
        public FormatSpecifierImpl(Twitter t, String id) {
            super(t, id, Twitter.FORMAT_SPECIFIER);
            addProperties(KEY, DESCRIPTION, CURRENT_VALUE);
        }
        public FormatSpecifierImpl(FormatSpecifierImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

        @Override
        public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new FormatSpecifierImpl(this, root); }
        }

    protected class TweetScoreBoardListener implements ScoreBoardListener {
        public TweetScoreBoardListener(String t) {
            tweet = t;
        }
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> e) {
            tweet(tweet);
        }

        protected String tweet;
    }
}
