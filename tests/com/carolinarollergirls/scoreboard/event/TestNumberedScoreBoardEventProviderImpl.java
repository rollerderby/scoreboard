package com.carolinarollergirls.scoreboard.event;

public class TestNumberedScoreBoardEventProviderImpl
    extends NumberedScoreBoardEventProviderImpl<TestNumberedScoreBoardEventProvider>
    implements TestNumberedScoreBoardEventProvider {
    public TestNumberedScoreBoardEventProviderImpl(TestScoreBoardEventProvider parent, int number) {
        super(parent, number, TestScoreBoardEventProvider.NUMBERED);
        addProperties(props);
        setCopy(RO_DIRECT_COPY, parent, TestScoreBoardEventProvider.INT, true);
        setCopy(RW_DIRECT_COPY, parent, TestScoreBoardEventProvider.INT, false);
    }
}
