(function () {
  'use strict';
  var gameId = _windowFunctions.getParam('game');

  setupGameAdvance($('#gameAdvance'), gameId, false);
  preparePltInputTable($('#pt1'), gameId, '1', 'pt');
  preparePltInputTable($('#pt2'), gameId, '2', 'pt');

  preparePenaltyEditor(gameId);

  prepareOptionsDialog(gameId, '', true);
  _windowFunctions.configureZoom();

  WS.AutoRegister();
  WS.Connect();
})();
