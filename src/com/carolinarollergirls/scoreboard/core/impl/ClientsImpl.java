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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.HumanIdGenerator;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class ClientsImpl extends ScoreBoardEventProviderImpl implements Clients {
    public ClientsImpl(ScoreBoard parent) {
        super(parent, null, "", ScoreBoard.Child.CLIENTS, Clients.class, Child.class);
    }

    @Override
    public Client addClient(String deviceId, String remoteAddr, String source, String platform) {
        synchronized (coreLock) {
            requestBatchStart();
            ClientImpl c = new ClientImpl(this, UUID.randomUUID().toString());
            Device d = (Device)get(Child.DEVICE, deviceId);
            c.set(Client.Value.DEVICE, d, Flag.INTERNAL);
            c.set(Client.Value.SOURCE, source, Flag.INTERNAL);
            c.set(Client.Value.REMOTE_ADDR, remoteAddr, Flag.INTERNAL);
            d.set(Device.Value.REMOTE_ADDR, remoteAddr, Flag.INTERNAL);
            c.set(Client.Value.PLATFORM, platform, Flag.INTERNAL);
            add(Child.CLIENT, c, Flag.INTERNAL);
            if (platform != null) {
              d.set(Device.Value.PLATFORM, platform, Flag.INTERNAL);
            }
            c.set(Client.Value.CREATED, System.currentTimeMillis(), Flag.INTERNAL);
            requestBatchEnd();
            return c;
        }
    }

    @Override
    public void removeClient(Client c) {
        synchronized (coreLock) {
            c.set(Client.Value.DEVICE, null, Flag.INTERNAL);
            remove(Child.CLIENT, c, Flag.INTERNAL);
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
    public Device getOrAddDevice(String sessionId) {
        synchronized (coreLock) {
            Device d = getDevice(sessionId);
            if (d == null) {
                requestBatchStart();
                d = new DeviceImpl(this, UUID.randomUUID().toString());
                d.set(Device.Value.SESSION_ID_SECRET, sessionId, Flag.INTERNAL);
                long now = System.currentTimeMillis();
                d.set(Device.Value.CREATED, now, Flag.INTERNAL);
                d.set(Device.Value.ACCESSED, now, Flag.INTERNAL);
                d.set(Device.Value.NAME, HumanIdGenerator.generate(), Flag.INTERNAL);
                add(Child.DEVICE, d, Flag.INTERNAL);
                requestBatchEnd();
            }
            return d;
        }
    }

    @Override
    public int gcOldDevices(long gcBefore) {
        synchronized (coreLock) {
            int removed = 0;
            requestBatchStart();
            for (ValueWithId d : getAll(Child.DEVICE)) {
                if ((Long)((Device)d).get(Device.Value.ACCESSED) < gcBefore) {
                   remove(Child.DEVICE, d.getId(), Flag.INTERNAL);
                   removed++;
                }
            }
            requestBatchEnd();
            return removed;
        }
    }

    @Override
    public boolean isWritable(Property prop, Flag flag) {
        // This file is security-related so needs more power and certainty than what
        // the existing system which is more suitable for accident prevention provides.
        return flag != null || true;
    }


    @Override
    public boolean remove(AddRemoveProperty prop, ValueWithId item, Flag flag) {
        // Do not allow removal from WS or autosave.
        if (flag != Flag.INTERNAL) return false;
        return super.remove(prop, item, flag);
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
                set(Value.WROTE, now, Flag.INTERNAL);
                ((Device)get(Value.DEVICE)).set(Device.Value.WROTE, now, Flag.INTERNAL);
            }
        }

        @Override
        public boolean isWritable(Property prop, Flag flag) {
          return (flag != Flag.FROM_AUTOSAVE && flag != null);
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
        public void access() {
            synchronized (coreLock) {
                set(Value.ACCESSED, System.currentTimeMillis(), Flag.INTERNAL);
            }
        }

        @Override
        public boolean isWritable(Property prop, Flag flag) {
            if (prop == Value.COMMENT) return true;
            return flag != null;
        }

        @Override
        protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
            if ((flag == Flag.FROM_AUTOSAVE || flag == null) && prop != Value.COMMENT) {
               // Only allow changing values from WS/load if they didn't already have one.
               if (last instanceof String && !prop.getDefaultValue().equals(last)) {
                 return last;
               }
               if (last != prop.getDefaultValue()) {
                 return last;
               }
            }
            return value;
        }
    }
}
