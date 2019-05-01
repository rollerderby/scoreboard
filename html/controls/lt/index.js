(function () {
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="lt.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	var teamId = _windowFunctions.getParam("team");

	preparePltTable($('#plt'), teamId, 'lt');
	
	prepareLtTable($('#lt'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);
})();
