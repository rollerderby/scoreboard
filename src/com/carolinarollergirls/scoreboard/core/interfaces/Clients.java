package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Clients extends ScoreBoardEventProvider {

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Boolean> NEW_DEVICE_WRITE = new Value<>(Boolean.class, "NewDeviceWrite", true, props);
    public static final Value<Boolean> ALL_LOCAL_DEVICES_WRITE =
        new Value<>(Boolean.class, "AllLocalDevicesWrite", true, props);

    public static final Child<Device> DEVICE = new Child<>(Device.class, "Device", props);

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
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "", props);
        public static final Value<String> PLATFORM = new Value<>(String.class, "Platform", "", props);
        public static final Value<String> SOURCE = new Value<>(String.class, "Source", "", props);
        public static final Value<Long> CREATED = new Value<>(Long.class, "Created", 0L, props);
        public static final Value<Long> WROTE = new Value<>(Long.class, "Wrote", 0L, props);
    }

    // A device is a HTTP cookie.
    public static interface Device extends ScoreBoardEventProvider {
        public String getName();

        public Boolean mayWrite();
        public Boolean isLocal();

        public void access();
        public void write();

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> SESSION_ID_SECRET =
            new Value<>(String.class, "SessionIdSecret", "", props);                           // The cookie.
        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props); // A human-readable name.
        public static final Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "", props);
        public static final Value<String> PLATFORM = new Value<>(String.class, "Platform", "", props);
        public static final Value<String> COMMENT = new Value<>(String.class, "Comment", "", props);
        public static final Value<Long> CREATED = new Value<>(Long.class, "Created", 0L, props);
        public static final Value<Long> WROTE = new Value<>(Long.class, "Wrote", 0L, props);
        public static final Value<Long> ACCESSED = new Value<>(Long.class, "Accessed", 0L, props);
        public static final Value<Boolean> MAY_WRITE = new Value<>(Boolean.class, "MayWrite", false, props);
        public static final Value<Integer> NUM_CLIENTS = new Value<>(Integer.class, "NumClients", 0, props);

        public static final Child<Client> CLIENT = new Child<>(Client.class, "Client", props);
    }
}
