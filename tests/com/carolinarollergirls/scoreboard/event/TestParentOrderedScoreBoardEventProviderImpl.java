package com.carolinarollergirls.scoreboard.event;

public class TestParentOrderedScoreBoardEventProviderImpl
        extends ParentOrderedScoreBoardEventProviderImpl<TestParentOrderedScoreBoardEventProvider>
        implements TestParentOrderedScoreBoardEventProvider {
    public TestParentOrderedScoreBoardEventProviderImpl(OrderedScoreBoardEventProvider<?> parent, String subId) {
        super(parent, subId,
                parent instanceof TestParentOrderedScoreBoardEventProvider
                        ? TestParentOrderedScoreBoardEventProvider.CO_ORDERED
                        : TestNumberedScoreBoardEventProvider.CO_ORDERED);
        addProperties(CO_ORDERED);
    }
    public TestParentOrderedScoreBoardEventProviderImpl(TestParentOrderedScoreBoardEventProviderImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new TestParentOrderedScoreBoardEventProviderImpl(this, root); }
}
