(function () {
	$('head').append('<link rel="stylesheet" href="plt.css" type="text/css" />');
	$('head').append('<link rel="stylesheet" href="../lt/lt.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	var teamId = _windowFunctions.getParam("team");

	preparePltTable($('#plt'), teamId, 'plt');
	
	prepareLtTable($('#lt'), teamId, 'plt');
	
	preparePenaltyEditor();
	
	prepareFieldingEditor(teamId);
})();
