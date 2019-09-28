package com.carolinarollergirls.scoreboard.utils;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClockConversion {
    public static Long fromHumanReadable(String v) {
        Matcher m = p.matcher(v);
        if (m.matches()) {
            MatchResult mr = m.toMatchResult();
            long min = Long.parseLong(mr.group(1));
            long sec = Long.parseLong(mr.group(2));
            long par = 0;
            if (mr.group(4) != null) {
                String parPad = mr.group(4) + "000";
                par = Long.parseLong(parPad.substring(0, 3));
            }

            return ((sec + (min * 60)) * 1000) + par;
        }

        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toHumanReadable(Long v) {
        Long minutes = v / 1000 / 60;
        Long seconds = (v / 1000) % 60;
        Long partial = v % 1000;

        if (partial == 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%d:%02d.%02d", minutes, seconds, partial);
        }
    }

    private static Pattern p = Pattern.compile("(\\d+):(\\d+)(\\.(\\d+))?");
}

