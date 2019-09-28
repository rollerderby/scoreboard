(function () {
	var teamId = _windowFunctions.getParam("team");
	

	preparePltInputTable($('#input'), teamId, 'plt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareAnnotationEditor(teamId);
	
	prepareFieldingEditor(teamId);

	prepareOptionsDialog(teamId);
	
	WS.AutoRegister();
	WS.Connect();
	
	if (!teamId) { openOptionsDialog(); }
})();
