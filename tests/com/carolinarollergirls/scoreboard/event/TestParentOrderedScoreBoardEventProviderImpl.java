package com.carolinarollergirls.scoreboard.event;

public class TestParentOrderedScoreBoardEventProviderImpl
        extends ParentOrderedScoreBoardEventProviderImpl<TestParentOrderedScoreBoardEventProvider>
        implements TestParentOrderedScoreBoardEventProvider {
    public TestParentOrderedScoreBoardEventProviderImpl(OrderedScoreBoardEventProvider<?> parent, String subId) {
        super(parent, subId,
                parent instanceof TestParentOrderedScoreBoardEventProvider ? TestParentOrderedScoreBoardEventProvider.Child.CO_ORDERED : TestNumberedScoreBoardEventProvider.Child.CO_ORDERED,
                        TestParentOrderedScoreBoardEventProvider.class, Child.class);
    }
}
