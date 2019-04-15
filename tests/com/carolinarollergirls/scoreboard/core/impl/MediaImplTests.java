package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Media.MediaType;
import com.carolinarollergirls.scoreboard.core.impl.MediaImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class MediaImplTests {

    private Media media;
    private File init;

    private BlockingQueue<ScoreBoardEvent> collectedEvents;
    private ScoreBoard sbMock;
    public ScoreBoardListener listener = new ScoreBoardListener() {

        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            synchronized(collectedEvents) {
                collectedEvents.add(event);
            }
        }
    };

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        collectedEvents = new LinkedBlockingQueue<>();

        sbMock = Mockito.mock(ScoreBoardImpl.class);

        dir.newFolder("html", "images", "teamlogo");
        init = dir.newFile("html/images/teamlogo/init.png");

        media = new MediaImpl(sbMock, dir.getRoot());
        media.addScoreBoardListener(listener);
    }

    @Test
    public void testFilesAddedAtStartup() {
        Media.MediaType mt = media.getFormat("images").getType("teamlogo");
        assertNotNull(mt);
        assertNotNull(mt.getFile("init.png"));
    }

    @Test
    public void testFileDeletionManual() throws Exception {
        init.delete();
        ScoreBoardEvent e = collectedEvents.poll(1, TimeUnit.SECONDS); //batch start
        e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(MediaType.Child.FILE, e.getProperty());
        assertTrue(e.isRemove());
        assertNull(media.getFormat("images").getType("teamlogo").getFile("init.png"));
    }

    @Test
    public void testFileDeletion() throws Exception {
        assertTrue(media.removeMediaFile("images", "teamlogo", "init.png"));
        ScoreBoardEvent e = collectedEvents.poll(1, TimeUnit.SECONDS); //batch start
        e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(MediaType.Child.FILE, e.getProperty());
        assertTrue(e.isRemove());
        assertNull(media.getFormat("images").getType("teamlogo").getFile("init.png"));
    }

    @Test
    public void testFileCreation() throws Exception {
        dir.newFile("html/images/teamlogo/new.png");
        ScoreBoardEvent e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(MediaType.Child.FILE, e.getProperty());
        assertNotNull(media.getFormat("images").getType("teamlogo").getFile("new.png"));
    }

}
