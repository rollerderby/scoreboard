package com.carolinarollergirls.scoreboard.event;

public interface TestNumberedScoreBoardEventProvider
        extends NumberedScoreBoardEventProvider<TestNumberedScoreBoardEventProvider> {
    Value<Integer> INT = new Value<>(Integer.class, "Int", 0);
    Value<Integer> RO_DIRECT_COPY = new Value<>(Integer.class, "RoDirectCopy", 0);
    Value<Integer> RW_DIRECT_COPY = new Value<>(Integer.class, "RwDirectCopy", 0);

    Child<TestParentOrderedScoreBoardEventProvider> CO_ORDERED = new Child<>(
            TestParentOrderedScoreBoardEventProvider.class, "CoOrdered");
}
