package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Penalty extends NumberedScoreBoardEventProvider<Penalty> {
    public int compareTo(Penalty other);

    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public String getCode();

    public boolean isServed();

    Value<Long> TIME = new Value<>(Long.class, "Time", 0L);
    Value<Jam> JAM = new Value<>(Jam.class, "Jam", null);
    Value<Integer> PERIOD_NUMBER = new Value<>(Integer.class, "PeriodNumber", 0);
    Value<Integer> JAM_NUMBER = new Value<>(Integer.class, "JamNumber", 0);
    Value<String> CODE = new Value<>(String.class, "Code", "");
    Value<Boolean> SERVING = new Value<>(Boolean.class, "Serving", false);
    Value<Boolean> SERVED = new Value<>(Boolean.class, "Served", false);
    Value<Boolean> FORCE_SERVED = new Value<>(Boolean.class, "ForceServed", false);
    Value<BoxTrip> BOX_TRIP = new Value<>(BoxTrip.class, "BoxTrip", null);

    Command REMOVE = new Command("Remove");
}
