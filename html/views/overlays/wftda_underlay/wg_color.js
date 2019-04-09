/**
 * Copyright (C) 2008-2013 Mr Temper <MrTemper@CarolinaRollergirls.com>, Rob Thomas, and WrathOfJon <crgscorespam@sacredregion.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "Pages(Overlay),ScoreBoard";

function setupMainDiv(div) {
  div.css({ position: "fixed" });

  _crgUtils.bindAndRun($(window), "resize", function() {
    var aspect16x9 = _windowFunctions.get16x9Dimensions();
    div.css(aspect16x9).css("fontSize", aspect16x9.height);
  });
  av.initialize();
  // div.bind("click", function() { av.initialize(); });
}

$sb(function() {
  WS.Connect();
  WS.AutoRegister();
  setupMainDiv($("#mainDiv"));

  var showClocks = function() {
    var pR = $sb("ScoreBoard.Clock(Period).Running").$sbIsTrue();
    var jR = $sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue();
    var lR = $sb("ScoreBoard.Clock(Lineup).Running").$sbIsTrue();
    var tR = $sb("ScoreBoard.Clock(Timeout).Running").$sbIsTrue();
    var iR = $sb("ScoreBoard.Clock(Intermission).Running").$sbIsTrue();
    var iN = $sb("ScoreBoard.Clock(Intermission).Number").$sbGet();

    if (jR) {
      $("a.ClockJLT").closest("div").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
    } else if (tR) {
      $("a.ClockJLT").closest("div").removeClass("ShowLineup ShowJam").addClass("ShowTimeout");
    } else if (lR) {
      $("a.ClockJLT").closest("div").removeClass("ShowJam ShowTimeout").addClass("ShowLineup");
    } else if (iR) {
      $("a.ClockJLT").closest("div").removeClass("ShowJam ShowTimeout ShowLineup");
    } else {
      $("a.ClockJLT").closest("div").removeClass("ShowLineup ShowTimeout").addClass("ShowJam");
    }

    if (pR) {
      $("a.ClockPI").closest("div").removeClass("ShowIntermission").addClass("ShowPeriod");
    } else if (iR && !jR && !lR && !tR) {
      if (iN == 2) { // Hide intermission clock too for Final
        $("a.ClockPI").closest("div").removeClass("ShowPeriod ShowIntermission");
      } else {
        $("a.ClockPI").closest("div").removeClass("ShowPeriod").addClass("ShowIntermission");
      }
    } else {
      $("a.ClockPI").closest("div").removeClass("ShowIntermission").addClass("ShowPeriod");
    }
  };

  $.each( [ "1", "2" ], function(i, team) {

    $sb("ScoreBoard.Team("+team+").AlternateName(overlay).Name").$sbElement("#Team"+team+"Name>a.AlternateName");
    $sb("ScoreBoard.Team("+team+").Name").$sbElement("#Team"+team+"Name>a.Name");
/*    $sb("ScoreBoard.Team("+team+").Color(overlay)").$sbBindColors($("#Team"+team+"Name")); */
//  We can do this better (i.e. deal with fg and bg colors separately) with WS.state stuff - fixed this
//    _crgUtils.bindColors("ScoreBoard.Team("+team+")", "overlay", $("#Team" + team + "Name"));
//    _crgUtils.bindColors("ScoreBoard.Team("+team+")", "swatch", $("#Team" + team + "Swatch"));

    WS.Register([ 'ScoreBoard.Team(' + team + ').Color' ], function(k, v) { $('#Team' + team + 'Name').css('color', WS.state['ScoreBoard.Team(' + team + ').Color(overlay_fg)']); $('#Team' + team + 'Swatch').css('background-color', WS.state['ScoreBoard.Team(' + team + ').Color(overlay_bg)']); $('#head' + team).css('background-color', WS.state['ScoreBoard.Team(' + team + ').Color(overlay_bg)']); } );


    $sb("ScoreBoard.Team("+team+").Score").$sbElement("#Team"+team+"Score>a");
    $sb("ScoreBoard.Team("+team+").JamScore").$sbElement("#Team"+team+"JamPoints>a");
    $sb("ScoreBoard.Team("+team+")").$sbBindAddRemoveEach("AlternateName", function(event, node) {
      if ($sb(node).$sbId == "overlay")
        $sb(node).$sb("Name").$sbBindAndRun("sbchange", function(event, val) {
          $("#Team"+team+"Name").toggleClass("AlternateName", ($.trim(val) != ""));
        });
    }, function(event, node) {
      if ($sb(node).$sbId == "overlay")
        $("#Team"+team+"Name").removeClass("AlternateName");
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

  $sb("ScoreBoard.Clock(Period).Number").$sbElement("#ClockPeriodNumber>a>span.Number");
  
  $sb("ScoreBoard.Clock(Jam).Number").$sbElement("#ClockJamNumber>a>span.Number");

  $.each( [ "Period", "Intermission", "Jam", "Lineup", "Timeout" ], function(i, clock) {
    $sb("ScoreBoard.Clock("+clock+").Time").$sbElement("#Clock"+clock+"Time>a", {
      sbelement: { convert: _timeConversions.msToMinSec } });
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", showClocks);
  });
  // This allows hiding the intermission clock during Final.
  $sb("ScoreBoard.Clock(Intermission).Number").$sbBindAndRun("sbchange", showClocks);



  // The following was brutally ripped from overlay.js . Team2 stuff is left in to make switching the logo a matter of a simple html edit for now

  $sb("Pages.Page(Overlay).Logo").$sbElement("#OverlayLogo>img", { sbelement: { autoFitText: true, autoFitTextContainer: "img" } });



  // Statusbar text.
  var statusTriggers = $sb("ScoreBoard.Clock(Jam).Running")
    .add($sb("ScoreBoard.Clock(Timeout).Running"))
    .add($sb("ScoreBoard.Clock(Lineup).Running"))
    .add($sb("ScoreBoard.Clock(Intermission).Running"))
    .add($sb("ScoreBoard.Clock(Intermission).Number"))
    .add($sb("ScoreBoard.TimeoutOwner"))
    .add($sb("ScoreBoard.OfficialReview"))
    .add($sb("ScoreBoard.OfficialScore"))
    .add($sb("ScoreBoard.InOvertime"));
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
    $sb("ScoreBoard.Team("+t+").DisplayLead").$sbBindAndRun("sbchange", function(event, val) {
      $("#WftdaT"+t+"LD").toggleClass("Show", isTrue(val), 1000);
    });
  });
});




function manageStatusBar() {
	// This is called when pretty much anything changes,  and updates the status string 
	var statusString = "Stand By";

	if ($sb("Scoreboard.InOvertime").$sbIsTrue()) {
		statusString = "Overtime";
	} else if ($sb("Scoreboard.Clock(Jam).Running").$sbIsTrue()) {
		statusString = "Jam";
	} else if ($sb("Scoreboard.Clock(Timeout).Running").$sbIsTrue()) { 
		// Who's timeout is it?
		if (!$sb("ScoreBoard.TimeoutOwner").$sbGet()) {
			statusString = "Timeout";
		} else if ($sb("ScoreBoard.OfficialReview").$sbIsTrue()) {
			statusString = "Review";
		} else {
			statusString = "Team T/O";
		}			
	} else if ($sb("Scoreboard.Clock(Lineup).Running").$sbIsTrue()) {
		statusString = "Lineup";
	} else if ($sb("Scoreboard.Clock(Intermission).Running").$sbIsTrue()) {
		var iNum = $sb("ScoreBoard.Clock(Intermission).Number").$sbGet();
		var official = $sb("ScoreBoard.OfficialScore").$sbIsTrue();
		if (iNum == 0)
			statusString = "Prebout";
		else if (iNum == 1)
			statusString = "Halftime";
		else if (iNum == 2)
			statusString = (official ? "Final" : "Unofficial");
	}



        // If the statusString has changed, fade out the old, and in the new
        var statusBar = $("#StatusBar>a");
        if (statusBar.data('current') != statusString) {
                statusBar.data('current', statusString);
                if (statusBar.data('fadingOut') != true) {
                        statusBar.queue(function(next) { $(this).data('fadingOut', true); next(); });
                        statusBar.animate({opacity: 0}, 500, function() {
                                $(this).data('fadingOut', false);
                                $(this).text($(this).data('current'));
                        });
                        statusBar.animate({opacity: 1}, 500);
                }
        }
        // $("#StatusBar>a").html(statusString);

		
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




// Audio Video Source
var av = {
        videoElement: null,
        audioSource: '',
        videoSource: '',

        gotSources: function (sourceInfos) {
//                                console.log('Did we make it here? 2');
                for (var i = 0; i !== sourceInfos.length; ++i) {
                        var sourceInfo = sourceInfos[i];
                        console.log(sourceInfo);
                        if (sourceInfo.kind === 'audio' && av.audioSource == "") {
                                av.audioSource = sourceInfo.id;
                        } else if (sourceInfo.kind === 'video' && ( i == 2 ) ) {
                                av.videoSource = sourceInfo.id;
                                console.log('Selecting 2nd camera');
                        } else {
                                console.log('Some other kind of source: ', sourceInfo);
                        }
                }
                av.start();
        },

        initialize: function() {
                // if (typeof MediaStreamTrack === 'undefined'){
                if (typeof navigator.mediaDevices === 'undefined'){
                        alert('This browser does not support navigator.mediaDevices.\n\nTry Chrome Canary.');
                } else {
                        //MediaStreamTrack.getSources(av.gotSources);
                        navigator.mediaDevices.enumerateDevices()
                        .then(av.gotSources);
                        // av.start();
                }
        },

        successCallback: function(stream) {
                window.stream = stream; // make stream available to console

                if (av.videoElement == null) {
                        av.videoElement = document.createElement("video");
                        av.videoElement.className = 'video_underlay';
                        document.body.appendChild(av.videoElement);
                }

                av.videoElement.src = window.URL.createObjectURL(stream);
                av.videoElement.play();
                $(document.body).addClass("HasUnderlay");
        },

        errorCallback: function(error) {
                console.log('navigator.getUserMedia error: ', error);
        },

        start: function() {
                if (!!window.stream) {
                        videoElement.src = null;
                        window.stream.stop();
                }
                var constraints = {
                        audio: {
                                optional: [{sourceId: av.audioSource}]
                        },
                        video: {
                                optional: [{sourceId: av.videoSource}]
                        },
                        width: {min: 640, ideal: window.innerWidth},
                        height: {min: 480, ideal: window.innerHeight},
                        aspectRatio: 1.5,
                };
                console.log(constraints);
                console.log(window);

                var getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
                navigator.webkitGetUserMedia(constraints, av.successCallback, av.errorCallback);
        }
}

