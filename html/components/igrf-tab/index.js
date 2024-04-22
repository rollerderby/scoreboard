'use strict';

$('table.Officials')
  .clone(true)
  .attr('officialType', 'Ref')
  .find('.Title')
  .text('Skating Officials')
  .end()
  .find('select.Head')
  .attr('sbControl', 'HR')
  .children('option[sbForeach]')
  .attr('sbForeach', 'Ref:: name: resort=Name')
  .end()
  .end()
  .find('select.Role')
  .empty()
  .append($('#RefRoles').children())
  .end()
  .find('tr.Official')
  .attr('sbForeach', 'Ref:: role: resort=Role')
  .end()
  .appendTo('#Igrf');

function igrfToggleHide(k, v, elem, event) {
  if (event.target.tagName !== 'INPUT') {
    $('#Igrf>.Name>span>:not(button), #Igrf>.Variables').toggleClass('sbHide');
  }
}

function igrfToJsonDl(k, v) {
  return '/game-data/json/' + v + '.json';
}

function igrfToXlsxDl(k, v) {
  return '/game-data/xlsx/' + v + '.xlsx';
}

function igrfIsNotAborted(k) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').';
  const curPeriod = WS.state[prefix + '.CurrentPeriodNumber'];
  const lastPeriod = WS.state[prefix + '.Rule(Period.Number)'];
  const pc = WS.state[prefix + '.Clock(Period).Time'];
  const official = isTrue(WS.state[prefix + '.OfficialScore']);

  return !official || (pc <= 0 && curPeriod == lastPeriod);
}

function igrfToAbortTime(k) {
  const prefix = 'ScoreBoard.Game(' + k.Game + ').';
  const curPeriod = WS.state[prefix + '.CurrentPeriodNumber'];
  const lastPeriod = WS.state[prefix + '.Rule(Period.Number)'];
  const pc = WS.state[prefix + '.Clock(Period).Time'];
  let text = 'Game ended ';

  if (pc > 0) {
    text = text + 'with ' + _timeConversions.msToMinSec(pc) + ' left in period ' + curPeriod;
  } else if (curPeriod != lastPeriod) {
    text = text + 'after period ' + curPeriod;
  }
  return text + '. Reason: ';
}

function igrfNoExpulsions(k, v) {
  return !v && !$('.Expulsions table tr[Expulsion]:not([Expulsion="' + k.Expulsion + '"])').length;
}

function igrfToggleInput(k, v, elem) {
  elem
    .siblings('input')
    .val(v === 'O' ? '' : v)
    .toggleClass('sbHide', v !== 'O');
}

function igrfAddOfficial(k, v, elem) {
  const row = elem.closest('tr');
  _igrfAddOfficial(
    k,
    elem.closest('[officialType]').attr('officialType'),
    row.find('input.Role').val(),
    row.find('input.Name').val(),
    row.find('input.League').val(),
    row.find('select.Cert').val()
  );
  row.find('select').val('');
  row.find('input').val('');
  elem.prop('disabled', true);
}

function _igrfAddOfficial(prefix, type, role, name, league, cert, id) {
  id = id || sbNewUuid();
  prefix = prefix + '.' + type + '(' + id + ').';
  WS.Set(prefix + 'Role', role);
  WS.Set(prefix + 'Name', name);
  WS.Set(prefix + 'League', league);
  WS.Set(prefix + 'Cert', cert);
}

function igrfUpdateAddButton(k, v, elem, event) {
  const button = elem.closest('tr').find('button.AddOfficial');
  button.prop('disabled', !elem.closest('tr').find('input.Name').val());
  if (!button.prop('disabled') && 13 === event.which) {
    // Enter
    button.trigger('click');
  }
}

function igrfPasteOfficials(k, v, elem, event) {
  const text = event.originalEvent.clipboardData.getData('text');
  const lines = text.split('\n');
  if (lines.length <= 1) {
    // Not pasting in many values, so paste as usual.
    return true;
  }

  // Treat as a tab-seperated roster.
  let knownNames = {};
  elem
    .closest('table')
    .find('.Official')
    .map(function (_, n) {
      n = $(n);
      knownNames[n.attr('role') + '_' + n.attr('name')] = n.attr('Nso') || n.attr('Ref');
    });

  for (let i = 0; i < lines.length; i++) {
    const cols = lines[i].split('\t');
    if (cols.length < 2) {
      continue;
    }
    const role = cols[0].trim();
    const name = cols[1].trim();
    if (name === '') {
      continue;
    }
    let league = '';
    if (cols.length > 2) {
      league = cols[2].trim();
    }
    let cert = '';
    if (cols.length > 3) {
      cert = cols[3].trim().charAt(0);
    }

    var id = knownNames[role + '_' + name];
    _igrfAddOfficial(k, elem.closest('[officialType]').attr('officialType'), role, name, league, cert, id);
  }
  return false;
}

function igrfFilterOtherRole(k, v, elem) {
  return elem.children('[value="' + v + '"]').length ? v : 'O';
}

function igrfOtherToEmpty(k, v) {
  return v === 'O' ? '' : v;
}

function igrfNotOtherRole(k, v, elem) {
  return elem.siblings('select').children('[value="' + v + '"]').length > 0;
}

function igrfIsNotPerTeam(k, v) {
  return ['Penalty Lineup Tracker', 'Scorekeeper', 'Lineup Tracker', 'Jammer Referee', 'Penalty Box Timer'].indexOf(v) == -1;
}

function igrfOpenRemoveDialog(k) {
  WS.SetupDialog($('#OfficialRemoveDialog'), k, {
    title: 'Remove Official',
    modal: true,
    width: 700,
    buttons: [
      {
        text: 'No, keep this official.',
        click: function () {
          $(this).dialog('close');
        },
      },
      {
        text: 'Yes, remove!',
        click: function () {
          WS.Set(k, null);
          $(this).dialog('close');
        },
      },
    ],
  });
}
