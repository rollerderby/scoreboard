(function () {
	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'plt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareAnnotationEditor(teamId);
	
	prepareFieldingEditor(teamId);

	WS.AutoRegister();
	WS.Connect();
})();
