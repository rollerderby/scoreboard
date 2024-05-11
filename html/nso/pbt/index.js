'use strict';

WS.Register('ScoreBoard.CurrentGame.Team(*).Position(*).CurrentFielding');

function toggleSwap() {
  $('#Unassigned').toggleClass('SwappedTeams');
}

function questionMarkIfEmpty(k, v) {
  return v == null ? '?' : v;
}

function positionDetails(k, v, elem) {
  const prefix = k.upTo('Team');
  const teamName = WS.state[prefix + '.AlternateName(plt)'] || WS.state[prefix + '.UniformColor'] || WS.state[prefix + '.Name'];
  WS.SetupDialog($('#PositionDetails'), k, {
    title: teamName + ' ' + k.Position,
    width: 700,
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
  });
}

function getFielding(k, v, elem) {
  return WS.state[WS._getContext(elem)[0] + '.CurrentFielding'];
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
