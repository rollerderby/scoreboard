package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Clients extends ScoreBoardEventProvider {
    public enum Child implements AddRemoveProperty {
        CLIENT(Client.class),
        DEVICE(Device.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public void postAutosaveUpdate();

    public Device getDevice(String sessionId);
    public Device getOrAddDevice(String sessionId);

    public Client addClient(String deviceId, String remoteAddr, String source);

    // An active websocket client.
    public static interface Client extends ScoreBoardEventProvider {
        public enum Value implements PermanentProperty {
            ID(String.class, ""),            
            DEVICE_ID(String.class, ""),
            REMOTE_ADDR(String.class, ""),
            SOURCE(String.class, "");

            private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
            private final Class<?> type;
            private final Object defaultValue;
            @Override
            public Class<?> getType() { return type; }
            @Override
            public Object getDefaultValue() { return defaultValue; }
        }
    }
 
    // A device is a HTTP cookie.
    public static interface Device extends ScoreBoardEventProvider {
        public String getName();
 
        public enum Value implements PermanentProperty {
            ID(String.class, ""),
            SESSION_ID_SECRET(String.class, ""),   // The cookie.
            NAME(String.class, "");                // A human-readable name.

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
