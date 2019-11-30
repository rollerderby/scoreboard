package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Clients;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.HumanIdGenerator;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class ClientsImpl extends ScoreBoardEventProviderImpl implements Clients {
    public ClientsImpl(ScoreBoard parent) {
        super(parent, null, "", ScoreBoard.Child.CLIENTS, Clients.class, Child.class);
    }

    public void postAutosaveUpdate() {
        // WS connections do not persist across processes, so
        // anything from the auto-save is stale.
        removeAll(Child.CLIENT);
    }

    @Override
    public Client addClient(String deviceId, String remoteAddr, String source, String platform) {
        synchronized (coreLock) {
            ClientImpl c = new ClientImpl(this, UUID.randomUUID().toString());
            Device d = (Device)get(Child.DEVICE, deviceId);
            c.set(Client.Value.DEVICE, d);
            c.set(Client.Value.SOURCE, source);
            c.set(Client.Value.REMOTE_ADDR, remoteAddr);
            d.set(Device.Value.REMOTE_ADDR, remoteAddr);
            c.set(Client.Value.PLATFORM, platform);
            add(Child.CLIENT, c);
            if (platform != null) {
              d.set(Device.Value.PLATFORM, platform);
            }
            c.set(Client.Value.CREATED, System.currentTimeMillis());
            return c;
        }
    }

    @Override
    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            if (prop == Child.DEVICE) {
              return new DeviceImpl(this, id);
            } else if (prop == Child.CLIENT) {
              return new ClientImpl(this, id);
            }
            return null;
        }
    }

    @Override
    public Device getDevice(String sessionId) {
        synchronized (coreLock) {
            for (ValueWithId d : getAll(Child.DEVICE)) {
                if (((Device)d).get(Device.Value.SESSION_ID_SECRET).equals(sessionId)) {
                   return (Device)d;
                }
            }
            return null;
        }
    }

    @Override
    public Device getOrAddDevice(String sessionId, Object session) {
        synchronized (coreLock) {
            Device d = getDevice(sessionId);
            if (d == null) {
                d = new DeviceImpl(this, UUID.randomUUID().toString());
                // TODO: Make all of this (except Comment) write protected
                // from the WS, while keeping auto-saves working.
                d.set(Device.Value.SESSION_ID_SECRET, sessionId);
                d.access();
                d.set(Device.Value.CREATED, d.get(Device.Value.ACCESSED));
                d.set(Device.Value.NAME, HumanIdGenerator.generate());
                d.getOrAddSession(session);
                add(Child.DEVICE, d);
            }
            return d;
        }
    }

    @Override
    public int gcOldDevices(long gcBefore) {
        synchronized (coreLock) {
            int removed = 0;
            for (ValueWithId d : getAll(Child.DEVICE)) {
                if ((Long)((Device)d).get(Device.Value.ACCESSED) < gcBefore) {
                   remove(Child.DEVICE, d.getId());
                   removed++;
                }
            }
            return removed;
        }
    }

    public class ClientImpl extends ScoreBoardEventProviderImpl implements Client {
        ClientImpl(Clients parent, String id) {
            super(parent, Value.ID, id, Clients.Child.CLIENT, Client.class, Value.class);
            setInverseReference(Value.DEVICE, Device.Child.CLIENT);
        }

        @Override
        public void write() {
            synchronized (coreLock) {
                long now = System.currentTimeMillis();
                set(Value.WROTE, now);
                ((Device)get(Value.DEVICE)).set(Device.Value.WROTE, now);
            }
        }

    }

    public class DeviceImpl extends ScoreBoardEventProviderImpl implements Device {
        protected DeviceImpl(Clients parent, String id) {
            super(parent, Value.ID, id, Clients.Child.DEVICE, Device.class, Value.class, Child.class);
        }

        @Override
        public String getName() {
            synchronized (coreLock) {
                return (String)get(Value.NAME);
            }
        }

        @Override
        public long getCreated() {
            synchronized (coreLock) {
                return (Long)get(Value.CREATED);
            }
        }

        @Override
        public void access() {
            synchronized (coreLock) {
                set(Value.ACCESSED, System.currentTimeMillis());
            }
        }

        @Override
        public Object getOrAddSession(Object session) {
            synchronized (coreLock) {
                 if (this.session == null) {
                   this.session = session;
                 }
                return this.session;
            }
        }

        Object session;
    }
}
