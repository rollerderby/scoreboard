$(function () {
  'use strict';
  var rulesetId = _windowFunctions.getParam('ruleset');
  createRulesetsTab($('#RulesetsTab'), rulesetId, false);

  WS.AutoRegister();
  WS.Connect();
});
//# sourceURL=settings\rulesets\index.js
