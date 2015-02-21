/**
 * Copyright (C) 2008-2013 Mr Temper <MrTemper@CarolinaRollergirls.com>, Rob Thomas, and WrathOfJon <crgscorespam@sacredregion.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "ScoreBoard";

function setupMainDiv(div) {
	div.css({ position: "fixed" });


	_crgUtils.bindAndRun($(window), "resize", function() {
		var aspect4x3 = _windowFunctions.get4x3Dimensions();
		div.css(aspect4x3).css("fontSize", aspect4x3.height);
	});

}

$sb(function() {
	setupMainDiv($("#mainDiv"));

	var showClocks = function() {
		var pR = $sb("ScoreBoard.Clock(Period).Running").$sbIsTrue();
		var jR = $sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue();
		var lR = $sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue();
		var tR = $sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue();
		var iR = $sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue();
		var iN = $sb("ScoreBoard.Clock(Intermission).Number").$sbGet();

		if (jR) {
			$("a.ClockJLT").closest("div").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
		} else if (tR) {
			$("a.ClockJLT").closest("div").removeClass("ShowLineup ShowJam").addClass("ShowTimeout");
		} else if (lR) {
			$("a.ClockJLT").closest("div").removeClass("ShowJam ShowTimeout").addClass("ShowLineup");
		} else if (iR) {
			$("a.ClockJLT").closest("div").removeClass("ShowJam ShowTimeout ShowLineup");
		} else {
			$("a.ClockJLT").closest("div").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
		}

		if (pR) {
			$("a.ClockPI").closest("div").removeClass("ShowIntermission").addClass("ShowPeriod");
		} else if (iR && !jR && !lR && !tR) {
			if (iN == 2) { // Hide intermission clock too for Final
				$("a.ClockPI").closest("div").removeClass("ShowPeriod ShowIntermission");
			} else {
				$("a.ClockPI").closest("div").removeClass("ShowPeriod").addClass("ShowIntermission");
			}
		} else {
			$("a.ClockPI").closest("div").removeClass("ShowIntermission").addClass("ShowPeriod");
		}
	};

	$.each( [ "1", "2" ], function(i, team) {
		$sb("ScoreBoard.Team("+team+").Name").$sbElement("#Team"+team+"Name>a.Name");
		_crgUtils.bindColors("ScoreBoard.Team("+team+")", "overlay", $("#Team" + team + "Name"));
		$sb("ScoreBoard.Team("+team+").Score").$sbElement("#Team"+team+"Score>a");
		$sb("ScoreBoard.Team("+team+")").$sbBindAddRemoveEach("AlternateName", function(event, node) {
			if ($sb(node).$sbId == "overlay")
				$sb(node).$sb("Name").$sbBindAndRun("sbchange", function(event, val) {
					val = $.trim(val);
					$("#Team"+team+"Name>a.AlternateName").html(val);
					$("#Team"+team+"Name").toggleClass("AlternateName", (val != ""));
				});
		}, function(event, node) {
			if ($sb(node).$sbId == "overlay")
				$("#Team"+team+"Name").removeClass("AlternateName");
		});

		// Pulsate Timeouts if they're currently active. They'll be hidden in manageTimeoutImages
		$.each( [ 0, 1, 2 ], function(x, i) {
			setupPulsate(
					function() { return (
							$sb("ScoreBoard.Team(1).Timeouts").$sbGet() == i &&
							$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
							!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
							$sb("ScoreBoard.TimeoutOwner").$sbGet() == 1); },
							$("#TCDGT1T"+(i+1)),
							1000
						);
			setupPulsate(
						function() { return (
								$sb("ScoreBoard.Team(2).Timeouts").$sbGet() == i &&
							$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
							!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
							$sb("ScoreBoard.TimeoutOwner").$sbGet() == 2); },
								$("#TCDGT2T"+(i+1)),
								1000
							);
		});

		// Pulsate OR buttons.
		$.each( [ 1, 2 ], function(x, i) {
			setupPulsate(
				function() { return (
						$sb("ScoreBoard.OfficialReview").$sbIsTrue() &&
						$sb("ScoreBoard.TimeoutOwner").$sbGet() == i) },
					$("#TCDGT"+i+"OR"),
					1000
			);
		});
	});

	$sb("ScoreBoard.Clock(Period).Number").$sbElement("#ClockPeriodNumber>a>span.Number");

	$sb("ScoreBoard.Clock(Jam).Number").$sbElement("#ClockJamNumber>a>span.Number");

	$.each( [ "Period", "Intermission", "Jam", "Lineup", "Timeout" ], function(i, clock) {
		$sb("ScoreBoard.Clock("+clock+").Time").$sbElement("#Clock"+clock+"Time>a", {
			sbelement: { convert: _timeConversions.msToMinSec } });
		$sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClocks);
	});
	// This allows hiding the intermission clock during Final.
	$sb("ScoreBoard.Clock(Intermission).Number").$sbBindAndRun("sbchange", showClocks);

	// Statusbar text.
	var statusTriggers = $sb("ScoreBoard.Clock(Jam).Running")
		.add($sb("ScoreBoard.Clock(Timeout).Running"))
		.add($sb("ScoreBoard.Clock(Lineup).Running"))
		.add($sb("ScoreBoard.Clock(Intermission).Running"))
		.add($sb("ScoreBoard.Clock(Intermission).Number"))
		.add($sb("ScoreBoard.TimeoutOwner"))
		.add($sb("ScoreBoard.OfficialReview"))
		.add($sb("ScoreBoard.OfficialScore"))
		.add($sb("ScoreBoard.InOvertime"));
	_crgUtils.bindAndRun(statusTriggers, "sbchange", function() { manageStatusBar(); });

	// Timeout images
	var timeoutTriggers = $sb("ScoreBoard.Team(1).OfficialReviews")
		.add($sb("ScoreBoard.Team(1).Timeouts"))
		.add($sb("ScoreBoard.Team(2).OfficialReviews"))
		.add($sb("ScoreBoard.Team(2).Timeouts"))
		.add($sb("ScoreBoard.TimeoutOwner"));
	_crgUtils.bindAndRun(timeoutTriggers, "sbchange", function() { manageTimeoutImages(); });

	// Lead Changes
	$.each([ 1, 2 ], function(i, t) {
		$sb("ScoreBoard.Team("+t+").LeadJammer").$sbBindAndRun("sbchange", function(event, val) {
			$("#TCDGT"+t+"LD").toggleClass("Show", val == "Lead", 1000);
		});
	});
});




function manageStatusBar() {
	// This is called when pretty much anything changes,	and updates the status string
	var statusString = "Stand By";

	if ($sb("Scoreboard.InOvertime").$sbIsTrue()) {
		statusString = "Overtime";
	} else if ($sb("Scoreboard.Clock(Jam).Running").$sbIsTrue()) {
		statusString = "JAM";
	} else if ($sb("Scoreboard.Clock(Timeout).Running").$sbIsTrue()) {
		// Who's timeout is it?
		if (!$sb("ScoreBoard.TimeoutOwner").$sbGet()) {
			statusString = "Timeout";
		} else if ($sb("ScoreBoard.OfficialReview").$sbIsTrue()) {
			statusString = "Official Review";
		} else {
			statusString = "Team Timeout";
		}
	} else if ($sb("Scoreboard.Clock(Lineup).Running").$sbIsTrue()) {
		statusString = "Lineup";
	} else if ($sb("Scoreboard.Clock(Intermission).Running").$sbIsTrue()) {
		var iNum = $sb("ScoreBoard.Clock(Intermission).Number").$sbGet();
		var official = $sb("ScoreBoard.OfficialScore").$sbIsTrue();
		if (iNum == 0)
			statusString = "Time To Derby";
		else if (iNum == 1)
			statusString = "Halftime";
		else if (iNum == 2)
			statusString = (official ? "Final" : "Unofficial");
	}

	// If the statusString has changed, fade out the old, and in the new
	if ($("#StatusBar>a").data('current') != statusString) {
		$("#StatusBar>a").data('current', statusString);
		$("#StatusBar>a").animate({opacity: 0}, 500, function() {
			$(this).text(statusString);})
			.animate({opacity: 1}, 500);
	}
	// $("#StatusBar>a").html(statusString);

}

function manageTimeoutImages() {
	// Called when something changes in relation to timeouts.
	$.each( [ 1, 2 ], function(x, i) {
		var thisTeam = $sb("ScoreBoard.Team("+i+")");
		var pageHTMLID = "#TCDGT"+i;
		// Have they one OR?
		if (thisTeam.$sb("OfficialReviews").$sbGet() == 0) {
			// Hide it
			$(pageHTMLID+"OR").hide();
		} else {
			// Show their OR Box
			$(pageHTMLID+"OR").show();
		}

		// How's their timeouts looking?
		var numTOs = thisTeam.$sb("Timeouts").$sbGet();
		for ( var timeout = 1; timeout <= 3; timeout++ ) {
			if (numTOs >= timeout )
				$(pageHTMLID+"T"+timeout).show();
			else
				$(pageHTMLID+"T"+timeout).hide();
		}
	});
}

////////////
// Animation
////////////

function setupPulsate(pulseCondition, pulseTarget, pulsePeriod) {
	var doPulse = function(next) {
		if (pulseCondition())
			pulseTarget
				.show()
				.animate({ opacity: 0 }, (pulsePeriod/2), "linear")
				.animate({ opacity: 1 }, (pulsePeriod/2), "linear");
		else
			pulseTarget.delay(500);
		pulseTarget.queue(doPulse);
		next();
	};
	doPulse($.noop);
}
