$(function () {
  'use strict';
  var rulesetId = _windowFunctions.getParam('ruleset');
  createRulesetsTab($('#RulesetsTab'), rulesetId, false);

  WS.Register('ScoreBoard.Rulesets.Ruleset(' + rulesetId + ').Name', function (k, v) {
    if (v == null) {
      window.close();
    } else {
      document.title = v + ' | Edit Ruleset | CRG ScoreBoard';
    }
  });

  WS.AutoRegister();
  WS.Connect();
});
