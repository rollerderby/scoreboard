$(function () {
  'use strict';
  WS.Register('ScoreBoard.Rulesets.Ruleset(' + _windowFunctions.getParam('ruleset') + ').Name', function (k, v) {
    if (v == null) {
      window.close();
    } else {
      document.title = v + ' | Edit Ruleset | CRG ScoreBoard';
    }
  });

  WS.Connect();
  WS.AutoRegister();
});
