(function () {
	var teamId = _windowFunctions.getParam("team");

	preparePltInputTable($('#input'), teamId, 'lt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	prepareAnnotationEditor(teamId);

	prepareFieldingEditor(teamId);

	WS.AutoRegister();
	WS.Connect();

})();
