$(function() {
  var teamId = _windowFunctions.getParam('team');
  createTeamsTab($('#TeamsTab'), null, teamId);

  WS.AutoRegister();
  WS.Connect();
});
//# sourceURL=settings\teams\index.js
