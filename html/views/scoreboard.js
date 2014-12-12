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
	var minAnimations = _windowFunctions.getParam("minAnimations");
	if (isTrue(minAnimations))
		$("body").addClass("MinAnimations");

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
		var sidePadding = sbViewOptions.$sb("SidePadding").$sbGet();
		$("#mainDiv").removeClass();
		if (boxStyle != "" && boxStyle != null)
			$("#mainDiv").addClass(boxStyle);
		if (backgroundStyle != "" && backgroundStyle != null)
			$("#mainDiv").addClass(backgroundStyle);
		if (hideJamTotals)
			$("#mainDiv").addClass("HideJamTotals");
		if (sidePadding != "" && sidePadding != null) {
			var left = sidePadding + "%";
			var width = (100 - (2 * sidePadding)) + "%";
			$("#sbDiv").css({ left: left, width: width });
		} else {
			$("#sbDiv").css({ left: "", width: "" });
		}
		$(window).trigger("resize");
	}
	sbViewOptions.$sb("BoxStyle").$sbBindAndRun("sbchange", styleSet);
	sbViewOptions.$sb("BackgroundStyle").$sbBindAndRun("sbchange", styleSet);
	sbViewOptions.$sb("HideJamTotals").$sbBindAndRun("sbchange", styleSet);
	sbViewOptions.$sb("SidePadding").$sbBindAndRun("sbchange", styleSet);

	var view = sbViewOptions.$sb("CurrentView");
	view.$sbBindAndRun("sbchange", function(event, value) {
		var showDiv = $("#"+value+"Div");
		if (!showDiv.length)
			showDiv = $("#sbDiv");
		showDiv.children("video").each(function() { this.play(); });
		$("#mainDiv>div.View").not(showDiv).removeClass("Show");
		showDiv.addClass("Show");
	});
});

// FIXME - needs to be a single call from scoreboard.js
function setupMainDiv(div) {
 	div.css({ position: "fixed" });
 
 	_crgUtils.bindAndRun($(window), "resize", function() {
		var aspect = _windowFunctions.get4x3Dimensions();
		div.css(aspect).css("fontSize", aspect.height);
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

		var teamDiv = $("#sbDiv>div.Team"+team);
		sbTeam.$sb("Name").$sbElement(teamDiv.find("div.Name>a"), { sbelement: { autoFitText: true } }, "Name");
		sbTeam.$sb("Logo").$sbElement(teamDiv.find("div.Logo img"), "Logo");
		sbTeam.$sb("Score").$sbElement(teamDiv.find("div.Score>a"), { sbelement: { autoFitText: { overage: 40 } } }, "Score");
		sbTeam.$sb("Position(Jammer).Name").$sbElement(teamDiv.find("div.Jammer>a"), { sbelement: { autoFitText: true } });

		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".Timeouts>.Team" + team + ">:not(.Active)"), null, { 'fg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".OfficialReviews>.Team" + team + ">:not(.Active)"), null, { 'fg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".Timeouts>.Team" + team + ">.Active"), null, { 'bg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".OfficialReviews>.Team" + team + ">.Active"), null, { 'bg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard", teamDiv.find("div.Name>a"));

		var resizeName = teamDiv.find("div.Name").data("AutoFit");
		sbTeam.$sb("Name").$sbBindAndRun("sbchange", function(event,value) {
			teamDiv.find("div.NameLogo").toggleClass("NoName", !value);
			resizeName();
		});
		sbTeam.$sb("Logo").$sbBindAndRun("sbchange", function(event,value) {
			teamDiv.find("div.NameLogo").toggleClass("NoLogo", !value);
			resizeName();
		});

		sbTeam.$sb("Position(Jammer).Name").$sbBindAndRun("sbchange", function(event, value) {
			teamDiv.find("div.Jammer,div.Lead").toggleClass("HaveJammer", !!value);
		});
		sbTeam.$sb("LeadJammer").$sbBindAndRun("sbchange", function(event, value) {
			teamDiv.find("div.Jammer,div.Lead").toggleClass("LeadJammer", isTrue(value));
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

		var timeouts = $sb("ScoreBoard.Team(" + team + ").Timeouts");
		var officialReviews = $sb("ScoreBoard.Team(" + team + ").OfficialReviews");
		var timeoutRunning = $sb("ScoreBoard.Clock(Timeout).Running");
		var isOfficialReview = $sb("ScoreBoard.OfficialReview");
		var timeoutOwner = $sb("ScoreBoard.TimeoutOwner");
		var pulseTimeout = function() {
			var t = timeouts.$sbGet();
			var or = officialReviews.$sbGet();
			var tr = timeoutRunning.$sbIsTrue();
			var isOR = isOfficialReview.$sbIsTrue();
			var owner = timeoutOwner.$sbGet();

			var pulse = (isOR && owner == team);
			$("div.Team" + team + ">div.Dot.OfficialReview").toggleClass("Active", pulse);
			$("div.Team" + team + ">div.Dot.OfficialReview").toggleClass("Used", or != 1);
			for (var to = 1; to <= 3; to++) {
				pulse = (t == to - 1 && tr && !isOR && owner == team);
				$("div.Team" + team + ">div.Dot.Timeout" + to).toggleClass("Active", pulse);
				$("div.Team" + team + ">div.Dot.Timeout" + to).toggleClass("Used", to > t);
			}
		}
		timeouts.$sbBindAndRun("sbchange", pulseTimeout);
		officialReviews.$sbBindAndRun("sbchange", pulseTimeout);
		timeoutRunning.$sbBindAndRun("sbchange", pulseTimeout);
		isOfficialReview.$sbBindAndRun("sbchange", pulseTimeout);
		timeoutOwner.$sbBindAndRun("sbchange", pulseTimeout);

		// // Pulsate OR buttons.
		// $.each( [ 1, 2 ], function(x, i) {
		// 	setupPulsate(
		// 		function() { return (
		// 			$sb("ScoreBoard.OfficialReview").$sbIsTrue() &&
		// 			$sb("ScoreBoard.TimeoutOwner").$sbGet() == i) },
		// 		$(".OfficialReviews>.Team"+i+">.OfficialReview.Active"),
		// 		750
		// 	);
		// });
	});

	// Timeout triggers
	// var timeoutTriggers = $sb("ScoreBoard.Team(1).OfficialReviews")
	// 	.add($sb("ScoreBoard.Team(1).Timeouts"))
	// 	.add($sb("ScoreBoard.Team(2).OfficialReviews"))
	// 	.add($sb("ScoreBoard.Team(2).Timeouts"))
	// 	.add($sb("ScoreBoard.TimeoutOwner"));
	// _crgUtils.bindAndRun(timeoutTriggers, "sbchange", manageTimeoutImages);
}

////////////////
// Clock control
////////////////

function setupClocks() {
	$.each( clockIds, function() {
		var id = String(this);
		var clock = id.replace(/_.*/,"");
		var sbClock = $sb("ScoreBoard.Clock("+clock+")");

		var clockDiv = $("div."+id+".Clock");
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
			clockRunningChange();
	};
	$sb("ScoreBoard.Clock(Jam).Running").$sbBindAndRun("sbchange", clockChange);
	$sb("ScoreBoard.Clock(Lineup).Running").$sbBindAndRun("sbchange", clockChange);
	$sb("ScoreBoard.Clock(Timeout).Running").$sbBindAndRun("sbchange", clockChange);
	$sb("ScoreBoard.Clock(Intermission).Running").$sbBindAndRun("sbchange", function(event, value) {
		clockChange(event, value, 1);
	});
}

function clockRunningChange() {
	if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue())
		showClocks("Jam");
	else if ($sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue())
		showClocks("Timeout");
	else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue())
		showClocks("Lineup");
	else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue())
		showClocks("Intermission");
	else
		showClocks("Jam");
}

var currentClock = "";
function showClocks(clock) {
	if (currentClock == clock) {
		return;
	}
	$(".Clock.Show"+clock).addClass("Show");
	$(".Clock:not(.Show"+clock+")").removeClass("Show");
	currentClock = clock;
}


/////////////////////////
// Sponsor Banner control
/////////////////////////

function setupSponsorBanners() {
	var div = $("#SponsorBox");
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
	var nextImgFunction = function() {
		var cur = $(div.find(".CurrentImg")[0]);
		var nex = $(div.find(".NextImg")[0]);
		var fin = $(div.find(".FinishedImg")[0]);
		cur.removeClass("CurrentImg").addClass("FinishedImg");
		nex.removeClass("NextImg").addClass("CurrentImg");
		fin.removeClass("FinishedImg").addClass("NextImg");
		setNextSrc();
	};
	setInterval(nextImgFunction, 5000);
	setNextSrc();
	nextImgFunction();
}
