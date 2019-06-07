(function () {
	$('head').append('<link rel="stylesheet" href="plt-input.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="../lt/lt-sheet.css" type="text/css" />');


	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'plt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);

	WS.AutoRegister();
	WS.Connect();
})();
