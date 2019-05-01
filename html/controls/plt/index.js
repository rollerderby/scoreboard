(function () {
	$('head').append('<link rel="stylesheet" href="plt-input.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="../lt/lt-sheet.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'plt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);
})();
