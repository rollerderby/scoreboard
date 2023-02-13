package com.carolinarollergirls.scoreboard.rules;

public enum Rule {
    NUMBER_PERIODS(new IntegerRule("Period.Number", "Number of periods", 2)),
    PERIOD_DURATION(new TimeRule("Period.Duration", "Duration of a period", "30:00")),
    PERIOD_DIRECTION(new BooleanRule("Period.ClockDirection", "Which way should the period clock count?", true,
                                     "Count Down", "Count Up")),
    PERIOD_END_BETWEEN_JAMS(new BooleanRule("Period.EndBetweenJams", "When can a period end?", true,
                                            "Anytime outside a jam", "Only on jam end")),

    JAM_NUMBER_PER_PERIOD(new BooleanRule("Jam.ResetNumberEachPeriod", "How to handle Jam Numbers", true,
                                          "Reset each period", "Continue counting")),
    JAM_DURATION(new TimeRule("Jam.Duration", "Maximum duration of a jam", "2:00")),
    JAM_DIRECTION(new BooleanRule("Jam.ClockDirection", "Which way should the jam clock count?", true, "Count Down",
                                  "Count Up")),
    SUDDEN_SCORING(new BooleanRule("Jam.SuddenScoring", "Use JRDA Sudden Scoring rule?", false, "Enabled", "Disabled")),
    SUDDEN_SCORING_MIN_POINTS_DIFFERENCE(
        new IntegerRule("Jam.SuddenScoringMinPointsDifference",
                        "Minimal score difference at halftime at which sudden scoring is activated.", 150)),
    SUDDEN_SCORING_MAX_TRAILING_POINTS(
        new IntegerRule("Jam.SuddenScoringMaxTrainingPoints",
                        "Maximum points the trailing team may have at halftime to trigger sudden scoring.", 25)),
    SUDDEN_SCORING_JAM_DURATION(new TimeRule("Jam.SuddenScoringDuration",
                                             "Maximum duration of a jam when sudden scoring is in effect", "1:00")),
    INJURY_CONTINUATION(new BooleanRule("Jam.InjuryContinuation",
                                        "A jam called for injury can be followed by a continuation jam.", false,
                                        "Enabled", "Disabled")),

    LINEUP_DURATION(new TimeRule("Lineup.Duration", "Duration of lineup", "0:30")),
    OVERTIME_LINEUP_DURATION(new TimeRule("Lineup.OvertimeDuration", "Duration of lineup before an overtime jam",
                                          "1:00")),
    LINEUP_DIRECTION(new BooleanRule("Lineup.ClockDirection", "Which way should the lineup clock count?", false,
                                     "Count Down", "Count Up")),

    TTO_DURATION(new TimeRule("Timeout.TeamTODuration", "Duration of a team timeout", "1:00")),
    TIMEOUT_DIRECTION(new BooleanRule("Timeout.ClockDirection", "Which way should the timeout clock count?", false,
                                      "Count Down", "Count Up")),
    STOP_PC_ON_TO(new BooleanRule(
        "Timeout.StopPeriodClockAlways",
        "Stop the period clock on every timeout? If false, the options below control the behaviour per type of timeout.",
        true, "True", "False")),
    STOP_PC_ON_OTO(new BooleanRule("Timeout.StopPeriodClockOnOTO", "Stop the period clock on official timeouts?", false,
                                   "True", "False")),
    STOP_PC_ON_TTO(new BooleanRule("Timeout.StopPeriodClockOnTTO", "Stop the period clock on team timeouts?", false,
                                   "True", "False")),
    STOP_PC_ON_OR(new BooleanRule("Timeout.StopPeriodClockOnOR", "Stop the period clock on official reviews?", false,
                                  "True", "False")),
    STOP_PC_AFTER_TO_DURATION(new TimeRule(
        "Timeout.StopPeriodClockAfterTODuration",
        "Stop the period clock, if a timeout lasts longer than this time. Set to a high value to disable.", "60:00")),
    EXTRA_JAM_AFTER_OTO(new BooleanRule(
        "Timeout.ExtraJamAfterOTO", "Can an OTO cause an extra Jam to be played when there wouldn't be one otherwise?",
        false, "True", "False")),

    INTERMISSION_DURATIONS(new StringRule(
        "Intermission.Durations",
        "List of the duration of intermissions as they appear in the game, separated by commas.", "15:00,60:00")),
    INTERMISSION_DIRECTION(new BooleanRule("Intermission.ClockDirection",
                                           "Which way should the intermission clock count?", true, "Count Down",
                                           "Count Up")),

    NUMBER_TIMEOUTS(new IntegerRule("Team.Timeouts", "How many timeouts each team is granted per game or period", 3)),
    TIMEOUTS_PER_PERIOD(new BooleanRule("Team.TimeoutsPer", "Are timeouts granted per period or per game?", false,
                                        "Period", "Game")),
    NUMBER_REVIEWS(new IntegerRule("Team.OfficialReviews",
                                   "How many official reviews each team is granted per game or period", 1)),
    REVIEWS_PER_PERIOD(new BooleanRule("Team.OfficialReviewsPer",
                                       "Are official reviews granted per period or per game?", true, "Period", "Game")),
    NUMBER_RETAINS(new IntegerRule("Team.MaxRetains",
                                   "How many times per game or period a team can retain an official review", 1)),
    RDCL_PER_HALF_RULES(new BooleanRule(
        "Team.RDCLPerHalfRules",
        "Restrict one TTO to the first two periods and one to the rest of the game. Stretch per period ORs to first two resp. all other periods.",
        false, "Enabled", "Disabled")),
    WFTDA_LATE_SCORE_RULE(new BooleanRule(
        "Score.WftdaLateChangeRule",
        "Score changes after the end of the following jam don't affect the game score. With less than 2 minutes left in the game this applies to changes after the next jam starts.",
        true, "Enabled", "Disabled")),

    PENALTIES_FILE(new StringRule("Penalties.DefinitionFile",
                                  "File that contains the penalty code definitions to be used",
                                  "/config/penalties/wftda2018.json")),
    FO_LIMIT(new IntegerRule(
        "Penalties.NumberToFoulout",
        "After how many penalties a skater has fouled out of the game. Note that the software currently does not support more than 9 penalties per skater.",
        7));

    private Rule(RuleDefinition r) { rule = r; }

    public RuleDefinition getRuleDefinition() { return rule; }
    @Override
    public String toString() {
        return rule.getName();
    }

    RuleDefinition rule;
}
