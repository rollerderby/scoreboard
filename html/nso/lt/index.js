(function () {
	var teamId = _windowFunctions.getParam('team');

	preparePltInputTable($('#input'), teamId, 'lt');
	
	prepareLtSheetTable($('#sheet'), teamId, 'plt');
	
	prepareAnnotationEditor(teamId);

	prepareFieldingEditor(teamId);

	prepareOptionsDialog(teamId);
	_windowFunctions.configureZoom();
	
	WS.AutoRegister();
	WS.Connect();

	if (!teamId) { openOptionsDialog(); }
})();
