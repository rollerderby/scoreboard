
jQuery.fn.sortDivs = function sortDivs() {
    $("> div", this[0]).sort(dec_sort).appendTo(this[0]);
    function dec_sort(a, b){ return ($(b).attr("data-sort")) < ($(a).attr("data-sort")) ? 1 : -1; }
}

$(initialize);


function initialize() {
	WS.Connect();
	WS.AutoRegister();


	WS.Register( [
		"ScoreBoard.Clock(Period).Running",
		"ScoreBoard.Clock(Jam).Running",
		"ScoreBoard.Clock(Lineup).Running",
		"ScoreBoard.Clock(Timeout).Running",
		"ScoreBoard.Clock(Intermission).Running" ], function(k, v) { clockRunner(k, v); } );

	WS.Register( [ "ScoreBoard.Clock(Intermission).Number",
		       "ScoreBoard.Clock(Intermission).MaximumNumber",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.PreGame)",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.Unofficial)",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.Official)",
		       "ScoreBoard.Setting(ScoreBoard.Intermission.Intermission)" ], function(k,v) { } );

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
		$('.OverlayPanel.'+v+' .SortBox').sortDivs(); 
		$('.OverlayPanel.' + v).addClass('Show'); 
	});

	WS.Register([ 'Custom.Overlay.LowerThird.Line1', 'Custom.Overlay.LowerThird.Line2' ] , function(k,v) { 
		sp = '.' + k.split('.').slice(2,4).join(' .');
		console.log(sp);
		$(sp).text(v);
	});


}

var skaterRegEx = /^Game.Team\((.+)\)\.Skater\((.+)\)\.(.+)$/;
var penaltyRegEx = /^Game.Team\((.+)\)\.Skater\((.+)\)\.Penalty\((.+)\).(.+)$/;
function teamData(team, k,v) {

	var skaterId;
	var key;

	var match = k.match(skaterRegEx);
	if(match) { skaterId = match[2]; key = match[3]; penalty = null; }

	var match = k.match(penaltyRegEx);
	if(match) { skaterId = match[2]; key = match[4]; penalty = match[3]; }

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

	if($(me).length == 0) { createRosterSkater(pa, skaterId); }


	if($(mb).length == 0) 	{ createPenaltySkater(pb, skaterId); }

	if(key == 'Code') 	{ createPenalty(mb,penalty,v); } 
	if(key == 'Number') 	{ updateSkaterNumber(me,mb,key,v); }

	if(key == 'Flags') {
		$('.'+key, me).attr('data-flag', v);
	} else {	
		c = $('.'+key, me).text(v);
		c = $('.'+key, mb).text(v);
	}

}

function updateSkaterNumber(me,mb,key,v) {
	sv = v;
	if(v == '' || v == '-') { sv = 'ZZZB'; v = '-'; }	
	$('.'+key,me).parent().attr('data-sort', sv);
	$('.'+key,mb).parent().attr('data-sort', sv);
	$(me).sortDivs(); $(mb).sortDivs();
}


function createRosterSkater(pa, skaterId) {
	// create the roster entry for this skater
	var xv = $('<div class="Skater"></div>');
	xv.attr('data-skaterId', skaterId);
	$('<div class="Number AutoFit">&nbsp;</div>').appendTo(xv);
	$('<div class="Name"></div>').appendTo(xv);
	$('<div class="Flags"></div>').appendTo(xv);
	$(pa).append(xv);
}

function createPenaltySkater(pb, skaterId) {
	// create the penalty box entry for this skater
	var xz = $('<div class="Skater"></div>');
	xz.attr('data-skaterId', skaterId);
	xz.attr('data-count', 0);
	$('<div class="Number">&nbsp;</div>').appendTo(xz);
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
		xv = $('<div data-sort="' + jam + '" class="AutoFit ColumnWidth GraphBlock Jam' + jam + '"></div>');
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


$(function() {
    $(document).keypress(function(e) {
        if (e.which === 32)   WS.Set('Custom.Overlay.Panel', 'Default');
        if (e.which === 112)  WS.Set('Custom.Overlay.Panel', 'PPJBox');
        if (e.which === 49)   WS.Set('Custom.Overlay.Panel', 'RosterTeam1');
        if (e.which === 50)   WS.Set('Custom.Overlay.Panel', 'RosterTeam2');
    });
});

$(document).ready(function() {
	$('#sb').css('opacity',1);
});

