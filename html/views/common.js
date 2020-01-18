
function jammer(k, v) {
	id = getTeamId(k);
	var prefix = "ScoreBoard.Team(" + id + ").";
	var jammerName = WS.state[prefix + "Position(Jammer).Name"];
	var pivotName = WS.state[prefix + "Position(Pivot).Name"];
	var leadJammer = isTrue(WS.state[prefix + "DisplayLead"]);
	var starPass = isTrue(WS.state[prefix + "StarPass"]);
	var inJam = isTrue(WS.state["ScoreBoard.InJam"]);

	if (jammerName == null || jammerName == "") {
		jammerName = leadJammer ? "Lead" : "";
	if (pivotName == null)
		pivotName = "";
	}

	var jn = !starPass ? jammerName : pivotName;
	if (!inJam) {
		jn = "";  // When no clocks are running, do not show jammer names.
	};
	$(".Team" + id + " .Lead").toggleClass("HasLead", (leadJammer && !starPass));
	$(".Team" + id).toggleClass("HasJammerName", (jn != ""));
	return jn
}

function getTeamId(k) {
	if (k.indexOf("Team(1)") > 0)
		return "1";
	if (k.indexOf("Team(2)") > 0)
		return "2";
	return null;
}

function nameUpdate(k, v) {
	$(".Team" + getTeamId(k)).toggleClass("HasName", v != "");
	return v;
}

function logoUpdate(k, v) {
	id = getTeamId(k);
	var prefix = "ScoreBoard.Team(" + id + ").";
	var logo = WS.state[prefix + "Logo"];
	if (logo == null)
		logo = "";
	if (logo != "")
		logo = 'url("' + logo + '")';

	$(".Team" + id + ">.Logo").css("background-image", logo);
	$(".Team" + id).toggleClass("HasLogo", logo != "");
	var nameAutoFit = $(".Team" + id + ">.Name>div").data("AutoFit");
	if (nameAutoFit)
		nameAutoFit();
}

function smallDescriptionUpdate(k, v) {
	var lc = WS.state["ScoreBoard.Clock(Lineup).Running"];
	var tc = WS.state["ScoreBoard.Clock(Timeout).Running"];
	var to = WS.state["ScoreBoard.TimeoutOwner"];
	var or = WS.state["ScoreBoard.OfficialReview"];
	var lcn = WS.state["ScoreBoard.Clock(Lineup).Name"];
	var tcn = WS.state["ScoreBoard.Clock(Timeout).Name"];
	var ret = '';

	console.log(lc,tc,to,or,lcn,tcn);

	$.each(["1", "2"], function (idx, id) {
		tto = WS.state["ScoreBoard.Team(" + id + ").Timeouts"];
		tor = WS.state["ScoreBoard.Team(" + id + ").OfficialReviews"];
		tror = WS.state["ScoreBoard.Team(" + id + ").RetainedOfficialReview"];
		$(".Team" + id + " .Timeout1").toggleClass("Used", tto < 1);
		$(".Team" + id + " .Timeout2").toggleClass("Used", tto < 2);
		$(".Team" + id + " .Timeout3").toggleClass("Used", tto < 3);
		$(".Team" + id + " .OfficialReview1").toggleClass("Used", tor < 1);
		$(".Team" + id + " .OfficialReview1").toggleClass("Retained", tror);
	});

	$(".Team .Dot").removeClass("Active");
	$(".Clock.Description,.Team>.Timeouts,.Team>.OfficialReviews").removeClass("Red");
	if (lc)
		ret = lcn;
	else if (tc) {
		$(".Clock.Description").addClass("Red");

		ret = tcn;
		if (to != "") {
			if (to == "O") {
				ret = "Official Timeout";
			} else {
				if (or) {
					ret = "Official Review";
					$(".Team" + to + ">.OfficialReviews:not(.Header)").addClass("Red");
					var dotSel = ".Team" + to + " .OfficialReview1";
					$(dotSel).addClass("Active");
				} else {
					ret = "Team Timeout";
					$(".Team" + to + ">.Timeouts").addClass("Red");
					var dotSel = ".Team" + to + " .Timeout" + (WS.state["ScoreBoard.Team(" + to + ").Timeouts"] + 1);
					$(dotSel).addClass("Active");
				}
			}
		}
	}
	return ret;
}

function intermissionDisplay() {
	var num = WS.state["ScoreBoard.Clock(Intermission).Number"];
	var max = WS.state["ScoreBoard.Rulesets.CurrentRule(Period.Number)"];
	var isOfficial = WS.state["ScoreBoard.OfficialScore"];
	var ret = '';

	if (num == 0) {
		ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)"];
	} else if (num != max)
		ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)"];
	else if (!isOfficial)
		ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)"];
	else
		ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)"];

	$(".Clock.Intermission .Time").toggleClass("Hide", num == max);
	return ret;
}

function toClockInitialNumber(k, v) {
	var ret = '';
	$.each(["Period", "Jam"], function (i, c) {
		if (k.indexOf("Clock(" + c + ")") > -1) {
			var name = WS.state["ScoreBoard.Clock(" + c + ").Name"];
			var number = WS.state["ScoreBoard.Clock(" + c + ").Number"];

			if (name != null && number != null)
				ret = name.substring(0, 1) + number;

			if (name == 'Period' && WS.state['ScoreBoard.Rulesets.CurrentRule(Period.Number)'] == 1) 
				ret = 'Game';
		}
	});
	return ret;
}

function toTime(k, v) {
	return _timeConversions.msToMinSecNoZero(v);
}

function toJamScore(k, v) {
	var prefix = "ScoreBoard.Team(" + getTeamId(k) + ").";
	var jamScore = WS.state[prefix + "JamScore"];
	var noInitial = isTrue(WS.state[prefix + "NoInitial"]);
	return (noInitial && !jamScore) ? '_' : jamScore;
}

function toSP(k, v) {
	return isTrue(v) ? 'SP' : '';
}

function clockRunner(k,v) {
			var lc = WS.state["ScoreBoard.Clock(Lineup).Running"];
			var tc = WS.state["ScoreBoard.Clock(Timeout).Running"];
			var ic = WS.state["ScoreBoard.Clock(Intermission).Running"];

			var clock = "Jam";
			if (isTrue(tc))
				clock = "Timeout";
			else if (isTrue(lc))
				clock = "Lineup";
			else if (isTrue(ic))
				clock = "Intermission";

			$(".Clock,.SlideDown").removeClass("Show");
			$(".SlideDown.ShowIn" + clock + ",.Clock.ShowIn" + clock).addClass("Show");
}


// Show Clocks
WS.Register( [
	"ScoreBoard.Clock(Period).Running",
	"ScoreBoard.Clock(Jam).Running",
	"ScoreBoard.Clock(Lineup).Running",
	"ScoreBoard.Clock(Timeout).Running",
	"ScoreBoard.Clock(Intermission).Running" ], function(k, v) { clockRunner(k,v); } );

WS.Register( 'ScoreBoard.Rulesets.CurrentRule(Period.Number)' );
WS.Register( 'ScoreBoard.InJam' );
