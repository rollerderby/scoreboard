package com.carolinarollergirls.scoreboard.core.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;

public class SettingsImplTests {

    private ScoreBoardImpl sb;
    private SettingsImpl settings;

    @Before
    public void setup() {
        sb = new ScoreBoardImpl(false);

        settings = new SettingsImpl(sb);
    }

    @Test
    public void test_set() {
        settings.set("Example", "ABC");

        assertSame("ABC", settings.get("Example"));

        assertFalse(Boolean.parseBoolean(settings.get(Clock.SETTING_SYNC)));
        settings.set(Clock.SETTING_SYNC, "true");
        assertTrue(Boolean.parseBoolean(settings.get(Clock.SETTING_SYNC)));
    }
}
