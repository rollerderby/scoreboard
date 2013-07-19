
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
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
    var aspect4x3 = _windowFunctions.get4x3Dimensions();
    div.css(aspect4x3).css("fontSize", aspect4x3.height);
  });
}

$sb(function() {
  setupMainDiv($("#mainDiv"));

  var showClockJLT = function() {
    if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue()) {
      $("a.ClockJLT").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
    } else if ($sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue()) {
      $("a.ClockJLT").removeClass("ShowLineup ShowJam").addClass("ShowTimeout");
    } else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue()) {
      $("a.ClockJLT").removeClass("ShowJam ShowTimeout").addClass("ShowLineup");
    } else {
      $("a.ClockJLT").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
    }
  };
  var showClockPI = function() {
    if ($sb("ScoreBoard.Clock(Period).Running").$sbIsTrue()) {
      $("a.ClockPI").removeClass("ShowIntermission").addClass("ShowPeriod");
    } else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue()) {
      $("a.ClockPI").removeClass("ShowPeriod").addClass("ShowIntermission");
    } else {
      $("a.ClockPI").removeClass("ShowIntermission").addClass("ShowPeriod");
    }
  };

  $.each( [ "1", "2" ], function(i, team) {
    if($sb("ScoreBoard.Team("+team+").AlternateName(overlay).Name").$sbGet()) {
      $sb("ScoreBoard.Team("+team+").AlternateName(overlay).Name").$sbElement("#Team"+team+"Name>a", {
    	sbelement: { autoFitText: true }
      }); 
	} else {
      $sb("ScoreBoard.Team("+team+").Name").$sbElement("#Team"+team+"Name>a", {
        sbelement: { autoFitText: true }
      });
	}
    $sb("ScoreBoard.Team("+team+").Score").$sbElement("#Team"+team+"Score>a", {
      sbelement: { autoFitText: true }
    });
  });

  $sb("ScoreBoard.Clock(Period).Number").$sbElement("#ClockPeriodNumber>a>span.Number", {
    sbelement: { autoFitText: true, autoFitTextContainer: "div" }
  });
  
  $sb("ScoreBoard.Clock(Jam).Number").$sbElement("#ClockJamNumber>a>span.Number", {
	    sbelement: { autoFitText: true, autoFitTextContainer: "div" }
  });

  var setupClock = function(clock) {
    $sb("ScoreBoard.Clock("+clock+").Time").$sbElement("#Clock"+clock+"Time>a", {
      sbelement: {
        autoFitText: true,
        convert: _timeConversions.msToMinSec
     } });
  };
  
  $.each( [ "Jam", "Lineup", "Timeout" ], function(i, clock) {
    setupClock(clock);
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClockJLT);
  });
  $.each( [ "Period", "Intermission" ], function(i, clock) {
    setupClock(clock);
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClockPI);
  });
  
  // Team Timeouts.
  $.each( [ 1, 2 ], function(i, team) {
    $sb("ScoreBoard.Team("+team+").Timeouts").$sbElement("#Team"+team+"TimeOuts>a", { sbelement: { autoFitText: true } });
  });

  // It puts the logo in the box, or else it gets the hose again.
  $.each( [ 1, 2 ], function(i, team) {
	  $sb("ScoreBoard.Team("+team+").Logo").$sbElement("#Team"+team+"Logo>img", { sbelement: { autoFitText: true, autoFitTextContainer: "img" } });
  });
  
  // Disable or Enable Logos
  $sb("ScoreBoard.Policy(PagePolicy_overlay.html).Enabled").$sbBindAndRun("sbchange", function(x, state) {
	  // Note that 'state' is a string, not a Bool.
	  if (state == "true") {
		  $(".logos>img").height("100%");
		  $(".logos>img").width("100%");
		  $(".logos").show(100);
	  } else {
		  $(".logos>img").height("0");
		  $(".logos>img").width("0");
		  $(".logos").hide(100);
	  }
  });
  
  // Toggle black background on logos
  $sb("Scoreboard.Policy(PagePolicy_overlay.html).Parameter(Black Background).Value").$sbBindAndRun("sbchange", function(x, state) {
	  if (state == "true") {
		  $(".logos").css("background-color", "black");
	  } else {
		  $(".logos").css("background-color", "#0f0");
	  }
  });
  
  // Statusbar text.
  var statusTriggers = $sb("ScoreBoard.TimeoutOwner").
    add($sb("ScoreBoard.Clock(Timeout).Running").
    add($sb("ScoreBoard.Clock(Lineup).Running").
    add($sb("ScoreBoard.OfficialReview"))));
  
  _crgUtils.bindAndRun(statusTriggers, "sbchange", function() { manageStatusBar(); });
  
});


function manageStatusBar() {
  // Display status bar in Lineup, Timeout, TTO and OR.
  if ($sb("ScoreBoard.Clock(Jam).Running").$sbGet() == "true") {
	// Make sure that the timeouts are back to pink
	$(".TimeOuts").animate({"background-color":'pink'}, 500);
    $("#StatusBar").hide();
  } else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbGet() == "true") {
	// You should NEVER go from a Penalty to Lineup. But, just in case someone does...
	$(".TimeOuts").animate({"background-color":'pink'}, 500);
	$("#StatusBar>a").html("Lineup");
	$("#StatusBar").show();
  } else {
	var timeoutOwner = $sb("ScoreBoard.TimeoutOwner").$sbGet();
	var statusString = "Error";
	if (!timeoutOwner) {
	  // It's an OTO
	  statusString = "Timeout";
	  $(".TimeOuts").animate({"background-color":'pink'}, 500);
	} else {
	  // It's owned. It'll either be an OR or a TTO.
	  // Set the background of the owning team to red.
	  $("#Team"+timeoutOwner+"TimeOuts").animate({"background-color":'red'}, 500);
	  if ($sb("ScoreBoard.OfficialReview").$sbGet() == "true") {
		  statusString = "Official Review";
	  } else {
		  statusString = "Team Timeout";
	  }
	}
	$("#StatusBar>a").html(statusString);
	$("#StatusBar").show();
  }
}