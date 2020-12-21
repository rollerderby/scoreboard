package com.carolinarollergirls.scoreboard.core.admin;

import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.admin.SettingsImpl;

public class SettingsImplTests {

    private ScoreBoardImpl sbMock;
    private SettingsImpl settings;

    @Before
    public void setup() {
        sbMock = Mockito.mock(ScoreBoardImpl.class);

        settings = new SettingsImpl(sbMock);
    }

    @Test
    public void test_set() {
        settings.set("Example", "ABC");

        assertSame("ABC", settings.get("Example"));
    }

}
