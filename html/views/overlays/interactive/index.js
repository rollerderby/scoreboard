
function str_sort(a, b){ return ( $(a).attr("data-sort") == null ||	$(b).attr("data-sort") < $(a).attr("data-sort") ) ? 1 : -1; }

jQuery.fn.sortDivs = function sortDivsStr() { $("> div", this[0]).sort(str_sort).appendTo(this[0]); }

$(initialize);

function initialize() {

	WS.Register( [ "ScoreBoard.Clock(Intermission).Number",
			"ScoreBoard.Rulesets.CurrentRule(Period.Number)",
			"ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)",
			"ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)",
			"ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)",
			"ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)" ]);

	WS.Register( [	'ScoreBoard.Clock(Timeout).Running',
			'ScoreBoard.TimeoutOwner',
			'ScoreBoard.OfficialReview',
			'ScoreBoard.Team(*).Timeouts',
			'ScoreBoard.Team(*).OfficialReviews',
			'ScoreBoard.Team(*).RetainedOfficialReview' ], function(k,v) { smallDescriptionUpdate(k,v); } );

	WS.Register( ['ScoreBoard.Period(*).Jam(*).TeamJam(*).JamScore',
			'ScoreBoard.Period(*).Jam(*).TeamJam(*).Lead',
			'ScoreBoard.Period(*).Jam(*).TeamJam(*).Lost'], function(k,v) { jamData(k,v); } );

	WS.Register( [
			'ScoreBoard.Team(*).Skater(*).Name',
			'ScoreBoard.Team(*).Skater(*).Number',
			'ScoreBoard.Team(*).Skater(*).Flags' ], function(k,v) {
				var me = '.RosterTeam' + k.Team + ' .Team' + k.Team + ' .Skater[data-skaterId=' + k.Skater + ']';
				var mb = '.PenaltyTeam' + k.Team + ' .Team' + k.Team + ' .Skater[data-skaterId=' + k.Skater + ']';
				if (v == null) {
					$(me).remove();
					$(mb).remove();
					return;
				}
				ensureSkaterExists(k.Skater, k.Team);

				if(k.field == 'Number')	{ updateSkaterNumber(me,mb,v); }
				if(k.field == 'Flags') {
					$('.'+k.field, me).attr('data-flag', v);
					$(mb).attr('data-flag', v);
				} else if (v == "") {
					c = $('.'+k.field, me).html('&nbsp;');
					c = $('.'+k.field, mb).html('&nbsp;');
				} else {
					c = $('.'+k.field, me).text(v);
					c = $('.'+k.field, mb).text(v);
				}
			});

	WS.Register( 'ScoreBoard.Team(*).Skater(*).Penalty(*).Code', function(k,v) {
		if (k.Penalty == 0) {
			// Foulout/Expulsion.
			return;
		}
		var sel = '.PenaltyTeam' + k.Team + ' .Team' + k.Team + ' .Skater[data-skaterId=' + k.Skater + ']';
		if (v == null) {
			$('.Number-'+k.Penalty, sel).remove();
			$(sel).attr('data-count', $('.Penalty', sel).length);
			return;
		}
		ensureSkaterExists(k.Skater, k.Team);
		createPenalty(sel, k.Penalty, v);
	} );

	WS.Register( ['ScoreBoard.Team(*).Color(overlay_bg)',
			'ScoreBoard.Team(*).Color(overlay_fg)' ], function(k,v) {
		var style;
		for(i in document.styleSheets) if(document.styleSheets[i].title =='jsStyle') style=document.styleSheets[i];
		if(style) {
			var ns,r;
			var rule;
			// chrome seems to like things in lowercase
			var rd = '#sb .colourteam'+k.Team;

			if(k.Color == 'overlay_bg') ns = 'background-color';
			if(k.Color == 'overlay_fg') ns = 'color';
			for(var r=0; r<style.rules.length ; r++ ) {
				var dd = style.rules[r];
				if(dd.selectorText == rd && dd.style[0] == ns) style.deleteRule(r);
			}
			if(v != null) style.addRule(rd, ns + ': ' + v);
		}
	});

	WS.Register( 'ScoreBoard.Team(*).Logo', function(k,v) {
		if(v && v != '') {
			$('img.TeamLogo'+k.Team).attr('src', v).css('display', 'block');
			$('img.TeamLogo'+k.Team).parent().removeClass('NoLogo');
		} else {
			$('img.TeamLogo'+k.Team).css('display', 'none');
			$('img.TeamLogo'+k.Team).parent().addClass('NoLogo');
		}
	});

	WS.Register( 'ScoreBoard.Clock(Period).Number', function(k,v) {
		if(v == 2) { $('.PPJBox .Team .Period2').show(); } else { $('.PPJBox .Team .Period2').hide(); }
	});

	WS.Register([ 'ScoreBoard.Settings.Setting(Overlay.Interactive.BackgroundColor)' ], function(k,v) {
		if (k == 'ScoreBoard.Settings.Setting(Overlay.Interactive.BackgroundColor)') {
			$('#VIDEO-BG').css('backgroundColor', v || 'transparent');
		}
	});

	WS.Register([ 'ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)', 'ScoreBoard.Settings.Setting(Overlay.Interactive.Score)' ], function(k,v) {
		$('div[data-setting="'+k+'"]').each(function() {
			if(v == 'On') { $(this).addClass('Show'); } else { $(this).removeClass('Show'); }
		});
	});

	WS.Register([ 'ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)' ], function(k,v) {
		$('div[data-setting="'+k+'"]').each(function() {
			if(v == 'On') { $(this).addClass('ShowJammers'); } else { $(this).removeClass('ShowJammers'); }
		});
	});


	WS.Register('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', function(k,v) {
		$('.OverlayPanel').removeClass('Show');
		// sort divs in the panel before we show, just in case it's changed
		if(v == 'PenaltyTeam1' || v == 'PenaltyTeam2') {
			c = $('.PenaltyTeam [data-flag="BC"]');
			c.empty().remove();
		}
		$('.OverlayPanel.'+v+' .SortBox').sortDivs();
		$('.OverlayPanel.' + v).addClass('Show');
	});

	WS.Register([ 'ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Line' ] , function(k,v) {
		sp = '.' + k.split('.').slice(4,6).join(' .').slice(0, -1);
		$(sp).text(v);
	});

			WS.Register([ 'ScoreBoard.Settings.Setting(Overlay.Interactive.LowerThird.Style)' ] , function(k,v) {
				$('.LowerThird .Line2').removeClass( 'ColourTeam1 ColourTeam2 ColourDefault' ).addClass(v);
			});

			$(document).keyup(function(e) {
				if(e.which == 74) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.ShowJammers)'] == 'On' ? 'Off' : 'On'); }
				if(e.which == 67) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Clock)'] == 'On' ? 'Off' : 'On'); }
				if(e.which == 83) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Score)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Score)'] == 'On' ? 'Off' : 'On'); }
				if(e.which == 49) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] == 'RosterTeam1' ? '' : 'RosterTeam1'); }
				if(e.which == 50) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] == 'RosterTeam2' ? '' : 'RosterTeam2'); }
				if(e.which == 51) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] == 'PenaltyTeam1' ? '' : 'PenaltyTeam1'); }
				if(e.which == 52) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', WS.state['ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)'] == 'PenaltyTeam2' ? '' : 'PenaltyTeam2'); }
				if(e.which == 32) { WS.Set('ScoreBoard.Settings.Setting(Overlay.Interactive.Panel)', ''); }
			});

			WS.AutoRegister();
			WS.Connect();

			setTimeout(function() { $('body').removeClass('preload'); }, 1000);
}



function ensureSkaterExists(skaterId, team) {
	if ($('.PenaltyTeam' + team + ' .Team' + team + ' .Skater[data-skaterId=' + skaterId + ']').length == 0) {
		// create the roster entry for this skater
		var xv = $('<div class="Skater"></div>');
		xv.attr('data-skaterId', skaterId);
		$('<div class="Number ColourTeam' + team +'"></div>').appendTo(xv);
		$('<div class="Name">&nbsp;</div>').appendTo(xv);
		$('<div class="Flags">&nbsp;</div>').appendTo(xv);
		$('.RosterTeam' + team + ' .Team' + team).append(xv);

		// create the penalty tracking entry for this skater
		var xz = $('<div class="Skater"></div>');
		xz.attr('data-skaterId', skaterId);
		xz.attr('data-count', 0);
		$('<div class="Number ColourTeam' + team + '">&nbsp;</div>').appendTo(xz);
		$('<div class="Name">&nbsp;</div>').appendTo(xz);
		$('<div class="Penalties">&nbsp;</div>').appendTo(xz);
		$('.PenaltyTeam' + team + ' .Team' + team).append(xz);
	}
}

function updateSkaterNumber(me,mb,v) {
	sv = v;
	if(v == '' || v == '-' || v == null) { sv = 'ZZZB'; v = '-'; }
	$('.Number',me).parent().attr('data-sort', sv);
	$('.Number',mb).parent().attr('data-sort', sv);
	$(me).parent().sortDivs(); $(mb).parent().sortDivs();
}

function createPenalty(mb, pnum, v) {
	$('.Number-'+pnum, mb).remove();
	var penalty = $('<div class="Penalty Number-' + pnum + ' Penalty-' + v + '">' + v + '</div>');
	$(mb).attr('data-count', $('.Penalty', mb).length+1);
	penalty.attr('data-sort', pnum);
	$(mb).children(".Penalties").append(penalty);
	$(mb).children(".Penalties").sortDivs();
}

function jamData(k,v) {
	var period = k.Period;
	var jam = k.Jam;
	var team = k.TeamJam;
	var key = k.field;

	pa = '.PPJBox .Team'+ team + ' .Period'+period;
	me = pa + ' .Jam'+jam;
	var $pId = $(pa);
	var $mId = $(me);

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

	switch(key) {
		case 'Lead':
			$(me).attr('lead', v);
			break;
		case 'Lost':
			$(me).attr('lost', v);
			break;
		case 'JamScore':
			setHeight = v*4 + 'px';
			$(me).css('height', setHeight);

			if(team == 1) {
				hid = $('.PPJBox .Team1 .Period').innerHeight();
				marg = parseInt(hid)-parseInt(setHeight);
				$(me).css('marginTop', marg);
			}
			if(v != 0) { $('.Points', me).text(v); }
			break;
	}

	pointsPerJamColumnWidths();
}

function pointsPerJamColumnWidths() {
	ne1 = $('.PPJBox .Team1 .GraphBlock').length;
	ne2 = $('.PPJBox .Team2 .GraphBlock').length;
	if(ne2 > ne1)	ne1=ne2;
	nel = ne1 + 3;
	wid = parseInt( $('.PPJBox').innerWidth() );
	newwidth = parseInt(wid / nel) - 3;
	$('.ColumnWidth').innerWidth(newwidth);
	$('.PPJBox .Team1 .GraphBlock').css('backgroundColor', WS.state['ScoreBoard.Team(1).Color(overlay_bg)']);
	$('.PPJBox .Team2 .GraphBlock').css('backgroundColor', WS.state['ScoreBoard.Team(2).Color(overlay_bg)']);
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
		if(to != "" && to != "O" && or) { ret = 'Official Review'; }
		if(to != "" && to != "O" && !or) { ret = 'Team Timeout'; }
		if(to == "O") { ret = 'Official Timeout'; }
		$('.ClockDescription').css('backgroundColor', 'red');
	} else if(lc) {
		ret = WS.state["ScoreBoard.Clock(Lineup).Name"];
		$('.ClockDescription').css('backgroundColor', '#888');
	} else if(ic) {
		var num = WS.state["ScoreBoard.Clock(Intermission).Number"];
		var max = WS.state["ScoreBoard.Rulesets.CurrentRule(Period.Number)"];
		var isOfficial = WS.state["ScoreBoard.OfficialScore"];
		if (num == 0)
			ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.PreGame)"];
		else if (num != max)
			ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Intermission)"];
		else if (!isOfficial)
			ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Unofficial)"];
		else
			ret = WS.state["ScoreBoard.Settings.Setting(ScoreBoard.Intermission.Official)"];

		$('.ClockDescription').css('backgroundColor', 'blue');
	} else {
		ret = 'Jam';
	}

	return ret;

}

//# sourceURL=views\overlays\interactive\index.js
