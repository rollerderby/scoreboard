package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Objects;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Clients;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.utils.HumanIdGenerator;

public class ClientsImpl extends ScoreBoardEventProviderImpl implements Clients {
    public ClientsImpl(ScoreBoard parent) {
        super(parent, "", ScoreBoard.Child.CLIENTS, Clients.class, Child.class);
        addWriteProtectionOverride(Child.CLIENT, Source.ANY_INTERNAL);
        addWriteProtectionOverride(Child.DEVICE, Source.ANY_INTERNAL);
    }

    @Override
    public Client addClient(String deviceId, String remoteAddr, String source, String platform) {
        synchronized (coreLock) {
            requestBatchStart();
            ClientImpl c = new ClientImpl(this, UUID.randomUUID().toString());
            Device d = get(Child.DEVICE, Device.class, deviceId);
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
            requestBatchEnd();
            return c;
        }
    }

    @Override
    public void removeClient(Client c) {
        synchronized (coreLock) {
            c.delete(Source.UNLINK);
        }
    }

    @Override
    public ScoreBoardEventProvider create(AddRemoveProperty prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == Child.DEVICE) {
                Device d = new DeviceImpl(this, id);
                if (source.isFile()) {
                    // work around write protection
                    add(Child.DEVICE, d);
                }
                return d;
            }
            return null;
        }
    }

    @Override
    public Device getDevice(String sessionId) {
        synchronized (coreLock) {
            for (Device d : getAll(Child.DEVICE, Device.class)) {
                if (d.get(Device.Value.SESSION_ID_SECRET).equals(sessionId)) {
                    return d;
                }
            }
            return null;
        }
    }

    @Override
    public Device getOrAddDevice(String sessionId) {
        synchronized (coreLock) {
            Device d = getDevice(sessionId);
            if (d == null) {
                requestBatchStart();
                d = new DeviceImpl(this, UUID.randomUUID().toString());
                d.set(Device.Value.SESSION_ID_SECRET, sessionId);
                long now = System.currentTimeMillis();
                d.set(Device.Value.CREATED, now);
                d.set(Device.Value.ACCESSED, now);
                // Try to find an unused name, fallback to a UUID.
                String name = UUID.randomUUID().toString();
                for (int i = 0; i < 10; i++) {
                    String n = HumanIdGenerator.generate();
                    if (getDeviceByName(n) == null) {
                        name = n;
                        break;
                    }
                }
                d.set(Device.Value.NAME, name);
                add(Child.DEVICE, d);
                requestBatchEnd();
            }
            return d;
        }
    }

    protected Device getDeviceByName(String name) {
        synchronized (coreLock) {
            for (Device d : getAll(Child.DEVICE, Device.class)) {
                if (d.get(Device.Value.NAME).equals(name)) {
                    return d;
                }
            }
            return null;
        }
    }

    @Override
    public int gcOldDevices(long gcBefore) {
        synchronized (coreLock) {
            int removed = 0;
            requestBatchStart();
            for (Device d : getAll(Child.DEVICE, Device.class)) {
                if ((Long) d.get(Device.Value.ACCESSED) > gcBefore) { continue; }
                if (!((String) d.get(Device.Value.COMMENT)).isEmpty()) { continue; }
                remove(Child.DEVICE, d.getId());
                removed++;
            }
            requestBatchEnd();
            return removed;
        }
    }

    public class ClientImpl extends ScoreBoardEventProviderImpl implements Client {
        ClientImpl(Clients parent, String id) {
            super(parent, id, Clients.Child.CLIENT, Client.class, Value.class);
            setInverseReference(Value.DEVICE, Device.Child.CLIENT);
            addWriteProtectionOverride(Value.DEVICE, Source.ANY_INTERNAL);
            addWriteProtectionOverride(Value.REMOTE_ADDR, Source.ANY_INTERNAL);
            addWriteProtectionOverride(Value.PLATFORM, Source.ANY_INTERNAL);
            addWriteProtectionOverride(Value.SOURCE, Source.ANY_INTERNAL);
            addWriteProtectionOverride(Value.CREATED, Source.ANY_INTERNAL);
            addWriteProtectionOverride(Value.WROTE, Source.ANY_INTERNAL);
        }

        @Override
        public void write() {
            synchronized (coreLock) {
                long now = System.currentTimeMillis();
                set(Value.WROTE, now);
                ((Device) get(Value.DEVICE)).set(Device.Value.WROTE, now);
            }
        }
    }

    public class DeviceImpl extends ScoreBoardEventProviderImpl implements Device {
        protected DeviceImpl(Clients parent, String id) {
            super(parent, id, Clients.Child.DEVICE, Device.class, Value.class, Child.class);
            addWriteProtectionOverride(Child.CLIENT, Source.ANY_INTERNAL);
        }

        @Override
        public String getName() {
            synchronized (coreLock) {
                return (String) get(Value.NAME);
            }
        }

        @Override
        public void access() {
            synchronized (coreLock) {
                set(Value.ACCESSED, System.currentTimeMillis());
            }
        }

        @Override
        public void write() {
            synchronized (coreLock) {
                long now = System.currentTimeMillis();
                set(Value.WROTE, now);
            }
        }

        @Override
        protected Object computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
            if (!source.isInternal() && prop != Value.COMMENT) {
                // Only allow changing values from WS/load if they didn't already have one.
                if (!Objects.equals(last, prop.getDefaultValue())) {
                    return last;
                }
            }
            return value;
        }
    }
}
