function createTeamsTab(tab, gameId, teamId) {
  'use strict';
  var table;
  if (gameId != null) {
    // show teams for given game
    table = _crgUtils.createRowTable(2).appendTo(tab).attr('id', 'Teams', true);
    createEditTeamTable(table.children('tr').children('td:eq(0)'), 'ScoreBoard.Game(' + gameId + ').Team(1)', true);
    createEditTeamTable(table.children('tr').children('td:eq(1)'), 'ScoreBoard.Game(' + gameId + ').Team(2)', true);
  } else {
    // show prepared teams
    table = _crgUtils.createRowTable(1).appendTo(tab).attr('id', 'Teams', true);

    createEditTeamTable(table.children('tr').children('td:eq(0)'), 'ScoreBoard.PreparedTeam(' + teamId + ')', false);
  }
}

function createEditTeamTable(element, teamPrefix, isGameTeam) {
  'use strict';
  var teamTable = $('<table>')
    .addClass('Team')
    .append($('<tr><td colspan="2"/></tr>').addClass('Name'))
    .append($('<tr><td colspan="2"/></tr>').addClass('Control'))
    .append($('<tr><td/><td/></tr>').addClass('League'))
    .append($('<tr><td/><td/></tr>').addClass('LTeam'))
    .append($('<tr><td/><td/></tr>').addClass('Color'))
    .append($('<tr><td colspan="2"/></tr>').addClass('Skaters'))
    .appendTo(element);

  var teamName = $('<span>').toggleClass('Hide', isGameTeam).appendTo(teamTable.find('tr.Name>td'));
  WS.Register(teamPrefix + '.FullName', function (k, v) {
    teamName.text(v);
  });
  if (isGameTeam) {
    var teamSelect = WSControl(teamPrefix + '.PreparedTeam', $('<select>')).appendTo(teamTable.find('tr.Name>td'));
    $('<option value="">Custom Team</option>').appendTo(teamSelect);
    var teamConnected = WSActiveButton(teamPrefix + '.PreparedTeamConnected', $('<button>'))
      .addClass('SyncButton')
      .text('Sync Changes')
      .button()
      .appendTo(teamTable.find('tr.Name>td'));
    var storeTeam = $('<button>')
      .addClass('StoreButton Hide')
      .text('Store Team')
      .button()
      .on('click', function () {
        storeTeamDialog.dialog('open');
      })
      .appendTo(teamTable.find('tr.Name>td'));

    var mergeTeamSelect = $('<select>').append($('<option value="">').text('None'));
    var storeTeamDialog = $('<div>')
      .addClass('StoreTeamDialog')
      .append($('<span>').text('Merge into: '))
      .append(mergeTeamSelect)
      .dialog({
        title: 'Store Team',
        width: '500px',
        modal: true,
        autoOpen: false,
        buttons: {
          Store: function () {
            WS.Set(teamPrefix + '.PreparedTeam', mergeTeamSelect.val(), 'change');
            $(this).dialog('close');
          },
        },
        close: function () {
          $(this).dialog('close');
        },
      });

    WS.Register(teamPrefix + '.PreparedTeam', function (k, v) {
      teamConnected.toggleClass('Hide', v === '');
      storeTeam.toggleClass('Hide', v !== '');
    });

    WS.Register('ScoreBoard.PreparedTeam(*).FullName', function (k, v) {
      teamSelect
        .add(mergeTeamSelect)
        .children('option[value="' + k.PreparedTeam + '"]')
        .remove();
      if (v == null || v === '') {
        return;
      }
      var option = $('<option>').attr('value', k.PreparedTeam).data('name', v).text(v);
      _windowFunctions.appendAlphaSortedByData(teamSelect, option, 'name', 1);
      _windowFunctions.appendAlphaSortedByData(mergeTeamSelect, option.clone(true), 'name', 1);
      if (WS.state[teamPrefix + '.PreparedTeam'] === k.PreparedTeam) {
        teamSelect.val(k.PreparedTeam);
      }
    });

    WS.Register(teamPrefix.substring(0, teamPrefix.length - 7) + 'State', function (k, v) {
      teamSelect.toggleClass('Hide', v !== 'Prepared');
      teamName.toggleClass('Hide', v === 'Prepared');
    });
  }

  $('<span>').text('League: ').appendTo(teamTable.find('tr.League>td:eq(0)'));
  WSControl(teamPrefix + '.LeagueName', $('<input type="text" class="Name" size="30">')).appendTo(teamTable.find('tr.League>td:eq(1)'));
  $('<span>').text('Team: ').appendTo(teamTable.find('tr.LTeam>td:eq(0)'));
  WSControl(teamPrefix + '.TeamName', $('<input type="text" class="Name" size="30">')).appendTo(teamTable.find('tr.LTeam>td:eq(1)'));
  if (isGameTeam) {
    $('<span>').text('Uniform Color: ').appendTo(teamTable.find('tr.Color>td:eq(0)'));
    var colorSelectors = $('<span>').addClass('Hide').appendTo(teamTable.find('tr.Color>td:eq(1)'));
    var colorInput = WSControl(teamPrefix + '.UniformColor', $('<input type="text" class="Name" size="15">'))
      .addClass('Hide')
      .appendTo(teamTable.find('tr.Color>td:eq(1)'));

    WS.Register(teamPrefix + '.PreparedTeam', function (k, v) {
      colorSelectors.children().addClass('Hide');
      if (v == null || v === '') {
        colorSelectors.addClass('Hide');
        colorInput.removeClass('Hide');
        return;
      }
      colorSelectors.removeClass('Hide');
      var selector = colorSelectors.children('[id="' + v + '"]');
      if (!selector.length) {
        selector = $('<select>')
          .attr('id', v)
          .append($('<option value="">').text('Other'))
          .val('')
          .on('change', function () {
            if (!$(this).hasClass('Hide')) {
              if ($(this).val() !== '') {
                colorInput.val($(this).val());
                colorInput.trigger('change');
              }
              colorInput.toggleClass('Hide', $(this).val() !== '');
            }
          })
          .appendTo(colorSelectors);
        WS.Register('ScoreBoard.PreparedTeam(' + v + ').UniformColor(*)', function (kk, vv) {
          selector.children('[id="' + kk.UniformColor + '"]').remove();
          if (vv != null) {
            var newOption = $('<option>').attr('value', vv).attr('id', kk.UniformColor).text(vv);
            _windowFunctions.appendAlphaSortedByAttr(selector, newOption, 'value', 1);
          }
          if (selector.children('[value="' + WS.state[teamPrefix + '.UniformColor'] + '"]').length) {
            selector.val(WS.state[teamPrefix + '.UniformColor']);
          } else {
            selector.val('');
          }
          selector.trigger('change');
        });
      } else {
        if (
          WS.state[teamPrefix + '.UniformColor'] !== '' &&
          selector.children('[value=' + WS.state[teamPrefix + '.UniformColor'] + ']').length
        ) {
          selector.val(WS.state[teamPrefix + '.UniformColor']);
        } else {
          selector.val('');
        }
      }
      selector.removeClass('Hide').trigger('change');
    });
  } else {
    $('<span>').text('Uniform Colors: ').appendTo(teamTable.find('tr.Color>td:eq(0)'));
    var colorList = $('<span>').appendTo(teamTable.find('tr.Color>td:eq(1)'));
    var newColorInput = $('<input type="text" class="Name" size="15">')
      .on('keyup', function (event) {
        if ($(this).val() !== '' && 13 === event.which) {
          WS.Set(teamPrefix + '.UniformColor(' + newUUID() + ')', $(this).val());
          $(this).val('');
        }
      })
      .appendTo(teamTable.find('tr.Color>td:eq(1)'));
    $('<button>')
      .text('Add')
      .button()
      .on('click', function () {
        if (newColorInput.val() !== '') {
          WS.Set(teamPrefix + '.UniformColor(' + newUUID() + ')', newColorInput.val());
          newColorInput.val('');
        }
      })
      .appendTo(teamTable.find('tr.Color>td:eq(1)'));
    WS.Register(teamPrefix + '.UniformColor(*)', function (k, v) {
      var entry = colorList.children('[id="' + k.UniformColor + '"]');
      if (v == null) {
        entry.remove();
      } else if (entry.length) {
        entry.children('span').text(v);
      } else {
        $('<span>')
          .addClass('RemoveColor')
          .attr('id', k.UniformColor)
          .append($('<span>').text(v))
          .append(
            $('<button>')
              .text('X')
              .button()
              .on('click', function () {
                WS.Set(k, null);
              })
          )
          .appendTo(colorList);
      }
    });
  }

  var controlTable = _crgUtils.createRowTable(3).appendTo(teamTable.find('tr.Control>td')).addClass('Control');
  var waitingOnUpload = '';
  var logoSelect = $('<select>').append($('<option value="">No Logo</option>')).appendTo(controlTable.find('td:eq(0)'));
  logoSelect.on('change', function () {
    WS.Set(teamPrefix + '.Logo', logoSelect.val() === '' ? '' : '/images/teamlogo/' + logoSelect.val());
  });
  WS.Register('ScoreBoard.Media.Format(images).Type(teamlogo).File(*).Name', function (k, v) {
    var val = logoSelect.val(); // Record this before we potentially remove and re-add it.
    logoSelect.children('[value="' + k.File + '"]').remove();
    if (v != null) {
      if (waitingOnUpload === k.File) {
        val = k.File;
        waitingOnUpload = '';
      }
      var option = $('<option>').attr('name', v).attr('value', k.File).text(v);
      _windowFunctions.appendAlphaSortedByAttr(logoSelect, option, 'name', 1);
      logoSelect.val(val);
      logoSelect.trigger('change');
    } else if (val === k.File) {
      logoSelect.val('');
      logoSelect.trigger('change');
    }
  });
  $('<input type="file" id="teamLogoUpload">')
    .fileupload({
      url: '/Media/upload',
      formData: [
        { name: 'media', value: 'images' },
        { name: 'type', value: 'teamlogo' },
      ],
      add: function (e, data) {
        var fd = new FormData();
        fd.append('f', data.files[0], data.files[0].name);
        data.files[0] = fd.get('f');
        data.submit();
        waitingOnUpload = fd.get('f').name;
      },
      fail: function (e, data) {
        /* jshint -W117 */
        console.log('Failed upload', data.errorThrown);
        /* jshint +W117 */
      },
    })
    .css('display', 'none')
    .appendTo(controlTable.find('td:eq(0)'));
  $('<button>')
    .text('Upload...')
    .appendTo(controlTable.find('td:eq(0)'))
    .on('click', function () {
      controlTable.find('#teamLogoUpload').trigger('click');
    });
  var alternameNameDialog = createAlternateNamesDialog(teamPrefix);
  $('<button>')
    .text('Alternate Names')
    .button()
    .on('click', function () {
      alternameNameDialog.dialog('open');
    })
    .appendTo(controlTable.find('td:eq(1)'));
  var colorsDialog = createColorsDialog(teamPrefix);
  $('<button>')
    .text('Colors')
    .button()
    .on('click', function () {
      colorsDialog.dialog('open');
    })
    .appendTo(controlTable.find('td:eq(2)'));

  var skatersTable = $('<table>')
    .addClass('Skaters Empty')
    .appendTo(teamTable.find('tr.Skaters>td'))
    .append('<col class="RosterNumber">')
    .append('<col class="Name">')
    .append('<col class="Flags">')
    .append('<col class="Button">')
    .append('<thead/><tbody/>')
    .children('thead')
    .append('<tr><th></th><th class="Title">Skaters</th><th id="skaterCount"></th><th></th></tr>')
    .append('<tr><th>Number</th><th>Name</th><th>Flags</th><th>Add</th>')
    .append('<tr class="AddSkater"><th/><th/><th/><th/><th/></tr>')
    .append('<tr><th colspan="4"><hr/></th></tr>')
    .end();

  var addSkater = function (number, name, flags, id) {
    id = id || newUUID();
    WS.Set(teamPrefix + '.Skater(' + id + ').RosterNumber', number);
    WS.Set(teamPrefix + '.Skater(' + id + ').Name', name);
    WS.Set(teamPrefix + '.Skater(' + id + ').Flags', flags);
  };

  var newSkaterNumber = $('<input type="text" size="5">').addClass('RosterNumber').appendTo(skatersTable.find('tr.AddSkater>th:eq(0)'));
  var newSkaterName = $('<input type="text" size="20">').addClass('Name').appendTo(skatersTable.find('tr.AddSkater>th:eq(1)'));
  var newSkaterFlags = $('<select>').addClass('Flags').appendTo(skatersTable.find('tr.AddSkater>th:eq(2)'));
  var newSkaterButton = $('<button>')
    .text('Add Skater')
    .button({ disabled: true })
    .addClass('AddSkater')
    .appendTo(skatersTable.find('tr.AddSkater>th:eq(3)'))
    .on('click', function () {
      addSkater(newSkaterNumber.val(), newSkaterName.val(), newSkaterFlags.val());
      newSkaterNumber.val('').trigger('focus');
      newSkaterFlags.val('');
      newSkaterName.val('');
      $(this).trigger('blur');
      newSkaterButton.button('option', 'disabled', true);
    });
  newSkaterName.add(newSkaterNumber).on('keyup', function (event) {
    newSkaterButton.button('option', 'disabled', !newSkaterName.val() && !newSkaterNumber.val());
    if (!newSkaterButton.button('option', 'disabled') && 13 === event.which) {
      // Enter
      newSkaterButton.trigger('click');
    }
  });
  newSkaterFlags.append($('<option>').attr('value', '').text('Skater'));
  newSkaterFlags.append($('<option>').attr('value', 'ALT').text('Not Skating'));
  newSkaterFlags.append($('<option>').attr('value', 'C').text('Captain'));
  newSkaterFlags.append($('<option>').attr('value', 'A').text('Alt Captain'));
  newSkaterFlags.append($('<option>').attr('value', 'BA').text('Bench Alt Captain'));
  newSkaterFlags.append($('<option>').attr('value', 'B').text('Bench Staff'));
  var pasteHandler = function (e) {
    var text = e.originalEvent.clipboardData.getData('text');
    var lines = text.split('\n');
    if (lines.length <= 1) {
      // Not pasting in many values, so paste as usual.
      return true;
    }

    // Treat as a tab-seperated roster.
    var knownNumbers = {};
    teamTable.find('.Skater').map(function (_, n) {
      n = $(n);
      knownNumbers[n.attr('skaternum')] = n.attr('skaterid');
    });

    for (var i = 0; i < lines.length; i++) {
      var cols = lines[i].split('\t');
      if (cols.length < 2) {
        continue;
      }
      var number = $.trim(cols[0]);
      if (number === '') {
        continue;
      }
      var name = $.trim(cols[1]);
      // Assume same number means same skater.
      var id = knownNumbers[number];
      addSkater(number, name, '', id);
    }
    return false;
  };
  newSkaterNumber.on('paste', pasteHandler);
  newSkaterName.on('paste', pasteHandler);

  var updateSkaterCount = function () {
    var count = 0;
    skatersTable.find('tr.Skater td.Flags select').each(function (_, f) {
      if (f.value !== 'BC' && f.value !== 'ALT') {
        count++;
      }
    });
    skatersTable.find('#skaterCount').text('(' + count + ' skating)');
  };
  updateSkaterCount();

  var handleTeamUpdate = function (k, v) {
    if (k.Skater != null) {
      var skaterRow = skatersTable.find('tr[skaterid="' + k.Skater + '"]');
      if (v == null) {
        skaterRow.remove();
        if (!skatersTable.find('tr[skaterid]').length) {
          skatersTable.children('tbody').addClass('Empty');
        }
        updateSkaterCount();
        return;
      }
      var skaterPrefix = teamPrefix + '.Skater(' + k.Skater + ')';

      if (skaterRow.length === 0) {
        skatersTable.removeClass('Empty');
        skaterRow = $('<tr class="Skater">')
          .attr('skaterid', k.Skater)
          .append('<td class="RosterNumber">')
          .append('<td class="Name">')
          .append('<td class="Flags">')
          .append('<td class="Remove">');
        var numberInput = $('<input type="text" size="5">').appendTo(skaterRow.children('td.RosterNumber'));
        var nameInput = $('<input type="text" size="20">').appendTo(skaterRow.children('td.Name'));
        nameInput.on('change', function () {
          WS.Set(skaterPrefix + '.Name', nameInput.val());
        });
        $('<button>')
          .text('Remove')
          .addClass('RemoveSkater')
          .button()
          .on('click', function () {
            createTeamsSkaterRemoveDialog(WS.state[teamPrefix + '.Name'], skaterPrefix);
          })
          .appendTo(skaterRow.children('td.Remove'));
        numberInput.on('change', function () {
          WS.Set(skaterPrefix + '.RosterNumber', numberInput.val());
          skaterRow.attr('skaternum', WS.state[skaterPrefix + '.RosterNumber']);
        });
        var skaterFlags = $('<select>').appendTo(skaterRow.children('td.Flags'));
        skaterFlags.append($('<option>').attr('value', '').text('Skater'));
        skaterFlags.append($('<option>').attr('value', 'ALT').text('Not Skating'));
        skaterFlags.append($('<option>').attr('value', 'C').text('Captain'));
        skaterFlags.append($('<option>').attr('value', 'A').text('Alt Captain'));
        skaterFlags.append($('<option>').attr('value', 'BA').text('Bench Alt Captain'));
        skaterFlags.append($('<option>').attr('value', 'B').text('Bench Staff'));
        skaterFlags.on('change', function () {
          WS.Set(skaterPrefix + '.Flags', skaterFlags.val());
        });
        _windowFunctions.appendAlphaSortedByAttr(skatersTable.children('tbody'), skaterRow, 'skaternum');
      }

      skaterRow
        .children('td.' + k.field)
        .children()
        .val(v);
      if (k.field === 'Flags') {
        updateSkaterCount();
      } else if (k.field === 'RosterNumber') {
        skaterRow.attr('skaternum', v);
        _windowFunctions.appendAlphaSortedByAttr(skatersTable.children('tbody'), skaterRow, 'skaternum');
      }
    } else {
      // Team update.
      switch (k.field) {
        case 'Logo':
          v = v || '';
          logoSelect.val(v.substring(v.lastIndexOf('/') + 1));
          break;
        case 'AlternateName':
          if (v == null) {
            alternameNameDialog.removeFunc(k.AlternateName);
            return;
          }
          alternameNameDialog.updateFunc(k.AlternateName, v);
          break;
        case 'Color':
          var colorId = k.Color.substring(0, k.Color.lastIndexOf('_'));
          if (v == null) {
            colorsDialog.removeFunc(colorId);
            return;
          }
          colorsDialog.addFunc(colorId);
          colorsDialog.updateFunc(colorId, k.Color.substring(k.Color.lastIndexOf('_') + 1), v);
          break;
      }
    }
  };

  WS.Register(
    [
      teamPrefix + '.AlternateName',
      teamPrefix + '.Color',
      teamPrefix + '.Logo',
      teamPrefix + '.Skater(*).Flags',
      teamPrefix + '.Skater(*).Name',
      teamPrefix + '.Skater(*).RosterNumber',
    ],
    handleTeamUpdate
  );

  return teamTable;
}

function createAlternateNamesDialog(prefix) {
  'use strict';
  var dialog = $('<div>').addClass('AlternateNamesDialog');

  $('<a>').text('Type:').appendTo(dialog);
  var newIdInput = $('<input type="text">').appendTo(dialog);
  $('<a>').text('Name:').appendTo(dialog);
  var newNameInput = $('<input type="text">').appendTo(dialog);

  var newFunc = function () {
    var newId = newIdInput.val();
    var newName = newNameInput.val();
    WS.Set(prefix + '.AlternateName(' + newId + ')', newName);
    newNameInput.val('');
    newIdInput.val('').trigger('focus');
  };

  newNameInput.on('keypress', function (event) {
    if (event.which === 13) {
      // Enter
      newFunc();
    }
  });
  $('<button>').button({ label: 'Add' }).on('click', newFunc).appendTo(dialog);

  var table = $('<table>').appendTo(dialog);
  var thead = $('<thead>').appendTo(table);
  $('<tr>').append('<th class="X">X</th>').append('<th class="Id">Id</th>').append('<th class="Name">Name</th>').appendTo(thead);
  var tbody = $('<tbody>').appendTo(table);

  dialog.updateFunc = function (id, v) {
    var tr = tbody.find('tr#' + id);
    if (tr.length === 0) {
      tr = $('<tr>').attr('id', id).append('<td class="X">').append('<td class="Id">').append('<td class="Name">');
      $('<button>')
        .button({ label: 'X' })
        .on('click', function () {
          WS.Set(prefix + '.AlternateName(' + id + ')', null);
        })
        .appendTo(tr.children('td.X'));
      $('<input type="text" size="20">')
        .on('input', function (e) {
          WS.Set(prefix + '.AlternateName(' + id + ')', e.target.value);
        })
        .appendTo(tr.children('td.Name'));
      _windowFunctions.appendAlphaSortedByAttr(tbody, tr, 'id');
    }
    tr.children('td.Id').text(id);
    tr.find('td.Name input').val(v);
  };
  dialog.removeFunc = function (id) {
    tbody.children('tr#' + id).remove();
  };

  dialog.dialog({
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

  newIdInput
    .autocomplete({
      minLength: 0,
      source: [
        { label: 'operator (Operator Controls)', value: 'operator' },
        { label: 'overlay (Video Overlay)', value: 'overlay' },
        { label: 'scoreboard (Scoreboard Display)', value: 'scoreboard' },
        { label: 'whiteboard (Penalty Whiteboard)', value: 'whiteboard' },
        { label: 'twitter (Twitter)', value: 'twitter' },
      ],
      appendTo: dialog.parent(),
    })
    .on('focus', function () {
      $(this).autocomplete('search', '');
    });

  return dialog;
}

function createColorsDialog(prefix) {
  'use strict';
  var dialog = $('<div>').addClass('ColorsDialog');

  $('<a>').text('Type:').appendTo(dialog);
  var newIdInput = $('<input type="text" size="10">').appendTo(dialog);

  var newFunc = function () {
    var newId = newIdInput.val();

    WS.Set(prefix + '.Color(' + newId + '_fg)', '');
    WS.Set(prefix + '.Color(' + newId + '_bg)', '');
    WS.Set(prefix + '.Color(' + newId + '_glow)', '');

    newIdInput.val('').trigger('focus');
  };

  $('<button>').button({ label: 'Add' }).on('click', newFunc).appendTo(dialog);

  var table = $('<table>').appendTo(dialog);
  var thead = $('<thead>').appendTo(table);
  $('<tr>')
    .append('<th class="X"></th>')
    .append('<th class="Id">Id</th>')
    .append('<th class="fg">Foreground</th>')
    .append('<th class="bg">Background</th>')
    .append('<th class="glow">Glow/Halo</th>')
    .appendTo(thead);
  var tbody = $('<tbody>').appendTo(table);

  dialog.addFunc = function (colorId) {
    var tr = tbody.children('[id="' + colorId + '"]');
    if (tr.length === 0 && colorId !== '') {
      tr = $('<tr>')
        .attr('id', colorId)
        .append('<td class="X">')
        .append('<td class="Id">')
        .append('<td class="fg">')
        .append('<td class="bg">')
        .append('<td class="glow">');
      tr.children('td.Id').text(colorId);
      $('<input type="color" cleared="true" value="#666666" suffix="fg">').appendTo(tr.children('td.fg'));
      $('<input type="color" cleared="true" value="#666666" suffix="bg">').appendTo(tr.children('td.bg'));
      $('<input type="color" cleared="true" value="#666666" suffix="glow">').appendTo(tr.children('td.glow'));
      tr.find('input').on('input', function (e) {
        WS.Set(prefix + '.Color(' + colorId + '_' + $(e.target).attr('suffix') + ')', e.target.value);
      });
      tr.find('input').after(
        $('<button class="ClearPrev">X</button>').on('click', function (e) {
          WS.Set(prefix + '.Color(' + colorId + '_' + $(e.target).prev().attr('suffix') + ')', '');
        })
      );

      $('<button>')
        .button({ label: 'X' })
        .on('click', function () {
          WS.Set(prefix + '.Color(' + colorId + '_fg)', null);
          WS.Set(prefix + '.Color(' + colorId + '_bg)', null);
          WS.Set(prefix + '.Color(' + colorId + '_glow)', null);
        })
        .appendTo(tr.children('td.X'));

      _windowFunctions.appendAlphaSortedByAttr(tbody, tr, 'id');
    }
  };
  dialog.removeFunc = function (colorId) {
    tbody.children('tr[id="' + colorId + '"]').remove();
  };
  dialog.updateFunc = function (colorId, suffix, v) {
    if (v == null || v === '') {
      tbody
        .children('tr[id="' + colorId + '"]')
        .children('td.' + suffix)
        .children('input')
        .attr('cleared', 'true')
        .val('#666666');
    } else {
      tbody
        .children('tr[id="' + colorId + '"]')
        .children('td.' + suffix)
        .children('input')
        .attr('cleared', 'false')
        .val(v);
    }
  };

  dialog.dialog({
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

  newIdInput
    .autocomplete({
      minLength: 0,
      source: [
        { label: 'operator (Operator Colors)', value: 'operator' },
        { label: 'overlay (Video Overlay Colors)', value: 'overlay' },
        { label: 'scoreboard (Scoreboard Colors)', value: 'scoreboard' },
        { label: 'scoreboard_dots (Scoreboard Dot Colors)', value: 'scoreboard_dots' },
      ],
      appendTo: dialog.parent(),
    })
    .on('focus', function () {
      $(this).autocomplete('search', '');
    });

  return dialog;
}

function createTeamsSkaterRemoveDialog(teamName, prefix) {
  'use strict';
  var dialog = $('<div>').addClass('TeamsRemoveDialog');

  $('<a>')
    .addClass('Title')
    .text('Team: ' + teamName)
    .appendTo(dialog);
  $('<br>').appendTo(dialog);

  var skaterName = WS.state[prefix + '.Name'];
  var skaterNumber = WS.state[prefix + '.RosterNumber'];
  $('<a>').addClass('Remove').text('Remove Skater: ').appendTo(dialog);
  $('<a>').addClass('Target').text(skaterNumber).appendTo(dialog);
  $('<br>').appendTo(dialog);
  if (skaterName) {
    $('<a>')
      .addClass('Name')
      .text('(Skater Name: ' + skaterName + ')')
      .appendTo(dialog);
    $('<br>').appendTo(dialog);
  }

  $('<hr>').appendTo(dialog);
  $('<a>').addClass('AreYouSure').text('Are you sure?').appendTo(dialog);
  $('<br>').appendTo(dialog);

  $('<button>')
    .addClass('No')
    .text('No, keep this skater.')
    .appendTo(dialog)
    .on('click', function () {
      dialog.dialog('close');
    })
    .button();
  $('<button>')
    .addClass('Yes')
    .text('Yes, remove!')
    .appendTo(dialog)
    .on('click', function () {
      WS.Set(prefix, null);
      dialog.dialog('close');
    })
    .button();

  dialog.dialog({
    title: 'Remove Skater',
    modal: true,
    width: 700,
    close: function () {
      $(this).dialog('destroy').remove();
    },
  });
}
