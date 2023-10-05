function updateSkaterCount(k, v) {
  let count = 0;
  $('#RosterTab [Team="' + k.Team + '"] tr.Skater').each(function (_, f) {
    let flag = $(f).attr('flag');
    if ($(f).attr('Skater') === k.Skater) {
      flag = v;
    }
    if (flag === '' || flag === 'C' || flag === 'A') {
      count++;
    }
  });
  $('#RosterTab [Team="' + k.Team + '"] #skaterCount').text('(' + count + ' skating)');

  return v;
}

function toPositionDisplay(k, v) {
  let position = '';
  const flag = WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').Skater(' + k.Skater + ').Flags'];
  const role = WS.state['ScoreBoard.CurrentGame.Team(' + k.Team + ').Skater(' + k.Skater + ').Role'];
  switch (role) {
    case 'Jammer':
    case 'Pivot':
    case 'Blocker':
      position = role;
      break;
    default:
      position = 'Skater';
      break;
  }
  switch (flag) {
    case '':
      return position === '' ? 'Skater' : position;
    case 'C':
      return position === '' ? 'Captain' : position + ' C';
    case 'A':
      return position === '' ? 'Alt Captain' : position + ' A';
    case 'ALT':
      return 'Not Skating';
    case 'BA':
      return 'Bench Alt Captain';
    case 'B':
      return 'Bench Staff';
  }
  return '';
}
