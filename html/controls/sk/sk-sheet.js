function prepareSkSheetTable(element, teamId, mode) {

	'use strict';
	$(initialize);

	var teamName = "";

	function initialize() {
		if (mode != 'operator') {
			WS.Register(['ScoreBoard.Team(' + teamId + ').Name'], function () { teamNameUpdate(); });
			WS.Register(['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name'], function () { teamNameUpdate(); });
	
			WS.Register(['ScoreBoard.Team(' + teamId + ').Color'], function (k, v) {
				element.find('#head').css('background-color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_bg).Color']);
				element.find('#head').css('color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_fg).Color']);
			});
		}
		
		WS.Register(['ScoreBoard.CurrentPeriodNumber'], function(k, v) { createPeriod(v); });
	}
	
	function teamNameUpdate() {
		teamName = WS.state['ScoreBoard.Team(' + teamId + ').Name'];

		if (WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name'] != null) {
			teamName = WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name']
		}

		element.find('#head .Team').text(teamName);
	}
	
	function createPeriod(nr) {
		if (nr > 0 && element.find('table.Period[nr='+nr+']').length == 0) {
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
			
			WS.Register(['ScoreBoard.Period('+nr+').CurrentJamNumber'], function(k, v) { createJam(nr, v); });
			WS.Register(['ScoreBoard.Period('+nr+').Duration'], function(k,v) { if (v > 0) finalizePeriod(nr); });
			WS.Register(['ScoreBoard.Period('+nr+').Number'], function(k,v) { if (v == null) table.remove(); });
		}
	}
	
	function createJam(p, nr) {
		var table = element.find('table.Period[nr='+p+']').find('tbody');
		if (table.find('tr.Jam[nr='+nr+']').length == 0) {
			if (nr > 1 && table.find('tr.SP[nr='+(nr-1)+']').length == 0) {	createJam(p, nr-1); }

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
			WS.Register(['ScoreBoard.Period('+p+').Jam('+nr+').StarPass'], function(k, v) { spRow.toggleClass('Hide', !isTrue(v)); });

			WS.Register([prefix+'Id'], function (k, v) { if (v == null) { jamRow.remove(); spRow.remove(); }});
			if (WS.state[prefix+'Id'] == null) { return; }

			WS.Register([prefix+'Fielding(Jammer).SkaterNumber'], function (k, v) {	jamRow.find('.Jammer').text(v);	});
			WS.Register([prefix+'Lost'], function(k, v) { jamRow.find('.Lost').text(isTrue(v)?'X':'')});
			WS.Register([prefix+'Lead'], function(k, v) { jamRow.find('.Lead').text(isTrue(v)?'X':'')});
			WS.Register([prefix+'Calloff', prefix+'Injury', prefix+'NoInitial', prefix+'StarPass',
					prefix+'ScoringTrip(1).AfterSP', prefix+'Fielding(Pivot).SkaterNumber'], function(k, v) {
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
			});
			WS.Register([prefix+'ScoringTrip(1).Score', prefix+'ScoringTrip(2).Score',
					prefix+'ScoringTrip(2).AfterSP', prefix+'ScoringTrip(2).Current'], function(k, v) {
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
			});
			$.each(new Array(7), function (idx) {
				var t = idx + 3;
				WS.Register([prefix+'ScoringTrip('+t+').Score', prefix+'ScoringTrip('+t+').Current', 
						prefix+'ScoringTrip('+t+').AfterSP'], function (k, v) {
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
				})
			});
			WS.Register([prefix+'ScoringTrip(10).Score', prefix+'ScoringTrip(10).AfterSP', 
						prefix+'ScoringTrip(11).Score', prefix+'ScoringTrip(11).AfterSP', 
						prefix+'ScoringTrip(12).Score', prefix+'ScoringTrip(12).AfterSP'], function (k, v) {
				var scoreBeforeSP = '';
				var scoreAfterSP = '';
				$.each(new Array(3), function(idx) {
					var t = idx + 10;
					var tripScore = WS.state[prefix+'ScoringTrip('+t+').Score'];
					if (tripScore != null) {
						if (isTrue(WS.state[prefix+'ScoringTrip('+t+').AfterSP'])) {
							scoreAfterSP = scoreAfterSP=='' ? tripScore : scoreAfterSP + "+" + tripScore;
						} else {
							scoreBeforeSP = scoreBeforeSP=='' ? tripScore : scoreBeforeSP + "+" + tripScore;
						}
					}
				});
				jamRow.find('Trip10').text(scoreBeforeSP);
				spRow.find('Trip10').text(scoreAfterSP);
			});
			if (mode != 'copyToStatsbook') {
				WS.Register([prefix+'JamScore', prefix+'AfterSPScore'], function(k, v) {
					jamRow.find('.JamTotal').text(WS.state[prefix+'JamScore'] - WS.state[prefix+'AfterSPScore']);
				});
				WS.Register([prefix+'AfterSPScore'], function(k, v) { spRow.find('.JamTotal').text(v)});
				WS.Register([prefix+'TotalScore', prefix+'AfterSPScore'], function(k, v) {
					jamRow.find('.GameTotal').text(WS.state[prefix+'TotalScore'] - WS.state[prefix+'AfterSPScore']);
				});
				WS.Register([prefix+'TotalScore'], function(k, v) { spRow.find('.GameTotal').text(v)});
			}
			WS.Register([prefix+'Number'], function(k, v) { if(v == null) { jamRow.remove(); spRow.remove(); }})
			
			if (mode == 'operator') {
				table.prepend(jamRow).prepend(spRow);
			} else {
				table.append(jamRow).append(spRow);
			}
		}
	}
	
	function finalizePeriod(nr) {} //TODO: implement
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
