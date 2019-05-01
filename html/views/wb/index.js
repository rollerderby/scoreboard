(function () {
	$('head').append('<link rel="stylesheet" href="/controls/plt/plt.css" type="text/css" />');

	WS.Connect();
	WS.AutoRegister();

	preparePltTable($('#pt1'), '1', 'pt');
	preparePltTable($('#pt2'), '2', 'pt');
})();
