package com.carolinarollergirls.scoreboard.utils;

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public class PropertyConversion {
    public static String toFrontend(Property prop) {
        if (prop instanceof Enum) {
            StringBuilder sb = new StringBuilder();
            for( String oneString : ((Enum<?>)prop).name().split("_") )
            {
                sb.append( oneString.substring(0,1) );
                sb.append( oneString.substring(1).toLowerCase() );
            }
            return sb.toString();
        }
        return prop.toString();
    }

    public static Property fromFrontend(String name, List<Class<? extends Property>> types) {
        for (Class<? extends Property> type : types) {
            for (Property prop : type.getEnumConstants()) {
                if (name.equals(toFrontend(prop))) {
                    return prop;
                }
            }
        }
        return null;
    }
}
