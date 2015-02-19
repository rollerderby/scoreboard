
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Copyright (C) 2013 Rob Thomas <xrobau@gmail.com>
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
		var aspect16x9 = _windowFunctions.get16x9Dimensions();
		div.css(aspect16x9).css("fontSize", aspect16x9.height);
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
			$("div.Clock.JLT").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
		} else if (tR) {
			$("div.Clock.JLT").removeClass("ShowLineup ShowJam").addClass("ShowTimeout");
		} else if (lR) {
			$("div.Clock.JLT").removeClass("ShowJam ShowTimeout").addClass("ShowLineup");
		} else if (iR) {
			$("div.Clock.JLT").removeClass("ShowJam ShowTimeout ShowLineup");
		} else {
			$("div.Clock.JLT").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
		}

		if (pR) {
			$("div.Clock.PI").removeClass("ShowIntermission").addClass("ShowPeriod");
		} else if (iR && !jR && !lR && !tR) {
			if (iN == 2) { // Hide intermission clock too for Final
				$("div.Clock.PI").removeClass("ShowPeriod ShowIntermission");
			} else {
				$("div.Clock.PI").removeClass("ShowPeriod").addClass("ShowIntermission");
			}
		} else {
			$("div.Clock.PI").removeClass("ShowIntermission").addClass("ShowPeriod");
		}
	};

	$.each( [ "1", "2" ], function(i, t) {
		var team = $sb("ScoreBoard.Team("+t+")");
		team.$sb("AlternateName(overlay).Name")
			.$sbElement("#MainBar>div.Team"+t+">div.Name>a.AlternateName");
		team.$sb("Name")
			.$sbElement("#MainBar>div.Team"+t+">div.Name>a.Name");
		team.$sb("Score")
			.$sbElement("#MainBar>div.Team"+t+">div.Score>a");
		team.$sbBindAddRemoveEach("AlternateName", function(event, node) {
			if ($sb(node).$sbId == "overlay")
				$sb(node).$sb("Name").$sbBindAndRun("sbchange", function(event, val) {
					$("#MainBar>div.Team"+t+">div.Name")
						.toggleClass("AlternateName", ($.trim(val) != ""));
				});
		}, function(event, node) {
			if ($sb(node).$sbId == "overlay")
				$("#MainBar>div.Team"+t+">div.Name").removeClass("AlternateName");
		});

		// Pulsate Timeouts if they're currently active. They'll be hidden in manageTimeoutImages
		$.each( [ 0, 1, 2 ], function(i, n) {
			setupPulsate( 
				function() { return (
					team.$sb("Timeouts").$sbIs(n) &&
						$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
						!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
						$sb("ScoreBoard.TimeoutOwner").$sbIs(t)); },
				$("#MainBar>div.Team"+t+">div.Timeouts>div.Timeout"+(n+1)),
				1000
			);
		});

		// Pulsate OR buttons.
		setupPulsate(
			function() { return (
				$sb("ScoreBoard.OfficialReview").$sbIsTrue() &&
					$sb("ScoreBoard.TimeoutOwner").$sbIs(t)); },
			$("#MainBar>div.Team"+t+">div.Timeouts>div.OfficialReview>div"),
			1000
		);

		// Timeout images
		team.$sb("Timeouts").$sbBindAndRun("sbchange", function() { manageTimeoutImages(t); });
		team.$sb("OfficialReviews").$sbBindAndRun("sbchange", function() { manageTimeoutImages(t); });

		// Lead Changes
		team.$sb("LeadJammer").$sbBindAndRun("sbchange", function(event, val) {
			s = "#MainBar>div.Team"+t+">div.Lead";
			$v = $(s);
			console.log('lead change ' + t + '/' + val);
			console.log(s);
			console.log($v);
			$v.toggleClass("IsLead", isTrue(val), 1000);
		});
	});

	$.each( [ "Period", "Jam" ], function(i, c) {
		$sb("ScoreBoard.Clock("+c+").Number")
			.$sbElement("#MainBar>div.Numbers>div."+c+".Number>a.Number");
	});

	$.each( [ "Period", "Jam", "Lineup", "Timeout", "Intermission" ], function(i, c) {
		$sb("ScoreBoard.Clock("+c+").Time")
			.$sbElement("#MainBar>div.Clock>div."+c+".Time>a.Time", {
				sbelement: { convert: _timeConversions.msToMinSec }
			});
		$sb("ScoreBoard.Clock("+c+").Running").$sbBindAndRun("sbchange", showClocks);
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
});

function manageStatusBar() {
	// This is called when pretty much anything changes, and updates the status string 
	var statusString = "Stand By";

	if ($sb("Scoreboard.InOvertime").$sbIsTrue()) {
		statusString = "Overtime";
	} else if ($sb("Scoreboard.Clock(Jam).Running").$sbIsTrue()) {
		statusString = "Jam";
	} else if ($sb("Scoreboard.Clock(Timeout).Running").$sbIsTrue()) { 
		// Who's timeout is it?
		if (!$sb("ScoreBoard.TimeoutOwner").$sbGet()) {
			statusString = "Timeout";
		} else if ($sb("ScoreBoard.OfficialReview").$sbIsTrue()) {
			statusString = "Review";
		} else {
			statusString = "Team T/O";
		}			
	} else if ($sb("Scoreboard.Clock(Lineup).Running").$sbIsTrue()) {
		statusString = "Lineup";
	} else if ($sb("Scoreboard.Clock(Intermission).Running").$sbIsTrue()) {
		var iNum = $sb("ScoreBoard.Clock(Intermission).Number").$sbGet();
		var official = $sb("ScoreBoard.OfficialScore").$sbIsTrue();
		if (iNum == 0)
			statusString = "Prebout";
		else if (iNum == 1)
			statusString = "Halftime";
		else if (iNum == 2)
			statusString = (official ? "Final" : "Unofficial");
	}

	// If the statusString has changed, fade out the old, and in the new
	var statusBar = $("#MainBar>div.Numbers>div.Status>a");
	console.log(statusBar);
	console.log(statusString);
	console.log(statusBar.data('current'));
	if (statusBar.data('current') != statusString) {
		statusBar.data('current', statusString);
		if (statusBar.data('fadingOut') != true) {
			statusBar.queue(function(next) { $(this).data('fadingOut', true); next(); });
			statusBar.animate({opacity: 0}, 500, function() {
				$(this).data('fadingOut', false);
				$(this).text($(this).data('current'));
			});
			statusBar.animate({opacity: 1}, 500);
		}
	}
}

function manageTimeoutImages(t) {
	// Called when something changes in relation to timeouts.
	var team = $sb("ScoreBoard.Team("+t+")");
	var teamDiv = $("#MainBar>div.Team"+t);
	teamDiv.toggleClass("HasOfficialReview", team.$sb("OfficialReviews").$sbGet() > 0);
	$.each( [ 1, 2, 3 ], function(i, n) {
		teamDiv.toggleClass("HasTimeout"+n, team.$sb("Timeouts").$sbGet() >= n);
	});
}

////////////
// Animation
////////////

function setupPulsate(pulseCondition, pulseTarget, pulsePeriod) {
	var doPulse = function(next) {
		if (pulseCondition())
			pulseTarget
				.addClass("Pulse", (pulsePeriod/2))
				.removeClass("Pulse", (pulsePeriod/2));
		else
			pulseTarget.first().delay(500);
		pulseTarget.first().queue(doPulse);
		next();
	};
	doPulse($.noop);
}	 
