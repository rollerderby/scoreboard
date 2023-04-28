package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam.PreparedSkater;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Skater extends ScoreBoardEventProvider {
    public int compareTo(Skater other);

    public Team getTeam();
    public String getName();
    public void setName(String id);
    public String getRosterNumber();
    public void setRosterNumber(String number);
    public Fielding getFielding(TeamJam teamJam);
    public Fielding getCurrentFielding();
    public void removeCurrentFielding();
    public void updateFielding(TeamJam teamJam);
    public Position getPosition();
    public void setPosition(Position position);
    public Role getRole();
    public Role getRole(TeamJam tj);
    public void setRole(Role role);
    public void setRoleToBase();
    public Role getBaseRole();
    public void setBaseRole(Role base);
    public void updateEligibility();
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);
    public String getFlags();
    public void setFlags(String flags);
    public Penalty getPenalty(String num);
    public List<Penalty> getUnservedPenalties();
    public boolean hasUnservedPenalties();
    public void mergeInto(PreparedSkater preparedSkater);

    public static Collection<Property<?>> props = new ArrayList<>();
    public static Collection<Property<?>> preparedProps = new ArrayList<>(); // also present on PreparedTeam.Skater

    public static final Value<PreparedSkater> PREPARED_SKATER =
        new Value<>(PreparedSkater.class, "PreparedSkater", null, props);
    public static final Value<String> NAME = new Value<>(String.class, "Name", "", preparedProps);
    public static final Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "", preparedProps);
    public static final Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null, props);
    public static final Value<String> CURRENT_BOX_SYMBOLS = new Value<>(String.class, "CurrentBoxSymbols", "", props);
    public static final Value<String> CURRENT_PENALTIES = new Value<>(String.class, "CurrentPenalties", "", props);
    public static final Value<Position> POSITION = new Value<>(Position.class, "Position", null, props);
    public static final Value<Role> ROLE = new Value<>(Role.class, "Role", null, props);
    public static final Value<Role> BASE_ROLE = new Value<>(Role.class, "BaseRole", null, props);
    public static final Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false, props);
    public static final Value<String> FLAGS = new Value<>(String.class, "Flags", "", preparedProps);
    public static final Value<String> PRONOUNS = new Value<>(String.class, "Pronouns", "", preparedProps);

    public static final Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding", props);

    public static final NumberedChild<Penalty> PENALTY = new NumberedChild<>(Penalty.class, "Penalty", props);

    public static final String FO_EXP_ID = "0";
}
