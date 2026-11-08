package com.carolinarollergirls.scoreboard.json;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AutoSaveJSONStateTests {
    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    private File[] backups() {
        File[] dirs = new File(dir.getRoot(), "backup").listFiles();
        assertEquals(1, dirs.length);
        return dirs[0].listFiles();
    }

    @Test
    public void testBackupKeepsNewestFile() throws Exception {
        for (int n = 0; n <= 6; n++) {
            FileUtils.writeStringToFile(new File(dir.getRoot(), "scoreboard-" + (n * 10) + "-secs-ago.json"),
                                        "{\"state\":{}," + n + "}", StandardCharsets.UTF_8);
        }
        new AutoSaveJSONState(new JSONStateManager(false), dir.getRoot(), false);

        File[] files = backups();
        assertEquals(1, files.length);
        assertEquals("scoreboard-0-secs-ago.json", files[0].getName());
        assertEquals("{\"state\":{},0}", FileUtils.readFileToString(files[0], StandardCharsets.UTF_8));
    }

    @Test
    public void testBackupWithoutNewestFile() throws Exception {
        // the same file a restart would load
        FileUtils.writeStringToFile(new File(dir.getRoot(), "scoreboard-30-secs-ago.json"), "{}",
                                    StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(dir.getRoot(), "scoreboard-50-secs-ago.json"), "{}",
                                    StandardCharsets.UTF_8);
        new AutoSaveJSONState(new JSONStateManager(false), dir.getRoot(), false);

        File[] files = backups();
        assertEquals(1, files.length);
        assertEquals("scoreboard-30-secs-ago.json", files[0].getName());
    }
}
