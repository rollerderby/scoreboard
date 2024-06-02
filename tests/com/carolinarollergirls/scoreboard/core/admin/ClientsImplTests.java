package com.carolinarollergirls.scoreboard.core.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Client;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Device;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter.JSONSet;

public class ClientsImplTests {

    private ScoreBoard sb;

    private ClientsImpl clients;

    @Before
    public void setUp() throws Exception {
        sb = new ScoreBoardImpl();
        clients = (ClientsImpl) sb.getClients();
    }

    @Test
    public void testLifecycle() {
        Device d = clients.getDevice("S1");
        assertNull(d);

        d = clients.getOrAddDevice("S1");
        assertNotNull(d);
        assertEquals(d, clients.getDevice("S1"));
        assertNotEquals("", d.getName());
        assertNotEquals("", d.get(Device.SESSION_ID_SECRET));
        assertEquals(0L, (long) d.get(Device.WROTE));
        assertNotEquals(0, (long) d.get(Device.CREATED));
        assertEquals(d.get(Device.ACCESSED), d.get(Device.CREATED));

        Client c = clients.addClient(d.getId(), "remoteaddr", "source", "platform");
        assertEquals(d, c.getParent());
        assertNotEquals(0, (long) c.get(Client.CREATED));
        assertEquals(0L, (long) c.get(Client.WROTE));
        assertEquals("remoteaddr", c.get(Client.REMOTE_ADDR));
        assertEquals("platform", c.get(Client.PLATFORM));
        assertEquals("source", c.get(Client.SOURCE));
        assertEquals(c, d.get(Device.CLIENT, c.getId()));
        assertEquals(c.get(Client.REMOTE_ADDR), d.get(Device.REMOTE_ADDR));
        assertEquals(c.get(Client.PLATFORM), d.get(Device.PLATFORM));
        // Session id must not leak anywhere.
        assertNotEquals(d.getId(), d.get(Device.SESSION_ID_SECRET));
        assertNotEquals(d.getName(), d.get(Device.SESSION_ID_SECRET));
        assertNotEquals(d.get(Device.COMMENT), d.get(Device.SESSION_ID_SECRET));
        assertNotEquals(c.getId(), d.get(Device.SESSION_ID_SECRET));
        assertNotEquals(c.get(Client.REMOTE_ADDR), d.get(Device.SESSION_ID_SECRET));
        assertNotEquals(c.get(Client.PLATFORM), d.get(Device.SESSION_ID_SECRET));

        d.set(Device.ACCESSED, 0L);
        assertEquals(0L, (long) d.get(Device.ACCESSED));
        d.access();
        assertNotEquals(0, (long) d.get(Device.ACCESSED));

        c.write();
        assertNotEquals(0, (long) c.get(Client.WROTE));
        assertNotEquals(0, (long) d.get(Device.WROTE));

        // 2nd client
        Client c2 = clients.addClient(d.getId(), "remoteaddr2", "source2", null);
        assertEquals(d, c2.getParent());
        assertEquals(c2, d.get(Device.CLIENT, c2.getId()));
        assertEquals(2, d.numberOf(Device.CLIENT));
        assertNotEquals(0, (long) c2.get(Client.CREATED));
        assertEquals(0L, (long) c2.get(Client.WROTE));
        assertEquals("remoteaddr2", c2.get(Client.REMOTE_ADDR));
        assertEquals(null, c2.get(Client.PLATFORM));
        assertEquals("source2", c2.get(Client.SOURCE));
        assertEquals(c2, d.get(Device.CLIENT, c2.getId()));
        // Latest value wines, but do not replace with a null.
        assertEquals("remoteaddr2", d.get(Device.REMOTE_ADDR));
        assertEquals("platform", d.get(Device.PLATFORM));

        // Remove, device is left but clients are gone.
        clients.removeClient(c);
        assertEquals(null, d.get(Device.CLIENT, c.getId()));
        assertEquals(1, d.numberOf(Device.CLIENT));
        clients.removeClient(c2);
        assertEquals(null, d.get(Device.CLIENT, c2.getId()));
        assertEquals(0, d.numberOf(Device.CLIENT));
        assertEquals(d, clients.get(Clients.DEVICE, d.getId()));
        assertNotEquals(0, (long) d.get(Device.WROTE));

        d.set(Device.COMMENT, "comment");
        assertEquals("comment", d.get(Device.COMMENT));
    }

    @Test
    public void testLoadSave() {
        // Device with one client.
        Map<String, Object> save = new HashMap<>();
        save.put("ScoreBoard.Clients.Device(d2).Id", "d2");
        save.put("ScoreBoard.Clients.Device(d2).Client(c3).Id", "c3");
        save.put("ScoreBoard.Clients.Device(d2).SessionIdSecret", "asecret");
        ScoreBoardJSONSetter.set(sb, save, Source.JSON);

        // No clients after the load is done.
        assertEquals(1, clients.numberOf(Clients.DEVICE));
        assertEquals(0, clients.get(Clients.DEVICE, "d2").numberOf(Device.CLIENT));
        assertEquals(0, clients.getDevice("asecret").numberOf(Device.CLIENT));
    }

    @Test
    public void testLoadSaveReplaceOrMerge() {
        // Resetting the scoreboard is the difference between replace and merge,
        // and it has no effect on clients so we can test both together.
        // This test attempts to verify that someone with the ability to
        // replace/merge saves cannot use that to e.g. hide that a device has
        // previously written.

        // Device with one client.
        Device d = clients.getOrAddDevice("d1s");
        Client c = clients.addClient(d.getId(), "c1r", "c1s", "c1p");
        Client c2 = clients.addClient(d.getId(), "c2r", "c2s", "c2p");
        c.write();

        Map<String, Object> save = new HashMap<>();
        // Try to change settings of existing device and client.
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").Comment", "d1c");
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").Accessed", 0);
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").Wrote", 0);
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").RemoteAddr", "d1r");
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").SessionIdSecret", "injected");
        save.put("ScoreBoard.Clients.Client(" + c.getId() + ").RemoteAddr", "c1r2");
        save.put("ScoreBoard.Clients.Client(" + c.getId() + ").Wrote", 0);
        // Try to remove existing device and client.
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ")", null);
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").Client(" + c2.getId() + ")", null);
        // Try to add a client.
        save.put("ScoreBoard.Clients.Device(" + d.getId() + ").Client(abc).Id", "abc");
        // New device and client.
        save.put("ScoreBoard.Clients.Device(d2).Id", "d2");
        save.put("ScoreBoard.Clients.Device(d2).Client(c3).Id", "c3");
        save.put("ScoreBoard.Clients.Device(d2).SessionIdSecret", "d2s");
        save.put("ScoreBoard.Clients.Device(d2).RemoteAddr", "d2r");
        save.put("ScoreBoard.Clients.Device(d2).Comment", "d2c");
        ScoreBoardJSONSetter.set(sb, save, Source.JSON);

        // New clients have not been added, existing clients remain.
        assertEquals(d, clients.get(Clients.DEVICE, d.getId()));
        assertEquals(c, d.get(Device.CLIENT, c.getId()));
        assertEquals(c2, d.get(Device.CLIENT, c2.getId()));
        assertNull(d.get(Device.CLIENT, "c3"));
        assertEquals(2, d.numberOf(Device.CLIENT));
        // New device has been added, without a client.
        Device d2 = clients.getDevice("d2s");
        assertEquals(d2, clients.get(Clients.DEVICE, "d2"));
        assertEquals(2, clients.numberOf(Clients.DEVICE));

        // Comment is allowed to be updated of the device settings,
        // as anyone can do that from the WS.
        // Remote was empty, so we take a
        assertEquals("d1c", d.get(Device.COMMENT));
        assertEquals("c2r", d.get(Device.REMOTE_ADDR));
        assertNotEquals(0, (long) d.get(Device.ACCESSED));
        assertNotEquals(0, (long) d.get(Device.WROTE));
        assertEquals("d1s", d.get(Device.SESSION_ID_SECRET));
        // New device has settings from save.
        assertEquals("d2c", d2.get(Device.COMMENT));
        assertEquals("d2r", d2.get(Device.REMOTE_ADDR));
        assertEquals("d2s", d2.get(Device.SESSION_ID_SECRET));
        // Client settings remain the same.
        assertEquals("c1r", c.get(Client.REMOTE_ADDR));
    }

    @Test
    public void testGCOldDevices() {
        Device d = clients.getOrAddDevice("S1");
        assertEquals(1, clients.numberOf(Clients.DEVICE));
        d.access();

        long accessed = d.get(Device.ACCESSED);
        // Still has comment.
        d.set(Device.COMMENT, "c");
        clients.gcOldDevices(accessed + 1);
        assertEquals(1, clients.numberOf(Clients.DEVICE));

        // Still below threshold.
        d.set(Device.COMMENT, "");
        clients.gcOldDevices(accessed - 1);
        assertEquals(1, clients.numberOf(Clients.DEVICE));

        clients.gcOldDevices(accessed + 1);
        assertEquals(0, clients.numberOf(Clients.DEVICE));
    }

    protected void fuzzSet(String path) {
        List<JSONSet> sets =
            new ArrayList<>(Arrays.asList(new JSONSet(path, null, null), new JSONSet(path, "", null),
                                          new JSONSet(path, "abc", null), new JSONSet(path, "20", null),
                                          // WS can use these flags.
                                          new JSONSet(path, "answer", Flag.CHANGE),
                                          new JSONSet(path, "42", Flag.CHANGE), new JSONSet(path, null, Flag.RESET)));
        for (JSONSet s : sets) {
            try {
                ScoreBoardJSONSetter.set(sb, Collections.singletonList(s), Source.WS);
            } catch (Exception e) {
                // Don't stop if one manages to cause an exception.
            }
        }
    }

    @Test
    public void testWritesPrevented() {
        Device d = clients.getOrAddDevice("S1");
        String dId = "ScoreBoard.Clients.Device(" + d.getId() + ")";
        d.set(Device.COMMENT, "original comment");
        Client c = clients.addClient(d.getId(), "remoteaddr", "source", "platform");
        String cId = "ScoreBoard.Clients.Device(" + d.getId() + ").Client(" + c.getId() + ")";
        c.write();
        long created = d.get(Device.CREATED);
        long accessed = d.get(Device.ACCESSED);
        long wrote = d.get(Device.WROTE);
        String name = d.get(Device.NAME);
        // Make times the same for convenience.
        c.set(Client.CREATED, created);
        assertEquals(wrote, (long) c.get(Client.WROTE));

        // A WS user who can do writes should not be able to hide that a write
        // was performed, or otherwise alter anything beyond a device comment.
        fuzzSet("ScoreBoard.Clients");
        fuzzSet("ScoreBoard.Clients.Device(foo).Id");
        fuzzSet(dId);
        fuzzSet(dId + ".Id");
        fuzzSet(dId + ".SessionIdSecret");
        fuzzSet(dId + ".Name");
        fuzzSet(dId + ".RemoteAddr");
        fuzzSet(dId + ".Platform");
        fuzzSet(dId + ".Created");
        fuzzSet(dId + ".Wrote");
        fuzzSet(dId + ".Accessed");
        fuzzSet(dId + ".NoSuchProperty");
        fuzzSet(dId + ".Client(foo).Id");
        fuzzSet(dId + ".Client(" + c.getId() + ").Id");
        fuzzSet("ScoreBoard.Clients.Device(" + d.getId() + ").Client(foo).Id");
        fuzzSet(cId);
        fuzzSet(cId + ".Id");
        fuzzSet(cId + ".RemoteAddr");
        fuzzSet(cId + ".Platform");
        fuzzSet(cId + ".Source");
        fuzzSet(cId + ".Created");
        fuzzSet(cId + ".Wrote");
        fuzzSet(cId + ".NoSuchProperty");

        // This should work.
        ScoreBoardJSONSetter.set(sb, Collections.singletonList(new JSONSet(dId + ".Comment", "new comment", null)),
                                 Source.WS);

        // Clients.
        assertNotNull(sb.getClients());
        assertEquals(1, clients.numberOf(Clients.DEVICE));
        assertEquals(d, clients.get(Clients.DEVICE, d.getId()));
        assertEquals(1, d.numberOf(Device.CLIENT));
        assertEquals(c, d.get(Device.CLIENT, c.getId()));

        // Device.
        assertEquals(1, d.numberOf(Device.CLIENT));
        assertEquals(c, d.get(Device.CLIENT, c.getId()));
        assertEquals("S1", d.get(Device.SESSION_ID_SECRET));
        assertEquals(name, d.get(Device.NAME));
        assertEquals("remoteaddr", d.get(Device.REMOTE_ADDR));
        assertEquals("platform", d.get(Device.PLATFORM));
        assertEquals(created, (long) d.get(Device.CREATED));
        assertEquals(wrote, (long) d.get(Device.WROTE));
        assertEquals(accessed, (long) d.get(Device.ACCESSED));
        assertEquals("new comment", d.get(Device.COMMENT));

        // Client.
        assertEquals(wrote, (long) c.get(Client.WROTE));
        assertEquals("remoteaddr", c.get(Client.REMOTE_ADDR));
        assertEquals("platform", c.get(Client.PLATFORM));
        assertEquals("source", c.get(Client.SOURCE));
        assertEquals(created, (long) c.get(Client.CREATED));
        assertEquals(wrote, (long) c.get(Client.WROTE));
    }

    @Test
    public void testLocalCheck() {
        clients.set(Clients.NEW_DEVICE_WRITE, false);
        clients.set(Clients.ALL_LOCAL_DEVICES_WRITE, false);

        Device d = clients.getOrAddDevice("S1");
        assertFalse(d.mayWrite());

        d.set(Device.REMOTE_ADDR, "127.0.0.1");
        assertFalse(d.mayWrite());

        clients.set(Clients.ALL_LOCAL_DEVICES_WRITE, true);
        assertTrue(d.mayWrite());

        d.set(Device.REMOTE_ADDR, "192.168.0.1");
        assertFalse(d.mayWrite());

        d.set(Device.REMOTE_ADDR, "0:0:0:0:0:0:0:1");
        assertTrue(d.mayWrite());
    }
}
