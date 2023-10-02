function createIgrfTab(tab, gameId) {
  'use strict';
  var gamePrefix = 'ScoreBoard.Game(' + gameId + ')';
  var table = $('<table>')
    .attr('id', 'Igrf')
    .appendTo(tab)
    .append($('<tr><td colspan="3"/></tr>').addClass('Name'))
    .append($('<tr><td colspan="3"/></tr>').addClass('Files'))
    .append($('<tr><td/><td/><td/></tr>').addClass('Location'))
    .append($('<tr><td colspan="2"/><td/></tr>').addClass('Event'))
    .append($('<tr><td colspan="3"/></tr>').addClass('Host'))
    .append($('<tr><td/><td/><td/></tr>').addClass('Time'))
    .append($('<tr><td colspan="3"><hr/></td></tr>').addClass('Separator Abort'))
    .append($('<tr><td colspan="3"/></tr>').addClass('Abort Info'))
    .append($('<tr><td colspan="3"><hr/></td></tr>').addClass('Separator'))
    .append($('<tr><td colspan="3"/></tr>').addClass('Summary'))
    .append($('<tr><td colspan="3"><hr/></td></tr>').addClass('Separator Expulsions Hide'))
    .append($('<tr><td colspan="3"><table><tr><th colspan="3">Expulsions</th></tr></table></td></tr>').addClass('Expulsions Hide'))
    .append($('<tr><td colspan="3"><hr/></td></tr>').addClass('Separator'))
    .append($('<tr><td colspan="3"/></tr>').addClass('NSOs'))
    .append($('<tr><td colspan="3"><hr/></td></tr>').addClass('Separator'))
    .append($('<tr><td colspan="3"/></tr>').addClass('Refs'));
  var gameName = $('<span>')
    .addClass('Name')
    .appendTo(table.find('tr.Name>td'))
    .on('click', function () {
      gameName.addClass('Hide');
      $('.NameFormat').removeClass('Hide');
      nameFormat.focus();
    });
  $('<span>').addClass('NameFormat Hide').text('Name Format: ').appendTo(table.find('tr.Name>td'));
  var nameFormat = WSControl(gamePrefix + '.NameFormat', $('<input type="text" size="30">'))
    .addClass('NameFormat Hide')
    .appendTo(table.find('tr.Name>td'))
    .on('focusout', function () {
      $('.NameFormat').addClass('Hide');
      gameName.removeClass('Hide');
    });
  $('<table>')
    .addClass('NameFormat Variables Hide')
    .append($('<tr>').append($('<th colspan="2">').text('Supported Variables:')))
    .append($('<tr>').append($('<td>').text('%g:')).append($('<td>').text('Game Number')))
    .append($('<tr>').append($('<td>').text('%G:')).append($('<td>').text('"Game <Game Number>: " if game number is set, empty otherwise')))
    .append($('<tr>').append($('<td>').text('%d:')).append($('<td>').text('Date of Game')))
    .append($('<tr>').append($('<td>').text('%t:')).append($('<td>').text('Start Time')))
    .append($('<tr>').append($('<td>').text('%1:')).append($('<td>').text('Team 1 Name')))
    .append($('<tr>').append($('<td>').text('%2:')).append($('<td>').text('Team 2 Name')))
    .append($('<tr>').append($('<td>').text('%s:')).append($('<td>').text('Game State')))
    .append($('<tr>').append($('<td>').text('%S:')).append($('<td>').text('"<Team 1 Score> - <Team 2 Score>"')))
    .appendTo(table.find('tr.Name>td'));
  var endButton = $('<button>')
    .text('End Game')
    .on('click', function () {
      WS.Set(gamePrefix + '.OfficialScore', true);
    })
    .button()
    .appendTo(table.find('tr.Name>td'));

  WS.Register(gamePrefix + '.State', function (k, v) {
    endButton.toggleClass('Hide', v !== 'Running');
  });

  $('<span>').text('Game Files: ').appendTo(table.find('tr.Files>td:eq(0)'));
  var updateButton = $('<button>')
    .text('Update')
    .on('click', function () {
      WS.Set(gamePrefix + '.Export', true);
    })
    .button()
    .appendTo(table.find('tr.Files>td:eq(0)'));
  var exportBlocked = $('<span>').appendTo(table.find('tr.Files>td:eq(0)'));
  var spinner = $('<div>').addClass('spin').appendTo(table.find('tr.Files>td:eq(0)'));
  var jsonButton = $('<a download>').text('Download JSON').button().appendTo(table.find('tr.Files>td:eq(0)'));
  var noJson = $('<span>').text(' No JSON yet ').appendTo(table.find('tr.Files>td:eq(0)'));
  var xlsxButton = $('<a download>').text('Download Statsbook').button().appendTo(table.find('tr.Files>td:eq(0)'));
  var noXlsx = $('<span>').text(' No Statsbook yet  ').appendTo(table.find('tr.Files>td:eq(0)'));
  $('<span>').text(' Last Updated: ').appendTo(table.find('tr.Files>td:eq(0)'));
  var downloadDate = $('<span>').appendTo(table.find('tr.Files>td:eq(0)'));
  var noBlankStatsbookWarning = $('<span>')
    .text('  Blank Statsbook not found.')
    .addClass('Warning')
    .appendTo(table.find('tr.Files>td:eq(0)'));
  var brokenBlankStatsbookWarning = $('<span>')
    .text('  Blank Statsbook not readable.')
    .addClass('Warning')
    .appendTo(table.find('tr.Files>td:eq(0)'));
  var checkingBlankStatsbookWarning = $('<span>')
    .text('  Checking Blank Statsbook.')
    .addClass('Warning')
    .appendTo(table.find('tr.Files>td:eq(0)'));

  WS.Register(gamePrefix + '.ExportBlockedBy', function (k, v) {
    exportBlocked.text('Export Blocked by ' + v).toggle(v != '');
    updateButton.toggle(v == '');
  });
  WS.Register(gamePrefix + '.UpdateInProgress', function (k, v) {
    spinner.toggle(isTrue(v));
  });
  WS.Register(gamePrefix + '.Filename', function (k, v) {
    jsonButton.attr('href', '/game-data/json/' + v + '.json');
    xlsxButton.attr('href', '/game-data/xlsx/' + v + '.xlsx');
  });
  WS.Register(gamePrefix + '.JsonExists', function (k, v) {
    jsonButton.toggle(isTrue(v));
    noJson.toggle(!isTrue(v));
  });
  WS.Register(gamePrefix + '.StatsbookExists', function (k, v) {
    xlsxButton.toggle(isTrue(v));
    noXlsx.toggle(!isTrue(v));
  });
  WS.Register(gamePrefix + '.LastFileUpdate', function (k, v) {
    downloadDate.text(v);
  });
  WS.Register('ScoreBoard.BlankStatsbookFound', function (k, v) {
    noBlankStatsbookWarning.toggle(v === 'none');
    brokenBlankStatsbookWarning.toggle(v === 'broken');
    checkingBlankStatsbookWarning.toggle(v === 'checking');
  });

  $('<span>').text('Tournament: ').appendTo(table.find('tr.Event>td:eq(0)'));
  WSControl(gamePrefix + '.EventInfo(Tournament)', $('<input type="text" size="50">')).appendTo(table.find('tr.Event>td:eq(0)'));

  $('<span>').text('Game: ').appendTo(table.find('tr.Event>td:eq(1)'));
  WSControl(gamePrefix + '.EventInfo(GameNo)', $('<input type="text" size="5">')).appendTo(table.find('tr.Event>td:eq(1)'));

  $('<span>').text('Venue: ').appendTo(table.find('tr.Location>td:eq(0)'));
  WSControl(gamePrefix + '.EventInfo(Venue)', $('<input type="text" size="30">')).appendTo(table.find('tr.Location>td:eq(0)'));

  $('<span>').text('City: ').appendTo(table.find('tr.Location>td:eq(1)'));
  WSControl(gamePrefix + '.EventInfo(City)', $('<input type="text" size="30">')).appendTo(table.find('tr.Location>td:eq(1)'));

  $('<span>').text('State / Province: ').appendTo(table.find('tr.Location>td:eq(2)'));
  WSControl(gamePrefix + '.EventInfo(State)', $('<input type="text" size="10">')).appendTo(table.find('tr.Location>td:eq(2)'));

  $('<span>').text('Host League: ').appendTo(table.find('tr.Host>td'));
  WSControl(gamePrefix + '.EventInfo(HostLeague)', $('<input type="text" size="50">')).appendTo(table.find('tr.Host>td'));

  $('<span>').text('Date: ').appendTo(table.find('tr.Time>td:eq(0)'));
  WSControl(gamePrefix + '.EventInfo(Date)', $('<input type="date">')).appendTo(table.find('tr.Time>td:eq(0)'));

  $('<span>').text('Start Time: ').appendTo(table.find('tr.Time>td:eq(1)'));
  WSControl(gamePrefix + '.EventInfo(StartTime)', $('<input type="time">')).appendTo(table.find('tr.Time>td:eq(1)'));

  var abortTime = $('<span>').text('').appendTo(table.find('tr.Abort.Info>td:eq(0)'));
  WSControl(gamePrefix + '.AbortReason', $('<input type="text" size="50">')).appendTo(table.find('tr.Abort.Info>td:eq(0)'));

  WS.Register(
    [
      gamePrefix + '.Clock(Period).Time',
      gamePrefix + '.CurrentPeriodNumber',
      gamePrefix + '.Rule(Period.Number)',
      gamePrefix + '.OfficialScore',
    ],
    function () {
      var curPeriod = WS.state[gamePrefix + '.CurrentPeriodNumber'];
      var lastPeriod = WS.state[gamePrefix + '.Rule(Period.Number)'];
      var pc = WS.state[gamePrefix + '.Clock(Period).Time'];
      var text = 'Game ended ';
      var show = isTrue(WS.state[gamePrefix + '.OfficialScore']);

      if (pc > 0) {
        text = text + 'with ' + _timeConversions.msToMinSec(pc) + ' left in period ' + curPeriod;
      } else if (curPeriod != lastPeriod) {
        text = text + 'after period ' + curPeriod;
      } else {
        show = false;
      }
      abortTime.text(text + '. Reason: ');
      $('tr.Abort').toggleClass('Hide', !show);
    }
  );

  WS.Register([
    gamePrefix + '.Period(*).Team1Points',
    gamePrefix + '.Period(*).Team1PenaltyCount',
    gamePrefix + '.Period(*).Team2Points',
    gamePrefix + '.Period(*).Team2PenaltyCount',
  ]);

  var summaryTable = $('<table>')
    .addClass('Summary')
    .append(
      $('<tr>')
        .attr('nr', 0)
        .addClass('Head')
        .append(WSDisplay(gamePrefix + '.Team(1).Name', $('<td colspan="5">').addClass('Value')))
        .append(WSDisplay(gamePrefix + '.Team(2).Name', $('<td colspan="5">').addClass('Value')))
    )
    .append(
      $('<tr>')
        .addClass('Total')
        .append($('<td colspan="2">').text('TOTAL POINTS:'))
        .append(WSDisplay(gamePrefix + '.Team(1).Score', $('<td>').addClass('Value')))
        .append($('<td>').text('PENALTIES:'))
        .append(WSDisplay(gamePrefix + '.Team(1).TotalPenalties', $('<td>').addClass('Value')))
        .append($('<td colspan="2">').text('TOTAL POINTS:'))
        .append(WSDisplay(gamePrefix + '.Team(2).Score', $('<td>').addClass('Value')))
        .append($('<td>').text('PENALTIES:'))
        .append(WSDisplay(gamePrefix + '.Team(2).TotalPenalties', $('<td>').addClass('Value')))
    )
    .appendTo(table.find('tr.Summary>td'));

  function addSummaryPeriodRow(nr) {
    var row = summaryTable.children('tr[nr=' + nr + ']');
    if (row.length) {
      return row;
    }
    var previousRow = summaryTable.children('tr[nr=' + (nr - 1) + ']');
    if (!previousRow.length) {
      previousRow = addSummaryPeriodRow(nr - 1);
    }
    return $('<tr>')
      .attr('nr', nr)
      .addClass('Period')
      .append(
        $('<td>')
          .addClass('Label')
          .text('Period ' + nr)
      )
      .append($('<td>').addClass('Label Small').text('Points'))
      .append(WSDisplay(gamePrefix + '.Period(' + nr + ').Team1Points', $('<td>').addClass('Value')))
      .append($('<td>').addClass('Label Small').text('Penalties'))
      .append(WSDisplay(gamePrefix + '.Period(' + nr + ').Team1PenaltyCount', $('<td>').addClass('Value')))
      .append(
        $('<td>')
          .addClass('Label')
          .text('Period ' + nr)
      )
      .append($('<td>').addClass('Label Small').text('Points'))
      .append(WSDisplay(gamePrefix + '.Period(' + nr + ').Team2Points', $('<td>').addClass('Value')))
      .append($('<td>').addClass('Label Small').text('Penalties'))
      .append(WSDisplay(gamePrefix + '.Period(' + nr + ').Team2PenaltyCount', $('<td>').addClass('Value')))
      .insertAfter(previousRow);
  }

  WS.Register('ScoreBoard.Game(' + gameId + ').Period(*).Number', function (k, v) {
    if (v == null) {
      summaryTable.children('tr[nr=' + k.Period + ']').remove();
    } else {
      addSummaryPeriodRow(v);
    }
  });

  function createExpulsionRow(id) {
    $('.Expulsions').removeClass('Hide');
    return $('<tr>')
      .attr('penalty', id)
      .append($('<td>').addClass('Info'))
      .append($('<td>').append(WSControl(gamePrefix + '.Expulsion(' + id + ').ExtraInfo', $('<input type="text" size="50">'))))
      .append(
        $('<td>').append(
          WSActiveButton(gamePrefix + '.Expulsion(' + id + ').Suspension', $('<button>').text('Suspension Recommended').button())
        )
      )
      .appendTo('.Expulsions table');
  }

  WS.Register(gamePrefix + '.Expulsion(*).Info', function (k, v) {
    var row = $('.Expulsions tr[penalty=' + k.Expulsion + ']');
    if (v == null) {
      row.remove();
      if ($('.Expulsions tr').length === 1) {
        $('.Expulsions').addClass('Hide');
      }
      return;
    } else if (row.length === 0) {
      row = createExpulsionRow(k.Expulsion);
    }
    row.children('.Info').text(v);
  });

  var createOfficialsTable = function (title) {
    return $('<table>')
      .addClass('Officials Empty')
      .append('<col class="Role">')
      .append('<col class="Name">')
      .append('<col class="League">')
      .append('<col class="Cert">')
      .append('<col class="Button">')
      .append('<thead/><tbody/>')
      .children('thead')
      .append('<tr><th colspan="2" class="Title">' + title + '</th><th class="Head">Head: </th></tr>')
      .append('<tr><th>Role</th><th>Name</th><th>League</th><th>Cert Level</th><th/>')
      .append('<tr class="AddOfficial"><th/><th/><th/><th/><th/></tr>')
      .append('<tr><th colspan="5"><hr/></th></tr>')
      .end();
  };

  var nsoTable = createOfficialsTable('Non Skating Officials').appendTo(table.find('tr.NSOs>td'));
  var hnsoSelect = WSControl(
    gamePrefix + '.HNSO',
    $('<select>').append($('<option>').attr('value', '').text('Not Set')).addClass('Head')
  ).appendTo(nsoTable.find('th.Head'));
  var refTable = createOfficialsTable('Skating Officials').appendTo(table.find('tr.Refs>td'));
  var hrSelect = WSControl(
    gamePrefix + '.HR',
    $('<select>').append($('<option>').attr('value', '').text('Not Set')).addClass('Head')
  ).appendTo(refTable.find('th.Head'));

  WS.Register(gamePrefix + '.Nso(*).Name', function (k, v) {
    var option = hnsoSelect.children('[value=' + k.Nso + ']');
    if (v == null) {
      option.remove();
      return;
    }
    if (option.length === 0) {
      option = $('<option>').attr('value', k.Nso);
    }
    option.data('name', v).text(v);
    _windowFunctions.appendAlphaSortedByData(hnsoSelect, option, 'name', 1);
    hnsoSelect.val(WS.state[gamePrefix + '.HNSO']);
  });
  WS.Register(gamePrefix + '.Ref(*).Name', function (k, v) {
    var option = hrSelect.children('[value=' + k.Ref + ']');
    if (v == null) {
      option.remove();
      return;
    }
    if (option.length === 0) {
      option = $('<option>').attr('value', k.Ref);
    }
    option.data('name', v).text(v);
    _windowFunctions.appendAlphaSortedByData(hrSelect, option, 'name', 1);
    hrSelect.val(WS.state[gamePrefix + '.HR']);
  });

  var addOfficial = function (type, role, name, league, cert, id) {
    id = id || newUUID();
    var prefix = gamePrefix + '.' + type + '(' + id + ').';
    WS.Set(prefix + 'Role', role);
    WS.Set(prefix + 'Name', name);
    WS.Set(prefix + 'League', league);
    WS.Set(prefix + 'Cert', cert);
  };

  var knownNsoRoles = [
    'Penalty Lineup Tracker',
    'Penalty Tracker',
    'Penalty Wrangler',
    'Inside Whiteboard Operator',
    'Jam Timer',
    'Scorekeeper',
    'ScoreBoard Operator',
    'Penalty Box Manager',
    'Penalty Box Timer',
    'Lineup Tracker',
    'Non-Skating Official Alternate',
  ];
  var knownRefRoles = ['Head Referee', 'Inside Pack Referee', 'Jammer Referee', 'Outside Pack Referee', 'Referee Alternate'];

  var makeRoleDropdown = function (knownRoles, prefix) {
    var otherRoleInput = $('<input type="text" size="20">')
      .addClass('OtherRole')
      .hide()
      .change(function () {
        if (prefix != null) {
          WS.Set(prefix + '.Role', $(this).val());
        }
      });
    var teamSelection = $('<span>').append($('<br/>')).append($('<span>').text('P1: ')).hide();
    var teamDropdown = $('<select>')
      .addClass('Team')
      .appendTo(teamSelection)
      .append($('<option>').attr('value', '').text(''))
      .append(
        $('<option>')
          .attr('value', gameId + '_1')
          .text('Team 1')
      )
      .append(
        $('<option>')
          .attr('value', gameId + '_2')
          .text('Team 2')
      )
      .change(function () {
        if (prefix != null) {
          WS.Set(prefix + '.P1Team', $(this).val());
        }
      });
    WS.Register(gamePrefix + '.Team(*).Name', function (k, v) {
      teamDropdown.find('option[value=' + gameId + '_' + k.Team + ']').text(v);
    });
    var swapButton = $('<button>')
      .addClass('Swap')
      .append($('<span>').text('Swap'))
      .button()
      .appendTo(teamSelection)
      .click(function () {
        if (prefix != null) {
          WS.Set(prefix + '.Swap', !$(this).hasClass('Active'));
        } else {
          $(this).toggleClass('Active');
        }
      });
    var dropdown = $('<select>').addClass('Role').append($('<option>').attr('value', '').text('None Selected'));
    $.each(knownRoles, function (i, role) {
      dropdown.append($('<option>').attr('value', role).text(role));
    });
    dropdown.append($('<option>').attr('value', 'O').text('Other'));
    dropdown.change(function () {
      otherRoleInput.hide();
      teamSelection.hide();
      if ($(this).val() === 'O') {
        otherRoleInput.show();
      } else if (prefix != null) {
        WS.Set(prefix + '.Role', $(this).val());
        if (
          ['Penalty Lineup Tracker', 'Scorekeeper', 'Lineup Tracker', 'Jammer Referee', 'Penalty Box Timer'].indexOf($(this).val()) > -1
        ) {
          teamSelection.show();
        }
      }
    });

    return $('<span>').addClass('Role').append(dropdown).append(otherRoleInput).append(teamSelection);
  };

  var makeCertDropdown = function () {
    return $('<select>')
      .addClass('Cert')
      .append($('<option>').attr('value', '').text('None'))
      .append($('<option>').attr('value', 'R').text('Recognized'))
      .append($('<option>').attr('value', '1').text('Level 1'))
      .append($('<option>').attr('value', '2').text('Level 2'))
      .append($('<option>').attr('value', '3').text('Level 3'));
  };

  var fillNewOfficialRow = function (officialsTable, type, roles) {
    var newRole = makeRoleDropdown(roles).appendTo(officialsTable.find('tr.AddOfficial>th:eq(0)'));
    var newName = $('<input type="text" size="30">').addClass('Name').appendTo(officialsTable.find('tr.AddOfficial>th:eq(1)'));
    var newLeague = $('<input type="text" size="30">').addClass('League').appendTo(officialsTable.find('tr.AddOfficial>th:eq(2)'));
    var newCert = makeCertDropdown().appendTo(officialsTable.find('tr.AddOfficial>th:eq(3)'));
    var newButton = $('<button>')
      .text('Add')
      .button({ disabled: true })
      .addClass('AddOfficial')
      .appendTo(officialsTable.find('tr.AddOfficial>th:eq(4)'))
      .on('click', function () {
        var dropdown = newRole.find('select.Role');
        var role = dropdown.val() === 'O' ? newRole.find('input.OtherRole').val() : dropdown.val();
        addOfficial(type, role, newName.val(), newLeague.val(), newCert.val());
        newRole.find('select.Role').val('').change();
        newRole.find('input.OtherRole').val('');
        newRole.find('select.Team').val('');
        newRole.find('button').removeClass('Active');
        newName.val('').trigger('focus');
        newLeague.val('');
        newCert.val('');
        $(this).trigger('blur');
        newButton.button('option', 'disabled', true);
      });
    newName.add(newLeague).on('keyup', function (event) {
      newButton.button('option', 'disabled', !newName.val());
      if (!newButton.button('option', 'disabled') && 13 === event.which) {
        // Enter
        newButton.trigger('click');
      }
    });
    var pasteHandler = function (e) {
      var text = e.originalEvent.clipboardData.getData('text');
      var lines = text.split('\n');
      if (lines.length <= 1) {
        // Not pasting in many values, so paste as usual.
        return true;
      }

      // Treat as a tab-seperated roster.
      var knownNames = {};
      officialsTable.find('.Official').map(function (_, n) {
        n = $(n);
        knownNames[n.attr('role') + '_' + n.attr('offname')] = n.attr('offid');
      });

      for (var i = 0; i < lines.length; i++) {
        var cols = lines[i].split('\t');
        if (cols.length < 2) {
          continue;
        }
        var role = $.trim(cols[0]);
        var name = $.trim(cols[1]);
        if (name === '') {
          continue;
        }
        var league = '';
        if (cols.length > 2) {
          league = $.trim(cols[2]);
        }
        var cert = '';
        if (cols.length > 3) {
          cert = $.trim(cols[3]).charAt(0);
        }

        var id = knownNames[role + '_' + name];
        addOfficial(type, role, name, league, cert, id);
      }
      return false;
    };
    newName.on('paste', pasteHandler);
    newLeague.on('paste', pasteHandler);
  };

  fillNewOfficialRow(nsoTable, 'Nso', knownNsoRoles);
  fillNewOfficialRow(refTable, 'Ref', knownRefRoles);

  var handleUpdate = function (k, v) {
    var officialsTable;
    var id;
    var prefix;
    var knownRoles;
    if (k.Nso != null) {
      officialsTable = nsoTable;
      id = k.Nso;
      prefix = gamePrefix + '.Nso(' + id + ')';
      knownRoles = knownNsoRoles;
    } else if (k.Ref != null) {
      officialsTable = refTable;
      id = k.Ref;
      prefix = gamePrefix + '.Ref(' + id + ')';
      knownRoles = knownRefRoles;
    } else if (k.field === 'Name') {
      gameName.text(v);
      return;
    }
    var row = officialsTable.find('tr[offid="' + id + '"]');
    if (v == null && !(row.length > 0 && k.field === 'P1Team')) {
      row.remove();
      if (!officialsTable.find('tr[offid]').length) {
        officialsTable.children('tbody').addClass('Empty');
      }
      return;
    }

    if (row.length === 0) {
      officialsTable.removeClass('Empty');
      row = $('<tr class="Official">')
        .attr('offid', id)
        .attr('role', WS.state[prefix + '.Role'])
        .append('<td class="Role">')
        .append('<td class="Name">')
        .append('<td class="League">')
        .append('<td class="Cert">')
        .append('<td class="Remove">');
      var roleInput = makeRoleDropdown(knownRoles, prefix).appendTo(row.children('td.Role'));
      var nameInput = $('<input type="text" size="30">')
        .appendTo(row.children('td.Name'))
        .on('change', function () {
          WS.Set(prefix + '.Name', $(this).val());
          row.attr('offname', $(this).val());
        });
      var leagueInput = $('<input type="text" size="30">')
        .appendTo(row.children('td.League'))
        .on('change', function () {
          WS.Set(prefix + '.League', $(this).val());
        });
      $('<button>')
        .text('Remove')
        .addClass('RemoveOfficial')
        .button()
        .on('click', function () {
          createOfficialRemoveDialog(prefix);
        })
        .appendTo(row.children('td.Remove'));
      var cert = makeCertDropdown()
        .appendTo(row.children('td.Cert'))
        .change(function () {
          WS.Set(prefix + '.Cert', $(this).val());
        });

      _windowFunctions.appendAlphaSortedByAttr(officialsTable.children('tbody'), row, 'role');
    }

    row
      .children('td.' + k.field)
      .children()
      .val(v);
    if (k.field === 'Role') {
      var dropdown = row.find('select.Role');
      if (!dropdown.find('option[value="' + v + '"]').length) {
        dropdown.val('O');
        row.find('td.Role input.OtherRole').val(v);
      } else {
        dropdown.val(v);
      }
      dropdown.change();
      row.attr('role', v);
      _windowFunctions.appendAlphaSortedByAttr(officialsTable.children('tbody'), row, 'role');
    } else if (k.field === 'P1Team') {
      row.find('select.Team').val(v);
    } else if (k.field === 'Swap') {
      row.find('button.Swap').toggleClass('Active', isTrue(v));
    } else if (k.field === 'Name') {
      row.children('td.Name>input').val(v);
      row.attr('offname', v);
    } else if (k.field === 'League') {
      row.children('td.League>input').val(v);
    } else if (k.field === 'Cert') {
      row.children('td.Cert>select').val(v);
    }
  };

  WS.Register([gamePrefix + '.Nso', gamePrefix + '.Ref', gamePrefix + '.Name'], handleUpdate);

  return table;
}

function createOfficialRemoveDialog(prefix) {
  'use strict';
  var dialog = $('<div>').addClass('OfficialRemoveDialog');

  $('<a>').addClass('Title').text('Officials').appendTo(dialog);
  $('<br>').appendTo(dialog);

  var name = WS.state[prefix + '.Name'];
  var role = WS.state[prefix + '.Role'];
  $('<a>').addClass('Remove').text('Remove Official: ').appendTo(dialog);
  $('<a>').addClass('Target').text(name).appendTo(dialog);
  $('<br>').appendTo(dialog);
  if (role) {
    $('<a>')
      .addClass('Role')
      .text('(Role: ' + role + ')')
      .appendTo(dialog);
    $('<br>').appendTo(dialog);
  }

  $('<hr>').appendTo(dialog);
  $('<a>').addClass('AreYouSure').text('Are you sure?').appendTo(dialog);
  $('<br>').appendTo(dialog);

  $('<button>')
    .addClass('No')
    .text('No, keep this official.')
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
    title: 'Remove Official',
    modal: true,
    width: 700,
    close: function () {
      $(this).dialog('destroy').remove();
    },
  });
}
