'use strict';

$('#teamLogoUpload').fileupload({
  url: '/Media/upload',
  formData: [
    { name: 'media', value: 'images' },
    { name: 'type', value: 'teamlogo' },
  ],
  add: function (e, data) {
    let fd = new FormData();
    fd.append('f', data.files[0], data.files[0].name);
    data.files[0] = fd.get('f');
    data.submit();
  },
  done: function (e, data) {
    WS.Set(WS._getContext($('#teamLogoUpload')) + '.Logo', '/images/teamlogo/' + data.files[0].name);
  },
  fail: function (e, data) {
    console.log('Failed upload', data.errorThrown);
  },
});

function tmeOpenStoreDialog(k) {
  WS.SetupDialog($('#StoreTeamDialog'), k, {
    title: 'Store Team',
    width: '500px',
    modal: true,
    buttons: {
      Store: function () {
        WS.Set(k + '.PreparedTeam', $(this).find('select').val(), 'change');
        $(this).dialog('close');
      },
    },
  });
}

function tmeTriggerUpload() {
  $('#teamLogoUpload').trigger('click');
}

function tmeOpenAlternateNamesDialog(k) {
  WS.SetupDialog($('#AlternateNamesDialog'), k, {
    title: 'Alternate Names',
    width: 700,
    autoOpen: false,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  })
    .find('#newType')
    .autocomplete({
      minLength: 0,
      source: [
        { label: 'operator (Operator Controls)', value: 'operator' },
        { label: 'plt (Penalty/Lineup Tracker)', value: 'plt' },
        { label: 'box (Penalty Box)', value: 'box' },
        { label: 'overlay (Video Overlay)', value: 'overlay' },
        { label: 'scoreboard (Scoreboard Display)', value: 'scoreboard' },
        { label: 'whiteboard (Penalty Whiteboard)', value: 'whiteboard' },
      ],
    })
    .end()
    .dialog('open');
}

function tmeOpenColorsDialog(k) {
  WS.SetupDialog($('#ColorsDialog'), k, {
    title: 'Team Colors',
    width: 800,
    autoOpen: false,
    buttons: {
      Close: function () {
        $(this).dialog('close');
      },
    },
  })
    .find('#newType')
    .autocomplete({
      minLength: 0,
      source: [
        { label: 'operator (Operator Colors)', value: 'operator' },
        { label: 'plt (PLT Colors)', value: 'plt' },
        { label: 'penalty (Penalty Clock Colors)', value: 'penalty' },
        { label: 'overlay (Video Overlay Colors)', value: 'overlay' },
        { label: 'scoreboard (Scoreboard Colors)', value: 'scoreboard' },
        { label: 'scoreboard_dots (Scoreboard Dot Colors)', value: 'scoreboard_dots' },
        { label: 'whiteboard (Penalty Whiteboard)', value: 'whiteboard' },
      ],
    })
    .end()
    .dialog('open');
}

function tmeIsPrepared(k) {
  return k.PreparedTeam != null;
}

function tmeNotPrepared(k) {
  return k.PreparedTeam == null;
}

function tmeFilterOtherUc(k, v, elem) {
  return elem.children('[value="' + v + '"]').length ? v : '';
}

function tmeIsNotOtherUc(k, v) {
  const preparedTeam = WS.state['ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').PreparedTeam'];
  return k.field === 'UniformColor' && !!WS.state['ScoreBoard.PreparedTeam(' + preparedTeam + ').UniformColor(' + v + ')'];
}

function tmeAddNewUc(k, v, elem, event) {
  if (event && event.type === 'keyup' && event.which !== 13) {
    return;
  }
  if ($('#newUc').val() !== '') {
    WS.Set(k + '.UniformColor(' + $('#newUc').val() + ')', $('#newUc').val());
    $('#newUc').val('');
  }
}

function tmeSkaterCount(k, v, elem) {
  return _tmeSkaterCount(elem.closest('table').find('tr.Skater'));
}

function _tmeSkaterCount(rows) {
  var count = 0;
  rows.find('td.Flags select').each(function (_, f) {
    if (f.value === '' || f.value === 'C' || f.value === 'A') {
      count++;
    }
  });
  return '(' + count + ' skating)';
}

function tmeNewSkaterInput(k, v, elem, event) {
  const button = elem.closest('tr').find('button.AddSkater');
  const disable = !elem.closest('tr').find('input.RosterNumber').val() && !elem.closest('tr').find('input.Name').val();
  button.prop('disabled', disable).toggleClass('ui-button-disabled ui-state-disabled', disable);
  if (!disable && 13 === event.which) {
    // Enter
    tmeAddSkater(k, v, elem);
  }
}

function tmePasteSkaters(k, v, elem, event) {
  const text = event.originalEvent.clipboardData.getData('text');
  const lines = text.split('\n');
  if (lines.length <= 1) {
    // Not pasting in many values, so paste as usual.
    return true;
  }

  // Treat as a tab-seperated roster.
  let knownNumbers = {};
  elem
    .closest('.Skaters')
    .find('.Skater')
    .map(function (_, n) {
      n = $(n);
      knownNumbers[n.attr('skaternum')] = n.attr('skaterid');
    });

  for (let i = 0; i < lines.length; i++) {
    const cols = lines[i].split('\t');
    if (cols.length < 2) {
      continue;
    }
    const number = cols[0].trim();
    if (number === '') {
      continue;
    }
    const name = cols[1].trim();
    const pronouns = cols.length > 2 ? cols[cols.length - 1].trim() : '';
    // Assume same number means same skater.
    const id = knownNumbers[number];
    _tmeAddSkater(k, number, name, pronouns, '', id);
  }
  return false;
}

function _tmeAddSkater(teamPrefix, number, name, pronouns, flags, id) {
  id = id || sbNewUuid();
  WS.Set(teamPrefix + '.Skater(' + id + ').RosterNumber', number);
  WS.Set(teamPrefix + '.Skater(' + id + ').Name', name);
  WS.Set(teamPrefix + '.Skater(' + id + ').Pronouns', pronouns);
  WS.Set(teamPrefix + '.Skater(' + id + ').Flags', flags);
}

function tmeAddSkater(k, v, elem) {
  const row = elem.closest('tr');
  _tmeAddSkater(
    k,
    row.children().children('.RosterNumber').val(),
    row.children().children('.Name').val(),
    row.children().children('.Pronouns').val(),
    row.children().children('.Flags').val()
  );
  row.children().children('.RosterNumber').val('').trigger('focus');
  row.children().children('.Name').val('');
  row.children().children('.Pronouns').val('');
  row.children().children('.Flags').val('');
  row.children().children('.AddSkater').prop('disabled', true).addClass('ui-button-disabled ui-state-disabled');
}

function _tmeCheckDuplicateSkaters(rows) {
  var lastNumber = null;
  var lastElem = null;
  rows.removeClass('duplicateNumber').each(function () {
    const loopElem = $(this);
    const thisNumber = loopElem.attr('rosterNumber');
    if (lastNumber === thisNumber) {
      loopElem.add(lastElem).addClass('duplicateNumber');
    }
    lastElem = loopElem;
    lastNumber = thisNumber;
  });
}

function tmeSkaterAdded(k, v, elem) {
  const rows = elem.siblings().addBack();
  elem.closest('table').find('.SkaterCount').text(_tmeSkaterCount(rows));
  _tmeCheckDuplicateSkaters(rows);
}

function tmeSkaterRemoved(k, v, elem) {
  const rows = elem.siblings();
  elem.closest('table').find('.SkaterCount').text(_tmeSkaterCount(rows));
  _tmeCheckDuplicateSkaters(rows);
}

function tmeOpenRemoveSkaterDialog(k) {
  WS.SetupDialog($('#RemoveSkaterDialog'), k, {
    title: 'Remove Skater',
    modal: true,
    width: 700,
    buttons: [
      {
        text: 'No, keep this skater.',
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

function tmeOpenRemoveAllSkatersDialog(k) {
  WS.SetupDialog($('#RemoveAllSkatersDialog'), k, {
    title: 'Remove All Skaters',
    modal: true,
    width: 700,
    buttons: [
      {
        text: 'No, keep skaters.',
        click: function () {
          $(this).dialog('close');
        },
      },
      {
        text: 'Yes, remove all!',
        click: function () {
          WS.Set(k + '.ClearSkaters', true);
          $(this).dialog('close');
        },
      },
    ],
  });
}

function tmeGetAlternateNameId(k) {
  return k.AlternateName;
}

function tmeAddAlternateName(k, v, elem, event) {
  if (event && event.type === 'keypress' && event.which !== 13) {
    return;
  }
  const typeinput = elem.closest('#AlternateNamesDialog').find('#newType');
  const nameinput = elem.closest('#AlternateNamesDialog').find('#newName');
  WS.Set(k + '.AlternateName(' + typeinput.val() + ')', nameinput.val());
  nameinput.val('');
  typeinput.val('').trigger('focus');
}

function tmeStartAutocomplete(k, v, elem) {
  elem.autocomplete('search', '');
}

function tmeAddColor(k, v, elem) {
  const typeinput = elem.closest('#ColorsDialog').find('#newType');
  _tmeCopyColor(k, 'preset', typeinput.val());
  typeinput.val('').trigger('focus');
}

function tmeUsePresetColor(k, v, elem) {
  const type = elem.closest('[Color]').attr('Color').split('.')[0];
  _tmeCopyColor(k, 'preset', type);
}

function _tmeCopyColor(k, source, target) {
  WS.Set(k + '.Color(' + target + '.fg)', WS.state[k + '.Color(' + source + '.fg)'] || '');
  WS.Set(k + '.Color(' + target + '.bg)', WS.state[k + '.Color(' + source + '.bg)'] || '');
  WS.Set(k + '.Color(' + target + '.glow)', WS.state[k + '.Color(' + source + '.glow)'] || '');
}

function tmeClearColor(k, v, elem) {
  const type = elem.closest('[Color]').attr('Color').split('.')[0];
  WS.Set(k + '.Color(' + type + '.fg)', null);
  WS.Set(k + '.Color(' + type + '.bg)', null);
  WS.Set(k + '.Color(' + type + '.glow)', null);
}

function tmeToColorType(k, v, elem) {
  return elem.parent().attr('Color').split('.')[0];
}

function tmeDefaultColorIfEmpy(k, v, elem) {
  return v || '#787878';
}
