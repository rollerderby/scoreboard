$(initialize);

function initialize() {

	WS.Connect();
	WS.AutoRegister();

	// Set Styles
	var view = "View";
	if (_windowFunctions.checkParam("preview", "true"))
		view = "Preview";
	WS.Register( "ScoreBoard.Settings.ScoreBoard." + view + "_SwapTeams", function (k, v) {
		$(".Team1").toggleClass("Left", !isTrue(v)).toggleClass("Right", isTrue(v));
		$(".Team2").toggleClass("Left", isTrue(v)).toggleClass("Right", !isTrue(v));
		$(".Team").toggleClass("Swapped", isTrue(v));
	});

	WS.Register( "ScoreBoard.Settings.ScoreBoard." + view + "_CurrentView", function(k, v) {
		$("div#video>video").each(function() { this.pause(); });
		$(".DisplayPane.Show").addClass("Hide");
		$(".DisplayPane").removeClass("Show");
		$("div#" + v + ".DisplayPane").addClass("Show");
		$("div#" + v + ".DisplayPane>video").each(function() { this.currentTime = 0; this.play(); });
	});

	WS.Register( "ScoreBoard.Settings.ScoreBoard." + view + "_Image", function(k, v) {
		$("div#image>img").attr("src", v);
	});
	WS.Register( "ScoreBoard.Settings.ScoreBoard." + view + "_Video", function(k, v) {
		$("div#video>video").attr("src", v);
	});
	WS.Register( "ScoreBoard.Settings.ScoreBoard." + view + "_CustomHtml", function(k, v) {
		$("div#html>iframe").attr("src", v);
	});

	WS.Register( [ "ScoreBoard.Settings.ScoreBoard." + view + "_BoxStyle",
		"ScoreBoard.Settings.ScoreBoard." + view + "_SidePadding" ], function(k, v) {
			var boxStyle = WS.state["ScoreBoard.Settings.ScoreBoard." + view + "_BoxStyle"];
			var sidePadding = WS.state["ScoreBoard.Settings.ScoreBoard." + view + "_SidePadding"];

			// change box_flat_bright to two separate classes in order to reuse much of the css
			if (boxStyle == 'box_flat_bright')
				boxStyle = 'box_flat bright';

			$("body").removeClass();
			if (boxStyle != "" && boxStyle != null)
				$("body").addClass(boxStyle);

			left = 0;
			right = 0;
			if (sidePadding != "" && sidePadding != null) {
				left = sidePadding;
				right = left;
			}
			$("div#scoreboard").css({ "left": left + "%", "width": (100 - left - right) + "%" });
			$(window).trigger("resize");

	});
	
	$.each([1, 2], function(idx, t) {
		WS.Register([ 'ScoreBoard.Team(' + t + ').Color' ], function(k, v) {
			if (v === null) {
				v = '';
			}
			switch (k){
				case 'ScoreBoard.Team(' + t + ').Color(scoreboard_fg)':
					$('.Team' + t + ' .Name').css('color', v);
					break;
				case 'ScoreBoard.Team(' + t + ').Color(scoreboard_bg)':
					$('.Team' + t + ' .Name').css('background-color', v);
					break;
				case 'ScoreBoard.Team(' + t + ').Color(scoreboard_glow)':
					var shadow = '0px 0px 0.2em ' + v;
					var shadowCSS = shadow + ', ' + shadow + ', ' + shadow;
					if (v === '') {
						shadowCSS = '';
					}
					$('.Team' + t + ' .Name').css('text-shadow',shadowCSS);
					break;
				case 'ScoreBoard.Team(' + t + ').Color(scoreboard_dots_fg)':
					var dotColor = v;
					if (dotColor == null) {dotColor = '#000000';}
					$('.Team' + t + ' .DotTimeouts .Dot').css('background', dotColor);
					$('.Team' + t + ' .Dot.OfficialReview1').css('background', dotColor);
					
					document.styleSheets[0].addRule('.Team' + t + ' .DotOfficialReviews .Dot.Retained:before', 
							'background: ' + dotColor + ';');
					document.styleSheets[0].addRule('.Team' + t + ' .DotOfficialReviews .Dot.Retained:after', 
							'background: ' + dotColor + ';');
					break;
			}
		})
	});



	(function() {
		var switchTimeMs = 5000;
		var banners = [];
		var div = $("#SponsorBox");
		var setNextSrc = function() {
			if (banners.length == 0) {
				div.find(".NextImg>img").prop("src", "").toggle(false);
			} else {
				// Use time so different scoreboards will be using the same images.
				var index = Math.round((new Date().getTime() / switchTimeMs) % banners.length);
				div.find(".NextImg>img").prop("src", banners[index].Src).toggle(true);

				// Also set the current image. This gets a banner up when the page is loaded,
				// and is otherwise a noop.
				div.find(".CurrentImg>img").prop("src", banners[index].Src).toggle(true);
			}
		};
		var nextImgFunction = function() {
			var cur = $(div.find(".CurrentImg")[0]);
			var nex = $(div.find(".NextImg")[0]);
			var fin = $(div.find(".FinishedImg")[0]);
			cur.removeClass("CurrentImg").addClass("FinishedImg");
			nex.removeClass("NextImg").addClass("CurrentImg");
			fin.removeClass("FinishedImg").addClass("NextImg");
			setNextSrc();
			// Align to clock, so different scoreboards will be synced.
			setTimeout(nextImgFunction, switchTimeMs - (new Date().getTime() % switchTimeMs));
		};
		WS.Register(['ScoreBoard.Media.images.sponsor_banner'], {triggerBatchFunc: function() {
			var images = {};
			for (var prop in WS.state) {
				if (WS.state[prop] == null) {
					continue;
				}
				var re = /ScoreBoard.Media.images.sponsor_banner\((.*)\)\.(\w+)/;
				var m = prop.match(re);
				if (m != null) {
					images[m[1]] = images[m[1]] || {};
					images[m[1]][m[2]] = WS.state[prop];
				}
			}
			banners = Object.values(images).sort(function (a, b) {a.Id.localeCompare(b.Id, "en")});
		}});

		setNextSrc();
		nextImgFunction();
	})();

}
//# sourceURL=views\standard\index.js
