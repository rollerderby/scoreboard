/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


// Set animation interval period (33 ~= 30fps)
$.fx.interval = 33;

var clockIds = [
	"Period", "Period_small",
	"Jam", "Jam_small",
	"Lineup",
	"Timeout",
	"Intermission"
];
var clockConversions = {
	Period: _timeConversions.msToMinSecNoZero,
	Period_small: _timeConversions.msToMinSecNoZero,
	Jam: _timeConversions.msToMinSecNoZero,
	Lineup: _timeConversions.msToMinSecNoZero,
	Timeout: _timeConversions.msToMinSecNoZero,
	Intermission: _timeConversions.msToMinSecNoZero
};
var animateTime = {
	sponsorOut: 990, /* time to animate visible sponsor banner out */
	sponsorIn: 1010, /* time to animate next sponsor banner into view (should be more than sponsorOut) */
	clock: 500, /* clocks animate out then in, so the full transition time is 2x this */
	team: 500, /* show/hide logo/name */
	leadjammerPulse: 1000
};


// Main setup function
$sb(function() {
	setupMainDiv($("#mainDiv")); // This needs to be part of scoreboard framework
	var sbViewOptions = $sbThisPage.$sb("ViewOptions");
	if (_windowFunctions.checkParam("preview", "true"))
		sbViewOptions = $sbThisPage.$sb("PreviewOptions");

	if (_windowFunctions.checkParam("videomuted", "true"))
		$("video").prop({ muted: true, volume: "0.0" }); // Not all browsers support muted
	if (_windowFunctions.checkParam("videocontrols", "true"))
		$("video").prop("controls", true);

	sbViewOptions.$sb("View(Image).Src").$sbElement("#imageDiv>img");
	sbViewOptions.$sb("View(Video).Src").$sbElement("#videoDiv>video");
	sbViewOptions.$sb("View(CustomHtml).Src").$sbElement("#htmlDiv>iframe");

	setupSponsorBanners();
	setupTeams();
	setupClocks();

	setupBackgrounds();

	sbViewOptions.$sb("SwapTeams").$sbBindAndRun("sbchange", function(event,value) {
		$("#sbDiv>div.Team,.Timeouts>div.Team,.OfficialReviews>div.Team,.JamPoints>div.Team").toggleClass("SwapTeams", isTrue(value));
	});

	var styleSet = function() {
		var boxStyle = sbViewOptions.$sb("BoxStyle").$sbGet();
		var backgroundStyle = sbViewOptions.$sb("BackgroundStyle").$sbGet();
		var hideJamTotals = isTrue(sbViewOptions.$sb("HideJamTotals").$sbGet());
		$("#mainDiv").removeClass();
		if (boxStyle != "")
			$("#mainDiv").addClass(boxStyle);
		if (backgroundStyle != "")
			$("#mainDiv").addClass(backgroundStyle);
		if (hideJamTotals)
			$("#mainDiv").addClass("HideJamTotals");
	}
	sbViewOptions.$sb("BoxStyle").$sbBindAndRun("sbchange", styleSet);
	sbViewOptions.$sb("BackgroundStyle").$sbBindAndRun("sbchange", styleSet);
	sbViewOptions.$sb("HideJamTotals").$sbBindAndRun("sbchange", styleSet);

	var view = sbViewOptions.$sb("CurrentView");
	view.$sbBindAndRun("sbchange", function(event, value) {
		var showDiv = $("#"+value+"Div");
		if (!showDiv.length)
			showDiv = $("#sbDiv");
		showDiv.children("video").each(function() { this.play(); });
		$("#mainDiv>div.View").not(showDiv).removeClass("Show", 500, function() {
			$(this).children("video").each(function() { this.pause(); });
		});
		showDiv.addClass("Show", 500);
	});
});

// FIXME - needs to be a single call from scoreboard.js
function setupMainDiv(div) {
	div.css({ position: "fixed" });

	_crgUtils.bindAndRun($(window), "resize", function() {
		var aspect4x3 = _windowFunctions.get4x3Dimensions();
		div.css(aspect4x3).css("fontSize", aspect4x3.height);
	});
}

function setupBackgrounds() {
	_crgUtils.bindAndRun($(window), "resize", function() {
		$("div.WhiteBox").each(function() {
			$(this).css("fontSize", $(this).height()+"px");
		});
	});
}


///////////////
// Team control
///////////////

function setupTeams() {
	var timeoutsName = $(".Timeouts>div.Name.TextContainer");
	_autoFit.enableAutoFitText(timeoutsName, { overage: -20 });
	var jamPointsName = $(".JamPoints>div.Name.TextContainer");
	_autoFit.enableAutoFitText(jamPointsName, { overage: -20 });

	$.each( [ "1", "2" ], function() {
		var team = String(this);
		var sbTeam = $sb("ScoreBoard.Team("+team+")");

		var teamJamPoints = $(".JamPoints>div.Team.Team"+team+".Number.TextContainer");
		var teamJamPointsA = $(".JamPoints>div.Team.Team"+team+".Number.TextContainer>a");
		_autoFit.enableAutoFitText(teamJamPoints);
		var teamJamPointsUpdate = function() {
			jamTotal = sbTeam.$sb("Score").$sbGet() - sbTeam.$sb("LastScore").$sbGet();
			teamJamPointsA.text(jamTotal);
		}
		sbTeam.$sb("Score").$sbBindAndRun("sbchange", teamJamPointsUpdate);
		sbTeam.$sb("LastScore").$sbBindAndRun("sbchange", teamJamPointsUpdate);

		var teamTimeouts = $(".Timeouts>div.Team"+team+".Number.TextContainer");
		sbTeam.$sb("Timeouts").$sbElement(teamTimeouts.children("a"), { sbelement: {
			autoFitText: { overage: 15, useMarginBottom: true }
		} }, "Number");

		var officialReviewsName = $(".OfficialReviews>div.Team"+team+".Name.TextContainer");
		_autoFit.enableAutoFitText(officialReviewsName, { overage: -20 });
		var teamOfficialReviews = $(".OfficialReviews>div.Team"+team+".Number.TextContainer");
		sbTeam.$sb("OfficialReviews").$sbElement(teamOfficialReviews.children("a"), { sbelement: {
			autoFitText: { overage: 15, useMarginBottom: true }
		} }, "Number");

		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".Timeouts>.Team" + team + ">:not(.Active)"), null, { 'fg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".OfficialReviews>.Team" + team + ">:not(.Active)"), null, { 'fg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".Timeouts>.Team" + team + ">.Active"), null, { 'bg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".OfficialReviews>.Team" + team + ">.Active"), null, { 'bg': 'background-color' } );

		var teamDiv = $("#sbDiv>div.Team"+team);
		sbTeam.$sb("Name").$sbElement(teamDiv.find("div.Name>a"), { sbelement: { autoFitText: true } }, "Name");
		sbTeam.$sb("Logo").$sbElement(teamDiv.find("div.Logo img"), "Logo");
		sbTeam.$sb("Score").$sbElement(teamDiv.find("div.Score>a"), { sbelement: { autoFitText: { overage: 40 } } }, "Score");
		sbTeam.$sb("Position(Jammer).Name").$sbElement(teamDiv.find("div.Jammer>div>a"), { sbelement: { autoFitText: true } });

		sbTeam.$sb("Name").$sbBindAndRun("sbchange", function(event,value) {
			teamDiv.find("div.Name,div.Logo").toggleClass("NoName", !value, animateTime.team);
		});

		_crgUtils.bindColors(sbTeam, "scoreboard", teamDiv.find("div.Name>a"));

// FIXME - This is the Team Name/Logo animation code - should be cleaned up/reduced
		var resizeName = teamDiv.find("div.Name").data("AutoFit");
		sbTeam.$sb("Logo").$sbBindAndRun("sbchange", function(event, newVal, oldVal) {
			var sbLogo = $sb(this);
			teamDiv.children(".TeamAnimationQueue").queue(function(next) {
				if (!!sbLogo.$sbGet() == teamDiv.find("div.TeamAnimationFlag").hasClass("ShowLogo"))
					next();
				else if (sbLogo.$sbGet()) {
					teamDiv.find("div.Name,div.TeamAnimationFlag").addClass("ShowLogo");
					var resizedCss = resizeName();
					teamDiv.find("div.Name").removeClass("ShowLogo"); resizeName();
					teamDiv.find("div.Name>a").animate(resizedCss, animateTime.team, function() {
						teamDiv.find("div.Name").addClass("ShowLogo"); resizeName();
						teamDiv.find("div.Logo").addClass("ShowLogo", animateTime.team, next);
					});
				} else {
					var curVal = teamDiv.find("div.Logo img").attr("src");
					if (!curVal)
						teamDiv.find("div.Logo img").attr("src", oldVal);
					teamDiv.find("div.Logo").removeClass("ShowLogo", animateTime.team, function() {
						if (!curVal)
							teamDiv.find("div.Logo img").attr("src", curVal);
						teamDiv.find("div.Name,div.TeamAnimationFlag").removeClass("ShowLogo");
						var resizedCss = resizeName();
						teamDiv.find("div.Name").addClass("ShowLogo"); resizeName();
						teamDiv.find("div.Name").animate(resizedCss, animateTime.team, function() {
							teamDiv.find("div.Name").removeClass("ShowLogo"); resizeName();
							next();
						});
					});
				}
			});
		});

		sbTeam.$sb("Position(Jammer).Name").$sbBindAndRun("sbchange", function(event, value) {
			teamDiv.find("div.Jammer>div").toggleClass("ShowJammer", !!value, animateTime.clock, "easeInQuart");
			teamDiv.find("div.Lead>div").toggle(!value);
		});
		sbTeam.$sb("LeadJammer").$sbBindAndRun("sbchange", function(event, value) {
			teamDiv.find("div.Lead>div").toggleClass("ShowLead", isTrue(value), animateTime.clock, "easeInQuart");
		});
 
		var showTimeoutRedBox = function() {
			// Called when anything changes to do with Timeouts.
			var ownTimeout = $sb("ScoreBoard.TimeoutOwner").$sbIs(team);
			var isOfficialReview = $sb("ScoreBoard.OfficialReview").$sbIsTrue();
			var isTeamTimeout = $sb("ScoreBoard.TimeoutOwner").$sbGet();
			var timeoutRunning = $sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue();
			if (isOfficialReview) {
				$(".Timeout>div.Name>a>span.Name").html("Off. Rev.");	
			} else if (isTeamTimeout) {
				$(".Timeout>div.Name>a>span.Name").html("Team T/O");
			} else {
				$(".Timeout>div.Name>a>span.Name").html("Time Out");
			}
			$(".Timeouts>div.WhiteBox.Team"+team+">div.RedBox").toggle(ownTimeout && !isOfficialReview && timeoutRunning);
			$(".OfficialReviews>div.WhiteBox.Team"+team+">div.RedBox").toggle(ownTimeout && isOfficialReview && timeoutRunning);
		};

		var redBoxTriggers = $sb("ScoreBoard.TimeoutOwner").add($sb("ScoreBoard.Clock(Timeout).Running").add($sb("ScoreBoard.OfficialReview")));
		_crgUtils.bindAndRun(redBoxTriggers, "sbchange", showTimeoutRedBox);

		setupPulsate(
			function() { return sbTeam.$sb("LeadJammer").$sbIsTrue(); },
			teamDiv.find("div.Jammer>div>a.Pulse"),
			animateTime.leadjammerPulse
		);

		// Pulsate Timeouts if they're currently active. They'll be hidden in manageTimeoutImages
		$.each( [ 0, 1, 2 ], function(x, i) {
			setupPulsate(
				function() { return (
					$sb("ScoreBoard.Team(1).Timeouts").$sbGet() == i &&
					$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
					!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
					$sb("ScoreBoard.TimeoutOwner").$sbGet() == 1); },
				$(".Timeouts>.Team1>.Timeout" + (i+1) + ".Active"),
				750);
			setupPulsate(
				function() { return (
					$sb("ScoreBoard.Team(2).Timeouts").$sbGet() == i &&
					$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
					!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
					$sb("ScoreBoard.TimeoutOwner").$sbGet() == 2); },
				$(".Timeouts>.Team2>.Timeout" + (i+1) + ".Active"),
				750);
		});

		// Pulsate OR buttons.
		$.each( [ 1, 2 ], function(x, i) {
			setupPulsate(
				function() { return (
					$sb("ScoreBoard.OfficialReview").$sbIsTrue() &&
					$sb("ScoreBoard.TimeoutOwner").$sbGet() == i) },
				$(".OfficialReviews>.Team"+i+">.OfficialReview.Active"),
				750
			);
		});
	});

	// Timeout triggers
	var timeoutTriggers = $sb("ScoreBoard.Team(1).OfficialReviews")
		.add($sb("ScoreBoard.Team(1).Timeouts"))
		.add($sb("ScoreBoard.Team(2).OfficialReviews"))
		.add($sb("ScoreBoard.Team(2).Timeouts"))
		.add($sb("ScoreBoard.TimeoutOwner"));
	_crgUtils.bindAndRun(timeoutTriggers, "sbchange", manageTimeoutImages);
}

////////////////
// Clock control
////////////////

function setupClocks() {
	$.each( clockIds, function() {
		var id = String(this);
		var clock = id.replace(/_.*/,"");
		var sbClock = $sb("ScoreBoard.Clock("+clock+")");

		var clockDiv = $("#sbDiv>div."+id+".Clock");
		sbClock.$sb("Time").$sbElement(clockDiv.find("div.Time>a>span"), {
			sbelement: {
				convert: clockConversions[id],
				autoFitText: { overage: 25 },
				autoFitTextContainer: "div"
			} }, "Time");
	});

	$sb("ScoreBoard.Clock(Period).Number").$sbElement(".Period>div.Name>a>span.Number,.Period_small>div.Name>a>span.Number");
	$sb("ScoreBoard.Clock(Jam).Number").$sbElement(".Jam>div.Name>a>span.Number,.Jam_small>div.Name>a>span.Number");

	$sb("ScoreBoard.InOvertime").$sbBindAndRun("sbchange", function(event, value) {
		if (isTrue(value)) {
			// we don't want this on the animation queue; it should change immediately,
			// since the intermission clock should be displayed now
			$(".Period,.Period_small").addClass("Overtime");
		} else {
			// use 1ms duration so this gets put on the animation queue,
			// which will allow the "Overtime" to slide out before changing back to "Period 2"
			$(".Period,.Period_small").removeClass("Overtime", 1);
		}
	});

// FIXME - this intermission stuff is a mess, can it get fixed up or simplified?!?
	var intermissionAutoFitText = _autoFit.enableAutoFitText(".Intermission>div.Name.TextContainer");
	$sbThisPage.$sbBindAddRemoveEach("Intermission", function(event,node) {
		//$(".Intermission>div.Name>a")
		//	.append($("<span>").addClass("Unofficial "+node.$sbId))
		//	.append($("<span>").addClass("Name "+node.$sbId))
		//	.children("span."+node.$sbId).toggle($sb("ScoreBoard.Clock(Intermission).Number").$sbIs(node.$sbId));

		node.$sb("ShowUnofficial").$sbElement(".Intermission>div.Name>a>span.Unofficial."+node.$sbId, { sbelement: {
			boolean: true,
			convert: { "true": "Unofficial ", "false": "" },
			autoFitText: true,
			autoFitTextContainer: "div"
		} });
		node.$sb("Text").$sbElement(".Intermission>div.Name>a>span.Name."+node.$sbId, { sbelement: {
			autoFitText: true,
			autoFitTextContainer: "div"
		} });

		$sb("ScoreBoard.OfficialScore").$sbBindAndRun("sbchange", function(event,value) {
			$(".Intermission>div.Name>a>span.Unofficial."+node.$sbId)
				.toggle(!isTrue(value) && $sb("ScoreBoard.Clock(Intermission).Number").$sbIs(node.$sbId));
			intermissionAutoFitText();
		});
		node.$sb("HideClock").$sbBindAndRun("sbchange", function(event,value) {
			if ($sb("ScoreBoard.Clock(Intermission).Number").$sbIs(node.$sbId))
				$(".Intermission>div.Time").toggle(!isTrue(value));		
		});
	}, function(event,node) {
		$(".Intermission>div.Name>a>span."+node.$sbId).remove();
	});
	$sb("ScoreBoard.Clock(Intermission).Number").$sbBindAndRun("sbchange", function(event,value) {
		$(".Intermission>div.Name>a>span")
			.filter(":not(."+value+")").hide().end()
			.filter("."+value).show()
			.filter(".Unofficial").toggle(!$sbThisPage.$sb("Intermission("+value+").Confirmed").$sbIsTrue());
		intermissionAutoFitText();
		$(".Intermission>div.Time").toggle(!$sbThisPage.$sb("Intermission("+value+").HideClock").$sbIsTrue());
	});

	var clockChange = function(event, value, intermission) {
		if (isTrue(value) || intermission)
			$("#sbDiv>div.ClockAnimationQueue").queue(clockRunningChange);
	};
	$sb("ScoreBoard.Clock(Jam).Running").$sbBindAndRun("sbchange", clockChange);
	$sb("ScoreBoard.Clock(Lineup).Running").$sbBindAndRun("sbchange", clockChange);
	$sb("ScoreBoard.Clock(Timeout).Running").$sbBindAndRun("sbchange", clockChange);
	$sb("ScoreBoard.Clock(Intermission).Running").$sbBindAndRun("sbchange", function(event, value) {
		clockChange(event, value, 1);
	});
}

function clockRunningChange(next) {
	if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue())
		showClocks("Jam", next);
	else if ($sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue())
		showClocks("Timeout", next);
	else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue())
		showClocks("Lineup", next);
	else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue())
		showClocks("Intermission", next);
	else
		showClocks("Jam", next);
}

var currentClock = "";
function showClocks(clock, next) {
	if (currentClock == clock) {
		next();
		return;
	}
	var toClass = "Show"+clock;
	var transClass = "Trans"+currentClock+clock;
	var fromClasses = "ShowPeriod ShowJam ShowLineup ShowTimeout ShowIntermission".replace(toClass, "");
	if (currentClock)
		$("#sbDiv div.ClockAnimation").switchClass(fromClasses, transClass, animateTime.clock, "easeInQuart");
	$("#sbDiv div.ClockAnimation").switchClass(transClass, toClass, animateTime.clock, "easeInQuart",
		function() { $(this).hasClass("ClockAnimationFlag") && next(); }
	);
	currentClock = clock;
}


/////////////////////////
// Sponsor Banner control
/////////////////////////

function setupSponsorBanners() {
	var div = $("<div>").attr("id", "SponsorBox").addClass("ClockAnimation").appendTo("#sbDiv")
		.append($("<div><img/></div>").addClass("CurrentImg"))
		.append($("<div><img/></div>").addClass("NextImg"));
	var setNextSrc = function() {
		var banners = $.makeArray($sb("Images.Type(sponsor_banner)").find("Image"));
		banners.sort(function(a, b) {
			var nameA = $sb(a).$sb("Name").$sbGet();
			var nameB = $sb(b).$sb("Name").$sbGet();
			if (nameA < nameB)
				return -1;
			else if (nameA > nameB)
				return 1;
			else {
				var srcA = $sb(a).$sb("Src").$sbGet();
				var srcB = $sb(a).$sb("Src").$sbGet();
				if (srcA < srcB)
					return -1;
				else if (srcA > srcB)
					return 1;
			}
			return 0;
		});
		var index = $.inArray(div.children("div.CurrentImg").data("banner"), banners) + 1;
		if ((0 > index) || (index >= banners.length))
			index = 0;
		var next = banners[index]||"";
		var nextSrc = (next?$sb(next).$sb("Src").$sbGet():"");
		div.children("div.NextImg").data("banner", next).children("img").prop("src", nextSrc).toggle(!!nextSrc);
	};
	setNextSrc();
	div.children("div").toggleClass("CurrentImg NextImg");
	setNextSrc();
	var nextImgFunction = function() {
		if (div.is(".ShowTimeout,.ShowLineup")) {
			div.children("div.CurrentImg").switchClass("CurrentImg", "FinishedImg", animateTime.sponsorOut);
			div.children("div.NextImg").switchClass("NextImg", "CurrentImg", animateTime.sponsorIn, function() {
				div.children("div.FinishedImg").toggleClass("FinishedImg NextImg");
				setNextSrc();
			});
		}
		div.delay(5000, "SponsorChangeQueue").queue("SponsorChangeQueue", nextImgFunction).dequeue("SponsorChangeQueue");
	};
	nextImgFunction();
}


////////////
// Animation
////////////

function manageTimeoutImages() {
	// Called when something changes in relation to timeouts.
	$.each( [ 1, 2 ], function(x, i) {
		var thisTeam = $sb("ScoreBoard.Team("+i+")");

		// Have they one OR?
		var elem = $(".OfficialReviews>.Team"+i+">.OfficialReview");
		elem.toggleClass("Used", thisTeam.$sb("OfficialReviews").$sbGet() == 0);
	
		// How's their timeouts looking?
		var numTOs = thisTeam.$sb("Timeouts").$sbGet();
		for ( var timeout = 1; timeout <= 3; timeout++ ) {
			var elem = $(".Timeouts>.Team"+i+">.Timeout"+timeout);
			elem.toggleClass("Used", numTOs < timeout);
		}
	});
}

function setupPulsate(pulseCondition, pulseTarget, pulsePeriod) {
	var doPulse = function(next) {
		if (pulseCondition())
			pulseTarget
				.animate({ opacity: 1 }, (pulsePeriod/2), "linear")
				.animate({ opacity: 0 }, (pulsePeriod/2), "linear");
		else {
			if (pulseTarget.hasClass("Used"))
				pulseTarget.delay(500).css("opacity", '');
			else
				pulseTarget.delay(500);
		}
		pulseTarget.queue(doPulse);
		next();
	};
	doPulse($.noop);
}

