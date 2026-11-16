package com.carolinarollergirls.scoreboard.core.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Media;
import com.carolinarollergirls.scoreboard.core.interfaces.Media.MediaType;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.BasePath;

public class MediaImplTests {

    // In general Java's macOS implementation takes longer for file system notifications.
    private static final int secondsToWait =
        (System.getProperty("os.name").toLowerCase().matches("^mac.*os.*$") ? 10 : 1);

    private Media media;
    private File init;

    private File oldDir;

    private BlockingQueue<ScoreBoardEvent<?>> collectedEvents;
    private ScoreBoard sb;
    public ScoreBoardListener listener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (collectedEvents) { collectedEvents.add(event); }
        }
    };

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        collectedEvents = new LinkedBlockingQueue<>();

        sb = new ScoreBoardImpl(false);

        dir.newFolder("html", "images", "teamlogo");
        init = dir.newFile("html/images/teamlogo/init.png");

        oldDir = BasePath.get();
        BasePath.set(dir.getRoot());
        media = new MediaImpl(sb);
        media.addScoreBoardListener(listener);
    }

    @After
    public void tearDown() throws Exception {
        BasePath.set(oldDir);
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
        ScoreBoardEvent<?> e = collectedEvents.poll(secondsToWait, TimeUnit.SECONDS); // batch start
        e = collectedEvents.poll(secondsToWait, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(MediaType.FILE, e.getProperty());
        assertTrue(e.isRemove());
        assertNull(media.getFormat("images").getType("teamlogo").getFile("init.png"));
    }

    @Test
    public void testFileDeletion() throws Exception {
        assertTrue(media.removeMediaFile("images", "teamlogo", "init.png"));
        ScoreBoardEvent<?> e = collectedEvents.poll(secondsToWait, TimeUnit.SECONDS); // batch start
        e = collectedEvents.poll(secondsToWait, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(MediaType.FILE, e.getProperty());
        assertTrue(e.isRemove());
        assertNull(media.getFormat("images").getType("teamlogo").getFile("init.png"));
    }

    @Test
    public void testFileCreation() throws Exception {
        dir.newFile("html/images/teamlogo/new.png");
        ScoreBoardEvent<?> e = collectedEvents.poll(secondsToWait, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(ScoreBoardEventProviderImpl.BATCH_START, e.getProperty());
        e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(MediaType.FILE, e.getProperty());
        assertNotNull(media.getFormat("images").getType("teamlogo").getFile("new.png"));
        e = collectedEvents.poll(1, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(ScoreBoardEventProviderImpl.BATCH_END, e.getProperty());
    }

    @Test
    public void testHiddenFilesIgnoredAtStartup() throws Exception {
        dir.newFile("html/images/teamlogo/.DS_Store");
        media = new MediaImpl(sb);
        MediaType mt = media.getFormat("images").getType("teamlogo");
        assertNull(mt.getFile(".DS_Store"));
        assertEquals(1, mt.getAll(MediaType.FILE).size());
    }

    @Test
    public void testHiddenFileCreationIgnored() throws Exception {
        dir.newFile("html/images/teamlogo/.hidden.png");
        dir.newFile("html/images/teamlogo/visible.png");
        // Wait until the visible file shows up, so the hidden one has been processed too.
        MediaType mt = media.getFormat("images").getType("teamlogo");
        long deadline = System.currentTimeMillis() + secondsToWait * 1000L;
        while (mt.getFile("visible.png") == null && System.currentTimeMillis() < deadline) { Thread.sleep(50); }
        assertNotNull(mt.getFile("visible.png"));
        assertNull(mt.getFile(".hidden.png"));
    }

    @Test
    public void testThemes() throws Exception {
        dir.newFolder("html", "themes", "custom", "dark", "fonts");
        dir.newFile("html/themes/custom/dark/theme.css");
        dir.newFile("html/themes/custom/light.css");
        dir.newFile("html/themes/custom/a.woff2");
        media = new MediaImpl(sb);
        MediaType mt = media.getFormat("themes").getType("custom");
        assertEquals("/themes/custom/dark/", mt.getFile("dark").getSrc());
        assertEquals("dark", mt.getFile("dark").getName());
        assertEquals("/themes/custom/light.css", mt.getFile("light.css").getSrc());
        // files a theme uses are not themes themselves
        assertNull(mt.getFile("a.woff2"));
        assertEquals(2, mt.getAll(MediaType.FILE).size());
    }
}
