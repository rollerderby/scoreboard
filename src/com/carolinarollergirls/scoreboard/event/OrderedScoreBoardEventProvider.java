package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface OrderedScoreBoardEventProvider<T extends OrderedScoreBoardEventProvider<T>> extends ScoreBoardEventProvider {
    public int getNumber();

    public T getPrevious();
    public boolean hasPrevious();
    public T getNext();
    public boolean hasNext();

    public enum IValue implements PermanentProperty {
        ID(String.class, ""),
        NUMBER(Integer.class, 0),
        PREVIOUS(OrderedScoreBoardEventProvider.class, null),
        NEXT(OrderedScoreBoardEventProvider.class, null);

        private IValue(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
}
