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
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.HumanIdGenerator;
import com.carolinarollergirls.scoreboard.utils.Logger;

public class ClientsImpl extends ScoreBoardEventProviderImpl<Clients> implements Clients {
    public ClientsImpl(ScoreBoard parent) {
        super(parent, "", ScoreBoard.CLIENTS);
        addProperties(NEW_DEVICE_WRITE, CLIENT, DEVICE);
        addWriteProtectionOverride(CLIENT, Source.ANY_INTERNAL);
        addWriteProtectionOverride(DEVICE, Source.ANY_INTERNAL);
    }

    @Override
    public void postAutosaveUpdate() {
        if (get(NEW_DEVICE_WRITE)) { return; }
        Boolean hasWritableClient = false;
        for (Device d : getAll(DEVICE)) {
            if (d.mayWrite()) { hasWritableClient = true; }
        }
        if (!hasWritableClient) {
            Logger.printMessage("No device with write access remaining -- enabling write access for new devices");
            set(NEW_DEVICE_WRITE, true);
        }
    }

    @Override
    public Client addClient(String deviceId, String remoteAddr, String source, String platform) {
        synchronized (coreLock) {
            requestBatchStart();
            ClientImpl c = new ClientImpl(this, UUID.randomUUID().toString());
            Device d = get(DEVICE, deviceId);
            c.set(Client.DEVICE, d);
            c.set(Client.SOURCE, source);
            c.set(Client.REMOTE_ADDR, remoteAddr);
            d.set(Device.REMOTE_ADDR, remoteAddr);
            c.set(Client.PLATFORM, platform);
            add(CLIENT, c);
            if (platform != null) {
                d.set(Device.PLATFORM, platform);
            }
            c.set(Client.CREATED, System.currentTimeMillis());
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
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == DEVICE) {
                Device d = new DeviceImpl(this, id);
                if (source.isFile()) {
                    // work around write protection
                    add(DEVICE, d);
                }
                return d;
            }
            return null;
        }
    }

    @Override
    public Device getDevice(String sessionId) {
        synchronized (coreLock) {
            for (Device d : getAll(DEVICE)) {
                if (d.get(Device.SESSION_ID_SECRET).equals(sessionId)) {
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
                d.set(Device.SESSION_ID_SECRET, sessionId);
                long now = System.currentTimeMillis();
                d.set(Device.CREATED, now);
                d.set(Device.ACCESSED, now);
                // Try to find an unused name, fallback to a UUID.
                String name = UUID.randomUUID().toString();
                for (int i = 0; i < 10; i++) {
                    String n = HumanIdGenerator.generate();
                    if (getDeviceByName(n) == null) {
                        name = n;
                        break;
                    }
                }
                d.set(Device.NAME, name);
                add(DEVICE, d);
                requestBatchEnd();
            }
            return d;
        }
    }

    protected Device getDeviceByName(String name) {
        synchronized (coreLock) {
            for (Device d : getAll(DEVICE)) {
                if (d.get(Device.NAME).equals(name)) {
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
            for (Device d : getAll(DEVICE)) {
                if (d.get(Device.ACCESSED) > gcBefore) { continue; }
                if (!d.get(Device.COMMENT).isEmpty()) { continue; }
                remove(DEVICE, d.getId());
                removed++;
            }
            requestBatchEnd();
            return removed;
        }
    }

    public class ClientImpl extends ScoreBoardEventProviderImpl<Client> implements Client {
        ClientImpl(Clients parent, String id) {
            super(parent, id, Clients.CLIENT);
            addProperties(DEVICE, REMOTE_ADDR, PLATFORM, SOURCE, CREATED, WROTE);
            setInverseReference(DEVICE, Device.CLIENT);
            addWriteProtectionOverride(DEVICE, Source.ANY_INTERNAL);
            addWriteProtectionOverride(REMOTE_ADDR, Source.ANY_INTERNAL);
            addWriteProtectionOverride(PLATFORM, Source.ANY_INTERNAL);
            addWriteProtectionOverride(SOURCE, Source.ANY_INTERNAL);
            addWriteProtectionOverride(CREATED, Source.ANY_INTERNAL);
            addWriteProtectionOverride(WROTE, Source.ANY_INTERNAL);
        }

        @Override
        public void write() {
            synchronized (coreLock) {
                long now = System.currentTimeMillis();
                set(WROTE, now);
                get(DEVICE).set(Device.WROTE, now);
            }
        }
    }

    public class DeviceImpl extends ScoreBoardEventProviderImpl<Device> implements Device {
        protected DeviceImpl(Clients parent, String id) {
            super(parent, id, Clients.DEVICE);
            addProperties(SESSION_ID_SECRET, NAME, REMOTE_ADDR, PLATFORM, COMMENT, CREATED, WROTE, ACCESSED, MAY_WRITE,
                    CLIENT);
            set(MAY_WRITE, parent.get(NEW_DEVICE_WRITE));
            addWriteProtectionOverride(CLIENT, Source.ANY_INTERNAL);
        }

        @Override
        public String getName() {
            synchronized (coreLock) {
                return get(NAME);
            }
        }

        @Override
        public Boolean mayWrite() {
            synchronized (coreLock) {
                return get(MAY_WRITE);
            }
        }

        @Override
        public void access() {
            synchronized (coreLock) {
                set(ACCESSED, System.currentTimeMillis());
            }
        }

        @Override
        public void write() {
            synchronized (coreLock) {
                long now = System.currentTimeMillis();
                set(WROTE, now);
            }
        }

        @Override
        protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
            if (!source.isInternal() && prop != COMMENT && prop != MAY_WRITE) {
                // Only allow changing values from WS/load if they didn't already have one.
                if (!Objects.equals(last, prop.getDefaultValue())) {
                    return last;
                }
            }
            return value;
        }
    }
}
