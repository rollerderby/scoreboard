package com.carolinarollergirls.scoreboard.event;

public interface TestScoreBoardEventProvider extends ScoreBoardEventProvider {
    PermanentProperty<Integer> INT = new PermanentProperty<>(Integer.class, "Int", 0);
    PermanentProperty<Integer> RO_INDIRECT_COPY = new PermanentProperty<>(Integer.class, "RoIndirectCopy", 0);
    PermanentProperty<Integer> RW_INDIRECT_COPY = new PermanentProperty<>(Integer.class, "RwIndirectCopy", 0);
    PermanentProperty<Integer> RECALCULATED = new PermanentProperty<>(Integer.class, "Recalculated", 0);
    PermanentProperty<TestScoreBoardEventProvider> REFERENCE = new PermanentProperty<>(
            TestScoreBoardEventProvider.class, "Reference", null);

    AddRemoveProperty<TestScoreBoardEventProvider> MULTIPLE = new AddRemoveProperty<>(TestScoreBoardEventProvider.class,
            "Multiple");
    AddRemoveProperty<TestScoreBoardEventProvider> SINGLETON = new AddRemoveProperty<>(
            TestScoreBoardEventProvider.class, "Singleton");

    NumberedProperty<TestNumberedScoreBoardEventProvider> NUMBERED = new NumberedProperty<>(
            TestNumberedScoreBoardEventProvider.class, "Numbered");

    CommandProperty TEST_COMMAND = new CommandProperty("TestCommand");
}
