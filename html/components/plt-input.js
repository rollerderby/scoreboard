function preparePltInputTable(element, gameId, teamId, mode, statsbookPeriod, alternateName) {
  /* Values supported for mode:
   * plt: Full LT and PT inputs with headers
   * pt: Full PT cells, no LT cells
   * lt: Full LT cells, no PT cells
   * edit: Layout as in the statsbook with all edit controls enabled
   * copyToStatsbook: Only PT cells that have to be manually typed in a WFTDA statsbook for the given period
   */

  'use strict';
  $(initialize);

  var periodNumber = null;
  var jamNumber = null;
  var totalPenalties = null;
  var totalPenaltyCount = 0;
  var tbody = null;

  function initialize() {
    if (alternateName == null) {
      alternateName = 'operator';
    }

    var table = $('<table cellpadding="0" cellspacing="0" border="1">')
      .addClass('PLT Team')
      .addClass('AlternateName_' + alternateName)
      .appendTo(element);
    var thead = $('<tr>').appendTo($('<thead>').appendTo(table));
    if (mode === 'plt' || mode === 'lt') {
      if (mode === 'lt') {
        $('<td>')
          .attr('colspan', '5')
          .attr('id', 'head')
          .text('Team ' + teamId)
          .on('click', openOptionsDialog)
          .appendTo(thead);
      } else {
        $('<td>').text('Bench').appendTo(thead);
        $('<td>').text('Jammer').appendTo(thead);
        $('<td>').text('Pivot').appendTo(thead);
        $('<td>').text('Blocker').appendTo(thead);
        $('<td>').text('Box').appendTo(thead);
      }
      $('<td>')
        .attr('id', 'StarPass')
        .text('SP')
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').StarPass', !$(this).hasClass('Active'));
        })
        .appendTo(thead);
    }
    if (mode === 'plt' || mode === 'pt' || mode === 'edit') {
      $('<td>').text('#').appendTo(thead);
    }
    $('<td>')
      .attr('colspan', '9')
      .attr('id', 'head')
      .text('Team ' + teamId)
      .toggleClass('Hide', mode === 'lt')
      .on('click', openOptionsDialog)
      .appendTo(thead);
    $('<td>')
      .text('FO_Ex')
      .toggleClass('Hide', mode === 'lt')
      .appendTo(thead);
    totalPenalties = $('<td>').attr('id', 'totalPenalties').text('Σ 0');
    if (mode !== 'copyToStatsbook') {
      totalPenalties.appendTo(thead);
    }
    tbody = $('<tbody>').appendTo(table);

    WS.Register(
      [
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Name',
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor',
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').AlternateName(' + alternateName + ')',
      ],
      teamNameUpdate
    );

    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Color'], function (k, v) {
      element
        .find('#head')
        .css('background-color', WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Color(' + alternateName + '_bg)'] || '');
      element
        .find('#head')
        .css('color', WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Color(' + alternateName + '_fg)'] || '');
    });
    WS.Register(['ScoreBoard.Game(' + gameId + ').Clock(Period).Number'], updatePeriod);
    WS.Register(['ScoreBoard.Game(' + gameId + ').Clock(Jam).Number'], updateJam);

    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater'], function (k, v) {
      skaterUpdate(teamId, k, v);
    });
    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').FieldingAdvancePending'], function (k, v) {
      element.find('.Advance').toggleClass('Active', isTrue(v));
    });
    WS.Register(['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').StarPass'], function (k, v) {
      element.find('#StarPass').toggleClass('Active', isTrue(v));
    });

    WS.Register(['ScoreBoard.Game(' + gameId + ').Rule(Penalties.NumberToFoulout)']);

    WS.Register(['ScoreBoard.Game(' + gameId + ').Period(*).CurrentJam']);
    WS.Register(['ScoreBoard.Game(' + gameId + ').Period(*).Jam(*).Id']);
    WS.Register(['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber']);
    WS.Register(['ScoreBoard.Game(' + gameId + ').UpcomingJam']);
    if (mode === 'plt' || mode === 'lt') {
      prepareUseLTDialog();
      WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)'], function (k, v) {
        useLTDialog.dialog(isTrue(v) ? 'close' : 'open');
      });
    } else {
      WS.Register(['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']);
    }
  }

  function updatePeriod(k, v) {
    periodNumber = v;
    updateCurrentPeriodStyle();
    updateCurrentJamPeriodStyle();
  }

  function updateJam(k, v) {
    jamNumber = v;
    updateCurrentJamPeriodStyle();
  }

  function updateCurrentPeriodStyle() {
    if (periodNumber == null) {
      return;
    }

    $('#current-period-style').remove();
    if (mode !== 'copyToStatsbook') {
      $('<style> .Box[period="' + periodNumber + '"] { font-weight: bold; color: #000; }</style>')
        .attr('id', 'current-period-style')
        .appendTo('head');
    }
  }

  function updateCurrentJamPeriodStyle() {
    if (jamNumber == null || periodNumber == null) {
      return;
    }

    $('#current-jam-style').remove();
    if (mode !== 'copyToStatsbook') {
      $('<style> .Box[period="' + periodNumber + '"][jam="' + jamNumber + '"] { text-decoration: underline; } </style>')
        .attr('id', 'current-jam-style')
        .appendTo('head');
    }
  }

  function skaterUpdate(t, k, v) {
    if (k.Skater == null) {
      return;
    }

    var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + k.Skater + ')';
    var field = k.substring(prefix.length + 1);
    if (field === 'RosterNumber') {
      tbody
        .children('.Skater.Penalty[id=' + k.Skater + ']')
        .children('.Total')
        .each(function (idx, elem) {
          totalPenaltyCount -= parseInt($(elem).text(), 10);
        });
      totalPenalties.text('Σ ' + totalPenaltyCount);
      tbody.children('.Skater[id=' + k.Skater + ']').remove();
      if (v == null) {
        return;
      }

      // New skater, or number has been updated.
      makeSkaterRows(t, k.Skater, v);
      for (var i = 1; i <= 9; i++) {
        displayPenalty(t, k.Skater, i, null);
      }
      displayPenalty(t, k.Skater, 0, null);
    } else if (field === 'Role') {
      if (mode !== 'copyToStatsbook' && v === 'NotInGame') {
        tbody
          .children('.Skater.Penalty[id=' + k.Skater + ']')
          .children('.Total')
          .each(function (idx, elem) {
            totalPenaltyCount -= parseInt($(elem).text(), 10);
          });
        totalPenalties.text('Σ ' + totalPenaltyCount);
        tbody.children('.Skater[id=' + k.Skater + ']').remove();
        return;
      } else if (tbody.children('.Skater[id=' + k.Skater + ']').length === 0 && WS.state[prefix + '.RosterNumber'] != null) {
        makeSkaterRows(t, k.Skater, WS.state[prefix + '.RosterNumber']);
        if (WS.state[prefix + '.Penalty(0).Code'] != null) {
          displayPenalty(t, k.Skater, 0);
        }
        var p = 1;
        while (p < 10 && WS.state[prefix + '.Penalty(' + p + ').Code'] != null) {
          displayPenalty(t, k.Skater, p);
          p++;
        }
      }
      tbody.find('.Skater.Penalty[id=' + k.Skater + ']').attr('role', v);
      tbody.find('.Skater.Penalty[id=' + k.Skater + '] .Role').removeClass('OnTrack');
      tbody.find('.Skater.Penalty[id=' + k.Skater + '] .' + v).addClass('OnTrack');
      tbody.find('.Skater.Penalty[id=' + k.Skater + '] .Number').toggleClass('OnTrack', v === 'Jammer' || v === 'Pivot' || v === 'Blocker');
      tbody
        .find('.Skater.Penalty[id=' + k.Skater + '] .Advance')
        .toggleClass('OnTrack', v === 'Jammer' || v === 'Pivot' || v === 'Blocker');
    } else if (field === 'PenaltyBox') {
      element.find('.Skater.Penalty[id=' + k.Skater + '] .Sitting').toggleClass('inBox', isTrue(v));
    } else if (field === 'Flags') {
      element.find('.Skater[id=' + k.Skater + ']').toggleClass('Captain', v === 'C');
    } else {
      // Look for penalty
      if (k.Penalty == null) {
        return;
      }
      displayPenalty(t, k.Skater, k.Penalty, k);
    }
  }

  function displayPenalty(t, s, p, k) {
    var codeRow = tbody.children('.Skater.Penalty[id=' + s + ']');
    if (codeRow.length === 0) {
      return;
    }
    var jamRow = tbody.children('.Skater.Jam[id=' + s + ']');
    var penaltyBox = codeRow.children('.Box' + p);
    var jamBox = jamRow.children('.Box' + p);
    var totalBox = codeRow.children('.Total');

    var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + s + ').Penalty(' + p + ')';

    var field = '';
    if (k != null) {
      field = k.field;
    }

    var penaltyPeriod = WS.state[prefix + '.PeriodNumber'] || null;
    if (field === 'Code' || field === 'PeriodNumber' || field === '') {
      var code = WS.state[prefix + '.Code'];
      if (code != null) {
        if (mode === 'copyToStatsbook') {
          if (penaltyPeriod === statsbookPeriod) {
            penaltyBox.text(code);
          } else {
            penaltyBox.html(''); // &nbsp; would confuse the forumla to calculate number of penalties.
          }
        } else {
          penaltyBox.text(code);
        }
      } else {
        if (mode === 'copyToStatsbook') {
          penaltyBox.html('');
        } else {
          penaltyBox.html('&nbsp;');
        }
      }
    }

    if (field === 'JamNumber' || field === 'PeriodNumber' || field === '') {
      var penaltyJam = WS.state[prefix + '.JamNumber'] || null;
      penaltyBox.attr('period', penaltyPeriod);
      penaltyBox.attr('jam', penaltyJam);
      jamBox.attr('period', penaltyPeriod);
      jamBox.attr('jam', penaltyJam);
      if (mode === 'copyToStatsbook') {
        if (penaltyPeriod === statsbookPeriod) {
          jamBox.text(penaltyJam);
        } else {
          jamBox.html('');
        }
      } else if (penaltyJam != null) {
        jamBox.text(penaltyPeriod + '-' + penaltyJam);
      } else {
        if (mode === 'copyToStatsbook') {
          jamBox.html('');
        } else {
          jamBox.html('&nbsp;');
        }
      }
    }

    if (field === 'Id' || field === '') {
      var oldId = penaltyBox.attr('pid');
      var newId = WS.state[prefix + '.Id'] || null;
      if (oldId === newId) {
        return;
      }
      penaltyBox.attr('pid', newId);
      jamBox.attr('pid', newId);

      var cnt = 0; // Change row colors for skaters on 5 or more penalties, or expulsion.
      var limit = WS.state['ScoreBoard.Game(' + gameId + ').Rule(Penalties.NumberToFoulout)'];
      var fo_exp = $($('.PLT.Team .Skater.Penalty[id=' + s + '] .Box0')[0]).attr('pid') != null;

      codeRow.children('.Box:not(.Box0)').each(function (idx, elem) {
        cnt += $(elem).attr('pid') != null ? 1 : 0;
      });
      if (mode !== 'copyToStatsbook') {
        totalBox.text(cnt);
      }
      tbody.children('.Skater[id=' + s + ']').toggleClass('Warn1', cnt === limit - 2 && !fo_exp);
      tbody.children('.Skater[id=' + s + ']').toggleClass('Warn2', cnt === limit - 1 && !fo_exp);
      tbody.children('.Skater[id=' + s + ']').toggleClass('Warn3', cnt >= limit || fo_exp);

      if (p !== 0) {
        if (oldId == null && newId != null) {
          totalPenaltyCount++;
        } else if (oldId != null && newId == null) {
          totalPenaltyCount--;
        }
        if (totalPenalties != null) {
          totalPenalties.text('Σ ' + totalPenaltyCount);
        }
      }
    }

    if (field === 'Serving' || field === '') {
      jamBox.toggleClass('Serving', isTrue(WS.state[prefix + '.Serving']));
      penaltyBox.toggleClass('Serving', isTrue(WS.state[prefix + '.Serving']));
    }
    if (field === 'Served' || field === '') {
      jamBox.toggleClass('Unserved', WS.state[prefix + '.Served'] === false);
      penaltyBox.toggleClass('Unserved', WS.state[prefix + '.Served'] === false);
      var anyUnserved = false;
      codeRow.children('.Box:not(.Box0)').each(function (idx, elem) {
        anyUnserved = anyUnserved || $(elem).hasClass('Unserved');
      });
      codeRow.children('.Sitting').toggleClass('Unserved', anyUnserved);
    }
  }

  function teamNameUpdate() {
    var head = element.find('#head');
    var teamName = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Name'];

    if (alternateName === 'operator' && WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor'] != null) {
      teamName = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').UniformColor'];
    }

    if (WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').AlternateName(' + alternateName + ')'] != null) {
      teamName = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').AlternateName(' + alternateName + ')'];
    }

    head.text(teamName);
  }

  function makeSkaterRows(t, id, number) {
    var role = WS.state['ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').Role'];
    if (mode !== 'copyToStatsbook' && role === 'NotInGame') {
      return;
    }

    var p = $('<tr>').addClass('Skater Penalty').attr('id', id).attr('number', number).attr('role', role);
    var j = $('<tr>').addClass('Skater Jam').attr('id', id);

    if (mode === 'lt' || mode === 'plt') {
      var benchCell = $('<td>')
        .addClass('Role Bench')
        .attr('rowspan', 2)
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').Role', 'Bench');
        })
        .append($('<span>').addClass('Num').text(number))
        .append($('<span>').addClass('Pos').text('Bench'));
      if (role === 'Bench') {
        benchCell.addClass('OnTrack');
      }
      p.append(benchCell);

      var jammerCell = $('<td>')
        .addClass('Role Jammer')
        .attr('rowspan', 2)
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').Role', 'Jammer');
        })
        .append($('<span>').addClass('Num').text(number))
        .append($('<span>').addClass('Pos').text('J'));
      if (role === 'Jammer') {
        jammerCell.addClass('OnTrack');
      }
      p.append(jammerCell);

      var pivotCell = $('<td>')
        .addClass('Role Pivot')
        .attr('rowspan', 2)
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').Role', 'Pivot');
        })
        .append($('<span>').addClass('Num').text(number))
        .append($('<span>').addClass('Pos').text('P'));
      if (role === 'Pivot') {
        pivotCell.addClass('OnTrack');
      }
      p.append(pivotCell);

      var blockerCell = $('<td>')
        .addClass('Role Blocker')
        .attr('rowspan', 2)
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').Role', 'Blocker');
        })
        .append($('<span>').addClass('Num').text(number))
        .append($('<span>').addClass('Pos').text('B'));
      if (role === 'Blocker') {
        blockerCell.addClass('OnTrack');
      }
      p.append(blockerCell);

      var boxCell = $('<td>')
        .addClass('Sitting')
        .attr('rowspan', 2)
        .text('Box')
        .on('click', function () {
          WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').PenaltyBox', !$(this).hasClass('inBox'));
        });
      if (isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Team(' + t + ').Skater(' + id + ').PenaltyBox'])) {
        boxCell.addClass('inBox');
      }
      p.append(boxCell);

      var advanceCell = $('<td>')
        .addClass('Advance')
        .attr('rowspan', 2)
        .on('click', function () {
          if (isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Team(' + t + ').FieldingAdvancePending'])) {
            WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + t + ').AdvanceFieldings', true);
          } else if (advanceCell.hasClass('OnTrack')) {
            openAnnotationEditor(gameId, t, id);
          }
        });
      if (isTrue(WS.state['ScoreBoard.Game(' + gameId + ').Team(' + t + ').FieldingAdvancePending'])) {
        advanceCell.addClass('Active');
      }
      if (role === 'Jammer' || role === 'Pivot' || role === 'Blocker') {
        advanceCell.addClass('OnTrack');
      }
      p.append(advanceCell);
    }
    if (mode === 'plt' || mode === 'pt' || mode === 'edit') {
      var numberCell = $('<td>')
        .addClass('Number')
        .attr('rowspan', 2)
        .text(number)
        .on('click', function () {
          openPenaltyEditor(gameId, t, id, 9);
        });
      if (role === 'Jammer' || role === 'Pivot' || role === 'Blocker') {
        numberCell.addClass('OnTrack');
      }
      p.append(numberCell);
    }
    $.each(new Array(9), function (idx) {
      var c = idx + 1;
      p.append(
        $('<td>')
          .addClass('Box Box' + c)
          .toggleClass('Hide', mode === 'lt')
          .html('&nbsp;')
          .on('click', function () {
            openPenaltyEditor(gameId, t, id, c);
          })
      );
      j.append(
        $('<td>')
          .addClass('Box Box' + c)
          .toggleClass('Hide', mode === 'lt')
          .html('&nbsp;')
          .on('click', function () {
            openPenaltyEditor(gameId, t, id, c);
          })
      );
    });

    p.append(
      $('<td>')
        .addClass('Box Box0')
        .toggleClass('Hide', mode === 'lt')
        .html('&nbsp;')
        .on('click', function () {
          openPenaltyEditor(gameId, t, id, 0);
        })
    );
    j.append(
      $('<td>')
        .addClass('Box Box0')
        .toggleClass('Hide', mode === 'lt')
        .html('&nbsp;')
        .on('click', function () {
          openPenaltyEditor(gameId, t, id, 0);
        })
    );
    if (mode !== 'copyToStatsbook') {
      p.append($('<td>').attr('rowspan', 2).addClass('Total').text('0'));
    }

    var inserted = false;
    tbody.find('tr.Penalty').each(function (idx, row) {
      row = $(row);
      if (row.attr('number') > number) {
        row.before(p).before(j);
        inserted = true;
        return false;
      }
    });
    if (!inserted) {
      tbody.append(p).append(j);
    }
  }
}

function openPenaltyEditor(g, t, id, which) {
  'use strict';
  var prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ')';
  var teamName = WS.state[prefix + '.AlternateName(operator)'] || WS.state[prefix + '.UniformColor'];
  if (teamName == null || teamName == '') {
    teamName = WS.state[prefix + '.Name'];
  }

  prefix = 'ScoreBoard.Game(' + g + ').Team(' + t + ').Skater(' + id + ')';
  var skaterName = WS.state[prefix + '.Name'];
  var skaterNumber = WS.state[prefix + '.RosterNumber'];

  var penaltyNumber = which;
  var penaltyId = null;
  var wasServed = !isTrue(WS.state['ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)']);

  $('#PenaltyEditor .Codes>div').removeClass('Active');

  penaltyEditor.dialog('option', 'title', teamName + ' ' + skaterNumber + ' (' + skaterName + ')');
  var periodNumber = WS.state['ScoreBoard.Game(' + g + ').CurrentPeriodNumber'];
  $('#PenaltyEditor .Period').val(periodNumber).trigger('change');
  $('#PenaltyEditor .Jam').val(WS.state['ScoreBoard.Game(' + g + ').Period(' + periodNumber + ').CurrentJam']);
  $('#PenaltyEditor #served').removeClass('checked');

  $('#PenaltyEditor .Codes>.Penalty').toggle(which !== 0);
  $('#PenaltyEditor .Codes>.FO_EXP').toggle(which === 0);

  var penaltyBox = $('.PLT.Team .Skater.Penalty[id=' + id + '] .Box' + which);
  var c = WS.state[prefix + '.Penalty(' + which + ').Code'];
  var p = WS.state[prefix + '.Penalty(' + which + ').PeriodNumber'];
  var j = WS.state[prefix + '.Penalty(' + which + ').Jam'];
  penaltyId = penaltyBox.attr('pid');
  var isNew = true;
  if (penaltyId != null) {
    if (c == null || j == null || p == null) {
      penaltyId = null;
    } else {
      wasServed = isTrue(WS.state[prefix + '.Penalty(' + which + ').Served']);
      $('#PenaltyEditor .Codes>div.' + (which === 0 ? 'FO_EXP' : 'Penalty') + '[code="' + c + '"]').addClass('Active');
      $('#PenaltyEditor .Period').val(p).trigger('change');
      $('#PenaltyEditor .Jam').val(j);
      isNew = false;
    }
  }
  $('#PenaltyEditor .clear').button();
  $('#PenaltyEditor #served').toggleClass('checked', wasServed);
  $('#PenaltyEditor .set').toggleClass('Hide', isNew);
  while (!isNaN(penaltyNumber) && penaltyNumber > 1 && WS.state[prefix + '.Penalty(' + (penaltyNumber - 1) + ').Code'] == null) {
    penaltyNumber--;
  }

  penaltyEditor.data({
    team: t,
    skater: id,
    pnr: penaltyNumber,
    pid: penaltyId,
    wasServed: wasServed,
    new: isNew,
  });
  penaltyEditor.dialog('open');
}

var penaltyEditor = null;

function preparePenaltyEditor(gameId) {
  'use strict';
  $(initialize);

  function initialize() {
    var topTable = $('<table>').appendTo($('#PenaltyEditor'));
    var tr = $('<tr>').appendTo(topTable);
    $('<td>')
      .append($('<span>').text('Period: '))
      .append($('<button>').addClass('period_minus small').text('-1').button())
      .append($('<select>').addClass('Period'))
      .append($('<button>').addClass('period_plus small').text('+1').button())
      .appendTo(tr);
    $('<td>')
      .append($('<span>').text('Jam: '))
      .append($('<button>').addClass('jam_minus small').text('-1').button())
      .append($('<select>').addClass('Jam'))
      .append($('<button>').addClass('jam_plus small').text('+1').button())
      .appendTo(tr);
    $('<td>').append($('<button>').addClass('set Hide').text('Set Period/Jam').button()).appendTo(tr);

    $('<div>').addClass('Codes').appendTo($('#PenaltyEditor'));

    var bottomTable = $('<table width="100%">').appendTo($('#PenaltyEditor'));
    var tr2 = $('<tr>').appendTo(bottomTable);
    $('<td width="50%">')
      .append(
        $('<button>')
          .text('Served')
          .attr('id', 'served')
          .button()
          .on('click', function () {
            var active = !$(this).hasClass('checked');
            $(this).toggleClass('checked', active);
            if (!isTrue(penaltyEditor.data('new'))) {
              var teamId = penaltyEditor.data('team');
              var skaterId = penaltyEditor.data('skater');
              var penaltyNumber = penaltyEditor.data('pnr');
              var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').Penalty(' + penaltyNumber + ')';
              WS.Set(prefix + '.ForceServed', active);
              penaltyEditor.dialog('close');
            }
          })
      )
      .appendTo(tr2);
    $('<td width="50%">').append($('<button>').addClass('clear').text('Delete').button()).appendTo(tr2);

    WS.Register(['ScoreBoard.Game(' + gameId + ').PenaltyCode'], penaltyCode);
    WS.Register(['ScoreBoard.Game(' + gameId + ').Rule(Period.Number)'], function (k, v) {
      setupPeriodSelect(v);
    });

    penaltyEditor = $('#PenaltyEditor').dialog({
      modal: true,
      closeOnEscape: false,
      title: 'Penalty Editor',
      autoOpen: false,
      width: '80%',
    });

    addFOCode();

    $('#PenaltyEditor .period_minus').on('click', function () {
      adjust('Period', 'prev');
    });
    $('#PenaltyEditor .period_plus').on('click', function () {
      adjust('Period', 'next');
    });
    $('#PenaltyEditor .jam_minus').on('click', function () {
      adjust('Jam', 'prev');
    });
    $('#PenaltyEditor .jam_plus').on('click', function () {
      adjust('Jam', 'next');
    });
    $('#PenaltyEditor .clear').on('click', function () {
      clear();
    });
    $('#PenaltyEditor .set').on('click', function () {
      if (!isTrue(penaltyEditor.data('new'))) {
        submitPenalty();
      }
    });
    $('#PenaltyEditor .Period').on('change', function () {
      setupJamSelect();
    });
  }

  function adjust(which, inc) {
    $('#PenaltyEditor .' + which + ' :selected')
      [inc]()
      .prop('selected', true);
    if (which === 'Period') {
      setupJamSelect();
    }
  }

  function submitPenalty() {
    var teamId = penaltyEditor.data('team');
    var skaterId = penaltyEditor.data('skater');
    var penaltyNumber = penaltyEditor.data('pnr');
    var wasServed = penaltyEditor.data('wasServed');

    var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').Penalty(' + penaltyNumber + ')';
    WS.Set(prefix + '.Code', $('#PenaltyEditor .Codes .Active').attr('code'));
    if (isTrue($('#PenaltyEditor #served').hasClass('checked'))) {
      WS.Set(prefix + '.ForceServed', true);
    }
    WS.Set(prefix + '.Jam', $('#PenaltyEditor .Jam').val());

    penaltyEditor.dialog('close');
  }

  function clear() {
    var teamId = penaltyEditor.data('team');
    var skaterId = penaltyEditor.data('skater');
    var penaltyId = penaltyEditor.data('pid');
    var penaltyNumber = penaltyEditor.data('pnr');

    if (penaltyId == null || skaterId == null) {
      penaltyEditor.dialog('close');
    } else {
      WS.Set('ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').Penalty(' + penaltyNumber + ').Remove', true);
      penaltyEditor.dialog('close');
    }
  }

  var codeIdRegex = /Code\(([^\)]+)\)/;
  function penaltyCode(k, v) {
    var match = (k || '').match(codeIdRegex);
    if (match == null || match.length === 0) {
      return;
    }

    var code = match[1];

    addPenaltyCode('Penalty', code, v);
    addPenaltyCode('FO_EXP', code, v);
  }

  function addFOCode() {
    addPenaltyCode('FO_EXP', 'FO', 'Foul-Out');
  }

  function addPenaltyCode(type, code, verbalCues) {
    var div = $('#PenaltyEditor .Codes .' + type + '[code="' + code + '"]');

    if (verbalCues == null) {
      div.remove();
      return;
    } else if (div.length > 0) {
      div.find('.Description').empty();
    } else {
      div = $('<div>')
        .attr('code', code)
        .addClass(type)
        .on('click', function (e) {
          $('#PenaltyEditor .Codes>div').removeClass('Active');
          div.addClass('Active');
          submitPenalty();
        });

      var title = code;

      if (type === 'FO_EXP' && code !== 'FO') {
        title = title + '(EXP)';
      }

      $('<div>').addClass('Code').text(title).appendTo(div);
      $('<div>').addClass('Description').appendTo(div);

      var codes = $('#PenaltyEditor .Codes');
      var inserted = false;
      codes.children().each(function (idx, c) {
        c = $(c);
        if (c.attr('code') > code) {
          c.before(div);
          inserted = true;
          return false;
        }
      });
      if (!inserted) {
        codes.append(div);
      }
    }

    var desc = div.find('.Description');
    verbalCues.split(',').forEach(function (d) {
      $('<div>').text(d).appendTo(desc);
    });
  }

  function setupPeriodSelect(num) {
    var select = $('#PenaltyEditor .Period');
    select.empty();
    for (var i = 1; i <= num; i++) {
      $('<option>').attr('value', i).text(i).appendTo(select);
    }
  }

  function setupJamSelect() {
    var p = $('#PenaltyEditor .Period').val();
    var prefix = 'ScoreBoard.Game(' + gameId + ').Period(' + p + ').';
    var min = WS.state[prefix + 'CurrentJamNumber'];
    if (min == null) {
      min = 1;
    }
    while (WS.state[prefix + 'Jam(' + (min - 1) + ').Id'] != null) {
      min--;
    }
    var max = WS.state[prefix + 'CurrentJamNumber'];
    if (max == null) {
      max = 0;
    }
    while (WS.state[prefix + 'Jam(' + (max + 1) + ').Id'] != null) {
      max++;
    }
    var select = $('#PenaltyEditor .Jam');
    select.empty();
    for (var i = min; i <= max; i++) {
      $('<option>')
        .attr('value', WS.state[prefix + 'Jam(' + i + ').Id'])
        .text(i)
        .appendTo(select);
    }
    if (p >= WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber']) {
      $('<option>')
        .attr('value', WS.state['ScoreBoard.Game(' + gameId + ').UpcomingJam'])
        .text(max + 1)
        .appendTo(select);
    }
  }
}

var annotationEditor = {};

function openAnnotationEditor(gameId, teamId, skaterId) {
  'use strict';
  var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(' + skaterId + ').';
  var skaterNumber = WS.state[prefix + 'RosterNumber'];
  var position = WS.state[prefix + 'Position'];
  position = position.slice(position.lastIndexOf('_') + 1);
  var fieldingPrefix = ').TeamJam(' + teamId + ').Fielding(' + position + ').';
  if (isTrue(WS.state['ScoreBoard.Game(' + gameId + ').InJam'])) {
    fieldingPrefix =
      'ScoreBoard.Game(' +
      gameId +
      ').Period(' +
      WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] +
      ').Jam(' +
      WS.state[
        'ScoreBoard.Game(' + gameId + ').Period(' + WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] + ').CurrentJamNumber'
      ] +
      fieldingPrefix;
  } else {
    fieldingPrefix =
      'ScoreBoard.Game(' + gameId + ').Jam(' + WS.state['ScoreBoard.Game(' + gameId + ').UpcomingJamNumber'] + fieldingPrefix;
  }
  annotationEditor[teamId].data('skaterNumber', skaterNumber);
  annotationEditor[teamId].data('position', position);
  annotationEditor[teamId].find('#subDropdown').val(skaterId);
  annotationEditor[teamId].find('#annotation').val(WS.state[fieldingPrefix + 'Annotation']);
  annotationEditor[teamId].find('.Box').toggleClass('Hide', WS.state[fieldingPrefix + 'CurrentBoxTrip'] === '');
  annotationEditor[teamId].find('.Box .Current').toggleClass('Hide', !isTrue(WS.state[prefix + 'PenaltyBox']));
  annotationEditor[teamId].find('.Box .Past').toggleClass('Hide', isTrue(WS.state[prefix + 'PenaltyBox']));
  annotationEditor[teamId].dialog('open');
}

function prepareAnnotationEditor(elem, gameId, teamId) {
  'use strict';
  $(initialize);

  var subDropdown = $('<select>').attr('id', 'subDropdown');
  var annotationField = $('<input type="text">').attr('size', '40').attr('id', 'annotation');
  var jamPrefix;

  function initialize() {
    var table = $('<table>').appendTo(elem);
    var row = $('<tr>').addClass('Box').appendTo(table);
    $('<td>')
      .append(
        $('<button>')
          .addClass('Past')
          .text('Unend Box Trip')
          .button()
          .on('click', function () {
            var prefix = jamPrefix + elem.data('position') + ').';
            WS.Set(prefix + 'UnendBoxTrip', true);
            elem.dialog('close');
          })
      )
      .append(subDropdown.addClass('Current'))
      .append(
        $('<button>')
          .addClass('Current')
          .text('Substitute')
          .button()
          .on('click', function () {
            var prefix = jamPrefix + elem.data('position') + ').';
            WS.Set(prefix + 'Skater', subDropdown.val());
            WS.Set(prefix + 'Annotation', 'Substitute for #' + elem.data('skaterNumber'));
            elem.dialog('close');
          })
      )
      .appendTo(row);
    $('<td>')
      .append(
        $('<button>')
          .text('No Penalty')
          .button()
          .on('click', function () {
            leaveBox('No Penalty');
          })
      )
      .appendTo(row);
    $('<td>')
      .append(
        $('<button>')
          .text('Penalty Overturned')
          .button()
          .on('click', function () {
            leaveBox('Penalty Overturned');
          })
      )
      .appendTo(row);

    row = $('<tr>').appendTo(table);
    $('<td>')
      .attr('colspan', '3')
      .append(
        annotationField.on('change', function () {
          var prefix = jamPrefix + elem.data('position') + ').';
          WS.Set(prefix + 'Annotation', $(this).val());
          elem.dialog('close');
        })
      )
      .appendTo(row);

    WS.Register([
      'ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber',
      'ScoreBoard.Game(' + gameId + ').UpcomingJamNumber',
      'ScoreBoard.Game(' + gameId + ').Period(*).CurrentJamNumber',
    ]);
    WS.Register(['ScoreBoard.Game(' + gameId + ').InJam'], function (k, v) {
      if (isTrue(v)) {
        jamPrefix =
          'ScoreBoard.Game(' +
          gameId +
          ').Period(' +
          WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] +
          ').Jam(' +
          WS.state[
            'ScoreBoard.Game(' +
              gameId +
              ').Period(' +
              WS.state['ScoreBoard.Game(' + gameId + ').CurrentPeriodNumber'] +
              ').CurrentJamNumber'
          ] +
          ').TeamJam(' +
          teamId +
          ').Fielding(';
      } else {
        jamPrefix =
          'ScoreBoard.Game(' +
          gameId +
          ').Jam(' +
          WS.state['ScoreBoard.Game(' + gameId + ').UpcomingJamNumber'] +
          ').TeamJam(' +
          teamId +
          ').Fielding(';
      }
    });

    WS.Register(
      [
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(*).Role',
        'ScoreBoard.Game(' + gameId + ').Team(' + teamId + ').Skater(*).RosterNumber',
      ],
      function (k, v) {
        processSkater(k, v);
      }
    );

    annotationEditor[teamId] = elem.dialog({
      modal: true,
      closeOnEscape: false,
      title: 'Annotation Editor',
      autoOpen: false,
      width: '500px',
    });
  }

  function leaveBox(annotation) {
    if (annotationField.val() !== '') {
      annotation = '; ' + annotation;
    }
    annotationField.val(annotationField.val() + annotation);
    var prefix = jamPrefix + elem.data('position') + ').';
    WS.Set(prefix + 'Annotation', annotationField.val());
    WS.Set(prefix + 'PenaltyBox', false);
    elem.dialog('close');
  }

  function processSkater(k, v) {
    var select = elem.find('#subDropdown');
    select.children('[value="' + k.Skater + '"]').remove();
    var prefix = 'ScoreBoard.Game(' + gameId + ').Team(' + k.Team + ').Skater(' + k.Skater + ').';
    if (v != null && WS.state[prefix + 'Role'] !== 'NotInGame') {
      var number = WS.state[prefix + 'RosterNumber'];
      var option = $('<option>').attr('number', number).val(k.Skater).text(number);
      _windowFunctions.appendAlphaSortedByAttr(select, option, 'number');
    }
  }
}

var optionsDialog;

function openOptionsDialog() {
  'use strict';
  optionsDialog.dialog('open');
}

function prepareOptionsDialog(gameId, teamId, onlySettings) {
  'use strict';
  var table = $('<table>').appendTo($('#OptionsDialog'));

  var zoomable = $('<label/><input type="checkbox"/>').addClass('ui-button-small');
  var id = newUUID();
  zoomable.first().attr('for', id);
  var zoomInput = zoomable.last().attr('id', id).button();
  zoomInput.prop('checked', _windowFunctions.checkParam('zoomable', 1));
  zoomInput.button('option', 'label', 'Pinch Zoom ' + (zoomInput.prop('checked') ? 'Enabled' : 'Disabled'));
  zoomInput.on('change', function (e) {
    zoomInput.button('option', 'label', 'Pinch Zoom ' + (zoomInput.prop('checked') ? 'Enabled' : 'Disabled'));
  });
  zoomInput.trigger('change');

  if (!onlySettings) {
    teamId = teamId === '2' ? '2' : '1'; // Ensure we start with a sane value.
    $('<tr>').append($('<th>').text('Select Team')).appendTo(table);
    $.each(['1', '2'], function () {
      var tId = String(this);
      var row = $('<tr>')
        .addClass('selectTeam' + tId)
        .appendTo(table);
      $('<td>')
        .append(
          $('<button>')
            .attr('team', tId)
            .addClass('name')
            .toggleClass('selected', tId === teamId)
            .button()
            .on('click', function () {
              teamId = tId;
              table.find('[team]').removeClass('selected');
              table.find('[team="' + tId + '"]').addClass('selected');
            })
        )
        .appendTo(row);
    });
    $('<tr>').append($('<th>').text('Options')).appendTo(table);

    WS.Register(
      [
        'ScoreBoard.Game(' + gameId + ').Team(*).Name',
        'ScoreBoard.Game(' + gameId + ').Team(*).UniformColor',
        'ScoreBoard.Game(' + gameId + ').Team(*).AlternateName(operator)',
      ],
      function (k, v) {
        var displayName = 'Team ' + k.Team + ': ' + WS.state['ScoreBoard.Game(' + gameId + ').Team(' + k.Team + ').Name'];
        var altName =
          WS.state['ScoreBoard.Game(' + gameId + ').Team(' + k.Team + ').AlternateName(operator)'] ||
          WS.state['ScoreBoard.Game(' + gameId + ').Team(' + k.Team + ').UniformColor'];
        if (altName != null && altName !== '') {
          displayName = displayName + ' / ' + altName;
        }
        $('.selectTeam' + k.Team + ' .name').text(displayName);
      }
    );
  }
  $('<tr>').append($('<td>').append(zoomable)).appendTo(table);

  var setURL = function () {
    var updated = window.location.href.replace(/[\?#].*|$/, '?zoomable=' + (zoomInput.prop('checked') ? 1 : 0));
    if (!onlySettings) {
      updated = updated + '&team=' + teamId;
    }
    updated = updated + '&game=' + gameId;
    if (updated !== window.location.href) {
      window.location.href = updated;
      optionsDialog.dialog('close');
    }
  };

  optionsDialog = $('#OptionsDialog').dialog({
    modal: true,
    closeOnEscape: true,
    title: 'Option Editor',
    buttons: [{ text: 'Save', click: setURL }],
    width: '500px',
    autoOpen: false,
  });
}

var useLTDialog;

function prepareUseLTDialog() {
  'use strict';
  useLTDialog = $('#UseLTDialog')
    .text('Use of Lineup Tracking is disabled.')
    .dialog({
      modal: true,
      closeOnEscape: true,
      title: 'Use Lineup Tracking',
      buttons: [
        {
          text: 'Enable',
          click: function () {
            WS.Set('ScoreBoard.Settings.Setting(ScoreBoard.Penalties.UseLT)', true);
            useLTDialog.dialog('close');
          },
        },
      ],
      width: '300px',
      autoOpen: false,
    });
}

function setupGameAdvance(element, gameId, auto) {
  'use strict';
  element
    .addClass('Hide GameAdvance')
    .text('Go To Current Game')
    .click(function () {
      window.location.href = window.location.href.replace(/game=[^&]*(&|$)|$/, 'game=' + WS.state['ScoreBoard.CurrentGame.Game']);
    });

  WS.Register('ScoreBoard.CurrentGame.Game', function (k, v) {
    var change = !(v === gameId || v == null);
    element.toggleClass('Hide', !change);
    if (auto && change) {
      element.click();
    }
  });
}
