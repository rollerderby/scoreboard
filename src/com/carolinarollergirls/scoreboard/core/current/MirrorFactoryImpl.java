package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentBoxTripImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentClockImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentExpulsionImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentFieldingImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentJamImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentOfficialImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentPenaltyImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentPeriodImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentPositionImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentScoringTripImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentSkaterImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentTeamImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentTeamJamImpl;
import com.carolinarollergirls.scoreboard.core.current.CurrentGameImpl.CurrentTimeoutImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Expulsion;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.event.MirrorFactory;
import com.carolinarollergirls.scoreboard.event.MirrorScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class MirrorFactoryImpl implements MirrorFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T>
    createMirror(ScoreBoardEventProvider parent, T mirrored) {
        if (mirrored instanceof Clock) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentClockImpl(parent, (Clock) mirrored);
        }
        if (mirrored instanceof Team) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentTeamImpl(parent, (Team) mirrored);
        }
        if (mirrored instanceof Skater) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentSkaterImpl(parent, (Skater) mirrored);
        }
        if (mirrored instanceof Penalty) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentPenaltyImpl(parent, (Penalty) mirrored);
        }
        if (mirrored instanceof Position) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentPositionImpl(parent, (Position) mirrored);
        }
        if (mirrored instanceof BoxTrip) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentBoxTripImpl(parent, (BoxTrip) mirrored);
        }
        if (mirrored instanceof Period) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentPeriodImpl(parent, (Period) mirrored);
        }
        if (mirrored instanceof Jam) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentJamImpl(parent, (Jam) mirrored);
        }
        if (mirrored instanceof TeamJam) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentTeamJamImpl(parent, (TeamJam) mirrored);
        }
        if (mirrored instanceof Fielding) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentFieldingImpl(parent, (Fielding) mirrored);
        }
        if (mirrored instanceof ScoringTrip) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentScoringTripImpl(parent, (ScoringTrip) mirrored);
        }
        if (mirrored instanceof Timeout) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentTimeoutImpl(parent, (Timeout) mirrored);
        }
        if (mirrored instanceof Official) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentOfficialImpl(parent, (Official) mirrored);
        }
        if (mirrored instanceof Expulsion) {
            return (MirrorScoreBoardEventProvider<T>) new CurrentExpulsionImpl(parent, (Expulsion) mirrored);
        }
        return null;
    }
}
