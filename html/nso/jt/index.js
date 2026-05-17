function jtPossiblInjCont(k) {
  return !isTrue(k.upTo('Game') + '.Rule(Jam.InjuryContinuation)') || !isTrue(k.upTo('Game') + '.Team(1).Injury');
}

function jtSettingsUnfit() {
  return WS.state['ScoreBoard.Settings.Setting(ScoreBoard.AutoStart)'] !== 'Timeout' || WS.state['ScoreBoard.Settings.Setting(ScoreBoard.AutoStart5)'] !== 'Jam';
}

function jtOpenSettings() {
  WS.SetupDialog($('#SettingsDialog'), 'ScoreBoard.Settings', {
    title: 'Settings',
    width: '90dvw',
    modal: true,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function jtSetEjtDefaults() {
  WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.AutoStart)', 'Timeout');
  WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.AutoStart5)', 'Jam');
  WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Auto5)', 'true');
  WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.AutoEndJam)', 'false');
  WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.AutoEndTTO)', 'true');
}
