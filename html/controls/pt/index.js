(function () {

	'use strict';
	$(initialize);
	var penaltyEditor = null;

	var periodNumber = null;
	var jamNumber = null;

	var teamId = null;
	var skaterId = null;
	var penaltyId = null;
	var penaltyNumber = null;

	function initialize() {

		WS.Connect();
		WS.AutoRegister();

		$.each([1, 2], function (idx, t) {
			WS.Register(['ScoreBoard.Team(' + t + ').Name'], function () { teamNameUpdate(t); });
			WS.Register(['ScoreBoard.Team(' + t + ').AlternateName(operator).Name'], function () { teamNameUpdate(t); });

			WS.Register(['ScoreBoard.Team(' + t + ').Color'], function (k, v) {
				$('#head' + t).css('background-color', WS.state['ScoreBoard.Team(' + t + ').Color(operator_bg)']);
				$('#head' + t).css('color', WS.state['ScoreBoard.Team(' + t + ').Color(operator_fg)']);
			});
		});
		WS.Register(['ScoreBoard.Clock(Period).Number'], updatePeriod);
		WS.Register(['ScoreBoard.Clock(Jam).Number'], updateJam);

		WS.Register(['ScoreBoard.Team(1).Skater'], function (k, v) { skaterUpdate(1, k, v); });
		WS.Register(['ScoreBoard.Team(2).Skater'], function (k, v) { skaterUpdate(2, k, v); });
		WS.Register(['ScoreBoard.PenaltyCodes.Code'], penaltyCode);

		WS.Register(['ScoreBoard.Rulesets.CurrentRule(Penalties.NumberToFoulout)']);
		WS.Register(['ScoreBoard.Rulesets.CurrentRule(Period.Number)'], function (k, v) { setupPeriodSelect(v); });
		
		WS.Register(['ScoreBoard.Period']);

		if (_windowFunctions.checkParam("autoFit", "true")) {
			$('.Team').addClass('auto-fit');
		}

		penaltyEditor = $('div.PenaltyEditor').dialog({
			modal: true,
			closeOnEscape: false,
			title: 'Penalty Editor',
			autoOpen: false,
			width: '80%',
		});

		addFOCode();

		$(".PenaltyEditor .period_minus").click(function () { adjust("Period", 'prev'); });
		$(".PenaltyEditor .period_plus").click(function () { adjust("Period", 'next'); });
		$(".PenaltyEditor .jam_minus").click(function () { adjust("Jam", 'prev'); });
		$(".PenaltyEditor .jam_plus").click(function () { adjust("Jam", 'next'); });
		$(".PenaltyEditor .clear").click(function () { clear(); });
		$(".PenaltyEditor .Period").change(function() { setupJamSelect(); })
	}

	function adjust(which, inc) {
		$(".PenaltyEditor ." + which + " :selected")[inc]().prop("selected", true);
		if (which == "Period") { setupJamSelect(); }
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
		$('<style> .Box.period-' + periodNumber +' { font-weight: bold; color: #000; }</style>')
			.attr('id','current-period-style')
			.appendTo('head');
	}

	function updateCurrentJamPeriodStyle() {
		if (jamNumber === null || periodNumber === null) {
			return;
		}

		$('#current-jam-style').remove();
		$("<style> .Box.period-" + periodNumber + ".jam-" + jamNumber + " { text-decoration: underline; } </style>")
			.attr('id', 'current-jam-style')
			.appendTo('head');
	}

	function clear() {
		if (penaltyId == null || skaterId == null) {
			penaltyEditor.dialog("close");
		} else {
			WS.Set("ScoreBoard.Team(" + teamId + ").Skater(" + skaterId + ").Penalty(" + penaltyNumber + ")", null);
			penaltyEditor.dialog('close');
		}
	}

	var skaterIdRegex = /Skater\(([^\)]+)\)/;
	var penaltyRegex = /Penalty\(([^\)]+)\)/;
	function skaterUpdate(t, k, v) {
		
		var match = (k || "").match(skaterIdRegex);
		if (match == null || match.length == 0)
			return;

		var id = match[1];
		var field = k.split('.').pop();
		var prefix = 'ScoreBoard.Team(' + t + ').Skater(' + id + ')';
		if (k == prefix + "." + field) {
			if (field === 'Number') {
				$('.Team' + t + ' .Skater[id=' + id + ']').remove();
				if (v == null) {
					return;
				}
	
				// New skater, or number has been updated.
				makeSkaterRows(t, id, v);
				for (var i = 1; i <= 9; i++)
					displayPenalty(t, id, i);
				displayPenalty(t, id, 'FO_EXP');
			} else if (field === 'Role') {
				var numberCell = $('.Team' + t + ' .Skater.Penalty[id=' + id + '] .Number');
				if(v === 'Jammer' || v === 'Pivot' || v === 'Blocker') {
					numberCell.addClass('onTrack');
				} else {
					numberCell.removeClass('onTrack');
				}
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
		var penaltyBox = $('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Box' + p);
		var jamBox = $('.Team' + t + ' .Skater.Jam[id=' + s + '] .Box' + p);
		var totalBox = $('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Total');

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
			penaltyBox.text(WS.state[prefix + ".Code"]);
			jamBox.text(penaltyPeriod + '-' + penaltyJam);

			penaltyBox.data("period", penaltyPeriod);
			penaltyBox.data("jam", penaltyJam);

			jamBox.addClass("period-" + penaltyPeriod).addClass("jam-" + penaltyJam);
			penaltyBox.addClass("period-" + penaltyPeriod).addClass("jam-" + penaltyJam);

		} else {
			penaltyBox.data("id", null);
			jamBox.data("id", null);
			penaltyBox.html("&nbsp;");
			jamBox.html("&nbsp;");
		}

		var cnt = 0; // Change row colors for skaters on 5 or more penalties, or expulsion.
		var limit = WS.state["ScoreBoard.Rulesets.CurrentRule(Penalties.NumberToFoulout)"];
		var fo_exp = ($($('.Team' + t + ' .Skater.Penalty[id=' + s + '] .BoxFO_EXP')[0]).data("id") != null);

		$('.Team' + t + ' .Skater.Penalty[id=' + s + '] .Box').each(function (idx, elem) { cnt += ($(elem).data("id") != null ? 1 : 0); });
		totalBox.text(cnt);
		$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn1", cnt == limit-2 && !fo_exp);
		$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn2", cnt == limit-1 && !fo_exp);
		$('.Team' + t + ' .Skater[id=' + s + ']').toggleClass("Warn3", cnt >= limit || fo_exp);
	}

	function teamNameUpdate(t) {
		var head = document.getElementById('head' + t);
		var teamName = WS.state['ScoreBoard.Team(' + t + ').Name'];

		if (WS.state['ScoreBoard.Team(' + t + ').AlternateName(operator).Name'] != null) {
			teamName = WS.state['ScoreBoard.Team(' + t + ').AlternateName(operator).Name']
		}

		head.innerHTML = '<span class="Team' + t + 'custColor"; style="font-size: 200%;">' + teamName + '</span>';
	}

	function makeSkaterRows(t, id, number) {
		var team = $('.Team' + t + ' tbody');
		var p = $('<tr>').addClass('Skater Penalty').attr('id', id).data('number', number);
		var j = $('<tr>').addClass('Skater Jam').attr('id', id);

		var numberCell = $('<td>').addClass('Number').attr('rowspan', 2).text(number).click(function () { openPenaltyEditor(t, id); });
		var role = WS.state['ScoreBoard.Team(' + t + ').Skater(' + id + ').Role'];
		
		if(role === 'Jammer' || role === 'Pivot' || role === 'Blocker') {
			numberCell.addClass('onTrack');
		}
		
		p.append(numberCell);

		$.each(new Array(9), function (idx) {
			var c = idx + 1;
			p.append($('<td>').addClass('Box Box' + c).html('&nbsp;').click(function () { openPenaltyEditor(t, id, c); }));
			j.append($('<td>').addClass('Box Box' + c).html('&nbsp;').click(function () { openPenaltyEditor(t, id, c); }));
		});

		p.append($('<td>').addClass('BoxFO_EXP').html('&nbsp;').click(function () { openPenaltyEditor(t, id, 'FO_EXP'); }));
		j.append($('<td>').addClass('BoxFO_EXP').html('&nbsp;').click(function () { openPenaltyEditor(t, id, 'FO_EXP'); }));
		p.append($('<td>').attr('rowspan', 2).addClass('Total').text('0'));

		var inserted = false;
		team.find('tr.Penalty').each(function (idx, row) {
			row = $(row);
			if (row.data('number') > number) {
				row.before(p).before(j);
				inserted = true;
				return false;
			}
		});
		if (!inserted)
			team.append(p).append(j);
	}
	
	function setupPeriodSelect(num) {
		var select = $('.PenaltyEditor .Period');
		select.empty();
		for (var i = 1; i <= num; i++) {
			$('<option>').attr('value', i).text(i).appendTo(select);
		}
	}
	
	function setupJamSelect() {
		var p = $('.PenaltyEditor .Period').val();
		var prefix = 'ScoreBoard.Period('+p+').';
		var min = WS.state[prefix + 'CurrentJamNumber'];
		while (WS.state[prefix + 'Jam('+ (min-1) +').Id'] != null) { min--; }
		var max = WS.state[prefix + 'CurrentJamNumber'];
		while (WS.state[prefix + 'Jam('+ (max+1) +').Id'] != null) { max++; }
		var select = $('.PenaltyEditor .Jam');
		select.empty();
		for (var i = min; i <= max; i++) {
			$('<option>').attr('value', WS.state[prefix + 'Jam('+i+').Id']).text(i).appendTo(select);
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
		teamId = t;
		skaterId = id;
		penaltyNumber = which;

		$('.PenaltyEditor .Codes>div').removeClass('Active');

		penaltyEditor.dialog('option', 'title', teamColor + ' ' + skaterNumber + ' (' + skaterName + ')');
		$('.PenaltyEditor .Period').val(periodNumber);
		setupJamSelect();
		$('.PenaltyEditor .Jam').val(WS.state["ScoreBoard.Period("+periodNumber+").Jam("+jamNumber+").Id"]);

		$('.Codes>.Penalty').toggle(which != 'FO_EXP');
		$('.Codes>.FO_EXP').toggle(which == 'FO_EXP');

		if (which != null) {
			var penaltyBox = $('.Team' + t + ' .Skater.Penalty[id=' + id + '] .Box' + which);
			var c = WS.state[prefix + '.Penalty(' + which + ').Code'];
			var p = WS.state[prefix + '.Penalty(' + which + ').PeriodNumber'];
			var j = WS.state[prefix + '.Penalty(' + which + ').Jam'];
			penaltyId = penaltyBox.data("id");
			if (penaltyId != null) {
				if (c == null || j == null || p == null) {
					penaltyId = null;
				} else {
					$('.PenaltyEditor .Codes>div.' + (which == 'FO_EXP' ? 'FO_EXP' : 'Penalty') + '[code="' + c + '"]').addClass('Active');
					$('.PenaltyEditor .Period').val(p);
					setupJamSelect();
					$('.PenaltyEditor .Jam').val(j);
				}
			}
			while (!isNaN(penaltyNumber) && penaltyNumber > 1 &&
					WS.state[prefix + '.Penalty(' + (penaltyNumber-1) + ').Code'] == null) {
				penaltyNumber--;
			}
		}

		penaltyEditor.dialog('open');
	}

	function submitPenalty() {
		var sortKey = 'ScoreBoard.Team(' + teamId + ').Skater(' + skaterId + ').SortPenalties';
		var prefix = 'ScoreBoard.Team(' + teamId + ').Skater(' + skaterId + ').Penalty(' + penaltyNumber + ')';
		WS.Set(sortKey, false);
		WS.Set(prefix + '.Code', $('.PenaltyEditor .Codes .Active').attr('code'));
		WS.Set(prefix + '.Jam', $('.PenaltyEditor .Jam').val());
		WS.Set(sortKey, true);

		penaltyEditor.dialog('close');
	}

	var codeIdRegex = /Code\(([^\)]+)\)/;
	function penaltyCode(k, v) {
		var match = (k || "").match(codeIdRegex);
		if (match == null || match.length == 0)
			return;

		var code = match[1];

		addPenaltyCode('Penalty', code, v);
		addPenaltyCode('FO_EXP', code, v);
	}

	function addFOCode() {
		addPenaltyCode('FO_EXP', 'FO', 'Foul-Out');
	}

	function addPenaltyCode(type, code, verbalCues) {

		var div = $('.Codes .' + type + '[code="' + code + '"]');

		if (verbalCues === null) {
			div.detach();
			return;
		} else if (div.length > 0) {
			div.find('.Description').empty();
		} else {
			div = $('<div>').attr('code', code).addClass(type).click(function (e) {
				$('.PenaltyEditor .Codes>div').removeClass('Active');
				div.addClass('Active');
				submitPenalty();
			});

			var title = code;

			if (type === 'FO_EXP' && code !== 'FO') {
				title = title + '(EXP)';
			}

			$('<div>').addClass('Code').text(title).appendTo(div);
			$('<div>').addClass('Description').appendTo(div);

			var codes = $('.PenaltyEditor .Codes');
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
})();
//# sourceURL=controls\pt\ptcolor.js
