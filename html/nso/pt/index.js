(function () {
  preparePltInputTable($('#pt1'), '1', 'pt');
  preparePltInputTable($('#pt2'), '2', 'pt');
  
  preparePenaltyEditor();

  prepareOptionsDialog('', true);
  _windowFunctions.configureZoom();

  WS.AutoRegister();
  WS.Connect();

})();
