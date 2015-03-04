WS.Connect(connected);

var registered = false;
function connected() {
	if (!registered) {
		registered = true;
		WS.AutoRegister();

		// Show Clocks
		WS.Register( [
			"ScoreBoard.Clock(Period).Running",
			"ScoreBoard.Clock(Jam).Running",
			"ScoreBoard.Clock(Lineup).Running",
			"ScoreBoard.Clock(Timeout).Running",
			"ScoreBoard.Clock(Intermission).Running" ], function(k, v) {
				$.each(["Period", "Jam", "Lineup", "Timeout", "Intermission"], function (i, c) {
					$(".Clock").toggleClass(c + "Running", WS.state["ScoreBoard.Clock(" + c + ").Running"]);
				});
		});

		// Show Proper Intermission Text
		WS.Register( [
			"ScoreBoard.Setting(Intermission.PreGame)",
			"ScoreBoard.Setting(Intermission.Intermission)",
			"ScoreBoard.Setting(Intermission.Unofficial)",
			"ScoreBoard.Setting(Intermission.Official)",
			"ScoreBoard.OfficialScore",
			"ScoreBoard.Clock(Intermission).Number",
			"ScoreBoard.Clock(Intermission).MaximumNumber" ], function(k, v) {
				var num = WS.state["ScoreBoard.Clock(Intermission).Number"];
				var max = WS.state["ScoreBoard.Clock(Intermission).MaximumNumber"];
				var isOfficial = WS.state["ScoreBoard.OfficialScore"];
				var text = '';
				if (num == 0)
					text = WS.state["ScoreBoard.Setting(Intermission.PreGame)"];
				else if (num != max)
					text = WS.state["ScoreBoard.Setting(Intermission.Intermission)"];
				else if (!isOfficial)
					text = WS.state["ScoreBoard.Setting(Intermission.Unofficial)"];
				else
					text = WS.state["ScoreBoard.Setting(Intermission.Official)"];

				$(".Clock.Intermission .Description").text(text);
				$(".Clock.Intermission .Time").toggleClass("Hide", num == max);
		});

		// Show Small Clock Description (and red box)
		WS.Register( [
			"ScoreBoard.Clock(Lineup).Name",
			"ScoreBoard.Clock(Lineup).Running",
			"ScoreBoard.Clock(Timeout).Name",
			"ScoreBoard.Clock(Timeout).Running",
			"ScoreBoard.TimeoutOwner",
			"ScoreBoard.OfficialReview" ], function(k, v) {
				var lc = WS.state["ScoreBoard.Clock(Lineup).Running"];
				var tc = WS.state["ScoreBoard.Clock(Timeout).Running"];
				var to = WS.state["ScoreBoard.TimeoutOwner"];
				var or = WS.state["ScoreBoard.OfficialReview"];
				var lcn = WS.state["ScoreBoard.Clock(Lineup).Name"];
				var tcn = WS.state["ScoreBoard.Clock(Timeout).Name"];
				var text = '';
				console.log(lc, tc, to, or, lcn, tcn);

				$(".Clock.Description>div,.Team>.Timeouts,.Team>.OfficialReviews").removeClass("Red");
				if (lc)
					text = lcn;
				else if (tcn) {
					$(".Clock.Description>div").addClass("Red");

					text = tcn;
					if (to != "" && !or) {
						text = "Team Timeout";
						$(".Team" + to + ">.Timeouts").addClass("Red");
					}
					if (to != "" && or) {
						text = "Official Review";
						$(".Team" + to + ">.OfficialReviews:not(.Header)").addClass("Red");
					}
				}
				$(".Clock.Description>div").text(text);
		});
	}
}

function getTeamId(k) {
	if (k.indexOf("Team(1)") > 0)
		return "1";
	if (k.indexOf("Team(2)") > 0)
		return "2";
	return null;
}

function nameUpdate(k, v) {
	id = getTeamId(k);
	var prefix = "ScoreBoard.Team(" + id + ").";
	var name = WS.state[prefix + "Name"];
	var altName = WS.state[prefix + "AlternateName(scoreboard)"];
	if (altName != null && altName != "")
		name = altName;

	$(".Team" + id + ">.Name").text(name);
	$(".Team" + id).toggleClass("HasName", name != "");
}

function logoUpdate(k, v) {
	id = getTeamId(k);
	var prefix = "ScoreBoard.Team(" + id + ").";
	var logo = WS.state[prefix + "Logo"];
	if (logo == null)
		logo = "";
	if (logo != "")
		logo = "url(" + logo + ")";

	$(".Team" + id + ">.Logo").css("background-image", logo);
	$(".Team" + id).toggleClass("HasLogo", logo != "");
}

function toTime(k, v) {
	return _timeConversions.msToMinSecNoZero(v);
}

function toInitial(k, v) {
	return v.substring(0, 1);
}
