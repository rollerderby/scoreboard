$(function () {
  'use strict';
  var teamId = _windowFunctions.getParam('team');
  createTeamsTab($('#TeamsTab'), null, teamId);

  WS.Register('ScoreBoard.PreparedTeam(' + teamId + ').Name', function (k, v) {
    document.title = v + ' | Edit Team | CRG ScoreBoard';
  });

  WS.AutoRegister();
  WS.Connect();
});
