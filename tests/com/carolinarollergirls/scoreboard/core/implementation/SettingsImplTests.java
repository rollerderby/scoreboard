package com.carolinarollergirls.scoreboard.core.implementation;

import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.carolinarollergirls.scoreboard.core.implementation.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.implementation.SettingsImpl;

public class SettingsImplTests {

    private ScoreBoardImpl sbModelMock;
    private SettingsImpl settingsModel;

    @Before
    public void setup() {
        sbModelMock = Mockito.mock(ScoreBoardImpl.class);

        settingsModel = new SettingsImpl(sbModelMock);
    }

    @Test
    public void test_set() {
        settingsModel.set("Example", "ABC");

        assertSame("ABC", settingsModel.get("Example"));
    }

}
