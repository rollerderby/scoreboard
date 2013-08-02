/**
 * Copyright (C) 2008-2013 Mr Temper <MrTemper@CarolinaRollergirls.com>, Rob Thomas, and WrathOfJon <crgscorespam@sacredregion.com>
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
    var aspect16x9 = _windowFunctions.get16x9Dimensions();
    div.css(aspect16x9).css("fontSize", aspect16x9.height);
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
    
    // Pulsate Timeouts if they're currently active. They'll be hidden in manageTimeoutImages
    $.each( [ 0, 1, 2 ], function(x, i) {
    	setupPulsate( 
    			function() { return (
    					$sb("ScoreBoard.Team(1).Timeouts").$sbGet() == i && 
    					$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
    					!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
    					$sb("ScoreBoard.TimeoutOwner").$sbGet() == 1); },
        			$("#WftdaT1T"+(i+1)),
        			1000
        		);
    	setupPulsate( 
        		function() { return (
        				$sb("ScoreBoard.Team(2).Timeouts").$sbGet() == i && 
    					$sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue() &&
    					!$sb("ScoreBoard.OfficialReview").$sbIsTrue() && // Note, Negated. NOT Official Review
    					$sb("ScoreBoard.TimeoutOwner").$sbGet() == 2); },
            		$("#WftdaT2T"+(i+1)),
            		1000
            	);
    });
    
    // Pulsate OR buttons.
    $.each( [ 1, 2 ], function(x, i) {
    	setupPulsate(
    		function() { return (
    				$sb("ScoreBoard.OfficialReview").$sbIsTrue() &&
    				$sb("ScoreBoard.TimeoutOwner").$sbGet() == i) },
    			$("#WftdaT"+i+"OR"),
    			1000
    	);
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
 
  // Statusbar text.
  var statusTriggers = $sb("ScoreBoard.Clock(Jam).Running")
    .add($sb("ScoreBoard.Clock(Timeout).Running"))
    .add($sb("ScoreBoard.Clock(Lineup).Running"))
    .add($sb("ScoreBoard.Clock(Intermission).Running"))
    .add($sb("ScoreBoard.OfficialReview"));
  _crgUtils.bindAndRun(statusTriggers, "sbchange", function() { manageStatusBar(); });
  
  // Timeout images
  var timeoutTriggers = $sb("ScoreBoard.Team(1).OfficialReviews")
  	.add($sb("ScoreBoard.Team(1).Timeouts"))
  	.add($sb("ScoreBoard.Team(2).OfficialReviews"))
  	.add($sb("ScoreBoard.Team(2).Timeouts"))
  	.add($sb("ScoreBoard.TimeoutOwner"));
  _crgUtils.bindAndRun(timeoutTriggers, "sbchange", function() { manageTimeoutImages(); });
  
  // Lead Changes
  $.each([ 1, 2 ], function(i, t) {
    $sb("ScoreBoard.Team("+t+").LeadJammer").$sbBindAndRun("sbchange", function(event, val) {
      $("#WftdaT"+t+"LD").toggleClass("Show", isTrue(val), 1000);
    });
  });
});




function manageStatusBar() {
	// This is called when pretty much anything changes,  and updates the status string 
	var statusString = "Stand By";
	// Is it jam?
	if ($sb("Scoreboard.Clock(Jam).Running").$sbIsTrue()) {
		statusString = "Jam";
	}

	// Is a timeout is running?
	if ($sb("Scoreboard.Clock(Timeout).Running").$sbIsTrue()) { 
		// Who's timeout is it?
		var timeoutOwner = $sb("ScoreBoard.TimeoutOwner").$sbGet();
		if (!timeoutOwner) {  // It's an OTO
			statusString = "Timeout";
		} else if ($sb("ScoreBoard.OfficialReview").$sbIsTrue()) {
			statusString = "Review";
		} else {
			statusString = "Team T/O";
		}			
	}
	
	// Is it lineup?
	if ($sb("Scoreboard.Clock(Lineup).Running").$sbIsTrue()) {
		statusString = "Lineup";
	}

	// This needs work, but perhaps it's the last piece?
	if ($sb("Scoreboard.Clock(Intermission).Running").$sbIsTrue()) {
		statusString = "I/M";
	}

	// WFTDA says always show the bar - show/hide stuff is now gone
	// That's good, since we can consolidate those images and speed things up a bit
	$("#StatusBar>a").html(statusString); 
		
}

function manageTimeoutImages() {
	// Called when something changes in relation to timeouts.
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
        .show()
        .animate({ opacity: 0 }, (pulsePeriod/2), "linear")
        .animate({ opacity: 1 }, (pulsePeriod/2), "linear");
    else
      pulseTarget.delay(500);
    pulseTarget.queue(doPulse);
    next();
  };
  doPulse($.noop);
}  

