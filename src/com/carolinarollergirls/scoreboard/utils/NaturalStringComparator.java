package com.carolinarollergirls.scoreboard.utils;

import java.util.Comparator;

public class NaturalStringComparator implements Comparator<String> {
    @Override
    public int compare(String s1, String s2) {
        // Skip all identical characters
        int len1 = s1.length();
        int len2 = s2.length();
        int i;
        char c1, c2;
        for (i = 0, c1 = 0, c2 = 0; (i < len1) && (i < len2) && (c1 = s1.charAt(i)) == (c2 = s2.charAt(i)); i++)
            ;

        // Check end of string
        if (c1 == c2) return (len1 - len2);

        // Check digit in first string
        if (Character.isDigit(c1)) {
            // Check digit only in first string
            if (!Character.isDigit(c2)) return (1);

            // Scan all integer digits
            int x1, x2;
            for (x1 = i + 1; (x1 < len1) && Character.isDigit(s1.charAt(x1)); x1++)
                ;
            for (x2 = i + 1; (x2 < len2) && Character.isDigit(s2.charAt(x2)); x2++)
                ;

            // Longer integer wins, first digit otherwise
            return (x2 == x1 ? c1 - c2 : x1 - x2);
        }

        // Check digit only in second string
        if (Character.isDigit(c2)) return (-1);

        // No digits
        return (c1 - c2);
    }
}
