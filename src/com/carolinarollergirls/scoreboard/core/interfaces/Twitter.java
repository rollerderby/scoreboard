package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Twitter extends ScoreBoardEventProvider {
    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    // URL browser should be sent to do OAuth.
    Value<String> AUTH_URL = new Value<>(String.class, "AuthUrl", "");
    // URL Twitter should send browser back to after OAuth.
    Value<String> CALLBACK_URL = new Value<>(String.class, "CallbackUrl", "");
    // The OAuth token we get back from Twitter goes here.
    Value<String> OAUTH_VERIFIER = new Value<>(String.class, "OauthVerifier", "");
    // The ultimate credentials we use to tweet.
    Value<String> ACCESS_TOKEN = new Value<>(String.class, "AccessToken", "");
    Value<String> ACCESS_TOKEN_SECRET = new Value<>(String.class, "AccessTokenSecret", "");
    // Setting this sends a tweet.
    Value<String> MANUAL_TWEET = new Value<>(String.class, "ManualTweet", "");
    // The last tweet we sent.
    Value<String> STATUS = new Value<>(String.class, "Status", "");
    Value<Boolean> LOGGED_IN = new Value<>(Boolean.class, "LoggedIn", false);
    Value<String> ERROR = new Value<>(String.class, "Error", "");
    Value<String> SCREEN_NAME = new Value<>(String.class, "ScreenName", "");
    Value<Boolean> TEST_MODE = new Value<>(Boolean.class, "TestMode", false);

    Child<ConditionalTweet> CONDITIONAL_TWEET = new Child<>(ConditionalTweet.class, "ConditionalTweet");
    Child<FormatSpecifier> FORMAT_SPECIFIER = new Child<>(FormatSpecifier.class, "FormatSpecifier");

    Command LOGIN = new Command("Login");
    Command LOGOUT = new Command("Logout");

    public interface ConditionalTweet extends ScoreBoardEventProvider {
        Value<String> CONDITION = new Value<>(String.class, "Condition", "");
        Value<String> TWEET = new Value<>(String.class, "Tweet", "");
    }

    public interface FormatSpecifier extends ScoreBoardEventProvider {
        Value<String> KEY = new Value<>(String.class, "Key", "");
        Value<String> DESCRIPTION = new Value<>(String.class, "Description", "");
        Value<String> CURRENT_VALUE = new Value<>(String.class, "CurrentValue", "");
    }
}
