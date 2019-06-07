(function () {
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt-input.css" type="text/css" />');


	preparePltInputTable($('#pt1'), '1', 'pt', null, 'scoreboard');
	preparePltInputTable($('#pt2'), '2', 'pt', null, 'scoreboard');

	WS.AutoRegister();
	WS.Connect();
})();
