
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 * Penalty Timing (C) 2013 Rob Thomas (The G33k) <xrobau@gmail.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

XML_ELEMENT_SELECTOR = "ScoreBoard";

$sb(function() {
  setupJamControlPage();
  setupPeriodTimePage();
  setupTeamScorePage();
  setupPenaltyTimePage();
});

function setupJamControlPage() {
  $sb("ScoreBoard.StartJam").$sbControl("#JamControlPage button.StartJam").val(true);
  $sb("ScoreBoard.StopJam").$sbControl("#JamControlPage button.StopJam").val(true);
  $sb("ScoreBoard.Timeout").$sbControl("#JamControlPage button.Timeout").val(true);
  $sb("ScoreBoard.Team(1).Timeout").$sbControl("#team1timeout").val(true);
  $sb("ScoreBoard.Team(2).Timeout").$sbControl("#team2timeout").val(true);

  $.each( [ "Period", "Jam", "Timeout" ], function(i, clock) {
    $sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("content", function(event, value) {
      $("#JamControlPage span.ClockBubble."+clock).toggleClass("Running", isTrue(value));
    });
  });
  
  // Period Clock
  $sb("ScoreBoard.Clock(Period).Time").$sbElement("#periodtime a", { sbelement: {
	    convert: _timeConversions.msToMinSec
  }});
  
  // Lineup Clock
  $sb("Scoreboard.Clock(Lineup).Time").$sbElement("#lineupdiv h1", { sbelement: {
	    convert: _timeConversions.msToMinSec
  }});

  // Jam Clock
  $sb("Scoreboard.Clock(Jam).Time").$sbElement("#jamdiv h1", { sbelement: {
	    convert: _timeConversions.msToMinSec
  }});

  // Timeout Clock
  $sb("Scoreboard.Clock(Timeout).Time").$sbElement("#timeoutdiv h1", { sbelement: {
	    convert: _timeConversions.msToMinSec
  }});

  
  // Update Period number
  $sb("ScoreBoard.Clock(Period).Number").$sbElement("#periodno");
  
  // Display text and info for what's happening. 
  // Lineup Clock
  $sb("ScoreBoard.Clock(Lineup).Running").$sbBindAndRun("content", function(e, v) {
	  if (v == "true") {
		  $.each([ '#jamdiv', '#timeoutdiv'], function(i, divname) {
			  $(divname).fadeOut('fast');
		  });
		  $("#lineupdiv").fadeIn('fast');
	  }
  });
  
  // Jam Clock
  $sb("ScoreBoard.Clock(Jam).Running").$sbBindAndRun("content", function(e, v) {
	  if (v == "true") {
		  $.each([ '#lineupdiv', '#timeoutdiv'], function(i, divname) {
			  $(divname).fadeOut('fast');
		  });
		  $("#jamdiv").fadeIn('fast');
	  }
  });
  
  // Timeout Clock
  $sb("ScoreBoard.Clock(Timeout).Running").$sbBindAndRun("content", function(e, v) {
	  if (v == "true") {
		  $.each([ '#jamdiv', '#lineupdiv'], function(i, divname) {
			  $(divname).fadeOut('fast');
		  });
		  $("#timeoutdiv").fadeIn('fast');
	  }
  });

  
  // 
  
}

function setupPeriodTimePage() {
  var time = $sb("ScoreBoard.Clock(Period).Time");

  time.$sbElement("#PeriodTimePage a.Time", { sbelement: {
    convert: _timeConversions.msToMinSec
  }});

  time.$sbControl("#PeriodTimePage button.TimeDown", { sbcontrol: {
    sbSetAttrs: { change: true }
  }});
  time.$sbControl("#PeriodTimePage button.TimeUp", { sbcontrol: {
    sbSetAttrs: { change: true }
  }});

  time.$sbControl("#PeriodTimePage input:text.SetTime,#PeriodTimePage button.SetTime", {
    sbcontrol: {
      convert: _timeConversions.minSecToMs,
      delayupdate: true,
      noSetControlValue: true
    }
  });
}

function setupPenaltyTimePage() {

  var ptime = $sb("ScoreBoard.Clock(Period).Time");
  var jtime = $sb("ScoreBoard.Clock(Jam).Time");
    
  jtime.$sbElement("#PenaltyPage a.JamTime", { sbelement: {
    convert: _timeConversions.msToMinSec
  }});
  ptime.$sbElement("#PenaltyPage a.PeriodTime", { sbelement: {
	    convert: _timeConversions.msToMinSec
	  }});
  // Set the times to 1:00
  $.each(["#Jammer1", "#Blocker1Team1","#Blocker2Team1", "#Blocker3Team1", "#Jammer2", "#Blocker1Team2","#Blocker2Team2", "#Blocker3Team2"], function(i, v) {
	  $(v).data('timeleft', 60000);
	  $(v).data('isrunning', false);
	  $(v).data('endtime', 0);
	  $(v).data('paused', false);
	  $(v).data('enabled', true);
	  $(v).data('set', 0);
	  $(v+"Time").hide();
  });
  $("#Blocker1Team1").data('enabled', true);
  $("#Blocker2Team1").data('enabled', true);
  $("#Blocker1Team2").data('enabled', true);
  $("#Blocker2Team2").data('enabled', true);
  $sb("ScoreBoard.Clock(Period).Time").$sbBindAndRun("content", function(e, v) { updatePenaltyClocks(parseInt(v)) });
  updatePenaltyClocks(1800000);
 
}


function setupTeamScorePage() {
  $.each([ "1", "2" ], function(i, n) {
    var team = $sb("ScoreBoard.Team("+n+")");
    var score = team.$sb("Score");

    $.each([ "#Team"+n+"ScorePage", "#TeamBothScorePage" ], function(ii, e) {
      team.$sb("Name").$sbElement(e+" a.Team"+n+".Name");
      score.$sbElement(e+" a.Team"+n+".Score");
      score.$sbControl(e+" button.Team"+n+".ScoreDown", { sbcontrol: {
        sbSetAttrs: { change: true }
      }});
      score.$sbControl(e+" button.Team"+n+".ScoreUp", { sbcontrol: {
        sbSetAttrs: { change: true }
      }});

    });

    score.$sbControl("#TeamBothScorePage input[type='number'].Team"+n+".SetScore,#TeamBothScorePage button.Team"+n+".SetScore", {
      sbcontrol: {
        delayupdate: true,
        noSetControlValue: true
      }
    });
  });
}
 
function updatePenaltyClocks(periodClock) {
	var dateobj = new Date();

	// Do we need to enable or disable any blocker seats?
	$.each(['Team1', 'Team2'], function(e, v) {
		
		// Logic is:
		// Are there two blockers in the box? Does one of them have less than 10 seconds left? If so, enable all buttons.
		// else
		//   if button 3 is running, disable which ever one isn't running.
		//   else
		//   disable button 3, enable button 1 and 2. 
		var blockers = 0;
		var standing = 0;
		$.each(['Blocker1', 'Blocker2', 'Blocker3'], function (x, blocker) {
			if ($('#'+blocker+v).data('isrunning')) { blockers++; }
			if ($('#'+blocker+v).data('timeleft') < 12000 ) { standing++; }
		}); 
		if (blockers >= 2 && standing !=  0) {
			// Enable all the blockers.
			$("#Blocker3"+v).data('enabled', true);
			$("#Blocker1"+v).data('enabled', true);
			$("#Blocker2"+v).data('enabled', true);
		} else {
			// If b3 is running, disable whichever one isn't.
			if ($("#Blocker3"+v).data('isrunning')) {
				if (!$("#Blocker1"+v).data('isrunning') && $("#Blocker2"+v).data('isrunning') ) { $("#Blocker1"+v).data('enabled', false); } 
				if (!$("#Blocker2"+v).data('isrunning') && $("#Blocker1"+v).data('isrunning') ) { $("#Blocker2"+v).data('enabled', false); }
				// If ONLY B3 is running, disable B2 and enable B1
				if (!$("#Blocker2"+v).data('isrunning') && !$("#Blocker1"+v).data('isrunning') ) {
					$("#Blocker1"+v).data('enabled', true);
					$("#Blocker2"+v).data('enabled', false); 
				}
			} else {
				$("#Blocker3"+v).data('enabled', false);
				$("#Blocker1"+v).data('enabled', true);
				$("#Blocker2"+v).data('enabled', true);
			}
		} 
	});
	
	$.each(["#Jammer1", "#Blocker1Team1","#Blocker2Team1", "#Blocker3Team1", "#Jammer2", "#Blocker1Team2","#Blocker2Team2", "#Blocker3Team2"], function(i, v) {
		if ($(v).data('isrunning') == true) {
			// Update the timeleft with the actual time left.
			if ($sb("ScoreBoard.Clock(Jam).Running").$sbIsTrue() && $(v).data('paused') == false) {
				$(v).data('timeleft', periodClock - $(v).data('endtime'));
			} else {
				$(v).data('endtime', periodClock - $(v).data('timeleft'));
			}
			// Has it hit zero? Reset it.
			if ($(v).data('timeleft') < 1) {
				$(v+"Box").css('background-color', '');
				$(v).data("isrunning", false);
				$(v).data("timeleft", 60*1000);
				$(v).parent().removeClass('ui-btn-up-e').removeClass('ui-btn-hover-e');
				$(v).parent().attr("data-theme", "b").trigger("mouseout");
			} else if ($(v).data('timeleft') < 3500) { // Colour, Number, go.
				$(v+"Box").css('background-color', 'red');
			} else if ($(v).data('timeleft') < 13500) { // Colour, Number, stand.
				$(v+"Box").css('background-color', 'yellow');
			}
		
			dateobj.setTime($(v).data('timeleft'));
		
			// Pad seconds with leading zero if needed
			var seconds = dateobj.getSeconds();
			if (seconds < 10) {
				seconds = "0"+seconds.toString(16);
			}
			$(v+"Time").html(dateobj.getMinutes()+":"+seconds);
		}
		
		// Do we need to enable or disable this clock?
		if ($(v).data('enabled') == true) {
			// Does it need to be enabled?
			if ($(v+"Time").is(':hidden')) {
				// Enable it.
				$(v).unbind('click');
				$(v).click(function(){ penaltyButtonClicked($(this)) });
				$(v).parent().removeClass('ui-btn-up-a').removeClass('ui-btn-hover-a');
				$(v).parent().attr('data-theme', 'b').trigger("mouseout");
				$(v+"Time").html("1:00");
				$(v+"Time").show();
			}
		} else { // It needs to be disabled. 
			if (!$(v+"Time").is(':hidden')) {
				// Disable it.
				$(v).unbind('click');
				$(v).parent().removeClass('ui-btn-up-b').removeClass('ui-btn-hover-b');
				$(v).parent().attr('data-theme', 'a').trigger("mouseout");
				$(v+"Time").hide();
			}
		}
		
	});
	

	
	// Only if BOTH Jammers are out of the bin, reset both their sets to zero.
	if (!$("#Jammer1").data('isrunning') && !$("#Jammer2").data('isrunning')) {
		$("#Jammer1").data('set', 0);
		$("#Jammer2").data('set', 0);
	}
}

function penaltyButtonClicked(bObj) {

	// navigator.vibrate(500);  // This doesn't work on ANYTHING yet. Sigh.
	var isrunning = bObj.data('isrunning');
	console.log('isrunning is '+isrunning);
	if (isrunning == false) {
		console.log(bObj.attr('id'))
		if (bObj.attr('id') == 'Jammer1' || bObj.attr('id') == 'Jammer2') {
			// It's not running, and a Jammer button has been selected. Open up the jammer popup,
			// and ask how long it's for.
			$.each(["#BtnJammer1m", "#BtnJammer2m", "#BtnJammerCancel"], function (v, e) {
				$(e).unbind("click");
			});
			$("#BtnJammer1m").click(function() { jammerin(bObj, 1)});
			$("#BtnJammer2m").click(function() { jammerin(bObj, 2)});
			$("#BtnJammerCancel").click(function() { $("#JammerPopup").popup("close"); });
			$("#JammerPopup").popup("open");
			
		} else {
			// Not a Jammer, just a blocker. They're simple.
			enablePenaltyButton(bObj, 60000);
		}
	} else {
		// The clock is running. Remove any left over event handlers..
		 $.each(["#BtnCancel", "#BtnClock", "#BtnAdd1", "#BtnDel1"], function (v, e) {
			$(e).unbind("click");
		});
		// Add some new ones
		$("#BtnCancel").click(function() { cancelPenalty(bObj) });
		$("#BtnClock").click(function() { togglePausePenalty(bObj) });
		$("#BtnAdd1").click(function() { 
			// Now, before we just wildly give them an extra minute, does the OTHER jammer have more than 60 secs? We don't care
			// about sets here, the rules just say 'more than 60 seconds'.
			var j = bObj.attr('id'); // Which jammer is this
			var other = (j == 'Jammer1')?'#Jammer2':"#Jammer1"; // This is the other jammer.
			if ($(other).data('timeleft') >= 60000 && $(other).data('isrunning')) {
				bObj.data('timeleft', bObj.data('timeleft') - 60000); 
				bObj.data('endtime', bObj.data('endtime') + 60000);
				// Decrease the other jammers set
				$(other).data('set', $(other).data('set') - 1);
			} else { // Other jammer has LESS than 60 secs.
				// Firstly, increase the set
				bObj.data('set', parseInt(bObj.data('set') + 1));
				console.log('Increasing set in btnadd1 - is now '+parseInt(bObj.data('set')));
			
				bObj.data('timeleft', bObj.data('timeleft') + 60000); 
				bObj.data('endtime', bObj.data('endtime') - 60000);
				$("#"+bObj.attr('id')+"Box").css('background-color', '');
				$("#PenaltyPopup").popup("close"); 
			}
		}); 
		$("#BtnDel1").click(function() { 
			bObj.data('timeleft', bObj.data('timeleft') - 60000); 
			bObj.data('endtime', bObj.data('endtime') + 60000);
			$("#PenaltyPopup").popup("close");
			bObj.data('set', parseInt(bObj.data('set') - 1));
			console.log('Decreasing set in btndel1 - is now '+parseInt(bObj.data('set')));
		});	
		// Set the details in the popup window
		$("#timeremaining").data('timeleft', bObj.data('timeleft'));
		$("#timeremaining").html(parseInt(bObj.data('timeleft')/1000));
		$("#PenaltyPopup").popup("open");
	 
	} 
}

function cancelPenalty(o) {
	o.data("isrunning", false);
	o.data("timeleft", 60*1000);
	$("#"+o.attr('id')+"Time").html("1:00");
	o.parent().removeClass('ui-btn-up-e').removeClass('ui-btn-hover-e');
	o.parent().attr("data-theme", "b").trigger("mouseout"); 
	$("#PenaltyPopup").popup("close");
	$("#"+o.attr('id')+"Box").css('background-color', '');
}

function togglePausePenalty(o) {
	if (o.data('paused') == false) {
		o.data('paused', true);
		// Change the colour of the button so they know it's paused.
		o.parent().removeClass('ui-btn-up-e').removeClass('ui-btn-hover-e');
		o.parent().attr("data-theme", "a").trigger('mouseout');
		// Set the time left to when the FIRST button was pushed, not when the
		// pause button was pushed. 
		o.data('timeleft', $("#timeremaining").data('timeleft'));
	} else {
		o.data('paused', false);
		o.parent().removeClass('ui-btn-up-a').removeClass('ui-btn-hover-a');
		o.parent().attr("data-theme", "e").trigger('mouseout');
	}
	$("#PenaltyPopup").popup("close");
}

function jammerin(o, t) {
	
	// OK, so this is probably the most complex part of the code. I'll try to make it readable.
	// Jammer rules! 
	// 7.3 - Jammers serve the same penalty length.
	// 7.3.1 - If a jammer RETURNS to the bin while the other is still serving, it's a different set. Keep. 
	// 7.3.2 - Both jammers sit in the box at the same time, they both get 10 seconds. (take as both have 60sec at the start of a jam)
	// 7.3.10 - If one jammer has more than 1min, and another jammer sits, immediately release j2, subtract 1m from j1.

	var j = o.attr('id'); // Which jammer is this
	var other = (j == 'Jammer1')?'#Jammer2':"#Jammer1"; // This is the other jammer.
	
	// Sets! We care about sets. 
	o.data('set', parseInt(o.data('set') + t));
	
	// Lets start with the easy one.
	// Jammer comes in, there's no other jammer.
	if ( !$(other).data('isrunning') ) {
		console.log('NOTrunning - this is '+o.data('set')+', other is '+$(other).data('set'));
		enablePenaltyButton(o, t*60000);
		$("#JammerPopup").popup('close');
		return;
	}
	
	// Jammer comes in, there's another jammer already there 
	if ($(other).data('isrunning')) {
			
		console.log('isrunning - this is '+o.data('set')+', other is '+$(other).data('set'));
				
		if ($(other).data('timeleft') > 60000) {
			// 7.3.10 explicity says that if the other jammer has more than 1min, release the arriving jammer (eg, this one)
			// immediately. She may still stay sitting if this was a two minute, but still..
			$(other).data('timeleft', $(other).data('timeleft') - 60000);
			$(other).data('endtime', $(other).data('endtime') + 60000);
			// Remove a set from the other jammer, as if it never happened!
			$(other).data('set', parseInt($(other).data('set') - 1));
			if (t == 1) {
				// You may now go.
				// FIXME - add a message saying release this jammer.
				enablePenaltyButton(o, 500);
				o.data('set', parseInt(o.data('set') - 1));
				$("#JammerPopup").popup('close');
				return;
			}
			// else 
			
			// Someone's come in with two minutes and the OTHER jammer has more than one.
			// This seems a bit complex, but it's not. 
			// Firstly, we're cancelling out the minute on both. (7.2.10) Bam, first problem solved.
			enablePenaltyButton($(other), 60000 - $(other).data('timeleft'));
			t == 1;
			// Remove the set from the other.
			$(other).data('set', parseInt($(other).data('set') - 1));
			// Now, it's just a matter making sure they're on the same set, and sending the first jammer out.
			// This'll happen automatically in the next bit.
		} 

		// If the jammers are on the same set, then the other can be released, and this one sits for the same time the other
		// one was in there for (plus, possibly, an extra minute)
	
		console.log('This one is '+o.data('set')+', the other is '+$(other).data('set'));
		if (o.data('set') == $(other).data('set')) {
			// Are they both on EXACTLY 60 seconds? If so, they've both been bad, and deserve 10 seconds in the bin.
			// (7.3.2)
			if (t == 1 && parseInt($(other).data('timeleft')) == 60000) {
				console.log('derp');
				enablePenaltyButton(o, 10000);
				enablePenaltyButton($(other), 10000);
				$("#JammerPopup").popup('close');
				return;
			}
			// Release other, set this one to the same time.
			if (t == 1) {
				console.log('wat '+o.data('timeleft'));
				enablePenaltyButton(o, 60000 - $(other).data('timeleft'));
				$(other).data('timeleft', 0);
				$(other).data('endtime', $(other).data('endtime') + 60000);
			} else {
				console.log('foop');
				enablePenaltyButton(o, 120000 - $(other).data('timeleft'));
				$(other).data('timeleft', 0);
				$(other).data('endtime', $(other).data('endtime') + 120000);
				o.data('set', parseInt(o.data('set') + 1)); // They're on the next set now.
			}
			$("#JammerPopup").popup('close');
			return;
		} else {
			console.log('OK, so now what?');
			// Well, they're on different sets. This one can just stay in the bin now.
			enablePenaltyButton(o, t*60000);
			$("#JammerPopup").popup('close');
			return;
		}
	}
}

function enablePenaltyButton(o, ms) {
	o.data('isrunning', true);
	o.data('endtime', parseInt($sb("ScoreBoard.Clock(Period).Time").$sbGet()) - ms);
	o.data('timeleft', ms);
	// Change the look of the icon so they know it's registered. This is yuck. I know.
	o.parent().removeClass('ui-btn-up-b').removeClass('ui-btn-hover-b');
	o.parent().attr("data-theme", "e").trigger("mouseout");
	updatePenaltyClocks(parseInt($sb("ScoreBoard.Clock(Period).Time").$sbGet()));
}










