package com.carolinarollergirls.scoreboard.event;

public interface TestParentOrderedScoreBoardEventProvider
        extends ParentOrderedScoreBoardEventProvider<TestParentOrderedScoreBoardEventProvider> {
    Child<TestParentOrderedScoreBoardEventProvider> CO_ORDERED = new Child<>(
            TestParentOrderedScoreBoardEventProvider.class, "CoOrdered");
}
