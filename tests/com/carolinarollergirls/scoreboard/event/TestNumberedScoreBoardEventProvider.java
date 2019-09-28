package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public interface TestNumberedScoreBoardEventProvider
        extends NumberedScoreBoardEventProvider<TestNumberedScoreBoardEventProvider> {
    public enum Value implements PermanentProperty {
        INT(Integer.class, 0),
        RO_DIRECT_COPY(Integer.class, 0),
        RW_DIRECT_COPY(Integer.class, 0);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        CO_ORDERED(TestParentOrderedScoreBoardEventProvider.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
}
