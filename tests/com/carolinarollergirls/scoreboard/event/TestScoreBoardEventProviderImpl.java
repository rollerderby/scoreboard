package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class TestScoreBoardEventProviderImpl extends ScoreBoardEventProviderImpl
        implements TestScoreBoardEventProvider {
    public TestScoreBoardEventProviderImpl() {
        super(null, "", null, TestScoreBoardEventProvider.class, Value.class, Child.class, NChild.class, Command.class);
        setupReferences();
    }
    public TestScoreBoardEventProviderImpl(TestScoreBoardEventProvider parent, String id, AddRemoveProperty type) {
        super(parent, id, type, TestScoreBoardEventProvider.class, Value.class, Child.class, NChild.class,
                Command.class);
        setupReferences();
    }

    private void setupReferences() {
        setInverseReference(Child.MULTIPLE, Value.REFERENCE);
        setInverseReference(Value.REFERENCE, Child.MULTIPLE);
        setCopy(Value.RO_INDIRECT_COPY, this, Value.REFERENCE, Value.INT, true);
        setCopy(Value.RW_INDIRECT_COPY, this, Value.REFERENCE, Value.INT, false);
        if (parent == null) { add(Child.SINGLETON, new TestScoreBoardEventProviderImpl(this, "", Child.SINGLETON)); }
        addWriteProtection(Child.SINGLETON);
        setRecalculated(Value.RECALCULATED).addSource(this, Value.INT).addIndirectSource(this, Value.REFERENCE,
                Value.INT);
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        valuesRecalculated++;
        if (prop == Value.RECALCULATED) {
            return -(Integer) value;
        }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        valuesChanged++;
    }

    @Override
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item, Source source) {
        itemsAdded++;
    }
    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item, Source source) {
        itemsRemoved++;
    }

    @Override
    public void execute(CommandProperty prop, Source source) {
        commmandsExecuted++;
    }

    protected int valuesRecalculated = 0;
    protected int valuesChanged = 0;
    protected int itemsAdded = 0;
    protected int itemsRemoved = 0;
    protected int commmandsExecuted = 0;
}
