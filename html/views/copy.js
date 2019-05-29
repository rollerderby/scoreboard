(function () {
	$('head').append('<link rel="stylesheet" href="roster-sheet.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="/controls/sk/sk-sheet.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt-input.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="/controls/lt/lt-sheet.css" type="text/css" />');

	prepareRosterSheetTable($('#roster1'), 1, 'copyToStatsbook');
	prepareRosterSheetTable($('#roster2'), 2, 'copyToStatsbook');

	prepareSkSheetTable($('#sk1'), 1, 'copyToStatsbook');
	prepareSkSheetTable($('#sk2'), 2, 'copyToStatsbook');

	preparePltInputTable($('#plt11'), 1, 'copyToStatsbook', 1);
	preparePltInputTable($('#plt12'), 2, 'copyToStatsbook', 1);
	preparePltInputTable($('#plt21'), 1, 'copyToStatsbook', 2);
	preparePltInputTable($('#plt22'), 2, 'copyToStatsbook', 2);
	
	prepareLtSheetTable($('#lt1'), 1, 'copyToStatsbook');
	prepareLtSheetTable($('#lt2'), 2, 'copyToStatsbook');

	WS.AutoRegister();
	WS.Connect();
})();

//# sourceURL=views\copy\copy.js
