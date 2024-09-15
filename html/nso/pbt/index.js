WS.AfterLoad(function () {
  $('#UsePBTDialog').dialog({
    modal: true,
    closeOnEscape: false,
    title: 'Use Penalty Box Timing',
    buttons: {
      Enable: function () {
        WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UsePBT)', true);
      },
    },
    width: '300px',
    autoOpen: false,
  });
  WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UsePBT)'], function (k, v) {
    $('#UsePBTDialog').dialog(isTrue(v) ? 'close' : 'open');
  });
});

WS.Register('ScoreBoard.CurrentGame.Team(*).Position(*).CurrentFielding');

function toggleSwap() {
  $('#Unassigned').toggleClass('SwappedTeams');
}

function positionDetails(k, v, elem) {
  const prefix = k.upTo('Team');
  const teamName = WS.state[prefix + '.AlternateName(plt)'] || WS.state[prefix + '.UniformColor'] || WS.state[prefix + '.Name'];
  WS.SetupDialog($('#PositionDetails'), k, {
    title: teamName + ' ' + k.Position,
    width: 700,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  });
}

function noUnend(k, v) {
  return isTrue(v) || !WS.state[k.upTo('Position') + '.CurrentBoxSymbols'];
}

function addPenalty(k) {
  const skaterPrefix = k.upTo('Team') + '.Skater(' + WS.state[k + '.Skater'] + ')';
  penSelectCode(skaterPrefix + '.Penalty(' + (WS.state[skaterPrefix + '.PenaltyCount'] + 1) + ')');
}

function removePenalty(k) {
  const skaterPrefix = k.upTo('Team') + '.Skater(' + WS.state[k + '.Skater'] + ')';
  WS.Set(skaterPrefix + '.Penalty(' + WS.state[skaterPrefix + '.PenaltyCount'] + ').Remove', true);
}

function closeDialogIfSkater(k, v, elem) {
  if (!!WS.state[k + '.Skater']) {
    sbCloseDialog(k, v, elem);
  }
}

function closeDialogIfInBox(k, v, elem) {
  if (isTrue(WS.state[WS._getContext(elem.parent())[0] + '.PenaltyBox'])) {
    sbCloseDialog(k, v, elem);
  }
}
