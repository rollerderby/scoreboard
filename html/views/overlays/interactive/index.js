
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

	WS.Register( [ "ScoreBoard.Clock(Intermission).Number",
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

	WS.Register( 'Game.Period', function(k,v) { jamData(k,v); } );

	WS.Register( 'Game.Team(1)', function(k,v) { teamData(1, k,v); } );
	WS.Register( 'Game.Team(2)', function(k,v) { teamData(2, k,v); } );

	WS.Register( 'ScoreBoard.Clock(Period).Number', function(k,v) {
		if(v == 2) { $('.PPJBox .Team .Period2').show(); } else { $('.PPJBox .Team .Period2').hide(); }
	});

	WS.Register([ 'Custom.Overlay.Clock', 'Custom.Overlay.Score' ], function(k,v) {  
		$('div[data-setting="'+k+'"]').each(function() {
			if(v == 'On') { $(this).addClass('Show'); } else { $(this).removeClass('Show'); }
		});
	});

	WS.Register([ 'Custom.Overlay.ShowJammers' ], function(k,v) {  
		$('div[data-setting="'+k+'"]').each(function() {
			if(v == 'On') { $(this).addClass('ShowJammers'); } else { $(this).removeClass('ShowJammers'); }
		});
	});

	WS.Register('Custom.Overlay.Transition', function(k,v) { 
		window.alert(k,v);
	});

	WS.Register('Custom.Overlay.Panel', function(k,v) { 
		$('.OverlayPanel').removeClass('Show'); 
		// sort divs in the panel before we show, just in case it's changed
		if(v == 'PenaltyTeam1' || v == 'PenaltyTeam2') {
			c = $('.PenaltyTeam [data-flag="BC"]');
			c.empty().remove();
		}
		$('.OverlayPanel.'+v+' .SortBox').sortDivs(); 
		$('.OverlayPanel.' + v).addClass('Show'); 
	});

	WS.Register([ 'Custom.Overlay.LowerThird.Line' ] , function(k,v) { 
		sp = '.' + k.split('.').slice(2,4).join(' .');
		$(sp).text(v);
	});

	WS.Register([ 'Custom.Overlay.LowerThird.Style' ] , function(k,v) { 
		$('.LowerThird .Line2').removeClass( 'ColourTeam1 ColourTeam2 ColourDefault' ).addClass(v);
	});


}


function teamData(team, k,v) {

	var skaterId;
	var key;
	var setting; 

	var skaterRegEx = /^Game\.Team\((.+)\)\.Skater\((.+)\)\.(.+)$/;
	var match = k.match(skaterRegEx);
	if(match) { 
		skaterId = match[2]; key = match[3]; penalty = null; 
	}

	var penaltyRegEx = /^Game\.Team\((.+)\)\.Skater\((.+)\)\.Penalty\((.+)\).(.+)$/;
	var match = k.match(penaltyRegEx);
	if(match) { skaterId = match[2]; key = match[4]; penalty = match[3]; }

	var teamInfoRegEx = /^Game\.Team\((.+)\)\.(.+)$/;
	var match = k.match(teamInfoRegEx);
	if(match) { 
		var subkey = match[2];
		if(subkey == 'Logo') { 
			if(v && v != '') {
				$('img.TeamLogo'+team).attr('src', v).css('display', 'block');
				$('img.TeamLogo'+team).parent().removeClass('NoLogo');
			} else {
				$('img.TeamLogo'+team).css('display', 'none');
				$('img.TeamLogo'+team).parent().addClass('NoLogo');
			}
		}
	}

	var colourRegEx = /^Game\.Team\((.+)\)\.Color\((.+)\)$/;
	var match = k.match(colourRegEx);
	if(match) { 
		var setting = match[2];
		var style;
		for(i in document.styleSheets) if(document.styleSheets[i].title =='jsStyle') style=document.styleSheets[i];
		if(style) {
			var ns,r;
			var rule;
			// chrome seems to like things in lowercase
			var rd = '#sb .colourteam'+team;

			if(setting == 'overlay_bg') ns = 'background-color';
			if(setting == 'overlay_fg') ns = 'color';
			if(ns) {
				for(var r=0; r<style.rules.length ; r++ ) {
					var dd = style.rules[r];
					if(dd.selectorText == rd && dd.style[0] == ns) style.deleteRule(r);
				}
				if(v != null) style.addRule(rd, ns + ': ' + v);
			}
		}
		return;
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
	$('<div class="Number ColourTeam' + team +'"></div>').appendTo(xv);
	$('<div class="Name"></div>').appendTo(xv);
	$('<div class="Flags"></div>').appendTo(xv);
	$(pa).append(xv);
}

function createPenaltySkater(pb, skaterId, team) {
	// create the penalty box entry for this skater
	var xz = $('<div class="Skater"></div>');
	xz.attr('data-skaterId', skaterId);
	xz.attr('data-count', 0);
	$('<div class="Number ColourTeam' + team + '">&nbsp;</div>').appendTo(xz);
	$('<div class="Name">&nbsp;</div>').appendTo(xz);
	$(pb).append(xz);
}

function createPenalty(mb, pnum, v) {
	$('.Number-'+pnum, mb).remove();
	var penalty = $('<div class="Penalty Number-' + pnum + ' Penalty-' + v + '">' + v + '</div>');
	$(mb).attr('data-count', $('.Penalty', mb).length+1);
	penalty.attr('data-sort', pnum);
	$(mb).append(penalty);
	$(mb).sortDivs();
}

var jamScoreRegEx = /^Game.Period\((.+)\)\.Jam\((.+)\)\.Team\((.+)\)\.(.+)$/;

function jamData(k,v) {
        match = k.match(jamScoreRegEx);
        if (match == null) return;

        var period = match[1];
        var jam = match[2];
        var team = match[3];
	var key = match[4];

	if (key != 'JamScore' && key != 'LeadJammer') return;

	pa = '.PPJBox .Team'+ team + ' .Period'+period;
	me = pa + ' .Jam'+jam;
	$pId = $(pa); $mId = $(me)

	if(v == null) {
		$(me).remove();
		$mId.sortDivs();
		return;
	}

	if($(me).length == 0) {
		pointsPerJamColumnWidths();
		xv = $('<div data-sort="' + jam + '" class="ColumnWidth GraphBlock Jam' + jam + '"></div>');
		$('<div class="JammerStar ColumnWidth"></div>').appendTo(xv);
		$('<div class="Points ColumnWidth"></div>').appendTo(xv);
		$pId.append(xv);
		$pId.sortDivs();
	}

	if(key == 'LeadJammer') {
		$(me).attr('lead', v);
	}

	if(key == 'JamScore') {
		setHeight = v*4 + 'px';
		$(me).css('height', setHeight);

		if(team == 1) {
			hid = $('.PPJBox .Team1 .Period').innerHeight();
			marg = parseInt(hid)-parseInt(setHeight);
			$(me).css('marginTop', marg);
		}
		if(v != 0) { $('.Points', me).text(v); }
	}
	pointsPerJamColumnWidths();

}

function pointsPerJamColumnWidths() {
	ne1 = $('.PPJBox .Team1 .GraphBlock').length;
	ne2 = $('.PPJBox .Team2 .GraphBlock').length;
	if(ne2 > ne1)  ne1=ne2;
	nel = ne1 + 3;
	wid = parseInt( $('.PPJBox').innerWidth() );
	newwidth = parseInt(wid / nel) - 3;
	$('.ColumnWidth').innerWidth(newwidth);
	$('.PPJBox .Team1 .GraphBlock').css('backgroundColor', WS.state['Game.Team(1).Color(overlay_bg)']);
	$('.PPJBox .Team2 .GraphBlock').css('backgroundColor', WS.state['Game.Team(2).Color(overlay_bg)']);
}

function clockType(k,v) {
	var ret;
	var to = WS.state["ScoreBoard.TimeoutOwner"];
	var or = WS.state["ScoreBoard.OfficialReview"];
	var tc = WS.state['ScoreBoard.Clock(Timeout).Running'];
	var lc = WS.state['ScoreBoard.Clock(Lineup).Running'];
	var ic = WS.state['ScoreBoard.Clock(Intermission).Running'];

	if(tc) {
		ret = WS.state["ScoreBoard.Clock(Timeout).Name"];
		if(to != "" && or) { ret = 'Official Review'; }
		if(to != "" && !or) { ret = 'Team Timeout'; }
		$('.ClockDescription').css('backgroundColor', 'red');
	} else if(lc) {
		ret = WS.state["ScoreBoard.Clock(Lineup).Name"];
		$('.ClockDescription').css('backgroundColor', '#888');
	} else if(ic) {
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

		$('.ClockDescription').css('backgroundColor', 'blue');
	} else {
		ret = 'Jam';
	}

	return ret;
	
}



$(document).ready(function() {
	$('#sb').css('opacity',1);
});

