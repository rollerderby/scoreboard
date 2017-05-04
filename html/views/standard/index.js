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

	WS.Register( "ScoreBoard.Setting(ScoreBoard." + view + "_CurrentView", function(k, v) {
		$("div#video>video").each(function() { this.pause(); });
		$(".DisplayPane.Show").addClass("Hide");
		$(".DisplayPane").removeClass("Show");
		$("div#" + v + ".DisplayPane").addClass("Show");
		$("div#" + v + ".DisplayPane>video").each(function() { this.currentTime = 0; this.play(); });
	});

	WS.Register( "ScoreBoard.Setting(ScoreBoard." + view + "_Image)", function(k, v) {
		$("div#image>img").attr("src", v);
	});
	WS.Register( "ScoreBoard.Setting(ScoreBoard." + view + "_Video)", function(k, v) {
		$("div#video>video").attr("src", v);
	});
	WS.Register( "ScoreBoard.Setting(ScoreBoard." + view + "_CustomHtml)", function(k, v) {
		$("div#html>iframe").attr("src", v);
	});

	WS.Register( [ "ScoreBoard.Setting(ScoreBoard." + view + "_BoxStyle)",
		"ScoreBoard.Setting(ScoreBoard." + view + "_BackgroundStyle)",
		"ScoreBoard.Setting(ScoreBoard." + view + "_HideJamTotals)",
		"ScoreBoard.Setting(ScoreBoard." + view + "_SidePadding)" ], function(k, v) {
			var boxStyle = WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_BoxStyle)"];
			var backgroundStyle = WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_BackgroundStyle)"];
			var showJamTotals = !isTrue(WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_HideJamTotals)"]);
			var sidePadding = WS.state["ScoreBoard.Setting(ScoreBoard." + view + "_SidePadding)"];

			// change box_flat_bright to two seperate classes in order to reuse much of the css
			if (boxStyle == 'box_flat_bright')
				boxStyle = 'box_flat bright';

			$("body").removeClass();
			if (boxStyle != "" && boxStyle != null)
				$("body").addClass(boxStyle);
			if (backgroundStyle != "" && backgroundStyle != null)
				$("body").addClass(backgroundStyle);
			$("div#scoreboard").toggleClass("JamScore", showJamTotals);

			left = 0;
			right = 0;
			if (sidePadding != "" && sidePadding != null) {
				left = sidePadding;
				right = left;
			}
			$("div#scoreboard").css({ "left": left + "%", "width": (100 - left - right) + "%" });
			$(window).trigger("resize");

	});

}
