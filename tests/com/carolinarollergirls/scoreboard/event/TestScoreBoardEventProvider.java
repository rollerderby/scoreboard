package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public interface TestScoreBoardEventProvider extends ScoreBoardEventProvider {
    public enum Value implements PermanentProperty {
        INT(Integer.class, 0),
        ID(String.class, ""),
        RO_INDIRECT_COPY(Integer.class, 0),
        RW_INDIRECT_COPY(Integer.class, 0),
        RECALCULATED(Integer.class, 0),
        REFERENCE(TestScoreBoardEventProvider.class, null);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        MULTIPLE(TestScoreBoardEventProvider.class),
        SINGLETON(TestScoreBoardEventProvider.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum NChild implements NumberedProperty {
        NUMBERED(TestNumberedScoreBoardEventProvider.class);

        private NChild(Class<? extends OrderedScoreBoardEventProvider<?>> t) { type = t; }
        private final Class<? extends OrderedScoreBoardEventProvider<?>> type;
        @Override
        public Class<? extends OrderedScoreBoardEventProvider<?>> getType() { return type; }
    }
    public enum Command implements CommandProperty {
        TEST_COMMAND;

        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }
}
