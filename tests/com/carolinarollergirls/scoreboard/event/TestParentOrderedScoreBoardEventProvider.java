package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public interface TestParentOrderedScoreBoardEventProvider
        extends ParentOrderedScoreBoardEventProvider<TestParentOrderedScoreBoardEventProvider> {
    public enum Child implements AddRemoveProperty {
        CO_ORDERED(TestParentOrderedScoreBoardEventProvider.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
}
