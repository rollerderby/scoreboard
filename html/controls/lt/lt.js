(function () {

	'use strict';
	$(initialize);

	var teamId = _windowFunctions.getParam("team");
	var mode = sheet;
	if (_windowFunctions.hasParam("mode")) {
		mode = _windowFunctions.getParam("mode");
	};
	var teamName = "";
	var fieldingEditor;

	function initialize() {

		WS.Connect();
		WS.AutoRegister();

		if (mode != 'plt') {
			WS.Register(['ScoreBoard.Team(' + teamId + ').Name'], function () { teamNameUpdate(); });
			WS.Register(['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name'], function () { teamNameUpdate(); });
	
			WS.Register(['ScoreBoard.Team(' + teamId + ').Color'], function (k, v) {
				$('#head').css('background-color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_bg)']);
				$('#head').css('color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_fg)']);
			});
		}
		
		WS.Register(['ScoreBoard.CurrentPeriodNumber'], function(k, v) { createPeriod(v); });
		WS.Register(['ScoreBoard.Team('+teamId+').Skater'], function(k,v) { processSkater(k,v); })
		WS.Register(['ScoreBoard.Team('+teamId+').BoxTrip'], function(k,v) { processBoxTrip(k,v); })

		fieldingEditor = $('div.FieldingEditor').dialog({
			modal: true,
			closeOnEscape: false,
			title: 'Fielding Editor',
			autoOpen: false,
			width: '600px',
		});
	}
	
	function teamNameUpdate() {
		teamName = WS.state['ScoreBoard.Team(' + teamId + ').Name'];

		if (WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name'] != null) {
			teamName = WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator).Name']
		}

		$('#head .Team').text(teamName);
	}
	
	function createPeriod(nr) {
		if (nr > 0 && $('#lt-tables').find('table.Period[nr='+nr+']').length == 0) {
			createPeriod(nr-1);
			var table = $('<table cellpadding="0" cellspacing="0" border="1">')
				.addClass('Period').attr('nr', nr);
			if (mode == 'plt') {
				table.prependTo($('#lt-tables'));
			} else {
				table.appendTo($('#lt-tables'));
			}
			if (mode != 'plt') {
				$('<div>').html('<span class ="Team">' + teamName + '</span> P' + nr)
					.prop('id','head').insertBefore(table);
				var header = $('<thead>').appendTo(table);
				var row = $('<tr>').appendTo(header);
				$('<td>').addClass('JamNumber').text('Jam').appendTo(row);
				$('<td>').addClass('NP').text('NP').appendTo(row);
				$('<td>').addClass('Skater').text('Jammer').appendTo(row);
				$('<td>').addClass('Box').text('Box').appendTo(row);
				$('<td>').addClass('Skater').text('Pivot').appendTo(row);
				$('<td>').addClass('Box').text('Box').appendTo(row);
				$('<td>').addClass('Skater').text('Blocker').appendTo(row);
				$('<td>').addClass('Box').text('Box').appendTo(row);
				$('<td>').addClass('Skater').text('Blocker').appendTo(row);
				$('<td>').addClass('Box').text('Box').appendTo(row);
				$('<td>').addClass('Skater').text('Blocker').appendTo(row);
				$('<td>').addClass('Box').text('Box').appendTo(row);
			}
			var body = $('<tbody>').appendTo(table);
			
			WS.Register(['ScoreBoard.Period('+nr+').CurrentJamNumber'], function(k, v) { createJam(nr, v); });
			
			$('#loading').addClass('Hide');
		}
	}
	
	function createJam(p, nr) {
		var table = $('#lt-tables').find('table.Period[nr='+p+']').find('tbody');
		if (table.find('tr.Jam[nr='+nr+']').length == 0) {
			if (nr > 1 && table.find('tr.SP[nr='+(nr-1)+']').length == 0) {	createJam(p, nr-1); }

			var prefix = 'ScoreBoard.Period('+p+').Jam('+nr+').TeamJam('+teamId+').';

			var jamRow = $('<tr>').addClass('Jam').attr('nr', nr);
			$('<td>').addClass('JamNumber Darker').text(nr).appendTo(jamRow);
			$('<td>').addClass('NP Darker').click(function() { WS.Set(prefix+'NoPivot', $(this).text() == ""); }).appendTo(jamRow);
			$.each( [ "Jammer", "Pivot", "Blocker1", "Blocker2", "Blocker3" ], function() {
				var pos = String(this);
				$('<td>').addClass('Skater '+pos).click(function() { setupFieldingEditor(p, nr, pos); }).appendTo(jamRow);
				$('<td>').addClass('Box Box'+pos).click(function() { setupFieldingEditor(p, nr, pos); }).appendTo(jamRow);
			});

			var spRow = jamRow.clone().removeClass('Jam').addClass('SP Hide');
			spRow.find('.Jammer').insertAfter(spRow.find('.BoxPivot'));
			spRow.find('.BoxJammer').insertAfter(spRow.find('.Jammer'));
			WS.Register(['ScoreBoard.Period('+p+').Jam('+nr+').StarPass'], function(k, v) { spRow.toggleClass('Hide', !isTrue(v)); });

			WS.Register([prefix+'StarPass'], function(k, v) {
				spRow.find('.JamNumber').text(isTrue(v)?'SP':'SP*');
				spRow.find('.NP').text(isTrue(v)?'X':'');
				$.each( [ "Jammer", "Pivot", "Blocker1", "Blocker2", "Blocker3" ], function() {
					var pos = String(this);
					spRow.find('.'+pos).text(isTrue(v)?WS.state[prefix+'Fielding('+pos+').SkaterNumber']:'');
					spRow.find('.Box'+pos).text(isTrue(v)?WS.state[prefix+'Fielding('+pos+').BoxTripSymbolsAfterSP']:'');
				});
			});
			$.each( [ "Jammer", "Pivot", "Blocker1", "Blocker2", "Blocker3" ], function() {
				var pos = String(this);
				WS.Register([prefix+'Fielding('+pos+').SkaterNumber'], function (k, v) {
					jamRow.find('.'+pos).text(v);
					if (isTrue(WS.state[prefix+'StarPass'])) { spRow.find('.'+pos).text(v); }
				});
				WS.Register([prefix+'Fielding('+pos+').BoxTripSymbolsBeforeSP'], function (k, v) {
					jamRow.find('.Box'+pos).text(v);
				});
				WS.Register([prefix+'Fielding('+pos+').BoxTripSymbolsAfterSP'], function (k, v) {
					if (isTrue(WS.state[prefix+'StarPass'])) { spRow.find('.Box'+pos).text(v); }
				});
			});
			
			if (mode=='plt') {
				table.prepend(jamRow).prepend(spRow);
			} else {
				table.append(jamRow).append(spRow);
			}
		}
	}
	
	function setupFieldingEditor(p, j, pos) {
		var prefix = 'ScoreBoard.Period('+p+').Jam('+j+').TeamJam('+teamId+').Fielding('+pos+').';
		WS.Register([prefix+'Skater', prefix+'NotFielded', prefix+'SitFor3']);
		
		fieldingEditor.dialog('option', 'title', 'Period ' + p + ' Jam ' + j + ' ' + (pos));
		fieldingEditor.find('.FieldingEditor #skater').val(WS.state[prefix+'Skater']).change(function() {
			WS.Set(prefix+'Skater', $(this).val());
			$(this).val(WS.state[prefix+'Skater']);
		});
		fieldingEditor.find('.FieldingEditor #notFielded').prop('checked', isTrue(WS.state[prefix+'NotFielded'])).click(function() {
			WS.Set(prefix+'NotFielded', $(this).prop('checked'));
			$(this).prop('checked', WS.state[prefix+'NotFielded']);
		});
		fieldingEditor.find('.FieldingEditor #sitFor3').prop('checked', isTrue(WS.state[prefix+'SitFor3'])).click(function() {
			WS.Set(prefix+'SitFor3', $(this).prop('checked'));
			$(this).prop('checked', WS.state[prefix+'SitFor3']);
		});
		fieldingEditor.find('.FieldingEditor .BoxTrip').addClass('Hide');
		fieldingEditor.find('.FieldingEditor .'+WS.state[prefix+'Id']).removeClass('Hide');
		fieldingEditor.find('.FieldingEditor #addTrip').click(function() {
			WS.Set(prefix+'AddBoxTrip', true);
		});
		fieldingEditor.find('#close').click(function() {
			fieldingEditor.dialog('close');
		});
		
		fieldingEditor.dialog('open');
	}
	
	function processSkater(k, v) {
		var match = (k || "").match(/Skater\(([^\)]+)\)\.Number/);
		if (match == null || match.length == 0)
			return;

		var id = match[1];
		console.log('k: ' + k);
		console.log('match: '+ match[0] + ", " + match[1]);
		var option = $(".FieldingEditor #skater option[value='"+id+"']")
		if (v != null && option.length == 0) {
			$('<option>').attr('value', id).text(v).appendTo($('.FieldingEditor #skater'));
		} else if (v == null) {
			option.remove();
		} else {
			option.text(v);
		}
	}
	
	function processBoxTrip(k, v) {
		var match = (k || "").match(/BoxTrip\(([^\)]+)\)\.([^\(]+)/);
		if (match == null || match.length == 0)
			return;

		var id = match[1];
		var key = match[2];
		console.log('k: ' + k);
		console.log('match: '+ match[0] + ", " + match[1] + ", " + match[2]);
		var prefix = 'ScoreBoard.Team('+teamId+').BoxTrip('+id+').';
		
		var row = $('.FieldingEditor .BoxTrip[id='+id+']');
		if (v != null && row.length == 0) {
			row = $('<tr>').addClass('BoxTrip').attr('id', id).insertBefore('.FieldingEditor #tripFooter');
			$('<td>').append($('<button>').addClass('tripModify').text('-').click(function() {
				WS.Set(prefix+'StartEarlier', true);
			})).append($('<span>').addClass('tripStartText'))
			.append($('<button>').addClass('tripModify').text('+').click(function() {
				WS.Set(prefix+'StartLater', true);
			})).appendTo(row);
			$('<td>').append($('<button>').addClass('tripModify').text('-').click(function() {
				WS.Set(prefix+'EndEarlier', true);
			})).append($('<span>').addClass('tripEndText').text('ongoing'))
			.append($('<button>').addClass('tripModify').text('+').click(function() {
				WS.Set(prefix+'EndLater', true);
			})).appendTo(row);
			$('<td>').append($('<button>').addClass('tripRemove').text('Remove').click(function() {
				WS.Set(prefix+'Delete', true);
			})).appendTo(row);
		}
		if (v == null) {
			if (key == 'Id') {
				row.remove();
			}
			if (key == 'Fielding') {
				row.removeClass(v);
			}
			return;
		}
		if (['StartJamNumber', 'StartBetweenJams', 'StartAfterSP'].includes(key)) {
			var between = isTrue(WS.state[prefix+'StartBetweenJams']);
			var afterSP = isTrue(WS.state[prefix+'StartAfterSP']);
			row.find('.tripStartText').text((between?'Before ':'') + 'Jam ' + WS.state[prefix+'StartJamNumber']
					+ (afterSP?' after SP':''));
		}
		if (['EndJamNumber', 'EndBetweenJams', 'EndAfterSP'].includes(key)) {
			var between = isTrue(WS.state[prefix+'EndBetweenJams']);
			var afterSP = isTrue(WS.state[prefix+'EndAfterSP']);
			row.find('.tripEndText').text((between?'Before ':'') + 'Jam ' + WS.state[prefix+'EndJamNumber']
					+ (afterSP?' after SP':''));
		}
		if (key == 'Fielding') {
			row.addClass(v);
		}
	}
})();
