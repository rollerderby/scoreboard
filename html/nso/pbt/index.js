'use strict';

WS.Register('ScoreBoard.CurrentGame.Team(*).Position(*).CurrentFielding');

function toggleSwap() {
  $('#Unassigned').toggleClass('SwappedTeams');
}

function questionMarkIfEmpty(k, v) {
  return v == null ? '?' : v;
}

function editLineup(k, v, elem) {
  WS.SetupDialog($('#LineupEditor'), k, {
    title: 'Lineup Editor',
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
