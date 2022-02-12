(function () {
  'use strict';
  var teamId = _windowFunctions.getParam('team');
  var gameId = _windowFunctions.getParam('game');

  setupGameAdvance($('#gameAdvance'), gameId, false);

  preparePltInputTable($('#input'), gameId, teamId, 'lt');

  prepareLtSheetTable($('#sheet'), gameId, teamId, 'plt');

  prepareAnnotationEditor($('#AnnotationEditor'), gameId, teamId);

  prepareFieldingEditor($('#FieldingEditor'), gameId, teamId);

  prepareOptionsDialog(gameId, teamId);
  _windowFunctions.configureZoom();

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

      document.title = 'LT ' + teamName + ' | CRG ScoreBoard';
    }
  );

  WS.AutoRegister();
  WS.Connect();

  if (!teamId) {
    openOptionsDialog();
  }
})();
