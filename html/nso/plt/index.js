(function () {
  var teamId = _windowFunctions.getParam('team');
  var gameId = _windowFunctions.getParam('game');

  setupGameAdvance($('#gameAdvance'), gameId, false);

  preparePltInputTable($('#input'), gameId, teamId, 'plt');
  
  prepareLtSheetTable($('#sheet'), gameId, teamId, 'plt');
  
  preparePenaltyEditor(gameId);
  
  prepareAnnotationEditor(gameId, teamId);
  
  prepareFieldingEditor(gameId, teamId);

  prepareOptionsDialog(gameId, teamId);
  _windowFunctions.configureZoom();

  WS.AutoRegister();
  WS.Connect();
  
  if (!teamId) { openOptionsDialog(); }
})();
