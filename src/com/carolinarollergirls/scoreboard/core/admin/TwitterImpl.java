package com.carolinarollergirls.scoreboard.core.admin;

import java.util.HashMap;
import java.util.Map;

import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
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
import com.carolinarollergirls.scoreboard.utils.Logger;
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
        addProperties(props);

        formatSpecifierViewer = new FormatSpecifierViewer(sb);
        int i = 0;
        for (Map.Entry<String, String> entry : formatSpecifierViewer.getFormatSpecifierDescriptions().entrySet()) {
            final FormatSpecifier fs = getOrCreate(FORMAT_SPECIFIER, String.valueOf(i++));
            final String key = entry.getKey();
            final FormatSpecifierViewer.ScoreBoardValue<?> value =
                formatSpecifierViewer.getFormatSpecifierScoreBoardValue(key);
            fs.set(FormatSpecifier.KEY, key);
            fs.set(FormatSpecifier.DESCRIPTION, entry.getValue());
            fs.set(FormatSpecifier.CURRENT_VALUE,
                   formatSpecifierViewer.getFormatSpecifierScoreBoardValue(key).getValue());
            // Provide current value of each specifier to the frontend.
            ConditionalScoreBoardListener<?> listener = new ConditionalScoreBoardListener<>(
                formatSpecifierViewer.getScoreBoardCondition(key), new ScoreBoardListener() {
                    @Override
                    public void scoreBoardChange(ScoreBoardEvent<?> e) {
                        fs.set(FormatSpecifier.CURRENT_VALUE, value.getValue());
                    }
                });
            scoreBoard.addScoreBoardListener(listener);
        }
        addWriteProtection(FORMAT_SPECIFIER);
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener<>(
            scoreBoard.getCurrentGame(), CurrentGame.GAME, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> e) {
                    for (String key : formatSpecifierViewer.getFormatSpecifierDescriptions().keySet()) {
                        formatSpecifierViewer.getFormatSpecifierScoreBoardValue(key).updateCondition();
                    }
                }
            }));

        twitter.addListener(new Listener());
    }

    @Override
    public void postAutosaveUpdate() {
        if (getAll(CONDITIONAL_TWEET).size() == 0) { addDefaultConditionalTweets(); }
        set(MANUAL_TWEET, "");
        set(ERROR, "");
        // If we were authenticated when shut down.
        if (isLoggedIn()) {
            twitter.setOAuthAccessToken(new AccessToken(get(ACCESS_TOKEN), get(ACCESS_TOKEN_SECRET)));
            twitter.verifyCredentials(); // This is async, and checks our credentials work.
        }
        initilized = true;
    }

    private void addDefaultConditionalTweets() {
        addConditionalTweet("00000000-0000-0000-0000-000000000001", "%citms=5:00 %ciN=0 %cir=true",
                            "5 minutes to Derby! %t1Nt vs %t2Nt");
        addConditionalTweet("00000000-0000-0000-0000-000000000002", "%sbip=true %sbio=false",
                            "Start Period %cpN, %t1Nt vs %t2Nt");
        addConditionalTweet("00000000-0000-0000-0000-000000000003", "%cptms=25:00",
                            "Period %cpN time %cptms, %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-000000000004", "%cptms=20:00",
                            "Period %cpN time %cptms, %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-000000000005", "%cptms=15:00",
                            "Period %cpN time %cptms, %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-000000000006", "%cptms=10:00",
                            "Period %cpN time %cptms, %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-000000000007", "%cptms=5:00 %cpN=1",
                            "Period %cpN time %cptms, %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-000000000008", "%cptms=5:00 %cpN=2",
                            "Last 5 Minutes! %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-000000000009", "%cjr=false %cpN=2 %cptms<5:00",
                            "End of jam %cjN, Clock %cptms, %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-00000000000a", "%sbip=false %cpN=1",
                            "Halftime: %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-00000000000b", "%citms=5:00 %ciN=1 %cir=true",
                            "5:00 until the Second Half Starts");
        addConditionalTweet("00000000-0000-0000-0000-00000000000c", "%cjr=false %sbos=false %sbip=false %cpN=2",
                            "Full time - UNOFFICIAL Final: %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-00000000000d", "%sbos=true %sbip=false %cpN=2",
                            "Official Final Score: %t1Nt %t1s, %t2Nt %t2s");
        addConditionalTweet("00000000-0000-0000-0000-00000000000e", "%sbio=true", "Overtime! %t1Nt %t1s, %t2Nt %t2s");
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
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
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
        if (prop == CONDITIONAL_TWEET) { removeConditionalListener(item.getId()); }
    }

    protected void removeConditionalListener(String id) {
        if (conditionalListeners.containsKey(id)) {
            scoreBoard.removeScoreBoardListener(conditionalListeners.get(id));
            conditionalListeners.remove(id);
        }
    }

    protected void addConditionalTweet(String id, String condition, String tweet) {
        ConditionalTweet ct = new ConditionalTweetImpl(this, id);
        add(CONDITIONAL_TWEET, ct);
        ct.set(ConditionalTweet.CONDITION, condition);
        ct.set(ConditionalTweet.TWEET, tweet);
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
    protected Map<String, ScoreBoardListener> conditionalListeners = new HashMap<>();

    class Listener extends TwitterAdapter {
        @Override
        public void onException(TwitterException te, TwitterMethod method) {
            Logger.printStackTrace(te);
            synchronized (coreLock) { set(ERROR, "Twitter Exception for " + method + ": " + te.getMessage()); }
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

    public class ConditionalTweetImpl
        extends ScoreBoardEventProviderImpl<ConditionalTweet> implements ConditionalTweet {
        public ConditionalTweetImpl(Twitter t, String id) {
            super(t, id, Twitter.CONDITIONAL_TWEET);
            addProperties(props);
        }

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
            addProperties(props);
            addWriteProtectionOverride(KEY, Source.ANY_INTERNAL);
            addWriteProtectionOverride(DESCRIPTION, Source.ANY_INTERNAL);
            addWriteProtectionOverride(CURRENT_VALUE, Source.ANY_INTERNAL);
        }
    }

    protected class TweetScoreBoardListener implements ScoreBoardListener {
        public TweetScoreBoardListener(String t) { tweet = t; }
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> e) {
            tweet(tweet);
        }

        protected String tweet;
    }
}
