'use strict';

WS.Register([
  'ScoreBoard.CurrentGame.Rule(Penalties.NumberToFoulout)',
  'ScoreBoard.CurrentGame.Team(*).BoxTrip(*).CurrentFielding',
  'ScoreBoard.CurrentGame.Team(*).BoxTrip(*).CurrentSkater',
  'ScoreBoard.CurrentGame.Team(*).Skater(*).PenaltyCount',
  'ScoreBoard.CurrentGame.Team(*).Skater(*).Penalty(0).Code',
  'ScoreBoard.CurrentGame.Team(*).Position(*).RosterNumber',
]);

function penFoOrExp(k, v) {
  const limit = WS.state[k.upTo('Game') + '.Rule(Penalties.NumberToFoulout)'];
  const skaterPrefix = k.upTo('Team') + '.Skater(' + v + ')';
  return WS.state[skaterPrefix + '.Penalty(0).Code'] || WS.state[skaterPrefix + '.PenaltyCount'] >= limit;
}

function penToInstruction(k, v) {
  return WS.state[k.upTo('BoxTrip') + '.EndFielding'] ? 'Done' : v > 10000 ? 'Sit' : 'Stand';
}

function penDetailButtons(k, v) {
  var content = '<span>';
  v.split(',').forEach(function (detailsText) {
    if (detailsText) {
      const details = detailsText.split('_');
      content =
        content +
        '<button sbContext="/' +
        k.upTo('Team') +
        '.Skater(' +
        details[0] +
        ').Penalty(' +
        details[1] +
        ')" sbDisplay="Code" sbCall="penSelectCode | sbCloseDialog"></button>';
    }
  });
  return content + '</span>';
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
