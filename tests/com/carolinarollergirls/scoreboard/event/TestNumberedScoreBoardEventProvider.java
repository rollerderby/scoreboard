package com.carolinarollergirls.scoreboard.event;

public interface TestNumberedScoreBoardEventProvider
        extends NumberedScoreBoardEventProvider<TestNumberedScoreBoardEventProvider> {
    PermanentProperty<Integer> INT = new PermanentProperty<>(Integer.class, "Int", 0);
    PermanentProperty<Integer> RO_DIRECT_COPY = new PermanentProperty<>(Integer.class, "RoDirectCopy", 0);
    PermanentProperty<Integer> RW_DIRECT_COPY = new PermanentProperty<>(Integer.class, "RwDirectCopy", 0);

    AddRemoveProperty<TestParentOrderedScoreBoardEventProvider> CO_ORDERED = new AddRemoveProperty<>(
            TestParentOrderedScoreBoardEventProvider.class, "CoOrdered");
}
