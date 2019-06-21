(function () {
	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'lt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);

	WS.AutoRegister();
	WS.Connect();

})();
