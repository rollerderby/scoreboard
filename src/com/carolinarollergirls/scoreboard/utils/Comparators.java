package com.carolinarollergirls.scoreboard.utils;

import java.util.Comparator;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class Comparators {
    public static class SbepComparator<T extends ScoreBoardEventProvider> implements Comparator<T> {
        public int compare(ScoreBoardEventProvider p1, ScoreBoardEventProvider p2) {
            if (p2 == null) { return -1; }
            if (p1 == null) { return 1; }
            if (p1 instanceof NumberedScoreBoardEventProvider<?> && p2 instanceof NumberedScoreBoardEventProvider<?> &&
                    p1.getParent() == p2.getParent()) {
                int n1 = ((NumberedScoreBoardEventProvider<?>)p1).getNumber();
                int n2 = ((NumberedScoreBoardEventProvider<?>)p2).getNumber();
                return n1 - n2;
            } else {
                return compare(p1.getParent(), p2.getParent());
            }
        }
    }

    public static Comparator<Period> PeriodComparator = new SbepComparator<Period>();
    public static Comparator<Jam> JamComparator = new SbepComparator<Jam>();
    public static Comparator<TeamJam> TeamJamComparator = new SbepComparator<TeamJam>();
    public static Comparator<Fielding> FieldingComparator = new SbepComparator<Fielding>();
    public static Comparator<Skater> SkaterComparator = new Comparator<Skater>() {
        public int compare(Skater s1, Skater s2) {
            if (s2 == null) { return 1; }
            if (s1 == null) { return -1; }
            String n1 = s1.getNumber();
            String n2 = s2.getNumber();
            if (n1 == null) { return -1; }
            if (n2 == null) { return 1; }

            return n1.compareTo(n2);
        }
    };

    public static Comparator<Penalty> PenaltyComparator = new Comparator<Penalty>() {
        public int compare(Penalty p1, Penalty p2) {
            if (p2 == null) { return -1; }
            if (p1 == null) { return 1; }
            int res = JamComparator.compare(p1.getJam(), p2.getJam());
            if (res == 0) {
                return (int) ((Long)p1.get(Penalty.Value.TIME) - (Long)p2.get(Penalty.Value.TIME));
            } else { return res; }
        }
    };
}
