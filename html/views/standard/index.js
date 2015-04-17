$(initialize);

function initialize() {
	WS.Connect();
	WS.AutoRegister();

	// Set Styles
	var view = "View";
	if (_windowFunctions.checkParam("preview", "true"))
		view = "Preview";
	WS.Register( "ScoreBoard.Setting(ScoreBoard." + view + "_SwapTeams)", function (k, v) {
		$(".Team1").toggleClass("Left", !isTrue(v)).toggleClass("Right", isTrue(v));
		$(".Team2").toggleClass("Left", isTrue(v)).toggleClass("Right", !isTrue(v));
		$(".Team").toggleClass("Swapped", isTrue(v));
	});

	WS.Register( [ "ScoreBoard.Setting(ScoreBoard." + view + "_BoxStyle)",
		"ScoreBoard.Setting(ScoreBoard." + view + "_BackgroundStyle)",
		"ScoreBoard.Setting(ScoreBoard." + view + "_HideJamTotals)",
		"ScoreBoard.Setting(ScoreBoard." + view + "_SidePadding)" ], function(k, v) {
			var boxStyle = WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_BoxStyle)"];
			var backgroundStyle = WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_BackgroundStyle)"];
			var showJamTotals = !isTrue(WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_HideJamTotals)"]);
			var sidePadding = WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_SidePadding)"];

			$("body").removeClass();
			if (boxStyle != "" && boxStyle != null)
				$("body").addClass(boxStyle);
			if (backgroundStyle != "" && backgroundStyle != null)
				$("body").addClass(backgroundStyle);
			$("#sb").toggleClass("JamScore", showJamTotals);

			left = 0;
			right = 0;
			if (sidePadding != "" && sidePadding != null) {
				left = sidePadding;
				right = left;
			}
			$("#sb").css({ "left": left + "%", "width": (100 - left - right) + "%" });
			$(window).trigger("resize");

	});

	// Show Clocks
	WS.Register( [
		"ScoreBoard.Clock(Period).Running",
		"ScoreBoard.Clock(Jam).Running",
		"ScoreBoard.Clock(Lineup).Running",
		"ScoreBoard.Clock(Timeout).Running",
		"ScoreBoard.Clock(Intermission).Running" ], function(k, v) { clockRunner(k,v); });

}

