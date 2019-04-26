function preparePltTable(element, teamId, mode, statsbookPeriod) {

	'use strict';
	$(initialize);

	var periodNumber = null;
	var jamNumber = null;

	function initialize() {
		
		var table = $('<table cellpadding="0" cellspacing="0" border="1">').addClass('PLT Team').appendTo(element);
		var thead = $('<tr>').appendTo($('<thead>').appendTo(table));
		if (mode != 'copyToStatsbook') {
			$('<td>').text('Bench').appendTo(thead);
			$('<td>').text('Jammer').appendTo(thead);
			$('<td>').text('Pivot').appendTo(thead);
			$('<td>').text('Blocker').appendTo(thead);
			$('<td>').attr('colspan', '2').text('Box').appendTo(thead);
			$('<td>').text('#').appendTo(thead);
		}
		$('<td>').attr('colspan', '9').attr('id', 'head').text('Penalty/Jam').appendTo(thead);
		$('<td>').text('FO_Ex').appendTo(thead);
		if (mode != 'copyToStatsbook') {
			$('<td>').text('Total').appendTo(thead);
		}
		$('<tbody>').appendTo(table);

		WS.Register(['ScoreBoard.Team(' + teamId + ').Name'], function () { teamNameUpdate(); });
		WS.Register(['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name'], function () { teamNameUpdate(); });

		WS.Register(['ScoreBoard.Team(' + teamId + ').Color'], function (k, v) {
			element.find('#head').css('background-color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_bg)']);
			element.find('#head').css('color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_fg)']);
		});
		WS.Register(['ScoreBoard.Clock(Period).Number'], updatePeriod);
		WS.Register(['ScoreBoard.Clock(Jam).Number'], updateJam);

		WS.Register(['ScoreBoard.Team('+teamId+').Skater'], function (k, v) { skaterUpdate(teamId, k, v); });

		WS.Register(['ScoreBoard.Rulesets.CurrentRule(Penalties.NumberToFoulout)']);
		
		WS.Register(['ScoreBoard.Period']);
		WS.Register(['ScoreBoard.CurrentPeriodNumber']);
		WS.Register(['ScoreBoard.UpcomingJam']);

		if (mode == "autoFit") {
			element.find('.Team').addClass('auto-fit');
		}
	}

	function updatePeriod(k, v) {
		periodNumber = v;
		updateCurrentPeriodStyle();
		updateCurrentJamPeriodStyle();
	}

	function updateJam(k, v) {
		jamNumber = v;
		updateCurrentJamPeriodStyle();
	}
	
	function updateCurrentPeriodStyle() {
		if(periodNumber === null) {
			return;
		}
		
		$('#current-period-style').remove();
		if (mode != 'copyToStatsbook') {
			$('<style> .Box.period-' + periodNumber +' { font-weight: bold; color: #000; }</style>')
				.attr('id','current-period-style')
				.appendTo('head');
		}
	}

	function updateCurrentJamPeriodStyle() {
		if (jamNumber === null || periodNumber === null) {
			return;
		}

		$('#current-jam-style').remove();
		if (mode != 'copyToStatsbook') {
			$("<style> .Box.period-" + periodNumber + ".jam-" + jamNumber + " { text-decoration: underline; } </style>")
				.attr('id', 'current-jam-style')
				.appendTo('head');
		}
	}

	var skaterIdRegex = /Skater\(([^\)]+)\)/;
	var penaltyRegex = /Penalty\(([^\)]+)\)/;
	function skaterUpdate(t, k, v) {
		
		var match = (k || "").match(skaterIdRegex);
		if (match == null || match.length == 0) { return; }
		
		var id = match[1];
		var field = k.split('.').pop();
		var prefix = 'ScoreBoard.Team(' + t + ').Skater(' + id + ')';
		if (k == prefix + "." + field) {
			if (field === 'Number') {
				element.find('.Skater[id=' + id + ']').remove();
				if (v == null) {
					return;
				}
	
				// New skater, or number has been updated.
				makeSkaterRows(t, id, v);
				for (var i = 1; i <= 9; i++)
					displayPenalty(t, id, i);
				displayPenalty(t, id, 0);
			} else if (field === 'Role') {
				element.find('.Skater.Penalty[id=' + id + '] .Role').removeClass('OnTrack');
				element.find('.Skater.Penalty[id=' + id + '] .'+v).addClass('OnTrack');
			} else if (field === 'PenaltyBox') {
				element.find('.Skater.Penalty[id=' + id + '] .Sitting').toggleClass('inBox', isTrue(v));
			} else if (field === 'CurrentBoxSymbols') {
				element.find('.Skater.Penalty[id=' + id + '] .Trips').text(v);
			}
		} else {
			// Look for penalty
			match = k.match(penaltyRegex);
			if (match == null || match.length == 0)
				return;
			var p = match[1];
			displayPenalty(t, id, p);
		}
	}

	function displayPenalty(t, s, p) {
		var penaltyBox = element.find('.Skater.Penalty[id=' + s + '] .Box' + p);
		var jamBox = element.find('.Skater.Jam[id=' + s + '] .Box' + p);
		var totalBox = element.find('.Skater.Penalty[id=' + s + '] .Total');

		var prefix = 'ScoreBoard.Team(' + t + ').Skater(' + s + ').Penalty(' + p + ')';
		var code = WS.state[prefix + ".Code"];

		var penaltyPeriod = penaltyBox.data("period");
		var penaltyJam = penaltyBox.data("jam");

		if (penaltyPeriod !== undefined) {
			penaltyBox.removeClass("period-" + penaltyPeriod);
			jamBox.removeClass("period-" + penaltyPeriod);
		}

		if (penaltyJam !== undefined) {
			penaltyBox.removeClass("jam-" + penaltyJam);
			jamBox.removeClass("jam-" + penaltyJam);
		}

		if (code != null) {
			penaltyPeriod = WS.state[prefix + ".PeriodNumber"];
			penaltyJam = WS.state[prefix + ".JamNumber"];

			penaltyBox.data("id", WS.state[prefix + ".Id"]);
			jamBox.data("id", WS.state[prefix + ".Id"]);
			if (mode == 'copyToStatsbook') {
				if (penaltyPeriod == statsbookPeriod) {
					penaltyBox.text(WS.state[prefix + ".Code"]);
					jamBox.text(penaltyJam);
				} else {
					penaltyBox.html('&nbsp;');
					jamBox.html('&nbsp;');
				}
			} else {
				penaltyBox.text(WS.state[prefix + ".Code"]);
				jamBox.text(penaltyPeriod + '-' + penaltyJam);
			}

			penaltyBox.data("period", penaltyPeriod);
			penaltyBox.data("jam", penaltyJam);

			jamBox.addClass("period-" + penaltyPeriod).addClass("jam-" + penaltyJam);
			penaltyBox.addClass("period-" + penaltyPeriod).addClass("jam-" + penaltyJam);
			
			jamBox.toggleClass("Unserved", !isTrue(WS.state[prefix + ".Served"]))
				.toggleClass("Serving", isTrue(WS.state[prefix + ".Serving"]));
			penaltyBox.toggleClass("Unserved", !isTrue(WS.state[prefix + ".Served"]))
				.toggleClass("Serving", isTrue(WS.state[prefix + ".Serving"]));

		} else {
			penaltyBox.data("id", null);
			jamBox.data("id", null);
			penaltyBox.html("&nbsp;");
			jamBox.html("&nbsp;");
			penaltyBox.removeClass("Serving Unserved");
			jamBox.removeClass("Serving Unserved");
		}

		var cnt = 0; // Change row colors for skaters on 5 or more penalties, or expulsion.
		var limit = WS.state["ScoreBoard.Rulesets.CurrentRule(Penalties.NumberToFoulout)"];
		var fo_exp = ($($('.PLT.Team .Skater.Penalty[id=' + s + '] .Box0')[0]).data("id") != null);

		element.find('.Skater.Penalty[id=' + s + '] .Box').each(function (idx, elem) {
			cnt += ($(elem).data("id") != null ? 1 : 0); });
		if (mode != 'copyToStatsbook') {
			totalBox.text(cnt);
		}
		element.find('.Skater[id=' + s + ']').toggleClass("Warn1", cnt == limit-2 && !fo_exp);
		element.find('.Skater[id=' + s + ']').toggleClass("Warn2", cnt == limit-1 && !fo_exp);
		element.find('.Skater[id=' + s + ']').toggleClass("Warn3", cnt >= limit || fo_exp);
	}

	function teamNameUpdate() {
		var head = element.find('#head');
		var teamName = WS.state['ScoreBoard.Team(' + teamId + ').Name'];

		if (WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name'] != null) {
			teamName = WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name']
		}

		head.innerHTML = '<span class="Team' + teamId + 'custColor"; style="font-size: 200%;">' + teamName + '</span>';
	}

	function makeSkaterRows(t, id, number) {
		var team = element.find('tbody');
		var p = $('<tr>').addClass('Skater Penalty').attr('id', id).data('number', number);
		var j = $('<tr>').addClass('Skater Jam').attr('id', id);

		if (mode != 'copyToStatsbook') {
			var role = WS.state['ScoreBoard.Team(' + t + ').Skater(' + id + ').Role'];
			var benchCell = $('<td>').addClass('Role Bench').attr('rowspan', 2).click(function() {
				WS.Set('ScoreBoard.Team(' + t + ').Skater(' + id + ').Role', 'Bench');
			}).append($('<span>').addClass('Num').text(number)).append($('<span>').addClass('Pos').text('Bench'));
			if (role == 'Bench') {
				benchCell.addClass('OnTrack');
			}
			p.append(benchCell);
			
			var jammerCell = $('<td>').addClass('Role Jammer').attr('rowspan', 2).click(function() {
				WS.Set('ScoreBoard.Team(' + t + ').Skater(' + id + ').Role', 'Jammer');
			}).append($('<span>').addClass('Num').text(number)).append($('<span>').addClass('Pos').text('J'));
			if (role == 'Jammer') {
				jammerCell.addClass('OnTrack');
			}
			p.append(jammerCell);
			
			var pivotCell = $('<td>').addClass('Role Pivot').attr('rowspan', 2).click(function() {
				WS.Set('ScoreBoard.Team(' + t + ').Skater(' + id + ').Role', 'Pivot');
			}).append($('<span>').addClass('Num').text(number)).append($('<span>').addClass('Pos').text('P'));
			if (role == 'Pivot') {
				pivotCell.addClass('OnTrack');
			}
			p.append(pivotCell);
			
			var blockerCell = $('<td>').addClass('Role Blocker').attr('rowspan', 2).click(function() {
				WS.Set('ScoreBoard.Team(' + t + ').Skater(' + id + ').Role', 'Blocker');
			}).append($('<span>').addClass('Num').text(number)).append($('<span>').addClass('Pos').text('B'));
			if (role == 'Blocker') {
				blockerCell.addClass('OnTrack');
			}
			p.append(blockerCell);
			
			var boxCell = $('<td>').addClass('Sitting').attr('rowspan', 2).text('Box').click(function() {
				WS.Set('ScoreBoard.Team('+t+').Skater('+id+').PenaltyBox', !$(this).hasClass('inBox'));
			});
			if (isTrue(WS.state['ScoreBoard.Team('+t+').Skater('+id+').PenaltyBox'])) {
				boxCell.addClass('inBox');
			}
			p.append(boxCell);
			var tripCell = $('<td>').addClass('Trips').attr('rowspan', 2)
				.text(WS.state['ScoreBoard.Team('+t+').Skater('+id+').CurrentBoxSymbols']);
			p.append(tripCell);
	
			var numberCell = $('<td>').addClass('Number').attr('rowspan', 2).text(number).click(function () { openPenaltyEditor(t, id, 10); });
			p.append(numberCell);
		}
		$.each(new Array(9), function (idx) {
			var c = idx + 1;
			p.append($('<td>').addClass('Box Box' + c).html('&nbsp;').click(function () { openPenaltyEditor(t, id, c); }));
			j.append($('<td>').addClass('Box Box' + c).html('&nbsp;').click(function () { openPenaltyEditor(t, id, c); }));
		});

		p.append($('<td>').addClass('Box Box0').html('&nbsp;').click(function () { openPenaltyEditor(t, id, 0); }));
		j.append($('<td>').addClass('Box Box0').html('&nbsp;').click(function () { openPenaltyEditor(t, id, 0); }));
		if (mode != 'copyToStatsbook') {
			p.append($('<td>').attr('rowspan', 2).addClass('Total').text('0'));
		}

		var inserted = false;
		team.find('tr.Penalty').each(function (idx, row) {
			row = $(row);
			if (row.data('number') > number) {
				row.before(p).before(j);
				inserted = true;
				return false;
			}
		});
		if (!inserted) { team.append(p).append(j); }
	}
}

function openPenaltyEditor(t, id, which) {
	var prefix = 'ScoreBoard.Team(' + t + ')';
	var teamColor = WS.state[prefix + '.AlternateName(team).Name'];
	if (teamColor == null)
		teamColor = WS.state[prefix + '.AlternateName(operator).Name'];
	if (teamColor == null)
		teamColor = WS.state[prefix + '.Name'];

	prefix = 'ScoreBoard.Team(' + t + ').Skater(' + id + ')';
	var skaterName = WS.state[prefix + '.Name'];
	var skaterNumber = WS.state[prefix + '.Number'];
	
	var penaltyNumber = which;
	var penaltyId = null;
	var wasServed = false;

	$('#PenaltyEditor .Codes>div').removeClass('Active');

	penaltyEditor.dialog('option', 'title', teamColor + ' ' + skaterNumber + ' (' + skaterName + ')');
	var periodNumber = WS.state["ScoreBoard.CurrentPeriodNumber"];
	$('#PenaltyEditor .Period').val(periodNumber).change();
	$('#PenaltyEditor .Jam').val(WS.state["ScoreBoard.Period("+periodNumber+").CurrentJam"]);

	$('#PenaltyEditor .Codes>.Penalty').toggle(which != 0);
	$('#PenaltyEditor .Codes>.FO_EXP').toggle(which == 0);

	if (which != null) {
		var penaltyBox = $('.PLT.Team .Skater.Penalty[id=' + id + '] .Box' + which);
		var c = WS.state[prefix + '.Penalty(' + which + ').Code'];
		var p = WS.state[prefix + '.Penalty(' + which + ').PeriodNumber'];
		var j = WS.state[prefix + '.Penalty(' + which + ').Jam'];
		wasServed = isTrue(WS.state[prefix + '.Penalty(' + which + ').Served']);
		penaltyId = penaltyBox.data("id");
		if (penaltyId != null) {
			if (c == null || j == null || p == null) {
				penaltyId = null;
			} else {
				$('#PenaltyEditor .Codes>div.' + (which == 0 ? 'FO_EXP' : 'Penalty') + '[code="' + c + '"]').addClass('Active');
				$('#PenaltyEditor .Period').val(p).change();
				$('#PenaltyEditor .Jam').val(j);
				$('#PenaltyEditor #served').prop('checked', wasServed);
			}
		}
		while (!isNaN(penaltyNumber) && penaltyNumber > 1 &&
				WS.state[prefix + '.Penalty(' + (penaltyNumber-1) + ').Code'] == null) {
			penaltyNumber--;
		}
	}

	penaltyEditor.data('team', t).data('skater', id).data('pnr', penaltyNumber).data('pid', penaltyId).data('wasServed', wasServed);
	penaltyEditor.dialog('open');
}

var penaltyEditor = null;

function preparePenaltyEditor() {
	
	'use strict';
	$(initialize);

	function initialize() {
		
		var topTable = $('<table width="80%">').appendTo($('#PenaltyEditor'));
		var tr = $('<tr>').appendTo(topTable);
		$('<td width="33%">').append($('<span>').text('Period: ')).append($('<button>').addClass('period_minus').text('-1'))
			.append($('<select>').addClass('Period')).append($('<button>').addClass('period_plus').text('+1')).appendTo(tr);
		$('<td width="33%">').append($('<span>').text('Jam: ')).append($('<button>').addClass('jam_minus').text('-1'))
			.append($('<select>').addClass('Jam')).append($('<button>').addClass('jam_plus').text('+1')).appendTo(tr);
		$('<td width="33%">').append($('<button>').addClass('clear').text('Clear')).appendTo(tr);
		
		var tr2 = $('<tr>').appendTo(topTable);
		$('<td>').append($('<input type="checkbox">').attr('id', 'served'))
			.append($('<span>').text(' has been served')).appendTo(tr2);
		$('<div>').addClass('Codes').appendTo($('#PenaltyEditor'));

		WS.Register(['ScoreBoard.PenaltyCodes.Code'], penaltyCode);
		WS.Register(['ScoreBoard.Rulesets.CurrentRule(Period.Number)'], function (k, v) { setupPeriodSelect(v); });
	
		penaltyEditor = $('#PenaltyEditor').dialog({
			modal: true,
			closeOnEscape: false,
			title: 'Penalty Editor',
			autoOpen: false,
			width: '80%',
		});
	
		addFOCode();
	
		$("#PenaltyEditor .period_minus").click(function () { adjust("Period", 'prev'); });
		$("#PenaltyEditor .period_plus").click(function () { adjust("Period", 'next'); });
		$("#PenaltyEditor .jam_minus").click(function () { adjust("Jam", 'prev'); });
		$("#PenaltyEditor .jam_plus").click(function () { adjust("Jam", 'next'); });
		$("#PenaltyEditor .clear").click(function () { clear(); });
		$("#PenaltyEditor .Period").change(function() { setupJamSelect(); })
	}

	function adjust(which, inc) {
		$("#PenaltyEditor ." + which + " :selected")[inc]().prop("selected", true);
		if (which == "Period") { setupJamSelect(); }
	}

	function submitPenalty() {
		var teamId = penaltyEditor.data('team');
		var skaterId = penaltyEditor.data('skater');
		var penaltyNumber = penaltyEditor.data('pnr');
		var wasServed = penaltyEditor.data('wasServed');

		var prefix = 'ScoreBoard.Team(' + teamId + ').Skater(' + skaterId + ').Penalty(' + penaltyNumber + ')';
		WS.Set(prefix + '.Code', $('#PenaltyEditor .Codes .Active').attr('code'));
		if ($('#PenaltyEditor #served').prop('checked') != wasServed) {
			WS.Set(prefix + '.ForceServed', !wasServed);
		}
		WS.Set(prefix + '.Jam', $('#PenaltyEditor .Jam').val());

		penaltyEditor.dialog('close');
	}

	function clear() {
		var teamId = penaltyEditor.data('team');
		var skaterId = penaltyEditor.data('skater');
		var penaltyId = penaltyEditor.data('pid');
		var penaltyNumber = penaltyEditor.data('pnr');

		if (penaltyId == null || skaterId == null) {
			penaltyEditor.dialog("close");
		} else {
			WS.Set("ScoreBoard.Team(" + teamId + ").Skater(" + skaterId + ").Penalty(" + penaltyNumber + ").Remove", true);
			penaltyEditor.dialog('close');
		}
	}

	var codeIdRegex = /Code\(([^\)]+)\)/;
	function penaltyCode(k, v) {
		var match = (k || "").match(codeIdRegex);
		if (match == null || match.length == 0) { return; }

		var code = match[1];

		addPenaltyCode('Penalty', code, v);
		addPenaltyCode('FO_EXP', code, v);
	}

	function addFOCode() {
		addPenaltyCode('FO_EXP', 'FO', 'Foul-Out');
	}

	function addPenaltyCode(type, code, verbalCues) {

		var div = $('#PenaltyEditor .Codes .' + type + '[code="' + code + '"]');

		if (verbalCues === null) {
			div.detach();
			return;
		} else if (div.length > 0) {
			div.find('.Description').empty();
		} else {
			div = $('<div>').attr('code', code).addClass(type).click(function (e) {
				$('#PenaltyEditor .Codes>div').removeClass('Active');
				div.addClass('Active');
				submitPenalty();
			});

			var title = code;

			if (type === 'FO_EXP' && code !== 'FO') {
				title = title + '(EXP)';
			}

			$('<div>').addClass('Code').text(title).appendTo(div);
			$('<div>').addClass('Description').appendTo(div);

			var codes = $('#PenaltyEditor .Codes');
			var inserted = false;
			codes.children().each(function (idx, c) {
				c = $(c);
				if (c.attr('code') > code) {
					c.before(div);
					inserted = true;
					return false;
				}
			});
			if (!inserted)
				codes.append(div);
		}

		var desc = div.find('.Description');
		verbalCues.split(',').forEach(function (d) {
			$('<div>').text(d).appendTo(desc);
		});
	}

	function setupPeriodSelect(num) {
		var select = $('#PenaltyEditor .Period');
		select.empty();
		for (var i = 1; i <= num; i++) {
			$('<option>').attr('value', i).text(i).appendTo(select);
		}
	}
	
	function setupJamSelect() {
		var p = $('#PenaltyEditor .Period').val();
		var prefix = 'ScoreBoard.Period('+p+').';
		var min = WS.state[prefix + 'CurrentJamNumber'];
		if (isNaN(min)) { min = 1; }
		while (WS.state[prefix + 'Jam('+ (min-1) +').Id'] != null) { min--; }
		var max = WS.state[prefix + 'CurrentJamNumber'];
		if (isNaN(max)) { max = 0; }
		while (WS.state[prefix + 'Jam('+ (max+1) +').Id'] != null) { max++; }
		var select = $('#PenaltyEditor .Jam');
		select.empty();
		for (var i = min; i <= max; i++) {
			$('<option>').attr('value', WS.state[prefix + 'Jam('+i+').Id']).text(i).appendTo(select);
		}
		if (p >= WS.state['ScoreBoard.CurrentPeriodNumber']) {
			$('<option>').attr('value', WS.state['ScoreBoard.UpcomingJam']).text(max+1).appendTo(select);
		}
	}
}
//# sourceURL=controls\plt\plt.js
