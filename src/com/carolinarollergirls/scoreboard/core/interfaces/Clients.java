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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Clients extends ScoreBoardEventProvider {
    Value<Boolean> NEW_DEVICE_WRITE = new Value<>(Boolean.class, "NewDeviceWrite", true);
    Value<Boolean> ALL_LOCAL_DEVICES_WRITE = new Value<>(Boolean.class, "AllLocalDevicesWrite", true);

    Child<Client> CLIENT = new Child<>(Client.class, "Client");
    Child<Device> DEVICE = new Child<>(Device.class, "Device");

    public void postAutosaveUpdate();

    public Device getDevice(String sessionId);
    public Device getOrAddDevice(String sessionId);
    public int gcOldDevices(long threshold);

    public Client addClient(String deviceId, String remoteAddr, String source, String platform);
    public void removeClient(Client c);

    // An active websocket client.
    public static interface Client extends ScoreBoardEventProvider {
        public void write();

        @SuppressWarnings("hiding")
        Value<Device> DEVICE = new Value<>(Device.class, "Device", null);
        Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "");
        Value<String> PLATFORM = new Value<>(String.class, "Platform", "");
        Value<String> SOURCE = new Value<>(String.class, "Source", "");
        Value<Long> CREATED = new Value<>(Long.class, "Created", 0L);
        Value<Long> WROTE = new Value<>(Long.class, "Wrote", 0L);
    }

    // A device is a HTTP cookie.
    public static interface Device extends ScoreBoardEventProvider {
        public String getName();

        public Boolean mayWrite();
        public Boolean isLocal();

        public void access();
        public void write();

        Value<String> SESSION_ID_SECRET = new Value<>(String.class, "SessionIdSecret", ""); // The cookie.
        Value<String> NAME = new Value<>(String.class, "Name", ""); // A human-readable name.
        Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "");
        Value<String> PLATFORM = new Value<>(String.class, "Platform", "");
        Value<String> COMMENT = new Value<>(String.class, "Comment", "");
        Value<Long> CREATED = new Value<>(Long.class, "Created", 0L);
        Value<Long> WROTE = new Value<>(Long.class, "Wrote", 0L);
        Value<Long> ACCESSED = new Value<>(Long.class, "Accessed", 0L);
        Value<Boolean> MAY_WRITE = new Value<>(Boolean.class, "MayWrite", false);

        @SuppressWarnings("hiding")
        Child<Client> CLIENT = new Child<>(Client.class, "Client");
    }
}
