(function () {
	$('head').append('<link rel="stylesheet" href="/controls/sk/sk.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="/controls/lt/lt.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	prepareSkTable($('#sk1'), 1, 'copyToStatsbook');
	prepareSkTable($('#sk2'), 2, 'copyToStatsbook');

	preparePltTable($('#plt11'), 1, 'copyToStatsbook', 1);
	preparePltTable($('#plt12'), 2, 'copyToStatsbook', 1);
	preparePltTable($('#plt21'), 1, 'copyToStatsbook', 2);
	preparePltTable($('#plt22'), 2, 'copyToStatsbook', 2);
	
	prepareLtTable($('#lt1'), 1, 'copyToStatsbook');
	prepareLtTable($('#lt2'), 2, 'copyToStatsbook');
})();