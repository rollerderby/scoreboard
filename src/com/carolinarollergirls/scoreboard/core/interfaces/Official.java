package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Official extends ScoreBoardEventProvider {
    Value<String> ROLE = new Value<>(String.class, "Role", "");
    Value<String> NAME = new Value<>(String.class, "Name", "");
    Value<String> LEAGUE = new Value<>(String.class, "League", "");
    Value<String> CERT = new Value<>(String.class, "Cert", "");
    Value<Team> P1_TEAM = new Value<>(Team.class, "P1Team", null);
    Value<Boolean> SWAP = new Value<>(Boolean.class, "Swap", false);

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
