package com.carolinarollergirls.scoreboard.event;

public interface TestScoreBoardEventProvider extends ScoreBoardEventProvider {
    Value<Integer> INT = new Value<>(Integer.class, "Int", 0);
    Value<Integer> RO_INDIRECT_COPY = new Value<>(Integer.class, "RoIndirectCopy", 0);
    Value<Integer> RW_INDIRECT_COPY = new Value<>(Integer.class, "RwIndirectCopy", 0);
    Value<Integer> RECALCULATED = new Value<>(Integer.class, "Recalculated", 0);
    Value<TestScoreBoardEventProvider> REFERENCE = new Value<>(TestScoreBoardEventProvider.class, "Reference", null);

    Child<TestScoreBoardEventProvider> MULTIPLE = new Child<>(TestScoreBoardEventProvider.class, "Multiple");
    Child<TestScoreBoardEventProvider> SINGLETON = new Child<>(TestScoreBoardEventProvider.class, "Singleton");

    NumberedChild<TestNumberedScoreBoardEventProvider> NUMBERED =
        new NumberedChild<>(TestNumberedScoreBoardEventProvider.class, "Numbered");

    Command TEST_COMMAND = new Command("TestCommand");
}
