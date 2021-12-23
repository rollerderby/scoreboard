(function () {
  var teamId = _windowFunctions.getParam('team');
  var gameId = _windowFunctions.getParam('game');

  setupGameAdvance($('#gameAdvance'), gameId, false);

  preparePltInputTable($('#input'), gameId, teamId, 'lt');
  
  prepareLtSheetTable($('#sheet'), gameId, teamId, 'plt');
  
  prepareAnnotationEditor($('#AnnotationEditor'), gameId, teamId);

  prepareFieldingEditor($('#FieldingEditor'), gameId, teamId);

  prepareOptionsDialog(gameId, teamId);
  _windowFunctions.configureZoom();
  
  WS.AutoRegister();
  WS.Connect();

  if (!teamId) { openOptionsDialog(); }
})();
