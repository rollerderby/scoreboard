
function dec_sort(a, b){ return ( parseInt($(b).attr("data-sort")) < parseInt($(a).attr("data-sort")) ) ? 1 : -1; }
function str_sort(a, b){ return ( $(b).attr("data-sort") < $(a).attr("data-sort") ) ? 1 : -1; }

jQuery.fn.sortDivs = function sortDivsStr() { $("> div", this[0]).sort(str_sort).appendTo(this[0]); }
jQuery.fn.sortDivsRev = function sortDivsRev() { $("> div", this[0]).sort(dec_sort).appendTo(this[0]); }

if(document.location.search == '?camera') {
	console.log('Setting up Camera');

	MediaStreamTrack.getSources(function(s) {
		for(var c=0;c<s.length;c++) {
			var src = s[c];
			if(src.kind == 'video') {
				console.log(src);
			}
		}
	});

	navigator.getUserMedia = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
	if (navigator.getUserMedia) {
		   navigator.getUserMedia({ video: true },
		   function(stream) {
			 var video = document.querySelector('video');
			 video.src = window.URL.createObjectURL(stream);
			 video.onloadedmetadata = function(e) { video.play(); $('#VIDEO-GREEN').hide(); };
		      },
		      function(err) {
			 console.log("The following error occured: " + err.name);
		      }
		   );
	} else {
	   	console.log("getUserMedia not supported. Camera Unavailable");
	}
}

$(initialize);

function initialize() {

	WS.Connect();
	WS.AutoRegister();

	// Show Clocks
	WS.Register( [
		"ScoreBoard.Clock(Period).Running",
		"ScoreBoard.Clock(Jam).Running",
		"ScoreBoard.Clock(Lineup).Running",
		"ScoreBoard.Clock(Timeout).Running",
		"ScoreBoard.Clock(Intermission).Running" ], function(k, v) { clockRunner(k,v); } );

	WS.Register( [ "ScoreBoard.Clock(Intermission).Number",
			   "ScoreBoard.Team(1)",
			   "ScoreBoard.Team(2)",
		       "ScoreBoard.Clock(Intermission).MaximumNumber",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.PreGame)",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.Unofficial)",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.Official)",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.Intermission)" ], function(k,v) { } );

	WS.Register( [  'ScoreBoard.Clock(Timeout).Running', 
		        'ScoreBoard.TimeoutOwner',
			'ScoreBoard.OfficialReview',
			'ScoreBoard.Team(1).Timeouts',
			'ScoreBoard.Team(2).Timeouts',
			'ScoreBoard.Team(1).OfficialReviews',
			'ScoreBoard.Team(2).OfficialReviews',
			'ScoreBoard.Team(1).RetainedOfficialReview',
			'ScoreBoard.Team(2).RetainedOfficialReview' ], function(k,v) { smallDescriptionUpdate(k,v); } );

	WS.Register( 'ScoreBoard.Team(1)', function(k,v) { teamData(1, k,v); } );
	WS.Register( 'ScoreBoard.Team(2)', function(k,v) { teamData(2, k,v); } );

	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.Score', 'ScoreBoard.FrontendSettings.Overlay.Top' ], function(k,v) {  
		$('div[data-setting="'+k+'"]').each(function() {
			if(v == 'On') { $(this).addClass('Show'); } else { $(this).removeClass('Show'); }
		});
	});

	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.ShowJammers' ], function(k,v) {  
		$('div[data-setting="'+k+'"]').each(function() {
			if(v == 'On') { $(this).addClass('ShowJammers'); } else { $(this).removeClass('ShowJammers'); }
		});
	});

	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.Mode' ], function(k,v) {  
		$('body').removeClass('alpha').removeClass('green').addClass(v);
	});
	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.Resolution' ], function(k,v) {  
		$('body').removeClass('FHD HD SDW SD LDW LD').addClass(v);
	});
	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.Font.Clock' ], function(k,v) {  
		$('body').removeClass('ClockFaceDigital7 ClockFaceErics ClockFaceTick ClockFaceLCD14 ClockFaceUbuntu ClockFaceVerdana ClockFaceHelvetica ClockFaceArial ClockFaceSerif ClockFaceSans').addClass(v);
	});

	WS.Register('ScoreBoard.FrontendSettings.Overlay.Transition', function(k,v) { 
		window.alert(k,v);
	});

	WS.Register('ScoreBoard.FrontendSettings.Overlay.Panel', function(k,v) { 
		console.log('changed panel', k, v);
		$('.OverlayPanel').removeClass('Show'); 
		// sort divs in the panel before we show, just in case it's changed
		if(v == 'PenaltyTeam1' || v == 'PenaltyTeam2') {
			c = $('.PenaltyTeam [data-flag="BC"]');
			c.empty().remove();
		}
		$('.OverlayPanel.' +v +' .SortBox').sortDivs(); 
		$('.OverlayPanel.' + v).addClass('Show'); 
	});

	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.LowerThird.Line' ] , function(k,v) { 
		sp = '.' + k.split('.').slice(2,4).join(' .');
		$(sp).text(v);
	});

	WS.Register([ 'ScoreBoard.FrontendSettings.Overlay.LowerThird.Style' ] , function(k,v) { 
		$('.LowerThird .Line2').removeClass( 'ColourTeam1 ColourTeam2 ColourDefault' ).addClass(v);
	});

	$(document).keyup(function(e) {
		if(e.which == 74) { WS.Set('ScoreBoard.FrontendSettings.Overlay.ShowJammers', WS.state['ScoreBoard.FrontendSettings.Overlay.ShowJammers'] == 'On' ? 'Off' : 'On'); }
		if(e.which == 67) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', WS.state['ScoreBoard.FrontendSettings.Overlay.Panel'] == 'ClockBox' ? '' : 'ClockBox'); }
		if(e.which == 69) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', WS.state['ScoreBoard.FrontendSettings.Overlay.Panel'] == 'PT1' ? '' : 'PT1'); } /* E */
		if(e.which == 49) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', WS.state['ScoreBoard.FrontendSettings.Overlay.Panel'] == 'RosterTeam1' ? '' : 'RosterTeam1'); }
		if(e.which == 50) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', WS.state['ScoreBoard.FrontendSettings.Overlay.Panel'] == 'RosterTeam2' ? '' : 'RosterTeam2'); }
		if(e.which == 51) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', WS.state['ScoreBoard.FrontendSettings.Overlay.Panel'] == 'PenaltyTeam1' ? '' : 'PenaltyTeam1'); }
		if(e.which == 52) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', WS.state['ScoreBoard.FrontendSettings.Overlay.Panel'] == 'PenaltyTeam2' ? '' : 'PenaltyTeam2'); }
		if(e.which == 84) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Top',   WS.state['ScoreBoard.FrontendSettings.Overlay.Top'] == 'Off' ? 'On': 'Off'); }
		if(e.which == 32) { WS.Set('ScoreBoard.FrontendSettings.Overlay.Panel', ''); }
	});

	setTimeout(function() { $('body').removeClass('preload'); }, 1000);
}

function teamData(team, k,v) {

	var skaterId;
	var key;
	var setting; 

	var skaterRegEx = /^ScoreBoard\.Team\((.+)\)\.Skater\((.+)\)\.(.+)$/;
	var match = k.match(skaterRegEx);
	if(match) { 
		skaterId = match[2]; key = match[3]; penalty = null; 
	}

	var penaltyRegEx = /^ScoreBoard\.Team\((.+)\)\.Skater\((.+)\)\.Penalty\((.+)\).(.+)$/;
	var match = k.match(penaltyRegEx);
	if(match) { skaterId = match[2]; key = match[4]; penalty = match[3]; }

	var teamInfoRegEx = /^ScoreBoard\.Team\((.+)\)\.(.+)$/;
	var match = k.match(teamInfoRegEx);
	if(match) { 
		var subkey = match[2];
		if(subkey == 'Logo') { 
			if(v && v != '') {
				$('img.TeamLogo'+team).attr('src', v);
				$('img.TeamLogo'+team).css('display', 'inline');
				$('img.TeamLogo'+team).parent().removeClass('NoLogo');
			} else {
				$('img.TeamLogo'+team).css('display', 'none');
				$('img.TeamLogo'+team).parent().addClass('NoLogo');
			}
		}
	}

	var colourRegEx = /^ScoreBoard\.Team\((.+)\)\.Color\((.+)\)$/;
	var match = k.match(colourRegEx);
	if(match) { 
		var setting = match[2];
		update_team_colors(setting, team, v);
	}

	if(key != 'Name' && key != 'Number' && key != 'Code' && key != 'Flags')  { return; }

	var pa = '.RosterTeam' + team + ' .Team' + team;
	var pb = '.PenaltyTeam' + team + ' .Team' + team;

	var me = pa + ' .Skater[data-skaterId=' + skaterId + ']';
	var mb = pb + ' .Skater[data-skaterId=' + skaterId + ']';

	if (v == null) {
		if(key == 'Code') {
			$('.Number-'+penalty, mb).remove();
		} else {
			$(mb).remove();
			$(me).remove();
		}
		return;
	}


	if($(me).length == 0)   { createRosterSkater(pa, skaterId, team); }
	if($(mb).length == 0) 	{ createPenaltySkater(pb, skaterId, team); }

	if(key == 'Code') 	{ createPenalty(mb,penalty,v); } 
	if(key == 'Number') 	{ updateSkaterNumber(me,mb,key,v); }

	if(key == 'Flags') {
		$('.'+key, me).attr('data-flag', v);
		$(mb).attr('data-flag', v);
	} else {	
		c = $('.'+key, me).text(v);
		c = $('.'+key, mb).text(v);
	}


}

function update_team_colors(setting, team, v) {
		var style;
		for(i in document.styleSheets) if(document.styleSheets[i].title =='jsStyle') style=document.styleSheets[i];
		if(style) {
			var ns,r;
			// chrome seems to like things in lowercase
			if(setting == 'overlay_bg') {
				_do_actual_update(style, '#sb .colourteam' + team, 'background-color', v);
				_do_actual_update(style, '#sb .colourteam' + team + 'reverse', 'color', v);

				_do_actual_update(style, '#sb .team' + team + 'colourbg', 'background-color', v);
				_do_actual_update(style, '#sb .team' + team + 'colourbgasfg', 'color', v);

				_do_actual_update(style, '#sb .Skater:nth-child(odd) .altcolourteam' + team, 'background-color', v);
				_do_actual_update(style, '#sb .Skater:nth-child(even) .altcolourteam' + team, 'background-color', shade(v, .3));
			}
			if(setting == 'overlay_fg') {
				_do_actual_update(style, '#sb .colourteam' + team, 'color', v);
				_do_actual_update(style, '#sb .colourteam' + team + 'reverse', 'background-color', v);
				_do_actual_update(style, '#sb .team' + team + 'colourfg', 'color', v);
				_do_actual_update(style, '#sb .team' + team + 'colourfgasbg', 'background-color', v);

				_do_actual_update(style, '#sb .Skater .altcolourteam' + team, 'color', v);
			}

		} else {
			console.log('NO style');
		}
	}

	function _do_actual_update(style, rd, ns, v) {
		for(i in document.styleSheets) if(document.styleSheets[i].title =='jsStyle') style=document.styleSheets[i];
		if(ns) {
			for(var r=0; r<style.rules.length ; r++ ) {
				var dd = style.rules[r];
				if(dd.selectorText == rd && dd.style[0] == ns) { console.log('deleting ', r); style.deleteRule(r); }
			}
			if(v != null && v != '') {
				style.addRule(rd, ns + ': ' + v);
			}
		}
	}
	
function updateSkaterNumber(me,mb,key,v) {
	sv = v;
	if(v == '' || v == '-' || v == null) { sv = 'ZZZB'; v = '-'; }	
	$('.'+key,me).parent().attr('data-sort', sv);
	$('.'+key,mb).parent().attr('data-sort', sv);
	$(me).sortDivs(); $(mb).sortDivs();
}


function createRosterSkater(pa, skaterId, team) {
	// create the roster entry for this skater
	var xv = $('<div class="Skater"></div>');
	xv.attr('data-skaterId', skaterId);
	$('<div class="Number AltColourTeam' + team +'"></div>').appendTo(xv);
	$('<div class="Name Team' + team + 'ColourFg"></div>').appendTo(xv);
	$('<div class="Flags"></div>').appendTo(xv);
	$(pa).append(xv);
}

function createPenaltySkater(pb, skaterId, team) {
	// create the penalty box entry for this skater
	var xz = $('<div class="Skater"></div>');
	xz.attr('data-skaterId', skaterId);
	xz.attr('data-count', 0);
	//$('<div class="Number" data-sort="0">&nbsp;</div>').appendTo(xz);
	$('<div class="Number AltColourTeam' + team +'" data-sort="0">&nbsp;</div>').appendTo(xz);
	$('<div class="Name AltColourTeam' + team + '" data-sort="0">&nbsp;</div>').appendTo(xz);
	$(pb).append(xz);
}

function createPenalty(mb, pnum, v) {
	$('.Number-'+pnum, mb).remove();
	var penalty = $('<div class="Penalty Number-' + pnum + ' Penalty-' + v + '">' + v + '</div>');
	$(mb).attr('data-count', $('.Penalty', mb).length+1);
	if(v=='EXP') { $(mb).attr('data-end', 'EXP'); }
	if(v=='FO') { $(mb).attr('data-end', 'FO'); }
	penalty.attr('data-sort', pnum);
	$(mb).append(penalty);
	$(mb).sortDivs();
}

function clockType(k,v) {
	var ret;
	var to = WS.state["ScoreBoard.TimeoutOwner"];
	var or = WS.state["ScoreBoard.OfficialReview"];
	var tc = WS.state['ScoreBoard.Clock(Timeout).Running'];
	var lc = WS.state['ScoreBoard.Clock(Lineup).Running'];
	var ic = WS.state['ScoreBoard.Clock(Intermission).Running'];
	var showDesc;

	if(tc) {
		ret = WS.state["ScoreBoard.Clock(Timeout).Name"];
		if(to != "" && to != "O" && or) { ret = 'Official Review'; }
		if(to != "" && to != "O" && !or) { ret = 'Team Timeout'; }
		if(to == "O") { ret = 'Official Timeout'; }
		if(to == "") { ret = 'Timeout'; }
		showDesc = true;
	} else if(lc) {
		ret = WS.state["ScoreBoard.Clock(Lineup).Name"];
		showDesc = false;
	} else if(ic) {
		showDesc = true;
		var num = WS.state["ScoreBoard.Clock(Intermission).Number"];
		var max = WS.state["ScoreBoard.Clock(Intermission).MaximumNumber"];
		var isOfficial = WS.state["ScoreBoard.OfficialScore"];
		if (num == 0)  
			ret = WS.state["ScoreBoard.Setting(ScoreBoard.Intermission.PreGame)"];
		else if (num != max)
			ret = WS.state["ScoreBoard.Setting(ScoreBoard.Intermission.Intermission)"];
		else if (!isOfficial)
			ret = WS.state["ScoreBoard.Setting(ScoreBoard.Intermission.Unofficial)"];
		else
			ret = WS.state["ScoreBoard.Setting(ScoreBoard.Intermission.Official)"];

	}

	if(showDesc) { $('.ClockDescription').addClass('Show'); } else { $('.ClockDescription').removeClass('Show'); }
	return ret;
}

function clockRunner(k,v) {
		var lc = WS.state["ScoreBoard.Clock(Lineup).Running"];
		var tc = WS.state["ScoreBoard.Clock(Timeout).Running"];
		var ic = WS.state["ScoreBoard.Clock(Intermission).Running"];
		var jc = WS.state["ScoreBoard.Clock(Jam).Running"];
		var pc = WS.state["ScoreBoard.Clock(Period).Running"];

		var clock = "Jam";

		if (isTrue(tc))
			clock = "Timeout";
		else if (isTrue(lc))
			clock = "Lineup";
		else if (isTrue(ic))
			clock = "Intermission";

		$(".Clock").removeClass("Show"); 
		$(".Clock.ShowIn" + clock).addClass("Show");
}

function updatePeriod(k,v) { return 'P' + v; }
function updateJam(k,v) { return 'J' + v; }

function smallDescriptionUpdate(k, v) {
	var lc = WS.state["ScoreBoard.Clock(Lineup).Running"];
	var tc = WS.state["ScoreBoard.Clock(Timeout).Running"];
	var to = WS.state["ScoreBoard.TimeoutOwner"];
	var or = WS.state["ScoreBoard.OfficialReview"];
	var lcn = WS.state["ScoreBoard.Clock(Lineup).Name"];
	var tcn = WS.state["ScoreBoard.Clock(Timeout).Name"];
	var ret = '';

	$.each(["1", "2"], function (idx, id) {
		tto = WS.state["ScoreBoard.Team(" + id + ").Timeouts"];
		tor = WS.state["ScoreBoard.Team(" + id + ").OfficialReviews"];
		tror = WS.state["ScoreBoard.Team(" + id + ").RetainedOfficialReview"];
		$(".Team" + id + " .Timeout1").toggleClass("Used", tto < 1);
		$(".Team" + id + " .Timeout2").toggleClass("Used", tto < 2);
		$(".Team" + id + " .Timeout3").toggleClass("Used", tto < 3);
		$(".Team" + id + " .OfficialReview1").toggleClass("Used", tor < 1);
		$(".Team" + id + " .OfficialReview1").toggleClass("Retained", tror);
	});

	$(".PanelWrapperTop .Dot").removeClass("Active");
	$(".PanelWrapperTop .Description, .PanelWrapperTop>.Timeouts,.PanelWrapperTop>.OfficialReviews").removeClass("Red");
	if (lc)
		ret = lcn;
	else if (tc) {
		$(".Clock.Description").addClass("Red");

		ret = tcn;
		if (to != "" && !or) {
			ret = "Team Timeout";
			$(".Team" + to + ">.Timeouts").addClass("Red");
			var dotSel = ".Team" + to + " .Timeout" + (WS.state["ScoreBoard.Team(" + to + ").Timeouts"] + 1);
			$(dotSel).addClass("Active");
		}
		if (to != "" && or) {
			ret = "Official Review";
			$(".Team" + to + ">.OfficialReviews:not(.Header)").addClass("Red");
			var dotSel = ".Team" + to + " .OfficialReview" + (WS.state["ScoreBoard.Team(" + to + ").OfficialReviews"] + 1);
			$(dotSel).addClass("Active");
		}
	}
	return ret;
}

function getTeamId(k) {
	if (k.indexOf("Team(1)") > 0)
		return "1";
	if (k.indexOf("Team(2)") > 0)
		return "2";
	return null;
}

function jammer(k, v) {
	id = getTeamId(k);
	var prefix = "ScoreBoard.Team(" + id + ").";
	var jammerId = WS.state[prefix + "Position(Jammer).Skater"];
	var jammerName = WS.state[prefix + "Skater(" + jammerId + ").Name"];
	var pivotId = WS.state[prefix + "Position(Pivot).Skater"];
	var pivotName = WS.state[prefix + "Skater(" + pivotId + ").Name"];
	var leadJammer = (WS.state[prefix + "LeadJammer"] === "Lead");
	var starPass = isTrue(WS.state[prefix + "StarPass"]);

	if (jammerName == null)
		jammerName = leadJammer ? "Lead" : "";
	if (pivotName == null)
		pivotName = "";

	var jn = !starPass ? jammerName : pivotName;
	$(".Team" + id + " .Lead").toggleClass("HasLead", (leadJammer && !starPass));
	$(".Team" + id).toggleClass("HasJammerName", (jn != "" && jn != null));
	return jn
}


function toTime(k, v) { return _timeConversions.msToMinSecNoZero(v); }
function toInitial(k, v) { return v == null ? '' : v.substring(0, 1); }

function shadeColor2(color, percent) {   
	    var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
	    return "#"+(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1);
}

function blendColors(c0, c1, p) {
	    var f=parseInt(c0.slice(1),16),t=parseInt(c1.slice(1),16),R1=f>>16,G1=f>>8&0x00FF,B1=f&0x0000FF,R2=t>>16,G2=t>>8&0x00FF,B2=t&0x0000FF;
	    return "#"+(0x1000000+(Math.round((R2-R1)*p)+R1)*0x10000+(Math.round((G2-G1)*p)+G1)*0x100+(Math.round((B2-B1)*p)+B1)).toString(16).slice(1);
}

function shade(color, percent){
	    if (color.length > 7 ) return shadeRGBColor(color,percent);
	    else return shadeColor2(color,percent);
}

function blend(color1, color2, percent){
	    if (color1.length > 7) return blendRGBColors(color1,color2,percent);
	    else return blendColors(color1,color2,percent);
}

$(document).ready(function() {
	/*setTimeout(function() { 
		$('#sb').css('z-index',-999)); 
	},2000); */
});

