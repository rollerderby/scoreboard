$('.NSOs table')
  .clone(true)
  .find('.Title')
  .text('Skating Officials')
  .end()
  .find('select.Head')
  .attr('sbControl', 'HR')
  .children('option[sbForeach]')
  .attr('sbForeach', 'Ref:: compareName: resort=Name')
  .end()
  .end()
  .find('select.Role')
  .empty()
  .append($('#RefRoles').children())
  .end()
  .find('tr.Official')
  .attr('sbForeach', 'Ref:: compareRole: resort=Role')
  .end()
  .appendTo('.Refs > td');

function toggleHide(k, v, elem) {
  'use strict';
  elem.siblings('table').addClass('sbHide');
  elem.siblings(':not(table):not(button)').add(elem).toggleClass('sbHide');
}

function toJsonDl(k, v) {
  'use strict';
  return '/game-data/json/' + v + '.json';
}

function toXlsxDl(k, v) {
  'use strict';
  return '/game-data/xlsx/' + v + '.xlsx';
}

function isNotAborted(k) {
  'use strict';
  const prefix = 'ScoreBoard.Game(' + k.Game + ').';
  const curPeriod = WS.state[prefix + '.CurrentPeriodNumber'];
  const lastPeriod = WS.state[prefix + '.Rule(Period.Number)'];
  const pc = WS.state[prefix + '.Clock(Period).Time'];
  const official = isTrue(WS.state[prefix + '.OfficialScore']);

  return !official || (pc <= 0 && curPeriod == lastPeriod);
}

function toAbortTime(k) {
  'use strict';
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

function prefixPeriod(k, v) {
  'use strict';
  return 'Period ' + v;
}

function noExpulsions(k, v) {
  'use strict';
  return !v && !$('.Expulsions table tr[Expulsion]:not([Expulsion="' + k.Expulsion + '"])').length;
}

function toggleInput(k, v, elem) {
  'use strict';
  elem
    .siblings('input')
    .val(v === 'O' ? '' : v)
    .toggleClass('sbHide', v !== 'O');
}

function addOfficial(k, v, elem) {
  'use strict';
  const row = elem.closest('tr');
  _addOfficial(
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

function _addOfficial(prefix, type, role, name, league, cert, id) {
  id = id || newUUID();
  prefix = prefix + '.' + type + '(' + id + ').';
  WS.Set(prefix + 'Role', role);
  WS.Set(prefix + 'Name', name);
  WS.Set(prefix + 'League', league);
  WS.Set(prefix + 'Cert', cert);
}

function updateAddButton(k, v, elem, event) {
  'use strict';
  const button = elem.closest('tr').find('button.AddOfficial');
  button.prop('disabled', !elem.closest('tr').find('input.Name').val());
  if (!button.prop('disabled') && 13 === event.which) {
    // Enter
    button.trigger('click');
  }
}

function pasteOfficials(k, v, elem, event) {
  'use strict';
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
    addOfficial(k, type, role, name, league, cert, id);
  }
  return false;
}

function filterOtherRole(k, v, elem) {
  'use strict';
  return elem.children('[value="' + v + '"]').length ? v : 'O';
}

function otherToEmpty(k, v) {
  'use strict';
  return v === 'O' ? '' : v;
}

function notOtherRole(k, v, elem) {
  'use strict';
  return elem.siblings('select').children('[value="' + v + '"]').length > 0;
}

function isNotPerTeam(k, v) {
  'use strict';
  return ['Penalty Lineup Tracker', 'Scorekeeper', 'Lineup Tracker', 'Jammer Referee', 'Penalty Box Timer'].indexOf(v) == -1;
}

function openRemoveDialog(k) {
  'use strict';
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
