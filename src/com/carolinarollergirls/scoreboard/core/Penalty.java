package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;

public interface Penalty extends NumberedScoreBoardEventProvider<Penalty> {
    public int compareTo(Penalty other);

    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public String getCode();

    public boolean isServed();

    // @formatter:off
    PermanentProperty<Long> TIME = new PermanentProperty<>(Long.class, "Time", 0L);
    PermanentProperty<Jam> JAM = new PermanentProperty<>(Jam.class, "Jam", null);
    PermanentProperty<Integer> PERIOD_NUMBER = new PermanentProperty<>(Integer.class, "PeriodNumber", 0);
    PermanentProperty<Integer> JAM_NUMBER = new PermanentProperty<>(Integer.class, "JamNumber", 0);
    PermanentProperty<String> CODE = new PermanentProperty<>(String.class, "Code", "");
    PermanentProperty<Boolean> SERVING = new PermanentProperty<>(Boolean.class, "Serving", false);
    PermanentProperty<Boolean> SERVED = new PermanentProperty<>(Boolean.class, "Served", false);
    PermanentProperty<Boolean> FORCE_SERVED = new PermanentProperty<>(Boolean.class, "ForceServed", false);
    PermanentProperty<BoxTrip> BOX_TRIP = new PermanentProperty<>(BoxTrip.class, "BoxTrip", null);

    CommandProperty REMOVE = new CommandProperty("Remove");
    // @formatter:on
}
