package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;

public class Command extends Property<Boolean> {
    public Command(String jsonName, Collection<Property<?>> propsToAddTo) {
        super(Boolean.class, jsonName, propsToAddTo);
    }
}
