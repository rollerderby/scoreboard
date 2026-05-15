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
  .appendTo('#main');

function toTitle(k, v) {
  if (v == null && $('#sbConnectionStatus').attr('status') === 'ready') {
    window.close();
  } else {
    return v + ' | Edit Officials Crew | CRG ScoreBoard';
  }
}

function toggleInput(k, v, elem) {
  elem
    .siblings('input')
    .val(v === 'O' ? '' : v)
    .toggleClass('sbHide', v !== 'O');
}

function addOfficial(k, v, elem) {
  const row = elem.closest('thead');
  const prepared = elem.closest('thead').find('select.Prepared').val();
  if (prepared) {
    const prefix = k + '.' + elem.closest('[officialType]').attr('officialType') + '(' + sbNewUuid() + ').';
    WS.Set(prefix + 'Role', elem.closest('thead').find('input.Role').val());
    WS.Set(prefix + 'PreparedOfficial', prepared);
  } else {
    _addOfficial(
      k,
      elem.closest('[officialType]').attr('officialType'),
      row.find('input.Role').val(),
      row.find('input.Name').val(),
      row.find('input.League').val(),
      row.find('input.Cert').val(),
    );
  }
  row.find('select').val('');
  row.find('input').val('');
  elem.prop('disabled', true).toggleClass('ui-button-disabled ui-state-disabled', true);
}

function _addOfficial(prefix, type, role, name, league, cert, id) {
  id = id || sbNewUuid();
  prefix = prefix + '.' + type + '(' + id + ').';
  const prepId = sbNewUuid();
  const prepPrefix = 'ScoreBoard.PreparedOfficial(' + prepId + ').';
  WS.Set(prepPrefix + 'League', league);
  WS.Set(prepPrefix + 'Cert', cert);
  WS.Set(prepPrefix + 'Name', name);

  WS.Set(prefix + 'Role', role);
  WS.Set(prefix + 'PreparedOfficial', prepId);
}

function updateAddButton(k, v, elem, event) {
  const button = elem.closest('thead').find('button.AddOfficial');
  const disable = !elem.closest('thead').find('input.Name').val() && !elem.closest('thead').find('select.Prepared').val();
  button.prop('disabled', disable).toggleClass('ui-button-disabled ui-state-disabled', disable);
  if (!disable && 13 === event.which) {
    // Enter
    button.trigger('click');
  }
}

function pasteOfficials(k, v, elem, event) {
  const text = event.originalEvent.clipboardData.getData('text');
  const lines = text.split('\n');
  if (lines.length <= 1) {
    // Not pasting in many values, so paste as usual.
    return true;
  }

  // Treat as a tab-seperated roster.
  var knownNames = {};
  elem
    .closest('table')
    .find('.Official')
    .map(function (_, n) {
      n = $(n);
      knownNames[n.attr('role') + '_' + n.attr('name')] = n.attr('Nso') || n.attr('Ref');
    });

  for (var i = 0; i < lines.length; i++) {
    const cols = lines[i].split('\t');
    if (cols.length < 2) {
      continue;
    }
    const role = cols[0].trim();
    const name = cols[1].trim();
    if (name === '') {
      continue;
    }
    var league = '';
    if (cols.length > 2) {
      league = cols[2].trim();
    }
    var cert = '';
    if (cols.length > 3) {
      cert = cols[3].trim().charAt(0);
    }

    var id = knownNames[role + '_' + name];
    _addOfficial(k, elem.closest('[officialType]').attr('officialType'), role, name, league, cert, id);
  }
  return false;
}

function filterOtherRole(k, v, elem) {
  return elem.children('[value="' + v + '"]').length ? v : 'O';
}

function otherToEmpty(k, v) {
  return v === 'O' ? '' : v;
}

function notOtherRole(k, v, elem) {
  return elem.siblings('select').children('[value="' + v + '"]').length > 0;
}

function igrfIsNotPerTeam(k, v) {
  return ['Penalty Lineup Tracker', 'Scorekeeper', 'Lineup Tracker', 'Jammer Referee', 'Penalty Box Timer'].indexOf(v) == -1;
}

function openRemoveDialog(k) {
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
