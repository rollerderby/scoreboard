package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Twitter extends ScoreBoardEventProvider {
    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    public static Collection<Property<?>> props = new ArrayList<>();

    // URL browser should be sent to do OAuth.
    public static final Value<String> AUTH_URL = new Value<>(String.class, "AuthUrl", "", props);
    // URL Twitter should send browser back to after OAuth.
    public static final Value<String> CALLBACK_URL = new Value<>(String.class, "CallbackUrl", "", props);
    // The OAuth token we get back from Twitter goes here.
    public static final Value<String> OAUTH_VERIFIER = new Value<>(String.class, "OauthVerifier", "", props);
    // The ultimate credentials we use to tweet.
    public static final Value<String> ACCESS_TOKEN = new Value<>(String.class, "AccessToken", "", props);
    public static final Value<String> ACCESS_TOKEN_SECRET = new Value<>(String.class, "AccessTokenSecret", "", props);
    // Setting this sends a tweet.
    public static final Value<String> MANUAL_TWEET = new Value<>(String.class, "ManualTweet", "", props);
    // The last tweet we sent.
    public static final Value<String> STATUS = new Value<>(String.class, "Status", "", props);
    public static final Value<Boolean> LOGGED_IN = new Value<>(Boolean.class, "LoggedIn", false, props);
    public static final Value<String> ERROR = new Value<>(String.class, "Error", "", props);
    public static final Value<String> SCREEN_NAME = new Value<>(String.class, "ScreenName", "", props);
    public static final Value<Boolean> TEST_MODE = new Value<>(Boolean.class, "TestMode", false, props);

    public static final Child<ConditionalTweet> CONDITIONAL_TWEET =
        new Child<>(ConditionalTweet.class, "ConditionalTweet", props);
    public static final Child<FormatSpecifier> FORMAT_SPECIFIER =
        new Child<>(FormatSpecifier.class, "FormatSpecifier", props);

    public static final Command LOGIN = new Command("Login", props);
    public static final Command LOGOUT = new Command("Logout", props);

    public interface ConditionalTweet extends ScoreBoardEventProvider {

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> CONDITION = new Value<>(String.class, "Condition", "", props);
        public static final Value<String> TWEET = new Value<>(String.class, "Tweet", "", props);
    }

    public interface FormatSpecifier extends ScoreBoardEventProvider {

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> KEY = new Value<>(String.class, "Key", "", props);
        public static final Value<String> DESCRIPTION = new Value<>(String.class, "Description", "", props);
        public static final Value<String> CURRENT_VALUE = new Value<>(String.class, "CurrentValue", "", props);
    }
}
