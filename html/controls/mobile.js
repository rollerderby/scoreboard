
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

	$.each( [ "1", "2" ], function(i, t) {
		$sb("ScoreBoard.Team("+t+")").$sbBindAddRemoveEach("AlternateName", function(event, node) {
			if ($sb(node).$sbId == "mobile")
				$sb(node).$sb("Name").$sbBindAndRun("sbchange", function(event2, val) {
					$(".Team"+t+".Name,.Team"+t+".AlternateName")
						.toggleClass("HasAlternateName", $.trim(val) != "");
				});
		});
	});
});

function setupJamControlPage() {
	$sb("ScoreBoard.StartJam").$sbControl("#JamControlPage button.StartJam").val(true);
	$sb("ScoreBoard.StopJam").$sbControl("#JamControlPage button.StopJam").val(true);
	$sb("ScoreBoard.Timeout").$sbControl("#JamControlPage button.Timeout").val(true);
	$sb("ScoreBoard.Team(1).Timeout").$sbControl("#JamControlPage div.Timeout button.Team1").val(true);
	$sb("ScoreBoard.Team(1).Name").$sbElement("#JamControlPage div.Timeout button.Team1>span.Name");
	$sb("ScoreBoard.Team(1).AlternateName(mobile).Name").$sbElement("#JamControlPage div.Timeout button.Team1>span.AlternateName");
	$sb("ScoreBoard.Timeout").$sbControl("#JamControlPage div.Timeout button.Official").val(true);
	$sb("ScoreBoard.Team(2).Timeout").$sbControl("#JamControlPage div.Timeout button.Team2").val(true);
	$sb("ScoreBoard.Team(2).Name").$sbElement("#JamControlPage div.Timeout button.Team2>span.Name");
	$sb("ScoreBoard.Team(2).AlternateName(mobile).Name").$sbElement("#JamControlPage div.Timeout button.Team2>span.AlternateName");

	$.each( [ "Period", "Jam", "Timeout" ], function(i, clock) {
		$sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", function(event, value) {
			$("#JamControlPage span.ClockBubble."+clock).toggleClass("Running", isTrue(value));
		});
	});
	
	// Period number
	$sb("ScoreBoard.Clock(Period).Number").$sbElement("#JamControlPage div.PeriodNumber a.Number");

	// Period Clock
	$sb("ScoreBoard.Clock(Period).Time").$sbElement("#JamControlPage div.PeriodTime a.Time", { sbelement: {
			convert: _timeConversions.msToMinSec
	}});
	
	var showJamControlClock = function(clock) {
		$("#JamControlPage div.Time").not("."+clock+"Time").hide().end()
			.filter("."+clock+"Time").show();
	};
	// In case no clocks are running now, default to showing only Jam
	showJamControlClock("Jam");

	// Setup clocks
	$.each([ "Jam", "Lineup", "Timeout" ], function(i, clock) {
		$sb("Scoreboard.Clock("+clock+").Time").$sbElement("#JamControlPage div."+clock+"Time a.Time", { sbelement: {
			convert: _timeConversions.msToMinSec
		}});
		$sb("ScoreBoard.Clock("+clock+").Running").$sbBindAndRun("sbchange", function(e, v) {
			if (isTrue(v))
				showJamControlClock(clock);
		});
	});
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
		$(v).data('timelocked', false);
		$(v+"Time").hide();
	});
	$("#Blocker1Team1").data('enabled', true);
	$("#Blocker2Team1").data('enabled', true);
	$("#Blocker1Team2").data('enabled', true);
	$("#Blocker2Team2").data('enabled', true);
	$sb("ScoreBoard.Clock(Period).Time").$sbBindAndRun("sbchange", function(e, v) { updatePenaltyClocks(parseInt(v)); });
	updatePenaltyClocks(1800000);
 
}


function setupTeamScorePage() {
	$.each([ "1", "2" ], function(i, n) {
		var team = $sb("ScoreBoard.Team("+n+")");
		var score = team.$sb("Score");

		team.$sb("Name").$sbElement("#TeamScorePage span.Team"+n+".Name");
		team.$sb("AlternateName(mobile).Name").$sbElement("#TeamScorePage span.Team"+n+".AlternateName");

		$.each([ "#Team"+n+"ScorePage", "#TeamSetScorePage", "#TeamBothScorePage" ], function(ii, e) {
			team.$sb("Name").$sbElement(e+" .Team"+n+".Name");
			team.$sb("AlternateName(mobile).Name").$sbElement(e+" .Team"+n+".AlternateName");
			score.$sbElement(e+" a.Team"+n+".Score");
			score.$sbControl(e+" button.Team"+n+".ScoreDown", { sbcontrol: {
				sbSetAttrs: { change: true }
			}});
			score.$sbControl(e+" button.Team"+n+".ScoreUp", { sbcontrol: {
				sbSetAttrs: { change: true }
			}});

		});

		score.$sbControl("#TeamSetScorePage input[type='number'].Team"+n+".SetScore,#TeamSetScorePage button.Team"+n+".SetScore", {
			sbcontrol: {
				delayupdate: true,
				noSetControlValue: true
			}
		});
	});

	var vert = false;
	$("#TeamBothScorePage a.HVButton").click(function() {
		vert = !vert;
		var text = (vert ? "Vertical" : "Horizontal");
		$(this).find(".ui-btn-text").text(text);
		$("#TeamBothScorePage div.HV")
			.toggleClass("IsHorizontal", !vert)
			.toggleClass("IsVertical", vert);
	}).click();
}
 
function updatePenaltyClocks(periodClock) {
	var dateobj = new Date();

	// Do we need to enable or disable any blocker seats?
	$.each(['Team1', 'Team2'], function(e, v) {
		
		// Logic is:
		// Are there two blockers in the box? Does one of them have less than 10 seconds left? If so, enable all buttons.
		// else
		//	 if button 3 is running, disable which ever one isn't running.
		//	 else
		//	 disable button 3, enable button 1 and 2. 
		var blockers = 0;
		var standing = 0;
		$.each(['Blocker1', 'Blocker2', 'Blocker3'], function (x, blocker) {
			if ($('#'+blocker+v).data('isrunning')) { blockers++; }
			if ($('#'+blocker+v).data('timeleft') < 12000 ) { standing++; }
		}); 
		if (blockers >= 2 && standing !=	0) {
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
				$(v).click(function(){ penaltyButtonClicked($(this)); });
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
	
	// Unlock times if both of them are out of the box
	if (!$("#Jammer1").data('isrunning') && !$("#Jammer2").data('isrunning')) {
		$("#Jammer1").data('timelocked', false);
		$("#Jammer2").data('timelocked', false);
	} 
}

function penaltyButtonClicked(bObj) {

	// navigator.vibrate(500);	// This doesn't work on ANYTHING yet. Sigh.
	var isrunning = bObj.data('isrunning');
	if (isrunning == false) {
		console.log(bObj.attr('id'));
		if (bObj.attr('id') == 'Jammer1' || bObj.attr('id') == 'Jammer2') {
			// It's not running, and a Jammer button has been selected. Open up the jammer popup,
			// and ask how long it's for.
			$.each(["#BtnJammer1m", "#BtnJammer2m", "#BtnJammerCancel"], function (v, e) {
				$(e).unbind("click");
			});
			$("#BtnJammer1m").click(function() { jammerin(bObj, 1); });
			$("#BtnJammer2m").click(function() { jammerin(bObj, 2); });
			$("#BtnJammerCancel").click(function() { $("#JammerPopup").popup("close"); });
			$("#JammerPopup").popup("open");
			
		} else {
			// Not a Jammer, just a blocker. They're simple.
			enablePenaltyButton(bObj, 60000);
		}
	} else {
		// This clock is currently running. Remove any left over event handlers..
		 $.each(["#BtnCancel", "#BtnClock", "#BtnAdd1", "#BtnDel1"], function (v, e) {
			$(e).unbind("click");
		});
		// Add some new ones
		$("#BtnCancel").click(function() { cancelPenalty(bObj); });
		$("#BtnClock").click(function() { togglePausePenalty(bObj); });
		$("#BtnAdd1").click(function() { 
			// Now, before we just wildly give them an extra minute, does the OTHER jammer have more than 60 secs? We don't care
			// about sets here, the rules just say 'more than a minute of penalty time'.	See 7.3.10.
			var j = bObj.attr('id'); // Which jammer is this
			var other = (j == 'Jammer1')?'#Jammer2':"#Jammer1"; // This is the other jammer.
			
			if ($(other).data('timeleft') >= 60001 && $(other).data('isrunning')) {
				bObj.data('timeleft', bObj.data('timeleft') - 60000); 
				bObj.data('endtime', bObj.data('endtime') + 60000);
			} else { // Other jammer has LESS than 60 secs.
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
	
	// Lets start with the easy one.
	// Jammer comes in, there's no other jammer.
	if ( !$(other).data('isrunning') ) {
		enablePenaltyButton(o, t*60000);
		$("#JammerPopup").popup('close');
		return;
	} else {
		// Jammer comes in, there's another jammer already there 
				
		// 7.3.10 explicity says that if the other jammer has more than 1 min, release the arriving jammer (eg, this one)
		// immediately. HOWEVER, this would have been picked up when the button was clicked originally.		The rules say
		// this: 

		// When a penalized Jammer is serving more than one consecutive minute in the penalty box and 
		// the opposing Jammer arrives in the penalty box when the first penalized Jammer has more than 
		// a minute of penalty time remaining to serve, the arriving Jammer will be released back into play 
		// by the penalty box official immediately after taking their seat in the penalty box. The remaining 
		// penalty time of the first Jammer is reduced by one minute
	
		if ( t == 2 ) { // I've been sent for 2
			if ( $(other).data('timeleft') > 120000 ) { // And they have MORE than 2
				// Wow. Lots of naughty. But, we can take off two minutes, and release this one.
				$(other).data('timeleft', $(other).data('timeleft') - 120000); 
				$(other).data('endtime', $(other).data('endtime') + 120000);
				enablePenaltyButton(o, 1000);
				$("#JammerPopup").popup('close');
				return;	 // Don't lock, all penalties are equal (0 seconds and 0 seconds)
			} else if ( $(other).data('timeleft') > 60000 ) { // They have more than 1
				// I can take off one minute off yours, and a minute off mine.
				$(other).data('timeleft', $(other).data('timeleft') - 60000); 
				$(other).data('endtime', $(other).data('endtime') + 60000);
				t = 1;
				// And proceed as normal.	 Note the one below will never match this.
				// We will always have less than 60000msec.
				}
		}
		if ( t == 1 && $(other).data('timeleft') > 60000) {
			// OK, I was given one minute, and the other jammer has more than 1. I can just drop a minute, and release this jammer.
			console.log("Caught it here");
			$(other).data('timeleft', $(other).data('timeleft') - 60000); 
			$(other).data('endtime', $(other).data('endtime') + 60000);
			enablePenaltyButton(o, 1000);
			$("#JammerPopup").popup('close');
			return;
		}
		
		// I probably need to care about 'undo' and 'I pushed the wrong button' mistakes here, too.

		// Are they both on EXACTLY 60 seconds? If so, they've both been bad, and deserve 10 seconds in the bin.
		// (7.3.2) - This should only happen when both jammers are sent to the box at the end, or beginning of
		// the jam. 
		if (t == 1 && parseInt($(other).data('timeleft')) == 60000) {
			enablePenaltyButton(o, 10000);
			enablePenaltyButton($(other), 10000);
			$("#JammerPopup").popup('close');
			return;
		}

		// Jammer Swapping!
		if (o.data('timelocked')) { console.log('o.data is locked'); } else { console.log('o.data is not locked'); }
		if ($(other).data('timelocked')) { console.log('$other.data is locked'); } else { console.log('$other.data is not locked'); }
		if (!o.data('timelocked') && !$(other).data('timelocked') ) {
			o.data('timelocked', true); // I cannot be released early again. 
			if (t == 1) {
				enablePenaltyButton(o, 60000 - $(other).data('timeleft'));
				$(other).data('timeleft', 0);
				$(other).data('endtime', $(other).data('endtime') + 60000);
			} else {
				// I've been given 2. The other jammer CAN be released, and I need to have 1 minute, plus the change.
				enablePenaltyButton(o, 120000 - $(other).data('timeleft'));
				$(other).data('timeleft', 0);
				$(other).data('endtime', $(other).data('endtime') + 60000);
			}
		} else {
			// This is an ABA. The other jammer should NOT be released. Jammerless Jam.
			// Need an alert.
			console.log("I am here. This is an ABA");
			enablePenaltyButton(o, 60000*t);
			o.data('timelocked', false);
			$(other).data('timelocked', false); // We're back in sync again. 
		} 
		$("#JammerPopup").popup('close');
		return;
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










