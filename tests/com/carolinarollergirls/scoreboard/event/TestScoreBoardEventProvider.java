package com.carolinarollergirls.scoreboard.event;

import java.util.ArrayList;
import java.util.Collection;

public interface TestScoreBoardEventProvider extends ScoreBoardEventProvider {

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Integer> INT = new Value<>(Integer.class, "Int", 0, props);
    public static final Value<Integer> RO_INDIRECT_COPY = new Value<>(Integer.class, "RoIndirectCopy", 0, props);
    public static final Value<Integer> RW_INDIRECT_COPY = new Value<>(Integer.class, "RwIndirectCopy", 0, props);
    public static final Value<Integer> RECALCULATED = new Value<>(Integer.class, "Recalculated", 0, props);
    public static final Value<TestScoreBoardEventProvider> REFERENCE =
        new Value<>(TestScoreBoardEventProvider.class, "Reference", null, props);

    public static final Child<TestScoreBoardEventProvider> MULTIPLE =
        new Child<>(TestScoreBoardEventProvider.class, "Multiple", props);
    public static final Child<TestScoreBoardEventProvider> SINGLETON =
        new Child<>(TestScoreBoardEventProvider.class, "Singleton", props);

    public static final NumberedChild<TestNumberedScoreBoardEventProvider> NUMBERED =
        new NumberedChild<>(TestNumberedScoreBoardEventProvider.class, "Numbered", props);

    public static final Command TEST_COMMAND = new Command("TestCommand", props);
}
