$(function () {
  'use strict';
  var gameId = _windowFunctions.getParam('game');
  createIgrfTab(createTab('IGRF', 'IgrfTab'), gameId);
  createTeamsTab(createTab('Teams', 'TeamsTab'), gameId);
  createRulesetsTab(createTab('Rules', 'RulesetsTab'), gameId, true);
  createSheetsTab(createTab('Sheets', 'SheetsTab'), gameId);

  WS.Register('ScoreBoard.Game(' + gameId + ').Name', function (k, v) {
    if (v == null) {
      window.close();
    } else {
      document.title = v + ' | Edit Game | CRG ScoreBoard';
    }
  });

  // Only connect after any registrations from the above are in place.
  // This avoids repeating work on the initial load.
  WS.AutoRegister();
  WS.Connect();

  $('#tabsDiv').tabs();
});

function createTab(title, tabId) {
  'use strict';
  if (typeof title === 'string') {
    title = $('<a>').html(title);
  }
  $('<li>')
    .append(title.attr('href', '#' + tabId))
    .appendTo('#tabsDiv>ul');
  return $('<div>').attr('id', tabId).addClass('TabContent').appendTo('#tabsDiv');
}
