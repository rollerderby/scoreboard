(function () {
  'use strict';
  var gameId = _windowFunctions.getParam('game');

  setupGameAdvance($('#gameAdvance'), gameId, true);
  preparePltInputTable($('#pt1'), gameId, '1', 'pt', null, 'whiteboard');
  preparePltInputTable($('#pt2'), gameId, '2', 'pt', null, 'whiteboard');

  WS.AutoRegister();
  WS.Connect();
})();
