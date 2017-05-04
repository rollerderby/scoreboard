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
	var ani = _windowFunctions.getParam("ani");
	if (ani == "min")
		$("body").addClass("MinAnimations");

	setupMainDiv($("#mainDiv")); // This needs to be part of scoreboard framework

	if (_windowFunctions.checkParam("videomuted", "true"))
		$("video").prop({ muted: true, volume: "0.0" }); // Not all browsers support muted
	if (_windowFunctions.checkParam("videocontrols", "true"))
		$("video").prop("controls", true);

	setupSponsorBanners();
	setupTeams();
	setupClocks();

	setupBackgrounds();

	var view = "View";
	if (_windowFunctions.checkParam("preview", "true"))
		view = "Preview";
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_Image)").$sbElement("#imageDiv>img");
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_Video)").$sbElement("#videoDiv>video");
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_CustomHtml)").$sbElement("#htmlDiv>iframe");
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_SwapTeams)").$sbBindAndRun("sbchange", function(event,value) {
		$("#sbDiv>div.Team,.Timeouts>div.Team,.OfficialReviews>div.Team,.JamPoints>div.Team").toggleClass("SwapTeams", isTrue(value));
	});

	var styleSet = function() {
		var boxStyle = $sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_BoxStyle)").$sbGet();
		var backgroundStyle = $sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_BackgroundStyle)").$sbGet();
		var hideJamTotals = isTrue($sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_HideJamTotals)").$sbGet());
		var sidePadding = $sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_SidePadding)").$sbGet();

		// change box_flat_bright to two seperate classes in order to reuse much of the css
		if (boxStyle == 'box_flat_bright')
			boxStyle = 'box_flat bright';

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
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_BoxStyle)").$sbBindAndRun("sbchange", styleSet);
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_BackgroundStyle)").$sbBindAndRun("sbchange", styleSet);
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_HideJamTotals)").$sbBindAndRun("sbchange", styleSet);
	$sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_SidePadding)").$sbBindAndRun("sbchange", styleSet);

	var v = $sb("ScoreBoard.Settings.Setting(ScoreBoard." + view + "_CurrentView)");
	v.$sbBindAndRun("sbchange", function(event, value) {
		$("video").each(function() { this.pause(); });
		var showDiv = $("#"+value+"Div");
		if (!showDiv.length)
			showDiv = $("#sbDiv");
		showDiv.children("video").each(function() { this.currentTime = 0; this.play(); });
		$("#mainDiv>div.View").not(showDiv).removeClass("Show");
		showDiv.addClass("Show");
	});
});

// FIXME - needs to be a single call from scoreboard.js
function setupMainDiv(div) {
 	div.css({ position: "fixed" });
	div.bind("click", function() { _windowFunctions.fullscreenRequest(true); });
	div.bind("dblclick", function() { _windowFunctions.fullscreenRequest(false); });
 
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
		// sbTeam.$sb("Name").$sbElement(teamDiv.find("div.Name>a"), { sbelement: { autoFitText: true } }, "Name");
		sbTeam.$sb("Logo").$sbElement(teamDiv.find("div.Logo img"), "Logo");
		sbTeam.$sb("Score").$sbElement(teamDiv.find("div.Score>a"), { sbelement: { autoFitText: { overage: 40 } } }, "Score");

		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".Timeouts>.Team" + team + ">:not(.Active)"), null, { 'fg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".OfficialReviews>.Team" + team + ">:not(.Active)"), null, { 'fg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".Timeouts>.Team" + team + ">.Active"), null, { 'bg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard_dots", $(".OfficialReviews>.Team" + team + ">.Active"), null, { 'bg': 'background-color' } );
		_crgUtils.bindColors(sbTeam, "scoreboard", teamDiv.find("div.Name>a"));

		var nameDiv = teamDiv.find("div.Name>a");
		var resizeName = _autoFit.enableAutoFitText(teamDiv.find("div.Name")); 
		var namePicker = function(event, value) {
			var n = sbTeam.$sb("Name").$sbGet();
			var an = sbTeam.$sb("AlternateName(scoreboard).Name").$sbGet();
			if (an != null && an != "")
				n = an;
			nameDiv.text(n);
			teamDiv.find("div.NameLogo").toggleClass("NoName", !(n != null && n != ""));
			resizeName();
		};
		sbTeam.$sb("Name").$sbBindAndRun("sbchange", namePicker);
		sbTeam.$sb("AlternateName(scoreboard).Name").$sbBindAndRun("sbchange", namePicker);
		sbTeam.$sb("Logo").$sbBindAndRun("sbchange", function(event,value) {
			teamDiv.find("div.NameLogo").toggleClass("NoLogo", !value);
			resizeName();
		});

		var jammerDiv = teamDiv.find("div.JammerBox");
		var jammerA = jammerDiv.find("div.Jammer>a");
		var pivotA = jammerDiv.find("div.Pivot>a");
		var leadA = jammerDiv.find("div.Lead>a");

		sbTeam.$sb("Position(Jammer).Name").$sbElement(jammerA, { sbelement: { autoFitText: true } });
		sbTeam.$sb("Position(Pivot).Name").$sbElement(pivotA, { sbelement: { autoFitText: true } });

		sbTeam.$sb("Position(Jammer).Name").$sbBindAndRun("sbchange", function(event, value) {
			jammerDiv.toggleClass("HaveJammer", !!value);
		});
		sbTeam.$sb("Position(Pivot).Name").$sbBindAndRun("sbchange", function(event, value) {
			jammerDiv.toggleClass("HavePivot", !!value);
		});
		sbTeam.$sb("LeadJammer").$sbBindAndRun("sbchange", function(event, value) {
			jammerDiv.toggleClass("LeadJammer", value == "Lead");
		});
		sbTeam.$sb("StarPass").$sbBindAndRun("sbchange", function(event, value) {
			jammerDiv.toggleClass("StarPass", isTrue(value));
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
		var retainedOfficialReview = $sb("ScoreBoard.Team(" + team + ").RetainedOfficialReview");
		var timeoutRunning = $sb("ScoreBoard.Clock(Timeout).Running");
		var isOfficialReview = $sb("ScoreBoard.OfficialReview");
		var timeoutOwner = $sb("ScoreBoard.TimeoutOwner");
		var pulseTimeout = function() {
			var t = timeouts.$sbGet();
			var or = officialReviews.$sbGet();
			var tr = timeoutRunning.$sbIsTrue();
			var isOR = isOfficialReview.$sbIsTrue();
			var isRetained = retainedOfficialReview.$sbIsTrue();
			var owner = timeoutOwner.$sbGet();

			var pulse = (isOR && owner == team);
			$("div.Team" + team + ">div.Dot.OfficialReview").toggleClass("Active", pulse);
			$("div.Team" + team + ">div.Dot.OfficialReview").toggleClass("Used", or != 1);
			$("div.Team" + team + ">div.Dot.OfficialReview").toggleClass("Retained", isRetained);
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
		retainedOfficialReview.$sbBindAndRun("sbchange", pulseTimeout);

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

	// Setup intermission
	var intermissionAutoFitText = _autoFit.enableAutoFitText(".Intermission>div.Name.TextContainer");
	var interNumber = $sb("ScoreBoard.Clock(Intermission).Number");
	var interMax = $sb("ScoreBoard.Clock(Intermission).MaximumNumber");
	var isOfficialScore = $sb("ScoreBoard.OfficialScore");
	var ruleset = $sb("ScoreBoard.Ruleset");
	var settings = $sb("ScoreBoard.Settings");
	var intermissionText = { };

	var intermissionUpdate = function() {
		var num = interNumber.$sbGet();
		var max = interMax.$sbGet();
		var isOfficial = isTrue(isOfficialScore.$sbGet());

		var a = $(".Intermission>div.Name>a");
		var time = $(".Intermission>div.Time");

		time.show();
		if (num == '0')
			a.text(intermissionText["ScoreBoard.Intermission.PreGame"]);
		else if (num == max) {
			time.hide();
			if (!isOfficial)
				a.text(intermissionText["ScoreBoard.Intermission.Unofficial"]);
			else
				a.text(intermissionText["ScoreBoard.Intermission.Official"]);
		} else
			a.text(intermissionText["ScoreBoard.Intermission.Intermission"]);
		intermissionAutoFitText();
	};

	settings.$sbBindAddRemoveEach("Setting",
		function (event, node) {
			var id = $sb(node).$sbId;
			if (id == "ScoreBoard.Intermission.PreGame" || id == "ScoreBoard.Intermission.Intermission" || id == "ScoreBoard.Intermission.Unofficial" || id == "ScoreBoard.Intermission.Official") {
				node.$sbBindAndRun("sbchange", function(event,val) {
					intermissionText[id] = $.trim(val);
					intermissionUpdate();
				});
			}
		},
		function (event, node) {
		});
	interNumber.$sbBindAndRun("sbchange", intermissionUpdate);
	interMax.$sbBindAndRun("sbchange", intermissionUpdate);
	isOfficialScore.$sbBindAndRun("sbchange", intermissionUpdate);

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
