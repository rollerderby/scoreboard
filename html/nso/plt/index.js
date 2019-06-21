(function () {
	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'plt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);

	WS.AutoRegister();
	WS.Connect();
})();
