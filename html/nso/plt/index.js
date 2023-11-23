(function () {
  'use strict';
  _windowFunctions.configureZoom();

  var teamId = _windowFunctions.getParam('team');
  var gameId = _windowFunctions.getParam('game');

  if (gameId && teamId) {
    WS.Register(
      [
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Name',
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor',
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').AlternateName(operator)',
      ],
      function () {
        var teamName = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Name'];

        if (WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor'] != null) {
          teamName = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor'];
        }

        if (WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').AlternateName(operator)'] != null) {
          teamName = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').AlternateName(operator)'];
        }

        document.title = 'PLT ' + teamName + ' | CRG ScoreBoard';
      }
    );
  }

  WS.Connect();
  WS.AutoRegister();
})();
