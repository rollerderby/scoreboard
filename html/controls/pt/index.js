(function () {
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt-input.css" type="text/css" />');

	preparePltInputTable($('#pt1'), '1', 'pt');
	preparePltInputTable($('#pt2'), '2', 'pt');
	
	preparePenaltyEditor();

	WS.AutoRegister();
	WS.Connect();

})();
