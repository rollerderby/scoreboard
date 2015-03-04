WS.Connect(connected);

var registered = false;
function connected() {
	if (!registered) {
		registered = true;

		$.each([1, 2], function(idx, id) {
			var prefix = "ScoreBoard.Team(" + id + ")";
			var nameDiv = $(".Team" + id + ".Name");
			var logoDiv = $(".Team" + id + ".Logo");
			var setName = function() {
				var name = WS.state[prefix + ".Name"];
				var altname = WS.state[prefix + ".AlternateName(scoreboard)"];

				var txt = "";
				if (altname != "" && altname != null)
					txt = altname;
				else if (name != "" && name != null)
					txt = name;

				if (txt != "")
					nameDiv.text(txt);
				nameDiv.toggleClass("HasName", txt != "");
				logoDiv.toggleClass("HasName", txt != "");
			};
			var setLogo = function(k, v) {
				if (v == null || v == "")
					v = "";
				else
					v = "url(" + v + ")";

				logoDiv.css("background-image", v);
				nameDiv.toggleClass("HasLogo", v != "");
				logoDiv.toggleClass("HasLogo", v != "");
			};

			var timeoutsDiv = $(".Team" + id + ".Timeouts");
			var officialReviewsDiv = $(".Team" + id + ".OfficialReviews");
			var updateTimeouts = function(k, v) {
				var timeouts = WS.state[prefix + ".Timeouts"];
				var officialReviews = WS.state[prefix + ".OfficialReviews"];
				timeoutsDiv.find(".Number").text(timeouts);
				officialReviewsDiv.find(".Number").text(officialReviews);
			}

			var updateJamScore = function() {
				var jamScore = WS.state[prefix + ".Score"] - WS.state[prefix + ".LastScore"];
				$(".Team" + id + ".JamScore").text(jamScore);
			}

			WS.Register("ScoreBoard.Team(" + id + ").Name", setName);
			WS.Register("ScoreBoard.Team(" + id + ").AlternateName(scoreboard)", setName);
			WS.Register("ScoreBoard.Team(" + id + ").Logo", setLogo);
			WS.Register("ScoreBoard.Team(" + id + ").Score", $(".Team" + id + ".Score"));

			WS.Register("ScoreBoard.Team(" + id + ").Score", updateJamScore);
			WS.Register("ScoreBoard.Team(" + id + ").LastScore", updateJamScore);

			WS.Register("ScoreBoard.Team(" + id + ").Timeouts", updateTimeouts);
			WS.Register("ScoreBoard.TimeoutOwner", updateTimeouts);
			WS.Register("ScoreBoard.OfficialReview", updateTimeouts);
			WS.Register("ScoreBoard.Clock(Timeout).Running", updateTimeouts);
			WS.Register("ScoreBoard.Team(" + id + ").OfficialReviews", updateTimeouts);
		});

		$.each(["Period", "Jam", "Lineup", "Timeout", "Intermission"], function(idx, id) {
			var ClockSwitching = function(k, v) {
				$(".Clock").toggleClass(id + "Running", v);
			}
			var NameInitial = function(k, v) {
				$("." + id + " .NameInitial").text(v.substring(0, 1));
			}

			WS.Register("ScoreBoard.Clock(" + id + ").Time", $(".Clock." + id + " .Time"), _timeConversions.msToMinSecNoZero);
			WS.Register("ScoreBoard.Clock(" + id + ").Number", $(".Clock." + id + " span.Number"));
			WS.Register("ScoreBoard.Clock(" + id + ").Name", $(".Clock." + id + " span.Name"));
			WS.Register("ScoreBoard.Clock(" + id + ").Name", NameInitial);
			WS.Register("ScoreBoard.Clock(" + id + ").Running", ClockSwitching);
		});

		var descriptionUpdate = function() {
			text = WS.state["ScoreBoard.Clock(Lineup).Name"];
			$(".Clock.Description>.Box, .Team.Timeouts:not(.Header), .Team.OfficialReviews:not(.Header)").removeClass("Red");
			if (WS.state["ScoreBoard.Clock(Timeout).Running"]) {
				$(".Clock.Description>.Box").addClass("Red");
				text = WS.state["ScoreBoard.Clock(Timeout).Name"];
				var owner = WS.state["ScoreBoard.TimeoutOwner"];
				if (owner != "") {
					if (WS.state["ScoreBoard.OfficialReview"]) {
						$(".Team" + owner + ".OfficialReviews:not(.Header)").addClass("Red");
						text = "Official Review";
					} else {
						$(".Team" + owner + ".Timeouts:not(.Header)").addClass("Red");
						text = "Team Timeout";
					}
				}
			}
			$(".Clock.Description>.Box").text(text);
		};
		WS.Register("ScoreBoard.Clock(Lineup).Running", descriptionUpdate);
		WS.Register("ScoreBoard.Clock(Timeout).Running", descriptionUpdate);
		WS.Register("ScoreBoard.TimeoutOwner", descriptionUpdate);
		WS.Register("ScoreBoard.OfficialReview", descriptionUpdate);

		var intermissionSetup = function(k, v) {
			var num = WS.state["ScoreBoard.Clock(Intermission).Number"];
			var max = WS.state["ScoreBoard.Clock(Intermission).MaximumNumber"];
			var showClock = true;
			var description = WS.state["ScoreBoard.Setting(Intermission.Intermission)"];
			if (num == 0)
				description = WS.state["ScoreBoard.Setting(Intermission.PreGame)"];
			else if (num == max) {
				showClock = false;
				if (WS.state["ScoreBoard.OfficialScore"])
					description = WS.state["ScoreBoard.Setting(Intermission.Official)"];
				else
					description = WS.state["ScoreBoard.Setting(Intermission.Unofficial)"];
			}
			$(".Clock.Intermission .Description").text(description);
			$(".Clock.Intermission .Time").toggleClass("Hide", !showClock);
		};
		WS.Register("ScoreBoard.OfficialScore", intermissionSetup);
		WS.Register("ScoreBoard.Clock(Intermission).Number", intermissionSetup);
		WS.Register("ScoreBoard.Clock(Intermission).MaximumNumber", intermissionSetup);
		WS.Register("ScoreBoard.Setting(Intermission.PreGame)", intermissionSetup);
		WS.Register("ScoreBoard.Setting(Intermission.Intermission)", intermissionSetup);
		WS.Register("ScoreBoard.Setting(Intermission.Unofficial)", intermissionSetup);
		WS.Register("ScoreBoard.Setting(Intermission.Official)", intermissionSetup);

		var view = "View";
		//if (_windowFunctions.checkParam("preview", "true"))
		//	view = "Preview";
		WS.Register("ScoreBoard.Setting(" + view + "_Image)", $("#imageDiv>img"), { attr: "src" });
		WS.Register("ScoreBoard.Setting(" + view + "_Video)", $("#videoDiv>video"), { attr: "src" });
		WS.Register("ScoreBoard.Setting(" + view + "_CustomHtml)", $("#htmlDiv>iframe"), { attr: "src" });
		WS.Register("ScoreBoard.Setting(" + view + "_SwapTeams)", function (k, v) {
			$("div.Team").toggleClass("Swapped", v == "true");
		});

		var styleSet = function() {
			var boxStyle = WS.state["ScoreBoard.Setting(" + view + "_BoxStyle)"];
			var backgroundStyle = WS.state["ScoreBoard.Setting(" + view + "_BackgroundStyle)"];
			var hideJamTotals = WS.state["ScoreBoard.Setting(" + view + "_HideJamTotals)"];
			var sidePadding = WS.state["ScoreBoard.Setting(" + view + "_SidePadding)"];

			console.log("styleSet", boxStyle, backgroundStyle, hideJamTotals, sidePadding);
			$("#sb").removeClass();
			if (boxStyle != "" && boxStyle != null)
				$("#sb").addClass(boxStyle);
			if (backgroundStyle != "" && backgroundStyle != null)
				$("#sb").addClass(backgroundStyle);
			if (hideJamTotals)
				$("#sb").addClass("HideJamTotals");
			if (sidePadding != "" && sidePadding != null) {
				var left = sidePadding + "%";
				var width = (100 - (2 * sidePadding)) + "%";
				$("#sb").css({ left: left, width: width });
			} else {
				$("#sb").css({ left: "", width: "" });
			}
			// $(window).trigger("resize");
		};
		WS.Register("ScoreBoard.Setting(" + view + "_BoxStyle)", styleSet);
		WS.Register("ScoreBoard.Setting(" + view + "_BackgroundStyle)", styleSet);
		WS.Register("ScoreBoard.Setting(" + view + "_HideJamTotals)", styleSet);
		WS.Register("ScoreBoard.Setting(" + view + "_SidePadding)", styleSet);
	}
}

(function($) {
	$.fn.textfill = function() {
		$.each(this, function(idx, elem) {
			c = elem.clone();
			console.log(c);
		});
	};
})(jQuery);
