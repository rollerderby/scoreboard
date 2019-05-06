(function () {
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt-input.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	preparePltInputTable($('#pt1'), '1', 'pt', null, 'scoreboard');
	preparePltInputTable($('#pt2'), '2', 'pt', null, 'scoreboard');
})();
