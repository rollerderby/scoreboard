package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Official extends ScoreBoardEventProvider {
    public int compareTo(Official other);

    public Child<Official> getType();

    public static Collection<Property<?>> props = new ArrayList<>();
    public static Collection<Property<?>> preparedProps = new ArrayList<>(); // also present on PreparedOfficial

    public static final Value<String> ROLE = new Value<>(String.class, "Role", "", props);
    public static final Value<String> NAME = new Value<>(String.class, "Name", "", preparedProps);
    public static final Value<String> LEAGUE = new Value<>(String.class, "League", "", preparedProps);
    public static final Value<String> CERT = new Value<>(String.class, "Cert", "", preparedProps);
    public static final Value<Team> P1_TEAM = new Value<>(Team.class, "P1Team", null, props);
    public static final Value<Boolean> SWAP = new Value<>(Boolean.class, "Swap", false, props);
    public static final Value<PreparedOfficial> PREPARED_OFFICIAL =
        new Value<>(PreparedOfficial.class, "PreparedOfficial", null, props);

    public static final Command STORE = new Command("Store", props);

    public static final String ROLE_HNSO = "Head Non-Skating Official";
    public static final String ROLE_PLT = "Penalty Lineup Tracker";
    public static final String ROLE_PT = "Penalty Tracker";
    public static final String ROLE_PW = "Penalty Wrangler";
    public static final String ROLE_WB = "Inside Whiteboard Operator";
    public static final String ROLE_JT = "Jam Timer";
    public static final String ROLE_SK = "Scorekeeper";
    public static final String ROLE_SBO = "ScoreBoard Operator";
    public static final String ROLE_PBM = "Penalty Box Manager";
    public static final String ROLE_PBT = "Penalty Box Timer";
    public static final String ROLE_LT = "Lineup Tracker";
    public static final String ROLE_ALTN = "Non-Skating Official Alternate";

    public static final String ROLE_HR = "Head Referee";
    public static final String ROLE_IPR = "Inside Pack Referee";
    public static final String ROLE_JR = "Jammer Referee";
    public static final String ROLE_OPR = "Outside Pack Referee";
    public static final String ROLE_ALTR = "Referee Alternate";
}
