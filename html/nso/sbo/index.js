(function () {
  WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Operator.*)', 'ScoreBoard.Settings.Setting(ScoreBoard.Operator_Default.*)']);

  $('#tabsDiv').tabs();

  _crgKeyControls.addCondition(function () {
    return !$('body>div.ui-dialog').is(':visible');
  });
  _crgKeyControls.addCondition(function () {
    return !$('#ControlsTab input:text:focus').length;
  });
  _crgKeyControls.addCondition(function () {
    return !$('#ControlsTab:hidden').length;
  });

  $(document).on('keydown', function (e) {
    if (e.originalEvent.key === 'CapsLock') {
      // Assume it'll be toggled. Different OSes actually change
      // the setting at different stages of the keypress, so
      // this is the best we can do. If it is wrong, it'll be
      // fixed at the next non-Caps Lock keypress.
      $('#capsLockWarning').toggleClass('sbHide');
    } else {
      $('#capsLockWarning').toggleClass('sbHide', !e.originalEvent.getModifierState('CapsLock'));
    }
  });
})();

WS.AfterLoad(function () {
  _login(_windowFunctions.getParam('operator'));
});

function _setOperatorSettings(op) {
  var opPrefix = 'ScoreBoard.Settings.Setting(ScoreBoard.Operator.' + op + '.';
  // Default settings are intentionally separate from settings of the default operator
  // This ensures users logging in for the first time always get the former and not whatever
  // the latter currently happens to be.
  var defPrefix = 'ScoreBoard.Settings.Setting(ScoreBoard.Operator_Default.';
  ['ScoreAdjustments', 'ReplaceButton', 'TabBar'].forEach(function (setting) {
    _opSetOperatorSetting(setting, isTrue(WS.state[opPrefix + setting + ')'] || WS.state[defPrefix + setting + ')']));
  });
}

function _login(name) {
  name = name || '';
  $('#operatorId').text(name);
  _sbUpdateUrl('operator', name);
  _crgKeyControls.setupKeyControls(name);
  _setOperatorSettings(name);
  if (!name) {
    showLoginDialog();
  }
}

function Logout() {
  _login('');
}

function showLoginDialog() {
  WS.SetupDialog($('#loginDialog'), '', {
    modal: true,
    title: 'Operator Login',
    buttons: {
      Login: function () {
        _login($(this).find('input').val() || 'default');
        $(this).dialog('close');
      },
    },
  });
}

function loginOnEnter(k, v, elem, event) {
  if (elem.val() && event.which === 13) {
    _login(elem.val() || 'default');
    sbCloseDialog(k, v, elem, event);
  }
}
