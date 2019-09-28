(function () {
	preparePltInputTable($('#pt1'), '1', 'pt');
	preparePltInputTable($('#pt2'), '2', 'pt');
	
	preparePenaltyEditor();

	WS.AutoRegister();
	WS.Connect();

})();
