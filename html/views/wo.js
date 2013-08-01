
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>, Rob Thomas, and WrathOfJon <crgscorespam@sacredregion.com>
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

  var showClockJLTI = function() {
    if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue()) {
      $("a.ClockJLTI").closest("div").removeClass("ShowLineup ShowTimeout ShowIntermission").addClass("ShowJam");
    } else if ($sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue()) {
      $("a.ClockJLTI").closest("div").removeClass("ShowLineup ShowJam ShowIntermission").addClass("ShowTimeout");
    } else if ($sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue()) {
      $("a.ClockJLTI").closest("div").removeClass("ShowJam ShowTimeout ShowIntermission").addClass("ShowLineup");
    } else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue()) {
      $("a.ClockJLTI").closest("div").removeClass("ShowJam ShowTimeout ShowLineup").addClass("ShowIntermission");
    } else {
      $("a.ClockJLTI").closest("div").removeClass("ShowLineup ShowTimeout ShowIntermission").addClass("ShowJam");
      $("#ClockJamTime").data("AutoFit").call();
    }
  };
  var showClockP = function() {
    if ($sb("ScoreBoard.Clock(Period).Running").$sbIsTrue()) {
      $("a.ClockP").closest("div").addClass("ShowPeriod");
    } else if ($sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue()) {
      $("a.ClockP").closest("div").removeClass("ShowPeriod");
    } else {
      $("a.ClockP").closest("div").addClass("ShowPeriod");
      $("#ClockPeriodTime").data("AutoFit").call();
    }
  };

  $.each( [ "1", "2" ], function(i, team) {
    $sb("ScoreBoard.Team("+team+").AlternateName(overlay).Name").$sbElement("#Team"+team+"Name>a.AlternateName", {
      sbelement: { autoFitText: true }
    });
    $sb("ScoreBoard.Team("+team+").Name").$sbElement("#Team"+team+"Name>a.Name", {
      sbelement: { autoFitText: true }
    });
    $sb("ScoreBoard.Team("+team+").Score").$sbElement("#Team"+team+"Score>a", {
      sbelement: { autoFitText: true }
    });
    $sb("ScoreBoard.Team("+team+")").$sbBindAddRemoveEach("AlternateName", function(event, node) {
      if ($sb(node).$sbId == "overlay")
        $sb(node).$sb("Name").$sbBindAndRun("sbchange", function(event, val) {
          $("#Team"+team+"Name").toggleClass("AlternateName", ($.trim(val) != "")).data("AutoFit").call();
        });
    }, function(event, node) {
      if ($sb(node).$sbId == "overlay")
        $("#Team"+team+"Name").removeClass("AlternateName").data("AutoFit").call();
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
  
  $.each( [ "Jam", "Lineup", "Timeout", "Intermission" ], function(i, clock) {
    setupClock(clock);
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClockJLTI);
  });
  setupClock("Period");
  $sb("ScoreBoard.Clock(Period).Running").$sbBindAndRun("sbchange", showClockP);
  $sb("ScoreBoard.Clock(Intermission).Running").$sbBindAndRun("sbchange", showClockP);
  
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
  var statusTriggers = $sb("ScoreBoard.TimeoutOwner")
    .add($sb("ScoreBoard.Clock(Timeout).Running"))
    .add($sb("ScoreBoard.Clock(Lineup).Running"))
    .add($sb("ScoreBoard.Clock(Intermission).Running"))
    .add($sb("ScoreBoard.OfficialReview"));
  
  _crgUtils.bindAndRun(statusTriggers, "sbchange", function() { manageStatusBar(); });
});


function manageStatusBar() {
	
	// This is called when pretty much anything changes.
	// Update the status string 
	var statusString = "";
	// ... if a timeout is running
	if ($sb("Scoreboard.Clock(Timeout).Running").$sbIsTrue()) { 
		// Who's timeout is it?
		var timeoutOwner = $sb("ScoreBoard.TimeoutOwner").$sbGet();
		if (!timeoutOwner) {  // It's an OTO
			statusString = "Timeout";
		} else if ($sb("ScoreBoard.OfficialReview").$sbIsTrue()) {
			statusSTrimg = "Offical Rev";
		} else {
			statusString = "Team T/O";
		}			
	}
	
	// ... Is it lineup?
	if ($sb("Scoreboard.Clock(Lineup).Running").$sbIsTrue()) {
		statusString = "Lineup";
	}
	
	// Update the status bar.
	$("#StatusBar>a").html(statusString);
	
	// Update the Timeout/OR images
	$.each( [ 1, 2 ], function(x, i) {
		var thisTeam = $sb("ScoreBoard.Team("+i+")");
		var pageHTMLID = "#WftdaT"+i;
		// Have they one OR?
		if (thisTeam.$sb("OfficialReviews").$sbGet() == 0) {
			// Hide it
			$(pageHTMLID+"OR").hide();
		} else {
			// Show their OR Box
			$(pageHTMLID+"OR").show();
		}
		
		// How's their timeouts looking?
		var numTOs = thisTeam.$sb("Timeouts").$sbGet();
		for ( var timeout = 1; timeout <= 3; timeout++ ) {
			if (numTOs >= timeout )
				$(pageHTMLID+"T"+timeout).show();
			else
				$(pageHTMLID+"T"+timeout).hide();
		}
	});
}

////////////
// Animation
////////////

function setupPulsate(pulseCondition, pulseTarget, pulsePeriod) {
  var doPulse = function(next) {
    if (pulseCondition())
      pulseTarget
        .animate({ opacity: 1 }, (pulsePeriod/2), "linear")
        .animate({ opacity: 0 }, (pulsePeriod/2), "linear");
    else
      pulseTarget.delay(500);
    pulseTarget.queue(doPulse);
    next();
  };
  doPulse($.noop);
}  
