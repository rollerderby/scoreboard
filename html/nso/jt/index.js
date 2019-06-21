
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Penalty Timing (C) 2013 Rob Thomas (The G33k) <xrobau@gmail.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

$(function() {
	setupJamControlPage();
	setupPeriodTimePage();

	WS.AutoRegister();
	WS.Connect();
});

function setupJamControlPage() {
	$("#JamControlPage button.StartJam").click(function() { WS.Set("ScoreBoard.StartJam", true); });
	$("#JamControlPage button.StopJam").click(function() { WS.Set("ScoreBoard.StopJam", true); });
	$("#JamControlPage button.Timeout").click(function() { WS.Set("ScoreBoard.Timeout", true); });
	$("#JamControlPage button.Undo").click(function() { WS.Set("ScoreBoard.ClockUndo", true); });
	$("#JamControlPage div.Timeout button.Official").click(function() { WS.Set("ScoreBoard.OfficialTimeout", true); });
	$("#JamControlPage div.Timeout button.Team1").click(function() { WS.Set("ScoreBoard.Team(1).Timeout", true); });
	$("#JamControlPage div.OfficialReview button.Team1").click(function() { WS.Set("ScoreBoard.Team(1).OfficialReview", true); });
	$("#JamControlPage div.Timeout button.Team2").click(function() { WS.Set("ScoreBoard.Team(2).Timeout", true); });
	$("#JamControlPage div.OfficialReview button.Team2").click(function() { WS.Set("ScoreBoard.Team(2).OfficialReview", true); });

	WS.Register(["ScoreBoard.Team(*).Name", "ScoreBoard.Team(*).AlternateName(operator)"], function(k, v) {
		var name = WS.state["ScoreBoard.Team("+k.Team+").AlternateName(operator)"];
		name = name || WS.state["ScoreBoard.Team("+k.Team+").Name"];
		$(".Name.Team"+k.Team).text(name);
	});

	// Setup clocks
	var showJamControlClock = function(clock) {
		$("#JamControlPage div.Time").not("."+clock+"Time").hide().end()
			.filter("."+clock+"Time").show();
	};
	// In case no clocks are running now, default to showing only Jam
	showJamControlClock("Jam");

	WS.Register("ScoreBoard.Clock(*).Running", function(k, v) {
		$("#JamControlPage span.ClockBubble."+k.Clock).toggleClass("Running", isTrue(v));
	});
	$.each( [ "Start", "Stop", "Timeout", "Undo" ], function(i, button) {
		WS.Register("ScoreBoard.Settings.Setting(ScoreBoard.Button."+button+"Label)", function(k, v) {
			$("#JamControlPage span."+button+"Label").text(v);
		});
	});
	WS.Register("ScoreBoard.Clock(*).Running", function(k, v) {
		if (isTrue(v)) {
			showJamControlClock(k.Clock);
		}
	});
}

function setupPeriodTimePage() {
	$("#PeriodTimePage button.TimeDown").click(function() {
		WS.Set("ScoreBoard.Clock(Period).Time", -1000, "change");
	});
	$("#PeriodTimePage button.TimeUp").click(function() {
		WS.Set("ScoreBoard.Clock(Period).Time", 1000, "change");
	});
	$("#PeriodTimePage button.SetTime").click(function() {
		var t = $("#PeriodTimePage input:text.SetTime");
		WS.Set("ScoreBoard.Clock(Period).Time", _timeConversions.minSecToMs(t.val()));
	});
}


function toTime(k, v) {
	return _timeConversions.msToMinSecNoZero(v);
}
//# sourceURL=controls\jt\index.js
