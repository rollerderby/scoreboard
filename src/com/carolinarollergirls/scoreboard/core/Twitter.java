package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Twitter extends ScoreBoardEventProvider {
    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    // URL browser should be sent to do OAuth.
    PermanentProperty<String> AUTH_URL = new PermanentProperty<>(String.class, "AuthUrl", "");
    // URL Twitter should send browser back to after OAuth.
    PermanentProperty<String> CALLBACK_URL = new PermanentProperty<>(String.class, "CallbackUrl", "");
    // The OAuth token we get back from Twitter goes here.
    PermanentProperty<String> OAUTH_VERIFIER = new PermanentProperty<>(String.class, "OauthVerifier", "");
    // The ultimate credentials we use to tweet.
    PermanentProperty<String> ACCESS_TOKEN = new PermanentProperty<>(String.class, "AccessToken", "");
    PermanentProperty<String> ACCESS_TOKEN_SECRET = new PermanentProperty<>(String.class, "AccessTokenSecret", "");
    // Setting this sends a tweet.
    PermanentProperty<String> MANUAL_TWEET = new PermanentProperty<>(String.class, "ManualTweet", "");
    // The last tweet we sent.
    PermanentProperty<String> STATUS = new PermanentProperty<>(String.class, "Status", "");
    PermanentProperty<Boolean> LOGGED_IN = new PermanentProperty<>(Boolean.class, "LoggedIn", false);
    PermanentProperty<String> ERROR = new PermanentProperty<>(String.class, "Error", "");
    PermanentProperty<String> SCREEN_NAME = new PermanentProperty<>(String.class, "ScreenName", "");
    PermanentProperty<Boolean> TEST_MODE = new PermanentProperty<>(Boolean.class, "TestMode", false);

    // @formatter:off
    AddRemoveProperty<ConditionalTweet> CONDITIONAL_TWEET = new AddRemoveProperty<>(ConditionalTweet.class, "ConditionalTweet");
    AddRemoveProperty<FormatSpecifier> FORMAT_SPECIFIER = new AddRemoveProperty<>(FormatSpecifier.class, "FormatSpecifier");
    // @formatter:on

    CommandProperty LOGIN = new CommandProperty("Login");
    CommandProperty LOGOUT = new CommandProperty("Logout");

    public interface ConditionalTweet extends ScoreBoardEventProvider {
        PermanentProperty<String> CONDITION = new PermanentProperty<>(String.class, "Condition", "");
        PermanentProperty<String> TWEET = new PermanentProperty<>(String.class, "Tweet", "");
    }

    public interface FormatSpecifier extends ScoreBoardEventProvider {
        PermanentProperty<String> KEY = new PermanentProperty<>(String.class, "Key", "");
        PermanentProperty<String> DESCRIPTION = new PermanentProperty<>(String.class, "Description", "");
        PermanentProperty<String> CURRENT_VALUE = new PermanentProperty<>(String.class, "CurrentValue", "");
    }
}
