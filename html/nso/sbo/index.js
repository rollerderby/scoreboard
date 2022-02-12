$(function () {
  'use strict';
  var gameId = _windowFunctions.getParam('game');
  setupGameAdvance($('#gameAdvance'), gameId, false);
  createTeamTimeTab(createTab('Controls', 'TeamTimeTab'), gameId);
  createTeamsTab(createTab('Teams', 'TeamsTab'), gameId);
  createRulesetsTab(createTab('Rules', 'RulesetsTab'), gameId, true);
  createIgrfTab(createTab('IGRF', 'IgrfTab'), gameId);
  createSheetsTab(createTab('Sheets', 'SheetsTab'), gameId);
  createScoreBoardSettingsTab(createTab('Settings', 'ScoreBoardSettingsTab'));
  WS.Register('ScoreBoard.Settings.Setting(ScoreBoard.*)', function (k, v) {
    setOperatorSettings(_windowFunctions.getParam('operator'));
  });
  // Only connect after any registrations from the above are in place.
  // This avoids repeating work on the initial load.
  WS.AutoRegister();
  WS.Connect();

  $('#tabsDiv').tabs();

  // FIXME - is there better way to avoid key controls when a dialog is visible?
  _crgKeyControls.addCondition(function () {
    return !$('body>div.ui-dialog').is(':visible');
  });
  // FIXME - maybe use something else to check if user is typing into a text input...
  // FIXME - also provide visual feedback that key-control is disabled while typing into input text box?
  _crgKeyControls.addCondition(function () {
    return !$('#TeamTime input:text.Editing').length;
  });

  $('<li>').text('Caps Lock is On').attr('id', 'capsLockWarning').addClass('Hidden').appendTo('#tabBar');
  $(document).on('keydown', function (e) {
    if (e.originalEvent.key === 'CapsLock') {
      // Assume it'll be toggled. Different OSes actually change
      // the setting at different stages of the keypress, so
      // this is the best we can do. If it is wrong, it'll be
      // fixed at the next non-Caps Lock keypress.
      $('#capsLockWarning').toggleClass('Hidden');
    } else {
      $('#capsLockWarning').toggleClass('Hidden', !e.originalEvent.getModifierState('CapsLock'));
    }
  });
  $('<button>').text('Logout').on('click', logout).button().css('float', 'right').appendTo('#tabBar');
});

function setOperatorSettings(op) {
  'use strict';
  var opPrefix = 'ScoreBoard.Settings.Setting(ScoreBoard.Operator__' + op + '.';
  // Default settings are intentionally separate from settings of the default operator
  // This ensures users logging in for the first time always get the former and not whatever
  // the latter currently happens to be.
  var defPrefix = 'ScoreBoard.Settings.Setting(ScoreBoard.Operator_Default.';
  setClockControls(isTrue(WS.state[opPrefix + 'StartStopButtons)'] || WS.state[defPrefix + 'StartStopButtons)']));
  setReplaceButton(isTrue(WS.state[opPrefix + 'ReplaceButton)'] || WS.state[defPrefix + 'ReplaceButton)']));
  setTabBar(isTrue(WS.state[opPrefix + 'TabBar)'] || WS.state[defPrefix + 'TabBar)']));
}

// FIXME - this is done after the team/time panel is loaded,
//         as the button setup needs to happen after that panel creates its buttons...
//         really, the keycontrol helper lib needs to have a per-tab interface so
//         each tab can setup its own keycontrol.
function initialLogin() {
  'use strict';
  var operator = _windowFunctions.getParam('operator');
  if (operator) {
    login(operator);
  } else {
    logout();
  }
}

function login(name) {
  'use strict';
  var gameId = _windowFunctions.getParam('game');
  $('#operatorId').text(name);
  if (window.history.replaceState) {
    window.history.replaceState(null, '', '?operator=' + $('#operatorId').text() + '&game=' + gameId);
  }
  _crgKeyControls.setupKeyControls(name);
  setOperatorSettings(name);
}

function logout() {
  'use strict';
  var gameId = _windowFunctions.getParam('game');
  $('#operatorId').text('');
  if (window.history.replaceState) {
    window.history.replaceState(null, '', '?game=' + gameId);
  }
  _crgKeyControls.destroyKeyControls();
  setOperatorSettings('');
  _crgUtils.showLoginDialog('Operator Login', 'Operator:', 'Login', function (value) {
    if (!value) {
      return false;
    }
    login(value);
    return true;
  });
}

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
