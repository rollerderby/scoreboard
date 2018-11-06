package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultSettingsModelTests {

    private DefaultScoreBoardModel sbModelMock;
    private DefaultSettingsModel settingsModel;

    @Before
    public void setup() {
        sbModelMock = Mockito.mock(DefaultScoreBoardModel.class);

        settingsModel = new DefaultSettingsModel(sbModelMock);
    }

    @Test
    public void test_set() {
        settingsModel.set("Example", "ABC");

        assertSame("ABC", settingsModel.get("Example"));
    }

}
