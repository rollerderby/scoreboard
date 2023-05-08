package com.carolinarollergirls.scoreboard.core.game;

import java.util.Timer;
import java.util.TimerTask;

import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreAdjustment;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public class ScoreAdjustmentImpl extends ScoreBoardEventProviderImpl<ScoreAdjustment> implements ScoreAdjustment {
    public ScoreAdjustmentImpl(Team t, String id) {
        super(t, id, Team.SCORE_ADJUSTMENT);
        game = t.getGame();
        initReferences();
    }

    private void initReferences() {
        addProperties(props);
        set(JAM_RECORDED, game.getCurrentPeriod().getCurrentJam());
        set(RECORDED_DURING_JAM, game.isInJam());
        set(LAST_TWO_MINUTES, game.isLastTwoMinutes());
        addWriteProtectionOverride(JAM_RECORDED, Source.ANY_FILE);
        addWriteProtectionOverride(RECORDED_DURING_JAM, Source.ANY_FILE);
        addWriteProtectionOverride(LAST_TWO_MINUTES, Source.ANY_FILE);
        setCopy(JAM_NUMBER_RECORDED, this, JAM_RECORDED, Jam.NUMBER, true);
        setCopy(PERIOD_NUMBER_RECORDED, this, JAM_RECORDED, Jam.PERIOD_NUMBER, true);
    }

    @Override
    public void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == OPEN && (Boolean) value == false) {
            addWriteProtection(OPEN);
            Team team = ((Team) parent);
            if (team.get(Team.ACTIVE_SCORE_ADJUSTMENT) == this) { team.set(Team.ACTIVE_SCORE_ADJUSTMENT, null); }
        }
        if (prop == APPLIED_TO && value != null) { ((Team) parent).applyScoreAdjustment(this); }
        if (prop == AMOUNT && source == Source.WS) {
            if (((Integer) value) == 0) {
                delete(source);
            } else {
                closeTimerTask.cancel();
                closeTimer.purge();
                closeTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        set(OPEN, false);
                    }
                };
                closeTimer.schedule(closeTimerTask, 4000);
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        synchronized (coreLock) {
            if (prop == DISCARD) {
                getJamRecorded().getTeamJam(parent.getProviderId()).possiblyChangeOsOffset(getAmount());
                delete(source);
            }
        }
    }

    @Override
    public int getAmount() {
        return get(AMOUNT);
    }

    @Override
    public Jam getJamRecorded() {
        return get(JAM_RECORDED);
    }

    @Override
    public boolean isRecordedInJam() {
        return get(RECORDED_DURING_JAM);
    }

    @Override
    public boolean isRecordedLastTwoMins() {
        return get(LAST_TWO_MINUTES);
    }

    @Override
    public ScoringTrip getTripAppliedTo() {
        return get(APPLIED_TO);
    }

    private Game game;

    private Timer closeTimer = new Timer();
    private TimerTask closeTimerTask = new TimerTask() {
        @Override
        public void run() {} // dummy, so the variable is not
                             // null at the first score entry
    };
}
