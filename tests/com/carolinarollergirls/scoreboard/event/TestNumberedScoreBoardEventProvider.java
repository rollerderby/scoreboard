package com.carolinarollergirls.scoreboard.event;

import java.util.ArrayList;
import java.util.Collection;

public interface TestNumberedScoreBoardEventProvider
    extends NumberedScoreBoardEventProvider<TestNumberedScoreBoardEventProvider> {

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Integer> INT = new Value<>(Integer.class, "Int", 0, props);
    public static final Value<Integer> RO_DIRECT_COPY = new Value<>(Integer.class, "RoDirectCopy", 0, props);
    public static final Value<Integer> RW_DIRECT_COPY = new Value<>(Integer.class, "RwDirectCopy", 0, props);

    public static final Child<TestParentOrderedScoreBoardEventProvider> CO_ORDERED =
        new Child<>(TestParentOrderedScoreBoardEventProvider.class, "CoOrdered", props);
}
