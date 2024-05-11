'use strict';

WS.Register([
  'ScoreBoard.CurrentGame.Team(*).BoxTrip(*).CurrentFielding',
  'ScoreBoard.CurrentGame.Team(*).BoxTrip(*).CurrentSkater',
  'ScoreBoard.CurrentGame.Team(*).Skater(*).PenaltyCount',
  'ScoreBoard.CurrentGame.Team(*).Position(*).RosterNumber',
]);

function penToInstruction(k, v) {
  return WS.state[k.upTo('BoxTrip') + '.EndFielding'] ? 'Done' : v > 10000 ? 'Sit' : 'Stand';
}

function penToPenaltyCodeDisplay(k, v) {
  let output = '<div class="Code">' + k.PenaltyCode + '</div><div class="Description">';
  v.split(',').forEach(function (d) {
    output = output + '<div>' + d + '</div>';
  });
  output = output + '</div>';
  return output;
}

function penSubstitute(k) {
  WS.SetupDialog($('#SubstituteSelector'), k.upTo('Team') + '.Position(' + WS.state[k + '.CurrentFielding'].split('_')[2] + ')', {
    title:
      'Substitute for ' +
      (WS.state[k.upTo('Team') + '.AlternateName(box)'] ||
        WS.state[k.upTo('Team') + '.UniformColor'] ||
        WS.state[k.upTo('Team') + '.Name']) +
      ' #' +
      WS.state[k + '.RosterNumber'],
    width: 700,
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
  });
}

function penReassign(k) {
  WS.SetupDialog($('#ReassignmentSelector'), k, {
    title: 'Reassign Box Trip',
    width: 500,
    buttons: {
      Cancel: function () {
        $(this).dialog('close');
      },
    },
  });
}

function penAdd(k) {
  const skaterPrefix = k.upTo('Team') + '.Skater(' + WS.state[k + '.CurrentSkater'] + ')';
  penSelectCode(skaterPrefix + '.Penalty(' + (WS.state[skaterPrefix + '.PenaltyCount'] + 1) + ')');
}

function penSelectCode(k) {
  WS.SetupDialog($('#PenaltySelector'), k, {
    title: 'Select Penalty Code',
    width: '80%',
  });
}
