(function () {
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt-input.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="lt-sheet.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'lt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);
})();
