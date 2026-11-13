package com.carolinarollergirls.scoreboard.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.utils.BasePath;

public class MediaServletTests {
    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    private File oldDir;
    private File typeDir;
    private MediaServlet ms;
    private FileItemFactory factory = new DiskFileItemFactory();

    @Before
    public void setUp() throws Exception {
        dir.newFolder("html", "themes", "custom");
        typeDir = new File(dir.getRoot(), "html/themes/custom");
        oldDir = BasePath.get();
        BasePath.set(dir.getRoot());
        ms = new MediaServlet(new ScoreBoardImpl(false), new File(dir.getRoot(), "html").getPath());
    }

    @After
    public void tearDown() throws Exception {
        BasePath.set(oldDir);
    }

    private FileItem item(String name) throws IOException {
        FileItem item = factory.createItem(null, null, false, name);
        OutputStream oS = item.getOutputStream();
        oS.write("test".getBytes());
        oS.close();
        return item;
    }

    private void upload(FileItem... items) throws IOException {
        List<FileItem> list = new ArrayList<>();
        for (FileItem item : items) { list.add(item); }
        ms.processFileItemList(list, "themes", "custom");
    }

    private void uploadZip(String... names) throws IOException {
        FileItem zip = factory.createItem(null, null, false, "upload.zip");
        ZipOutputStream zoS = new ZipOutputStream(zip.getOutputStream());
        for (String name : names) {
            zoS.putNextEntry(new ZipEntry(name));
            zoS.write("test".getBytes());
            zoS.closeEntry();
        }
        zoS.close();
        List<FileItem> list = new ArrayList<>();
        ms.processZipFileItem(factory, zip, list);
        ms.processFileItemList(list, "themes", "custom");
    }

    @Test
    public void testUpload() throws Exception {
        upload(item("my.css"));
        assertTrue(new File(typeDir, "my.css").exists());
    }

    @Test
    public void testUploadOutsideTypeDir() throws Exception {
        try {
            upload(item("../../../outside.txt"));
            fail();
        } catch (IllegalArgumentException e) {}
        assertFalse(new File(dir.getRoot(), "outside.txt").exists());

        try {
            upload(item(new File(dir.getRoot(), "absolute.txt").getAbsolutePath()));
            fail();
        } catch (IllegalArgumentException e) {}
        assertFalse(new File(dir.getRoot(), "absolute.txt").exists());
    }

    @Test
    public void testZipWithFolders() throws Exception {
        uploadZip("mine/theme.css", "mine/fonts/a.woff2");
        assertTrue(new File(typeDir, "mine/theme.css").exists());
        assertTrue(new File(typeDir, "mine/fonts/a.woff2").exists());
    }

    @Test
    public void testZipOutsideTypeDir() throws Exception {
        try {
            uploadZip("ok.css", "../../../outside.txt");
            fail();
        } catch (IllegalArgumentException e) {}
        assertFalse(new File(dir.getRoot(), "outside.txt").exists());
        // nothing is written when one entry is refused
        assertFalse(new File(typeDir, "ok.css").exists());
        assertEquals(0, typeDir.list().length);
    }
}
