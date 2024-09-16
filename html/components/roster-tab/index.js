function rstUpdateSkaterCount(k, v) {
  var count = 0;
  $('#RosterTab [Team="' + k.Team + '"] tr.Skater').each(function (_, f) {
    var flag = $(f).attr('flag');
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

function rstToPositionDisplay(k, v) {
  var position = '';
  const flag = WS.state[k.upTo('Skater') + '.Flags'];
  const role = WS.state[k.upTo('Skater') + '.Role'];
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
      return position;
    case 'C':
      return position === 'Skater' ? 'Captain' : position + ' C';
    case 'A':
      return position === 'Skater' ? 'Alt Captain' : position + ' A';
    case 'ALT':
      return 'Not Skating';
    case 'BA':
      return 'Bench Alt Captain';
    case 'B':
      return 'Bench Staff';
  }
  return '';
}
