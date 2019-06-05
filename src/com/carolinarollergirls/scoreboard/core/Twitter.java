package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Twitter extends ScoreBoardEventProvider {
    /** Update state after restoring from autosave */
    public void postAutosaveUpdate();

    public enum Value implements PermanentProperty {
        // URL browser should be sent to do OAuth.
        AUTH_URL(String.class, ""),
        // URL Twitter should send browser back to after OAuth.
        CALLBACK_URL(String.class, ""),
        // The OAuth token we get back from Twitter goes here.
        OAUTH_VERIFIER(String.class, ""),
        // The ultimate credentials we use to tweet.
        ACCESS_TOKEN(String.class, ""),
        ACCESS_TOKEN_SECRET(String.class, ""),
        // Setting this sends a tweet.
        MANUAL_TWEET(String.class, ""),
        // The last tweet we sent.
        STATUS(String.class, ""),
        LOGGED_IN(Boolean.class, false),
        ERROR(String.class, ""),
        SCREEN_NAME(String.class, ""),
        TEST_MODE(Boolean.class, false);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }

    public enum Child implements AddRemoveProperty {
        CONDITIONAL_TWEET(ConditionalTweet.class),
        FORMAT_SPECIFIER(FormatSpecifier.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum Command implements CommandProperty {
        LOGIN,
        LOGOUT;

        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }

    public interface ConditionalTweet extends ScoreBoardEventProvider {
        public enum Value implements PermanentProperty {
            ID(String.class, ""),
            CONDITION(String.class, ""),
            TWEET(String.class, "");

            private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
            private final Class<?> type;
            private final Object defaultValue;
            @Override
            public Class<?> getType() { return type; }
            @Override
            public Object getDefaultValue() { return defaultValue; }
        }
    }

    public interface FormatSpecifier extends ScoreBoardEventProvider {
        public enum Value implements PermanentProperty {
            ID(String.class, ""),
            KEY(String.class, ""),
            DESCRIPTION(String.class, ""),
            CURRENT_VALUE(String.class, "");

            private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
            private final Class<?> type;
            private final Object defaultValue;
            @Override
            public Class<?> getType() { return type; }
            @Override
            public Object getDefaultValue() { return defaultValue; }
        }
    }
}
