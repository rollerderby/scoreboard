package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

/**
 * A tournament on several tracks: one scoreboard is the tournament server, where the games are prepared. Every
 * track's scoreboard pulls its games from there and pushes them back while they run, so the tournament server always
 * has every game. When a game is over, the track hands it over so the head NSO can review it on the tournament server.
 */
public interface Tracks extends ScoreBoardEventProvider {
    /** Start pulling the game list and pushing games, on a track. */
    public void start();

    /** One round on a track: refresh the tournament server's game list and push the games that changed. */
    public void tick();

    /** Answers a request from a track. Throws a TracksException for a request that can't be done. */
    public Map<String, Object> handle(String action, Map<String, String> params, Map<String, Object> body);

    /** On the tournament server: the live status a track sends over its WebSocket. Returns an error, or null. */
    public String trackStatus(String track, Map<String, Object> status, Object connection, String remoteAddr);

    /** On the tournament server: the track's WebSocket closed. */
    public void trackDisconnected(String track, Object connection);

    /** Why a change to this key isn't allowed (the game runs on another scoreboard), null if it is allowed. */
    public String readOnlyReason(String key);

    public static Collection<Property<?>> props = new ArrayList<>();

    /** Last contact with the tournament server, on a track: empty when it worked, the error otherwise. */
    public static final Value<String> SERVER_ERROR = new Value<>(String.class, "ServerError", "", props);
    public static final Value<Long> SERVER_CONTACT = new Value<>(Long.class, "ServerContact", 0L, props);
    /** On a track: whether its WebSocket to the tournament server is open. */
    public static final Value<Boolean> SERVER_CONNECTED = new Value<>(Boolean.class, "ServerConnected", false, props);

    public static final Child<ServerGame> SERVER_GAME = new Child<>(ServerGame.class, "ServerGame", props);
    public static final Child<PulledGame> PULLED_GAME = new Child<>(PulledGame.class, "PulledGame", props);
    public static final Child<TrackGame> TRACK_GAME = new Child<>(TrackGame.class, "TrackGame", props);
    public static final Child<Track> TRACK = new Child<>(Track.class, "Track", props);

    public static final String SETTING_TOURNAMENT_SERVER = "ScoreBoard.Tracks.TournamentServer";
    public static final String SETTING_SERVER = "ScoreBoard.Tracks.Server";
    public static final String SETTING_TRACK = "ScoreBoard.Tracks.Track";

    public static final String INFO_TRACK = "Track";

    /** On a track: a game on the tournament server. */
    public static interface ServerGame extends ScoreBoardEventProvider {
        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
        public static final Value<String> GAME_NO = new Value<>(String.class, "GameNo", "", props);
        public static final Value<String> DATE = new Value<>(String.class, "Date", "", props);
        public static final Value<String> START_TIME = new Value<>(String.class, "StartTime", "", props);
        public static final Value<String> TRACK = new Value<>(String.class, "Track", "", props);
        public static final Value<String> STATE = new Value<>(String.class, "State", "", props);
        public static final Value<String> RUNS_ON = new Value<>(String.class, "RunsOn", "", props);
        public static final Value<Boolean> HANDED_OVER = new Value<>(Boolean.class, "HandedOver", false, props);

        public static final Command PULL = new Command("Pull", props);
    }

    /** On a track: a game pulled from the tournament server. */
    public static interface PulledGame extends ScoreBoardEventProvider {
        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
        public static final Value<String> SERVER = new Value<>(String.class, "Server", "", props);
        public static final Value<String> TRACK = new Value<>(String.class, "Track", "", props);
        public static final Value<Long> PULLED = new Value<>(Long.class, "Pulled", 0L, props);
        public static final Value<Long> PUSHED = new Value<>(Long.class, "Pushed", 0L, props);
        public static final Value<String> ERROR = new Value<>(String.class, "Error", "", props);
        public static final Value<Long> HANDED_OVER = new Value<>(Long.class, "HandedOver", 0L, props);
        /** Taken back after a handover: it isn't handed over again by itself. */

        public static final Command PUSH = new Command("Push", props);
        public static final Command HAND_OVER = new Command("HandOver", props);
        public static final Command TAKE_BACK = new Command("TakeBack", props);
        public static final Command FORGET = new Command("Forget", props);
    }

    /** On the tournament server: a game a track has pulled. */
    public static interface TrackGame extends ScoreBoardEventProvider {
        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
        public static final Value<String> TRACK = new Value<>(String.class, "Track", "", props);
        public static final Value<Long> PULLED = new Value<>(Long.class, "Pulled", 0L, props);
        public static final Value<Long> PUSHED = new Value<>(Long.class, "Pushed", 0L, props);
        public static final Value<Long> HANDED_OVER = new Value<>(Long.class, "HandedOver", 0L, props);

        /** Let another track pull the game, e.g. when the track's scoreboard broke down. */
        public static final Command RELEASE = new Command("Release", props);
    }

    /** On the tournament server: a track's scoreboard, as it reports itself over its WebSocket. */
    public static interface Track extends ScoreBoardEventProvider {
        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<Boolean> CONNECTED = new Value<>(Boolean.class, "Connected", false, props);
        public static final Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "", props);
        public static final Value<Long> LAST_SEEN = new Value<>(Long.class, "LastSeen", 0L, props);
        public static final Value<String> GAME = new Value<>(String.class, "Game", "", props);
        public static final Value<String> GAME_NAME = new Value<>(String.class, "GameName", "", props);
        public static final Value<String> GAME_STATE = new Value<>(String.class, "GameState", "", props);
        public static final Value<Integer> PERIOD = new Value<>(Integer.class, "Period", 0, props);
        public static final Value<Integer> JAM = new Value<>(Integer.class, "Jam", 0, props);
        public static final Value<Boolean> IN_JAM = new Value<>(Boolean.class, "InJam", false, props);
        /** The clock that matters now: Jam, Lineup, Timeout, Intermission, or empty. */
        public static final Value<String> CLOCK = new Value<>(String.class, "Clock", "", props);
        public static final Value<Long> CLOCK_TIME = new Value<>(Long.class, "ClockTime", 0L, props);
        public static final Value<Long> PERIOD_CLOCK = new Value<>(Long.class, "PeriodClock", 0L, props);
        public static final Value<String> TEAM_1 = new Value<>(String.class, "Team1", "", props);
        public static final Value<String> TEAM_2 = new Value<>(String.class, "Team2", "", props);
        public static final Value<Integer> SCORE_1 = new Value<>(Integer.class, "Score1", 0, props);
        public static final Value<Integer> SCORE_2 = new Value<>(Integer.class, "Score2", 0, props);
        public static final Value<Integer> TIMEOUTS_1 = new Value<>(Integer.class, "Timeouts1", 0, props);
        public static final Value<Integer> TIMEOUTS_2 = new Value<>(Integer.class, "Timeouts2", 0, props);
        public static final Value<Integer> OFFICIAL_REVIEWS_1 =
            new Value<>(Integer.class, "OfficialReviews1", 0, props);
        public static final Value<Integer> OFFICIAL_REVIEWS_2 =
            new Value<>(Integer.class, "OfficialReviews2", 0, props);
        public static final Value<Boolean> RETAINED_OFFICIAL_REVIEW_1 =
            new Value<>(Boolean.class, "RetainedOfficialReview1", false, props);
        public static final Value<Boolean> RETAINED_OFFICIAL_REVIEW_2 =
            new Value<>(Boolean.class, "RetainedOfficialReview2", false, props);
        /** Lead jammer, as the main scoreboard shows it. */
        public static final Value<Boolean> LEAD_1 = new Value<>(Boolean.class, "Lead1", false, props);
        public static final Value<Boolean> LEAD_2 = new Value<>(Boolean.class, "Lead2", false, props);
        /** Whose timeout runs, as the main scoreboard shows it: 1, 2, O (officials) or empty. */
        public static final Value<String> TIMEOUT_OWNER = new Value<>(String.class, "TimeoutOwner", "", props);
        public static final Value<Boolean> OFFICIAL_REVIEW = new Value<>(Boolean.class, "OfficialReview", false, props);

        public static final Command REMOVE = new Command("Remove", props);
    }

    public static class TracksException extends RuntimeException {
        public TracksException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() { return status; }

        private final int status;
    }
}
