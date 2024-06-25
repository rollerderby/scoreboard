package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Penalty extends NumberedScoreBoardEventProvider<Penalty> {
    public int compareTo(Penalty other);

    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public String getCode();

    public boolean isServed();

    public String getDetails();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Long> TIME = new Value<>(Long.class, "Time", 0L, props);
    public static final Value<Jam> JAM = new Value<>(Jam.class, "Jam", null, props);
    public static final Value<Integer> PERIOD_NUMBER = new Value<>(Integer.class, "PeriodNumber", 0, props);
    public static final Value<Integer> JAM_NUMBER = new Value<>(Integer.class, "JamNumber", 0, props);
    public static final Value<String> CODE = new Value<>(String.class, "Code", "", props);
    public static final Value<Boolean> SERVING = new Value<>(Boolean.class, "Serving", false, props);
    public static final Value<Boolean> SERVED = new Value<>(Boolean.class, "Served", false, props);
    public static final Value<Boolean> FORCE_SERVED = new Value<>(Boolean.class, "ForceServed", false, props);
    public static final Value<BoxTrip> BOX_TRIP = new Value<>(BoxTrip.class, "BoxTrip", null, props);

    public static final Command REMOVE = new Command("Remove", props);
}
