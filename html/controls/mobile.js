
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Penalty Timing (C) 2013 Rob Thomas (The G33k) <xrobau@gmail.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "ScoreBoard";

$sb(function() {
	setupJamControlPage();
	setupPeriodTimePage();

	$.each( [ "1", "2" ], function(i, t) {
		$sb("ScoreBoard.Team("+t+")").$sbBindAddRemoveEach("AlternateName", function(event, node) {
			if ($sb(node).$sbId == "mobile")
				$sb(node).$sb("Name").$sbBindAndRun("sbchange", function(event2, val) {
					$(".Team"+t+".Name,.Team"+t+".AlternateName")
						.toggleClass("HasAlternateName", $.trim(val) != "");
				});
		});
	});
});

function setupJamControlPage() {
	$sb("ScoreBoard.StartJam").$sbControl("#JamControlPage button.StartJam").val(true);
	$sb("ScoreBoard.StopJam").$sbControl("#JamControlPage button.StopJam").val(true);
	$sb("ScoreBoard.Timeout").$sbControl("#JamControlPage button.Timeout").val(true);
	$sb("ScoreBoard.ClockUndo").$sbControl("#JamControlPage button.Undo").val(true);
	$sb("ScoreBoard.Team(1).Timeout").$sbControl("#JamControlPage div.Timeout button.Team1").val(true);
	$sb("ScoreBoard.Team(1).OfficialReview").$sbControl("#JamControlPage div.OfficialReview button.Team1").val(true);
	$sb("ScoreBoard.Team(1).Name").$sbElement("#JamControlPage div.Timeout button.Team1>span.Name");
	$sb("ScoreBoard.Team(1).AlternateName(operator).Name").$sbElement("#JamControlPage div.Timeout button.Team1>span.AlternateName");
	$sb("ScoreBoard.Team(1).Name").$sbElement("#JamControlPage div.OfficialReview button.Team1>span.Name");
	$sb("ScoreBoard.Team(1).AlternateName(operator).Name").$sbElement("#JamControlPage div.OfficialReview button.Team1>span.AlternateName");
	$sb("ScoreBoard.OfficialTimeout").$sbControl("#JamControlPage div.Timeout button.Official").val(true);
	$sb("ScoreBoard.Team(2).Timeout").$sbControl("#JamControlPage div.Timeout button.Team2").val(true);
	$sb("ScoreBoard.Team(2).OfficialReview").$sbControl("#JamControlPage div.OfficialReview button.Team2").val(true);
	$sb("ScoreBoard.Team(2).Name").$sbElement("#JamControlPage div.Timeout button.Team2>span.Name");
	$sb("ScoreBoard.Team(2).AlternateName(operator).Name").$sbElement("#JamControlPage div.Timeout button.Team2>span.AlternateName");
	$sb("ScoreBoard.Team(2).Name").$sbElement("#JamControlPage div.OfficialReview button.Team2>span.Name");
	$sb("ScoreBoard.Team(2).AlternateName(operator).Name").$sbElement("#JamControlPage div.OfficialReview button.Team2>span.AlternateName");

	$.each( [ "Period", "Jam", "Timeout" ], function(i, clock) {
		$sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", function(event, value) {
			$("#JamControlPage span.ClockBubble."+clock).toggleClass("Running", isTrue(value));
		});
	});
	$.each( [ "Start", "Stop", "Timeout", "Undo" ], function(i, button) {
		$sb("ScoreBoard.Settings.Setting(ScoreBoard.Button."+button+"Label)").$sbBindAndRun("sbchange", function(event, val) {
			$("#JamControlPage span."+button+"Label").html(val);
		});
	});
	
	// Period number
	$sb("ScoreBoard.Clock(Period).Number").$sbElement("#JamControlPage div.PeriodNumber a.Number");

	// Period Clock
	$sb("ScoreBoard.Clock(Period).Time").$sbElement("#JamControlPage div.PeriodTime a.Time", { sbelement: {
			convert: _timeConversions.msToMinSec
	}});
	
	var showJamControlClock = function(clock) {
		$("#JamControlPage div.Time").not("."+clock+"Time").hide().end()
			.filter("."+clock+"Time").show();
	};
	// In case no clocks are running now, default to showing only Jam
	showJamControlClock("Jam");

	// Setup clocks
	$.each([ "Jam", "Lineup", "Timeout" ], function(i, clock) {
		$sb("Scoreboard.Clock("+clock+").Time").$sbElement("#JamControlPage div."+clock+"Time a.Time", { sbelement: {
			convert: _timeConversions.msToMinSec
		}});
		$sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", function(e, v) {
			if (isTrue(v))
				showJamControlClock(clock);
		});
	});
}

function setupPeriodTimePage() {
	var time = $sb("ScoreBoard.Clock(Period).Time");

	time.$sbElement("#PeriodTimePage a.Time", { sbelement: {
		convert: _timeConversions.msToMinSec
	}});

	time.$sbControl("#PeriodTimePage button.TimeDown", { sbcontrol: {
		sbSetAttrs: { change: true }
	}});
	time.$sbControl("#PeriodTimePage button.TimeUp", { sbcontrol: {
		sbSetAttrs: { change: true }
	}});

	time.$sbControl("#PeriodTimePage input:text.SetTime,#PeriodTimePage button.SetTime", {
		sbcontrol: {
			convert: _timeConversions.minSecToMs,
			delayupdate: true,
			noSetControlValue: true
		}
	});
}

//# sourceURL=controls\mobile.js
