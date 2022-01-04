package com.carolinarollergirls.scoreboard.event;

import java.util.ArrayList;
import java.util.Collection;

public interface TestParentOrderedScoreBoardEventProvider
    extends ParentOrderedScoreBoardEventProvider<TestParentOrderedScoreBoardEventProvider> {

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Child<TestParentOrderedScoreBoardEventProvider> CO_ORDERED =
        new Child<>(TestParentOrderedScoreBoardEventProvider.class, "CoOrdered", props);
}
