package com.carolinarollergirls.scoreboard.utils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ooxml.POIXMLProperties.CustomProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public class StatsbookExporterTests {
    private static XSSFWorkbook roundTrip(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void testSetVersionProperty() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        StatsbookExporter.setVersionProperty(wb, "v2026.1");

        CustomProperties props = roundTrip(wb).getProperties().getCustomProperties();
        assertEquals("v2026.1", props.getProperty("CRG ScoreBoard Version").getLpwstr());
    }

    @Test
    public void testSetVersionPropertyReplacesExisting() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        wb.getProperties().getCustomProperties().addProperty("CRG ScoreBoard Version", "v2025.1");
        StatsbookExporter.setVersionProperty(wb, "v2026.1");

        CustomProperties props = roundTrip(wb).getProperties().getCustomProperties();
        assertEquals("v2026.1", props.getProperty("CRG ScoreBoard Version").getLpwstr());
    }
}
