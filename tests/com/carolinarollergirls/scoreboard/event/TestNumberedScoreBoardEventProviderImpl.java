package com.carolinarollergirls.scoreboard.event;

public class TestNumberedScoreBoardEventProviderImpl
        extends NumberedScoreBoardEventProviderImpl<TestNumberedScoreBoardEventProvider>
        implements TestNumberedScoreBoardEventProvider {
    public TestNumberedScoreBoardEventProviderImpl(TestScoreBoardEventProvider parent, int number) {
        super(parent, number, TestScoreBoardEventProvider.NUMBERED);
        addProperties(INT, RO_DIRECT_COPY, RW_DIRECT_COPY, CO_ORDERED);
        setCopy(RO_DIRECT_COPY, parent, TestScoreBoardEventProvider.INT, true);
        setCopy(RW_DIRECT_COPY, parent, TestScoreBoardEventProvider.INT, false);
    }
    public TestNumberedScoreBoardEventProviderImpl(TestNumberedScoreBoardEventProviderImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new TestNumberedScoreBoardEventProviderImpl(this, root); }
}
