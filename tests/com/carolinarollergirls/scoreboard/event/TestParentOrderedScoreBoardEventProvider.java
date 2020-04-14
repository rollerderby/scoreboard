package com.carolinarollergirls.scoreboard.event;

public interface TestParentOrderedScoreBoardEventProvider
        extends ParentOrderedScoreBoardEventProvider<TestParentOrderedScoreBoardEventProvider> {
    AddRemoveProperty<TestParentOrderedScoreBoardEventProvider> CO_ORDERED = new AddRemoveProperty<>(
            TestParentOrderedScoreBoardEventProvider.class, "CoOrdered");
}
