package com.carolinarollergirls.scoreboard.event;

public class TestNumberedScoreBoardEventProviderImpl
        extends NumberedScoreBoardEventProviderImpl<TestNumberedScoreBoardEventProvider> 
        implements TestNumberedScoreBoardEventProvider {
    public TestNumberedScoreBoardEventProviderImpl(TestScoreBoardEventProvider parent, int number) {
        super(parent, number, TestScoreBoardEventProvider.NChild.NUMBERED,
                TestNumberedScoreBoardEventProvider.class, Value.class, Child.class);
        setCopy(Value.RO_DIRECT_COPY, parent, TestScoreBoardEventProvider.Value.INT, true);
        setCopy(Value.RW_DIRECT_COPY, parent, TestScoreBoardEventProvider.Value.INT, false);
    }
}
