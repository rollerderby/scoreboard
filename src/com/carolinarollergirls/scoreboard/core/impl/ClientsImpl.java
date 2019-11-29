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
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class ClientsImpl extends ScoreBoardEventProviderImpl implements Clients {
    public ClientsImpl(ScoreBoard parent) {
        super(parent, null, "", ScoreBoard.Child.CLIENTS, Clients.class, Child.class);
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
    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            if (prop == Child.DEVICE) {
              return new DeviceImpl(this, id);
            }
            return null;
        }
    }

    @Override
    public Device getOrAddDevice(String sessionId) {
        synchronized (coreLock) {
            Device d = getDevice(sessionId);
            if (d == null) {
                d = new DeviceImpl(this, UUID.randomUUID().toString(), sessionId);
                add(Child.DEVICE, d);
            }
            return d;
        }
    }

    public class ClientImpl extends ScoreBoardEventProviderImpl implements Client {
        ClientImpl(Clients parent, String id, String deviceId, String remoteAddr) {
            super(parent, Value.ID, id, Clients.Child.CLIENT, Client.class, Value.class);
            set(Value.DEVICE_ID, deviceId);
            addWriteProtection(Value.DEVICE_ID);
            set(Value.REMOTE_ADDR, remoteAddr);
            addWriteProtection(Value.REMOTE_ADDR);
        }
    }

    public class DeviceImpl extends ScoreBoardEventProviderImpl implements Device {
        DeviceImpl(Clients parent, String id, String sessionId) {
            super(parent, Value.ID, id, Clients.Child.DEVICE, Device.class, Value.class, Child.class);
            // TODO: Make all of this write protected from the WS, while keeping
            // auto-saves working.
            set(Value.SESSION_ID_SECRET, sessionId);
        }
        protected DeviceImpl(Clients parent, String id) {
            super(parent, Value.ID, id, Clients.Child.DEVICE, Device.class, Value.class, Child.class);
        }
        @Override
        public String getAttribute(String name) {
            synchronized (coreLock) {
                ValueWithId v = get(Child.ATTRIBUTES, name);
                if (v == null) {
                   return null;
                }
                return v.getValue();
            }
        }
        public void setAttribute(String name, String value) {
            synchronized (coreLock) {
                add(Child.ATTRIBUTES, new ValWithId(name, value));
            }
        }
    }
}
