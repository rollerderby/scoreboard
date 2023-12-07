$(function () {
  'use strict';
  WS.Register('ScoreBoard.PreparedTeam(' + _windowFunctions.getParam('team') + ').Name', function (k, v) {
    if (v == null) {
      window.close();
    } else {
      document.title = v + ' | Edit Team | CRG ScoreBoard';
    }
  });

  WS.Connect();
  WS.AutoRegister();
});
