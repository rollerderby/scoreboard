function prepareSkSheetTable(element, teamId, mode) {

	'use strict';
	$(initialize);

	var teamName = "";

	// Looking these up via the DOM is slow, so cache them.
	var periodElements = {};
	var jamElements = {};

	function initialize() {
		if (mode != 'operator') {
			WS.Register(['ScoreBoard.Team(' + teamId + ').Name'], function () { teamNameUpdate(); });
			WS.Register(['ScoreBoard.Team(' + teamId + ').AlternateName(operator)'], function () { teamNameUpdate(); });

			WS.Register(['ScoreBoard.Team(' + teamId + ').Color'], function (k, v) {
				element.find('#head').css('background-color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_bg)']);
				element.find('#head').css('color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_fg)']);
			});
		}

		WS.Register(['ScoreBoard.Period(*).Number',
				'ScoreBoard.Period(*).Jam(*).Number',
				'ScoreBoard.Period(*).Jam(*).StarPass',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').AfterSPScore',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Calloff',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').JamScore',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Injury',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Lead',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Lost',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').NoInitial',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').StarPass',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').TotalScore',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(Jammer).SkaterNumber',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(Pivot).SkaterNumber',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').ScoringTrip(*).AfterSP',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').ScoringTrip(*).Current',
				'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').ScoringTrip(*).Score'
		], handleUpdate);
	}

	function teamNameUpdate() {
		teamName = WS.state['ScoreBoard.Team(' + teamId + ').Name'];

		if (WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator)'] != null) {
			teamName = WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator)']
		}

		element.find('#head .Team').text(teamName);
	}

	function handleUpdate(k, v) {
		// Ensure periods/jams exist.
		if (!k.Period || k.Period == 0) return;
		if (v == null && k == 'ScoreBoard.Period('+k.Period+').Number') {
			element.children('table.Period[nr='+k.Period+']').remove();
			delete periodElements[k.Period];
			delete jamElements[k.Period];
		} else if (v != null){
			createPeriod(k.Period);
		}
		if (!k.Jam || k.Jam == 0) return;
		var prefix = 'ScoreBoard.Period('+k.Period+').Jam('+k.Jam+').';
		if (v == null && k == prefix + 'Number') {
			element.children('table.Period[nr='+k.Period+']').find('tr[nr='+k.Jam+']').remove();
			delete jamElements[k.Period][k.Jam];
		} else if (v != null) {
			createJam(k.Period, k.Jam);
		}

		var je = (jamElements[k.Period] || {})[k.Jam];
		if (je == null) return;
		var jamRow = je[0];
		var spRow = je[1];
		if (k == prefix + 'StarPass') {
			spRow.toggleClass('Hide', !isTrue(v));
		}

		// Everything after here is team specific.
		if (k.TeamJam != teamId) return;
		prefix = prefix + 'TeamJam(' + teamId + ').';
		switch (k.substring(prefix.length)) {
			case 'Fielding(Jammer).SkaterNumber':
				jamRow.find('.Jammer').text(v);
				break;
			case 'Lost':
				jamRow.find('.Lost').text(isTrue(v)?'X':'');
				break;
			case 'Lead':
				jamRow.find('.Lead').text(isTrue(v)?'X':'');
				break;

			case 'JamScore': case 'AfterSPScore': case 'TotalScore':
				if (mode != 'copyToStatsbook') {
						jamRow.find('.JamTotal').text(WS.state[prefix+'JamScore'] - WS.state[prefix+'AfterSPScore']);
						spRow.find('.JamTotal').text(WS.state[prefix+'AfterSPScore']);
						jamRow.find('.GameTotal').text(WS.state[prefix+'TotalScore'] - WS.state[prefix+'AfterSPScore']);
						spRow.find('.GameTotal').text(WS.state[prefix+'TotalScore']);
				}
				break;

			case 'Calloff': case 'Injury': case 'NoInitial': case 'StarPass':
			case 'ScoringTrip(1).AfterSP': case 'Fielding(Pivot).SkaterNumber':
				var row = jamRow;
				var otherRow = spRow;
				if (isTrue(WS.state[prefix+'StarPass'])) {
					row = spRow;
					otherRow = jamRow;
				}
				row.find('.Calloff').text(isTrue(WS.state[prefix+'Calloff'])?'X':'');
				row.find('.Injury').text(isTrue(WS.state[prefix+'Injury'])?'X':'');
				row.find('.NoInitial').text(isTrue(WS.state[prefix+'NoInitial'])?'X':'');
				otherRow.find('.Calloff').text('');
				otherRow.find('.Injury').text('');
				otherRow.find('.NoInitial').text(isTrue(WS.state[prefix+'ScoringTrip(1).AfterSP'])?'X':'');
				spRow.find('.JamNumber').text(isTrue(WS.state[prefix+'StarPass'])?'SP':'SP*');
				spRow.find('.Jammer').text(isTrue(WS.state[prefix+'StarPass']) ? WS.state[prefix+'Fielding(Pivot).SkaterNumber'] : '');
				break;

			 case 'ScoringTrip(1).Score': case 'ScoringTrip(2).Score':
			 case 'ScoringTrip(2).AfterSP': case 'ScoringTrip(2).Current':
				var trip1Score = WS.state[prefix+'ScoringTrip(1).Score'];
				var trip2Score = WS.state[prefix+'ScoringTrip(2).Score'];
				var trip2Current = isTrue(WS.state[prefix+'ScoringTrip(2).Current']);
				var scoreText = '';
				if (trip2Score == 0 && trip2Current) {
					trip2Score = '.';
				}
				if (trip1Score > 0) {
					if (trip2Score == null) {
						scoreText = trip1Score + ' + NI';
					} else {
						scoreText = trip1Score + ' + ' + trip2Score;
					}
				} else if (trip2Score != null) {
					scoreText = trip2Score; 
				}
				var row = jamRow;
				var otherRow = spRow;
				if (isTrue(WS.state[prefix+'ScoringTrip(2).AfterSP'])) {
					row = spRow;
					otherRow = jamRow;
				}
				row.find('.Trip2').text(scoreText);
				otherRow.find('.Trip2').text('');
				break;

			 default:
				if (k.parts[4] == 'ScoringTrip' && k.ScoringTrip >= 3 && k.ScoringTrip < 10) {
					var t = k.ScoringTrip;
					var row = jamRow;
					var otherRow = spRow;
					if (isTrue(WS.state[prefix+'ScoringTrip('+t+').AfterSP'])) {
						row = spRow;
						otherRow = jamRow;
					}
					var score = WS.state[prefix+'ScoringTrip('+t+').Score'];
					var current = isTrue(WS.state[prefix+'ScoringTrip('+t+').Current']);
					row.find('.Trip'+t).text(score == null ? '' : current && score == 0 ? '.' : score);
					otherRow.find('.Trip'+t).text('');
				} else if (k.parts[4] == 'ScoringTrip' && k.ScoringTrip >= 10) {
					var scoreBeforeSP = '';
					var scoreAfterSP = '';
					var t = 10;
					while (true) {
						var tripScore = WS.state[prefix+'ScoringTrip('+t+').Score'];
						if (tripScore == null) break;
						if (isTrue(WS.state[prefix+'ScoringTrip('+t+').AfterSP'])) {
							scoreAfterSP = scoreAfterSP=='' ? tripScore : scoreAfterSP + " + " + tripScore;
						} else {
							scoreBeforeSP = scoreBeforeSP=='' ? tripScore : scoreBeforeSP + " + " + tripScore;
						}
						t++;
					}
					jamRow.find('.Trip10').text(scoreBeforeSP);
					spRow.find('.Trip10').text(scoreAfterSP);
				}

		}
	}

	function createPeriod(nr) {
		if (nr > 0 && periodElements[nr] == null) {
			createPeriod(nr-1);
			var table = $('<table cellpadding="0" cellspacing="0" border="1">')
				.addClass('SK Period').attr('nr', nr);
			if (mode == 'operator') {
				table.prependTo(element);
			} else {
				table.appendTo(element);
			}
			if (mode != 'operator') {
				var header = $('<thead><tr>').appendTo(table);
				$('<td>').addClass('JamNumber').text('JAM').appendTo(header);
				$('<td>').addClass('Jammer').text('JAMMER').appendTo(header);
				$('<td>').addClass('SmallHead').append($('<div>').text('LOST')).appendTo(header);
				$('<td>').addClass('SmallHead').append($('<div>').text('LEAD')).appendTo(header);
				$('<td>').addClass('SmallHead').append($('<div>').text('CALL')).appendTo(header);
				$('<td>').addClass('SmallHead').append($('<div>').text('INJ')).appendTo(header);
				$('<td>').addClass('SmallHead').append($('<div>').text('NI')).appendTo(header);
				$('<td>').html('<span class ="Team">' + teamName + '</span> P' + nr)
					.attr('colspan', 9).prop('id','head').appendTo(header);
				if (mode != 'copyToStatsbook') {
					$('<td>').addClass('JamTotal').text('JAM').appendTo(header);
					$('<td>').addClass('GameTotal').text('TOTAL').appendTo(header);
				}
			}
			var body = $('<tbody>').appendTo(table);
			periodElements[nr] = body;
			jamElements[nr] = {};
		}
	}


	function createJam(p, nr) {
		var table = periodElements[p];
		if (nr > 0 && jamElements[p][nr] == null) {
			createJam(p, nr-1);

			var prefix = 'ScoreBoard.Period('+p+').Jam('+nr+').TeamJam('+teamId+').';

			var jamRow = $('<tr>').addClass('Jam').attr('nr', nr);
			$('<td>').addClass('JamNumber Darker').text(nr).appendTo(jamRow);
			$('<td>').addClass('Jammer').appendTo(jamRow);
			$('<td>').addClass('Lost Narrow Darker').click(function() { WS.Set(prefix+'Lost', $(this).text() == ""); }).appendTo(jamRow);
			$('<td>').addClass('Lead Narrow Darker').click(function() { WS.Set(prefix+'Lead', $(this).text() == ""); }).appendTo(jamRow);
			$('<td>').addClass('Calloff Narrow Darker').click(function() { WS.Set(prefix+'Calloff', $(this).text() == ""); }).appendTo(jamRow);
			$('<td>').addClass('Injury Narrow Darker').click(function() { WS.Set(prefix+'Injury', $(this).text() == ""); }).appendTo(jamRow);
			$('<td>').addClass('NoInitial Narrow Darker').click(function() { setupTripEditor(p, nr, teamId, 1); }).appendTo(jamRow);
			$.each(new Array(9), function (idx) {
				var t = idx + 2;
				$('<td>').addClass('Trip Trip'+t).click(function() { setupTripEditor(p, nr, teamId, t); }).appendTo(jamRow);
			});
			if (mode != 'copyToStatsbook') {
				$('<td>').addClass('JamTotal').appendTo(jamRow);
				$('<td>').addClass('GameTotal').appendTo(jamRow);
			}

			var spRow = jamRow.clone(true).removeClass('Jam').addClass('SP Hide');
			if (mode == 'operator') {
				table.prepend(jamRow).prepend(spRow);
			} else {
				table.append(jamRow).append(spRow);
			}
			jamElements[p][nr] = [jamRow, spRow];

		}
	}

}

var tripEditor;

function setupTripEditor(p, j, teamId, t) {
	var prefix = 'ScoreBoard.Period('+p+').Jam('+j+').TeamJam('+teamId+').ScoringTrip('+t+').';

	tripEditor.dialog('option', 'title', 'Period ' + p + ' Jam ' + j + ' Trip ' + (t==1?'Initial':t));
	var scoreField = tripEditor.find('#score').val(WS.state[prefix+'Score']);
	var afterSPField = tripEditor.find('#afterSP').prop('checked', isTrue(WS.state[prefix+'AfterSP']));
	tripEditor.find('#submit, #remove').unbind('click');
	tripEditor.find('#submit').click(function() {
		if (scoreField.val() == "") {
			WS.Set(prefix+'Remove', true);
		} else {
			WS.Set(prefix+'Score', scoreField.val());
			WS.Set(prefix+'AfterSP', afterSPField.prop('checked'));
		}
		tripEditor.dialog('close');
	});
	tripEditor.find('#remove').click(function() {
		WS.Set(prefix+'Remove', true);
		tripEditor.dialog('close');
	});
	tripEditor.find(":input").keydown(function(event) { if (event.which == 13) tripEditor.find('#submit').click(); });

	tripEditor.dialog('open');
}

function prepareTripEditor() {

	'use strict';
	$(initialize);

	function initialize() {

		tripEditor = $('#TripEditor').dialog({
			modal: true,
			closeOnEscape: false,
			title: 'Trip Editor',
			autoOpen: false,
			width: '300px',
		});

		tripEditor.append($('<table>')
				.append($('<tr>')
						.append($('<td colspan="2">')
								.append($('<input type="number" min="0">').attr('id', 'score'))
								.append($('<input type="checkbox">').attr('id', 'afterSP'))
								.append($('<span>').addClass('Infotext').text('SP in this or prior trip'))))
				.append($('<tr>')
						.append($('<td width="50%">').append($('<button>').attr('id','submit').text('Submit')))
						.append($('<td width="50%">').append($('<button>').attr('id','remove').text('Remove')))));
	}
}

//# sourceURL=controls\sk\sk-sheet.js
