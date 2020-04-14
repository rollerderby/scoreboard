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
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Clients extends ScoreBoardEventProvider {
    AddRemoveProperty<Client> CLIENT = new AddRemoveProperty<>(Client.class, "Client");
    AddRemoveProperty<Device> DEVICE = new AddRemoveProperty<>(Device.class, "Device");

    public Device getDevice(String sessionId);
    public Device getOrAddDevice(String sessionId);
    public int gcOldDevices(long threshold);

    public Client addClient(String deviceId, String remoteAddr, String source, String platform);
    public void removeClient(Client c);

    // An active websocket client.
    public static interface Client extends ScoreBoardEventProvider {
        public void write();

        @SuppressWarnings("hiding")
        PermanentProperty<Device> DEVICE = new PermanentProperty<>(Device.class, "Device", null);
        PermanentProperty<String> REMOTE_ADDR = new PermanentProperty<>(String.class, "RemoteAddr", "");
        PermanentProperty<String> PLATFORM = new PermanentProperty<>(String.class, "Platform", "");
        PermanentProperty<String> SOURCE = new PermanentProperty<>(String.class, "Source", "");
        PermanentProperty<Long> CREATED = new PermanentProperty<>(Long.class, "Created", 0L);
        PermanentProperty<Long> WROTE = new PermanentProperty<>(Long.class, "Wrote", 0L);
    }

    // A device is a HTTP cookie.
    public static interface Device extends ScoreBoardEventProvider {
        public String getName();

        public void access();
        public void write();

        // @formatter:off
        PermanentProperty<String> SESSION_ID_SECRET = new PermanentProperty<>(String.class, "SessionIdSecret", ""); // The cookie.
        PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", ""); // A human-readable name.
        PermanentProperty<String> REMOTE_ADDR = new PermanentProperty<>(String.class, "RemoteAddr", "");
        PermanentProperty<String> PLATFORM = new PermanentProperty<>(String.class, "Platform", "");
        PermanentProperty<String> COMMENT = new PermanentProperty<>(String.class, "Comment", "");
        PermanentProperty<Long> CREATED = new PermanentProperty<>(Long.class, "Created", 0L);
        PermanentProperty<Long> WROTE = new PermanentProperty<>(Long.class, "Wrote", 0L);
        PermanentProperty<Long> ACCESSED = new PermanentProperty<>(Long.class, "Accessed", 0L);

        @SuppressWarnings("hiding")
        AddRemoveProperty<Client> CLIENT = new AddRemoveProperty<>(Client.class, "Client");
        // @formatter:on
    }
}
