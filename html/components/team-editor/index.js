function postTeamInit() {
  $('#StoreTeamDialog').dialog({
    title: 'Store Team',
    width: '500px',
    modal: true,
    autoOpen: false,
    buttons: {
      Store: function () {
        WS.Set(teamPrefix + '.PreparedTeam', $('#StoreTeamDialog select').val(), 'change');
        $(this).dialog('close');
      },
    },
    close: function () {
      $(this).dialog('close');
    },
  });

  $('#AlternateNamesDialog')
    .each(function () {
      $(this).attr('sbContext', WS._getContext($(this)));
    })
    .dialog({
      title: 'Alternate Names',
      modal: true,
      width: 700,
      autoOpen: false,
      buttons: {
        Close: function () {
          $(this).dialog('close');
        },
      },
    });

  $('#AlternateNamesDialog #newType')
    .autocomplete({
      minLength: 0,
      source: [
        { label: 'operator (Operator Controls)', value: 'operator' },
        { label: 'overlay (Video Overlay)', value: 'overlay' },
        { label: 'scoreboard (Scoreboard Display)', value: 'scoreboard' },
        { label: 'whiteboard (Penalty Whiteboard)', value: 'whiteboard' },
      ],
    })
    .on('focus', function () {
      $(this).autocomplete('search', '');
    });

  $('#AlternateNamesDialog #newName').on('keypress', function (event) {
    if (event.which === 13) {
      //Enter
      addAlternateName(WS._enrichProp(WS._getContext($(this))), null, $(this));
    }
  });

  $('#ColorsDialog')
    .each(function () {
      $(this).attr('sbContext', WS._getContext($(this)));
    })
    .dialog({
      title: 'Team Colors',
      modal: true,
      width: 800,
      autoOpen: false,
      buttons: {
        Close: function () {
          $(this).dialog('close');
        },
      },
    });

  $('#ColorsDialog #newType')
    .autocomplete({
      minLength: 0,
      source: [
        { label: 'operator (Operator Colors)', value: 'operator' },
        { label: 'overlay (Video Overlay Colors)', value: 'overlay' },
        { label: 'scoreboard (Scoreboard Colors)', value: 'scoreboard' },
        { label: 'scoreboard_dots (Scoreboard Dot Colors)', value: 'scoreboard_dots' },
      ],
    })
    .on('focus', function () {
      $(this).autocomplete('search', '');
    });

  $('#RemoveSkaterDialog').dialog({
    title: 'Remove Skater',
    modal: true,
    width: 700,
    autoOpen: false,
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
          WS.Set($(this).attr('path'), null);
          $(this).dialog('close');
        },
      },
    ],
  });
}

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
    /* jshint -W117 */
    console.log('Failed upload', data.errorThrown);
    /* jshint +W117 */
  },
});

$('#newUc').on('keyup', function (event) {
  if ($(this).val() !== '' && 13 === event.which) {
    WS.Set(WS._getContext($(this)) + '.UniformColor(' + newUUID() + ')', $(this).val());
    $(this).val('');
  }
});

$('.AddSkater>th>input')
  .on('keyup', function (event) {
    const elem = $(this);
    elem
      .parent()
      .find('button.AddSkater')
      .button(
        'option',
        'disabled',
        !elem.parent().parent().children('.RosterNumber').val() && !elem.parent().parent().children('.Name').val()
      );
    if (!elem.parent().parent().find('button.AddSkater').button('option', 'disabled') && 13 === event.which) {
      // Enter
      addSkater(WS._enrichProp(WS._getContext(elem)));
    }
  })
  .on('paste', function (e) {
    const elem = $(this);
    const text = e.originalEvent.clipboardData.getData('text');
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
      const number = $.trim(cols[0]);
      if (number === '') {
        continue;
      }
      const name = $.trim(cols[1]);
      const pronouns = cols.length > 2 ? $.trim(cols[cols.length - 1]) : '';
      // Assume same number means same skater.
      const id = knownNumbers[number];
      _addSkater(WS._enrichProp(WS._getContext(elem)), number, name, pronouns, '', id);
    }
    return false;
  });

function triggerUpload() {
  'use strict';
  $('#teamLogoUpload').trigger('click');
}

function openAlternateNamesDialog(k) {
  'use strict';
  if (k.Team) {
    $('#AlternateNamesDialog[team="' + k.Team + '"]').dialog('open');
  } else {
    $('#AlternateNamesDialog').dialog('open');
  }
}

function openColorsDialog(k) {
  'use strict';
  if (k.Team) {
    $('#ColorsDialog[team="' + k.Team + '"]').dialog('open');
  } else {
    $('#ColorsDialog').dialog('open');
  }
}

function isPrepared(k, v) {
  'use strict';
  return k.PreparedTeam != null;
}

function notPrepared(k, v) {
  'use strict';
  return k.PreparedTeam == null;
}

function isSelectorTeam(k, v, elem) {
  'use strict';
  return v === elem.attr('team');
}

function filterOtherUc(k, v, elem) {
  'use strict';
  elem.children('[value="' + v + '"]').length ? v : '';
}

function isNotOtherUc(k, v) {
  'use strict';
  const preparedTeam = WS.state['ScoreBoard.Game(' + k.Game + ').Team(' + k.Team + ').PreparedTeam'];
  return !preparedTeam || $('#PreparedUc [PreparedTeam="' + preparedTeam + '"] [value="' + v + '"]').length;
}

function addNewUc(k, v, elem) {
  'use strict';
  if ($('#newUc').val() !== '') {
    WS.Set(k + '.UniformColor(' + newUUID() + ')', $('#newUc').val());
    $('#newUc').val('');
  }
}

function skaterCount(k, v, elem) {
  'use strict';
  var count = 0;
  elem
    .closest('table')
    .find('tr.Skater td.Flags select')
    .each(function (_, f) {
      if (f.value === '' || f.value === 'C' || f.value === 'A') {
        count++;
      }
    });
  return '(' + count + ' skating)';
}

function _addSkater(teamPrefix, number, name, pronouns, flags, id) {
  'use strict';
  id = id || newUUID();
  WS.Set(teamPrefix + '.Skater(' + id + ').RosterNumber', number);
  WS.Set(teamPrefix + '.Skater(' + id + ').Name', name);
  WS.Set(teamPrefix + '.Skater(' + id + ').Pronouns', pronouns);
  WS.Set(teamPrefix + '.Skater(' + id + ').Flags', flags);
}

function addSkater(k) {
  'use strict';
  _addSkater(
    k,
    $('.AddSkater>>.RosterNumber').val(),
    $('.AddSkater>>.Name').val(),
    $('.AddSkater>>.Pronouns').val(),
    $('.AddSkater>>.Flags').val()
  );
  $('.AddSkater>>.RosterNumber').val('').trigger('focus');
  $('.AddSkater>>.Name').val('');
  $('.AddSkater>>.Pronouns').val('');
  $('.AddSkater>>.Flags').val('');
  $('button.AddSkater').trigger('blur');
  $('button.AddSkater').button('option', 'disabled', true);
}

function openRemoveSkaterDialog(k, v, elem) {
  'use strict';
  $('#RemoveSkaterDialog .Title').text(
    'Team: ' +
      WS.state[
        k.PreparedTeam
          ? 'ScoreBoard.PreparedTeam(' + k.PreparedTeam + ').FullName'
          : 'ScoreBaord.Game(' + k.Game + ').Team(' + k.Team + ').FullName'
      ]
  );
  $('#RemoveSkaterDialog .Target').text(WS.state[k + '.RosterNumber']);
  $('#RemoveSkaterDialog .Name').text('(Skater Name: ' + WS.state[k + '.Name'] + ')');
  $('#RemoveSkaterDialog').attr('path', k).dialog('open');
}

function getAlternateNameId(k) {
  'use strict';
  return k.AlternateName;
}

function addAlternateName(k, v, elem) {
  'use strict';
  const typeinput = elem.closest('#AlternateNamesDialog').children('#newType');
  const nameinput = elem.closest('#AlternateNamesDialog').children('#newName');
  WS.Set(k + '.AlternateName(' + typeinput.val() + ')', nameinput.val());
  nameinput.val('');
  typeinput.val('').trigger('focus');
}

function addColor(k, v, elem) {
  'use strict';
  const typeinput = elem.closest('#ColorsDialog').children('#newType');
  WS.Set(k + '.Color(' + typeinput.val() + '.fg)', '');
  WS.Set(k + '.Color(' + typeinput.val() + '.bg)', '');
  WS.Set(k + '.Color(' + typeinput.val() + '.glow)', '');
  typeinput.val('').trigger('focus');
}

function clearColor(k, v, elem) {
  'use strict';
  const type = elem.closest('[Color]').attr('Color').split('.')[0];
  WS.Set(k + '.Color(' + type + '.fg)', null);
  WS.Set(k + '.Color(' + type + '.bg)', null);
  WS.Set(k + '.Color(' + type + '.glow)', null);
}

function toColorType(k, v, elem) {
  'use strict';
  return elem.parent().attr('Color').split('.')[0];
}

function defaultColorIfEmpy(k, v, elem) {
  'use strict';
  return v || '#787878';
}
