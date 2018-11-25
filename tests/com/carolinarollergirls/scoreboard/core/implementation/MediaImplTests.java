package com.carolinarollergirls.scoreboard.core.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.implementation.MediaImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class MediaImplTests {

    private Media media;
    private File init;

    private BlockingQueue<ScoreBoardEvent> collectedEvents;
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
        collectedEvents = new LinkedBlockingQueue<ScoreBoardEvent>();

        dir.newFolder("html", "images", "teamlogo");
        init = dir.newFile("html/images/teamlogo/init.png");

        media = new MediaImpl(dir.getRoot());
        media.addScoreBoardListener(listener);
    }

    @Test
    public void testFilesAddedAtStartup() {
        Map<String, Media.MediaFile> tm = media.getMediaFiles("images", "teamlogo");
        assertNotNull(tm);
        assertNotNull(tm.get("init.png"));
    }

    @Test
    public void testFileDeletionManual() throws Exception {
        init.delete();
        ScoreBoardEvent e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(Media.EVENT_REMOVE_FILE, e.getProperty());
        Map<String, Media.MediaFile> tm = media.getMediaFiles("images", "teamlogo");
        assertNull(tm.get("init.png"));
    }

    @Test
    public void testFileDeletion() throws Exception {
        assertTrue(media.removeMediaFile("images", "teamlogo", "init.png"));
        ScoreBoardEvent e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(Media.EVENT_REMOVE_FILE, e.getProperty());
        Map<String, Media.MediaFile> tm = media.getMediaFiles("images", "teamlogo");
        assertNull(tm.get("init.png"));
    }

    @Test
    public void testFileCreation() throws Exception {
        dir.newFile("html/images/teamlogo/new.png");
        ScoreBoardEvent e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(Media.MediaFile.EVENT_FILE, e.getProperty());
        Map<String, Media.MediaFile> tm = media.getMediaFiles("images", "teamlogo");
        assertNotNull(tm.get("new.png"));
    }

}
