function prepareLtSheetTable(element, teamId, mode) {

  'use strict';
  $(initialize);

  var teamName = '';
  // Looking these up via the DOM is slow, so cache them.
  var periodElements = {};
  var jamElements = {};

  function initialize() {

    if (mode !== 'plt') {
      WS.Register([ 'ScoreBoard.Team(' + teamId + ').Name' ], function () {
        teamNameUpdate();
      });
      WS.Register([ 'ScoreBoard.Team(' + teamId + ').AlternateName(operator)' ], function () {
        teamNameUpdate();
      });

      WS.Register([ 'ScoreBoard.Team(' + teamId + ').Color' ], function (k, v) {
        element.find('#head').css('background-color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_bg)']);
        element.find('#head').css('color', WS.state['ScoreBoard.Team(' + teamId + ').Color(operator_fg)']);
      });
    }

    // for fielding editor
    WS.Register([ 'ScoreBoard.Jam(*).TeamJam(' + teamId + ').NoPivot',
      'ScoreBoard.Jam(*).TeamJam(' + teamId + ').StarPass',
      'ScoreBoard.Jam(*).TeamJam(' + teamId + ').Fielding(*)' ]);
    WS.Register([ 'ScoreBoard.Period(*).Number',
      'ScoreBoard.Period(*).Jam(*).Number',
      'ScoreBoard.Period(*).Jam(*).StarPass',
      'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').NoPivot',
      'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').StarPass',
      'ScoreBoard.Period(*).Jam(*).TeamJam(' + teamId + ').Fielding(*)',
      ], handleUpdate);

    if (mode === 'plt') {
      WS.Register([ 'ScoreBoard.UpcomingJamNumber' ], function (k, v) {
        element.find('#upcoming .JamNumber').text(v);
      });
      WS.Register([ 'ScoreBoard.Team(' + teamId + ').NoPivot' ], function (k, v) {
        element.find('#upcoming .NP').text(isTrue(v) ? 'X' : '');
      });

      WS.Register([ 'ScoreBoard.InJam', 'ScoreBoard.CurrentPeriodNumber' ], function () {
        for ( var p in periodElements) {
          periodElements[p].find('#upcoming').toggleClass(
              'Hide',
              isTrue(WS.state['ScoreBoard.InJam']) ||
              WS.state['ScoreBoard.CurrentPeriodNumber'] !== p);
        }
      });

      WS.Register([ 'ScoreBoard.Team(' + teamId + ').Position(*).RosterNumber',
        'ScoreBoard.Team(' + teamId + ').Position(*).CurrentBoxSymbols',
        'ScoreBoard.Team(' + teamId + ').Position(*).Annotation' ], function (k, v) {
        if (k.field === 'RosterNumber') {
          element.find('#upcoming .Skater.' + k.Position).text(v);
        } else if (k.field === 'CurrentBoxSymbols') {
          element.find('#upcoming .Box.Box' + k.Position).text(v);
        } else if (k.field === 'Annotation') {
          element.find('#upcoming .Skater.' + k.Position).toggleClass('hasAnnotation', v !== '');
        }
      });
    }
  }

  function teamNameUpdate() {
    teamName = WS.state['ScoreBoard.Team(' + teamId + ').Name'];

    if (WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator)'] !== null) {
      teamName = WS.state['ScoreBoard.Team(' + teamId + ').AlternateName(operator)'];
    }

    element.find('#head .Team').text(teamName);
  }

  function handleUpdate(k, v) {
    if (!k.Period || k.Period === 0) { return; }
    if (v === null && k === 'ScoreBoard.Period(' + k.Period + ').Number') {
      element.children('table.Period[nr=' + k.Period + ']').remove();
      delete periodElements[k.Period];
      delete jamElements[k.Period];
    } else if (v !== null) {
      createPeriod(k.Period);
    }
    if (!k.Jam || k.Jam === 0) { return; }
    var prefix = 'ScoreBoard.Period(' + k.Period + ').Jam(' + k.Jam + ').';
    if (v === null && k === prefix + 'Number') {
      element.children('table.Period[nr=' + k.Period + ']').find('tr[nr=' + k.Jam + ']').remove();
      delete jamElements[k.Period][k.Jam];
    } else if (v !== null) {
      createJam(k.Period, k.Jam);
    }

    var je = (jamElements[k.Period] || {})[k.Jam];
    if (je === null) { return; }
    var jamRow = je[0];
    var spRow = je[1];
    if (k === prefix + 'StarPass') {
      if (isTrue(v)) {
        if (mode === 'plt') {
          jamRow.before(spRow);
        } else {
          jamRow.after(spRow);
        }
      } else {
        spRow.detach();
      }
    }

    // Everything after here is team specific.
    if (k.TeamJam !== teamId) { return; }
    prefix = prefix + 'TeamJam(' + teamId + ').';
    switch (k.substring(prefix.length)) {
    case 'NoPivot':
      jamRow.find('.NP').text(isTrue(v) ? 'X' : '');
      break;
    case 'StarPass':
      spRow.children('.JamNumber').text(isTrue(v) ? 'SP' : 'SP*');
      spRow.children('.NP').text(isTrue(v) ? 'X' : '');
      $.each([ 'Jammer', 'Pivot', 'Blocker1', 'Blocker2', 'Blocker3' ], function () {
        var pos = String(this);
        spRow.children('.' + pos).text(isTrue(v) ? WS.state[prefix + 'Fielding(' + pos + ').SkaterNumber'] : '');
        setBoxTripSymbols(spRow, '.Box' + pos, isTrue(v) ? WS.state[prefix + 'Fielding(' + pos + ').BoxTripSymbolsAfterSP'] : '');
      });
      break;
    default:
      if (k.Fielding !== null) {
        if (k.SkaterNumber !== null) {
          jamRow.children('.' + k.Fielding).text(v);
          if (isTrue(WS.state[prefix + 'StarPass'])) {
            spRow.children('.' + k.Fielding).text(v);
          }
        } else if (k.BoxTripSymbolsBeforeSP !== null) {
          setBoxTripSymbols(jamRow, '.Box' + k.Fielding, v);
        } else if (k.BoxTripSymbolsAfterSP !== null) {
          if (isTrue(WS.state[prefix + 'StarPass'])) {
            setBoxTripSymbols(spRow, '.Box' + k.Fielding, v);
          }
        } else if (k.Annotation !== null) {
          jamRow.children('.' + k.Fielding).toggleClass('hasAnnotation', v !== null && v !== '');
        }
      }
    break;
    }
  }

  function createPeriod(nr) {
    if (nr > 0 && periodElements[nr] === null) {
      createPeriod(nr - 1);
      var table = $('<table cellpadding="0" cellspacing="0" border="1">').addClass('Period LT').attr('nr', nr);
      if (mode === 'plt') {
        table.prependTo(element).addClass('Backwards');
      } else {
        table.appendTo(element).addClass('Forewards');
      }
      if (mode !== 'plt') {
        $('<div class="LT">').html('<span class ="Team">' + teamName + '</span> P' + nr).prop('id', 'head').insertBefore(table);
        var header = $('<thead>').appendTo(table);
        var row = $('<tr>').appendTo(header);
        if (mode !== 'copyToStatsbook') {
          $('<td>').addClass('JamNumber').text('Jam').appendTo(row);
          $('<td>').addClass('NP').text('NP').appendTo(row);
          $('<td>').addClass('Skater').text('Jammer').appendTo(row);
        }
        $('<td>').addClass('Box').attr('colspan', mode === 'copyToStatsbook' ? '3' : '1').text('Box').appendTo(row);
        $('<td>').addClass('Skater').text('Pivot').appendTo(row);
        $('<td>').addClass('Box').attr('colspan', mode === 'copyToStatsbook' ? '3' : '1').text('Box').appendTo(row);
        $('<td>').addClass('Skater').text('Blocker').appendTo(row);
        $('<td>').addClass('Box').attr('colspan', mode === 'copyToStatsbook' ? '3' : '1').text('Box').appendTo(row);
        $('<td>').addClass('Skater').text('Blocker').appendTo(row);
        $('<td>').addClass('Box').attr('colspan', mode === 'copyToStatsbook' ? '3' : '1').text('Box').appendTo(row);
        $('<td>').addClass('Skater').text('Blocker').appendTo(row);
        $('<td>').addClass('Box').attr('colspan', mode === 'copyToStatsbook' ? '3' : '1').text('Box').appendTo(row);
      }
      var body = $('<tbody>').appendTo(table);
      periodElements[nr] = body;
      jamElements[nr] = {};

      if (mode === 'plt') {
        var jamRow = $('<tr>').addClass('Jam').attr('id', 'upcoming');
        var jamBox = $('<td>').addClass('JamNumber Darker').appendTo(jamRow);
        $('<td>').addClass('NP Darker').appendTo(jamRow);
        $.each([ 'Jammer', 'Pivot', 'Blocker1', 'Blocker2', 'Blocker3' ], function () {
          var pos = String(this);
          $('<td>').addClass('Skater ' + pos).on('click', function () {
            openFieldingEditor(nr, jamBox.text(), teamId, pos, true);
          }).appendTo(jamRow);
          $('<td>').addClass('Box Box' + pos).on('click', function () {
            openFieldingEditor(nr, jamBox.text(), teamId, pos, true);
          }).appendTo(jamRow);

        });
        jamRow.prependTo(body);
      }
    }
  }

  function createJam(p, nr) {

    var table = periodElements[p];
    if (nr > 0 && jamElements[p][nr] === null) {
      createJam(p, nr - 1);

      var prefix = 'ScoreBoard.Period(' + p + ').Jam(' + nr + ').TeamJam(' + teamId + ').';

      var jamRow = $('<tr>').addClass('Jam').attr('nr', nr);
      if (mode !== 'copyToStatsbook') {
        $('<td>').addClass('JamNumber Darker').text(nr).appendTo(jamRow);
        $('<td>').addClass('NP Darker').on('click', function () {
          WS.Set(prefix + 'NoPivot', $(this).text() === '');
        }).appendTo(jamRow);
      }
      $.each([ 'Jammer', 'Pivot', 'Blocker1', 'Blocker2', 'Blocker3' ], function () {
        var pos = String(this);
        if (mode === 'copyToStatsbook') {
          $('<td>').addClass('Skater ' + pos).appendTo(jamRow);
          $('<td>').addClass('Box Box' + pos + '_1').appendTo(jamRow);
          $('<td>').addClass('Box Box' + pos + '_2').appendTo(jamRow);
          $('<td>').addClass('Box Box' + pos + '_3').appendTo(jamRow);
        } else {
          $('<td>').addClass('Skater ' + pos).on('click', function () {
            openFieldingEditor(p, nr, teamId, pos);
          }).appendTo(jamRow);
          $('<td>').addClass('Box Box' + pos).on('click', function () {
            openFieldingEditor(p, nr, teamId, pos);
          }).appendTo(jamRow);
        }
      });

      var spRow = jamRow.clone(true).removeClass('Jam').addClass('SP');
      spRow.children('.Jammer').insertBefore(spRow.children('.Blocker1'));
      spRow.children('.BoxJammer').insertAfter(spRow.children('.Jammer'));
      spRow.children('.BoxJammer_3').insertAfter(spRow.children('.Jammer'));
      spRow.children('.BoxJammer_2').insertAfter(spRow.children('.Jammer'));
      spRow.children('.BoxJammer_1').insertAfter(spRow.children('.Jammer'));

      if (mode === 'copyToStatsbook') {
        jamRow.children('.Jammer').addClass('Hide');
        spRow.children('.Pivot').addClass('Hide');
      }
      jamElements[p][nr] = [ jamRow, spRow ];

      if (mode === 'plt') {
        table.find('#upcoming').after(jamRow);
      } else {
        table.append(jamRow);
      }
    }
  }

  function setBoxTripSymbols(row, classPrefix, symbols) {
    if (mode === 'copyToStatsbook') {
      var syms = symbols.split(' ');
      row.find(classPrefix + '_1').text(syms[1]);
      row.find(classPrefix + '_2').text(syms[2]);
      row.find(classPrefix + '_3').text(syms.slice(3).join(''));
    } else {
      row.find(classPrefix).text(symbols);
    }
  }
}

var fieldingEditor;

function openFieldingEditor(p, j, t, pos, upcoming) {
  var prefix = 'ScoreBoard.' + (isTrue(upcoming) ? '' : 'Period(' + p + ').') + 'Jam(' + j + ').TeamJam(' + t + ').Fielding(' + pos + ').';

  fieldingEditor.dialog('option', 'title', 'Period ' + p + ' Jam ' + j + ' ' + (pos));
  fieldingEditor.find('#skater').val(WS.state[prefix + 'Skater']);
  fieldingEditor.find('#notFielded').attr('checked', isTrue(WS.state[prefix + 'NotFielded']));
  fieldingEditor.find('#sitFor3').attr('checked', isTrue(WS.state[prefix + 'SitFor3']));
  fieldingEditor.find('#annotation').val(WS.state[prefix + 'Annotation']);
  fieldingEditor.find('#notFielded').toggleClass('Hide', isTrue(upcoming));
  fieldingEditor.find('.BoxTripComments').toggleClass('Hide', WS.state[prefix + 'CurrentBoxTrip'] === '');
  fieldingEditor.find('.BoxTrip').addClass('Hide');
  fieldingEditor.find('.' + WS.state[prefix + 'Id']).removeClass('Hide');
  fieldingEditor.data('prefix', prefix);
  fieldingEditor.dialog('open');
}

function prepareFieldingEditor(teamId) {

  'use strict';
  $(initialize);

  function initialize() {
    var table = $('<table>').appendTo($('#FieldingEditor'));

    var row = $('<tr>').addClass('Skater').appendTo(table);
    $('<td>').append(
        $('<select>').attr('id', 'skater').append($('<option>').attr('value', '').text('None/Unknown'))
        .on('change', function () {
          WS.Set(fieldingEditor.data('prefix') + 'Skater', $(this).val());
        })).appendTo(row);
    $('<td>').append($('<button>').attr('id', 'notFielded').button().text('No Skater fielded').on('click', function () {
      var check = $(this).attr('checked') === null;
      $(this).attr('checked', check);
      WS.Set(fieldingEditor.data('prefix') + 'NotFielded', check);
    })).appendTo(row);
    $('<td>').append($('<button>').attr('id', 'sitFor3').button().text('Sit out next 3').on('click', function () {
      var check = $(this).attr('checked') === null;
      $(this).attr('checked', check);
      WS.Set(fieldingEditor.data('prefix') + 'SitFor3', check);
    })).appendTo(row);

    row = $('<tr>').addClass('Skater').appendTo(table);
    $('<td>').attr('colspan', '3').append($('<input type="text">').attr('size', '40').attr('id', 'annotation').on('change', function () {
      WS.Set(fieldingEditor.data('prefix') + 'Annotation', $(this).val());
    })).appendTo(row);

    row = $('<tr>').addClass('Skater BoxTripComments').appendTo(table);
    $('<td>').append($('<button>').text('No Penalty').button().on('click', function () {
      appendAnnotation('No Penalty');
    })).appendTo(row);
    $('<td>').append($('<button>').text('Penalty Overturned').button().on('click', function () {
      appendAnnotation('Penalty Overturned');
    })).appendTo(row);

    row = $('<tr>').addClass('tripHeader').appendTo(table);
    $('<td>').attr('colspan', '2').text('Box Trips').appendTo(row);
    $('<td>').appendTo(row);

    row = $('<tr>').addClass('tripHeader').appendTo(table);
    $('<td>').text('Start').appendTo(row);
    $('<td>').text('End').appendTo(row);
    $('<td>').appendTo(row);

    row = $('<tr>').attr('id', 'tripFooter').appendTo(table);
    $('<td>').append($('<button>').attr('id', 'addTrip').text('Add Box Trip').button().on('click', function () {
      WS.Set(fieldingEditor.data('prefix') + 'AddBoxTrip', true);
    })).appendTo(row);
    $('<td>').appendTo(row);
    $('<td>').addClass('ButtonCell').append($('<button>').attr('id', 'close').text('Close').button()).on('click', function () {
      fieldingEditor.dialog('close');
    }).appendTo(row);

    WS.Register([ 'ScoreBoard.Team(' + teamId + ').Skater(*).Role', 'ScoreBoard.Team(' + teamId + ').Skater(*).RosterNumber' ],
        function (k, v) {
      processSkater(k, v);
    });
    WS.Register([ 'ScoreBoard.Team(' + teamId + ').BoxTrip' ], function (k, v) {
      processBoxTrip(k, v);
    });

    fieldingEditor = $('#FieldingEditor').dialog(
        { modal: true,
          closeOnEscape: false,
          title: 'Fielding Editor',
          autoOpen: false,
          width: '450px', });
  }

  function appendAnnotation(annotation) {
    var annotationField = fieldingEditor.find('#annotation');
    if (annotationField.val() !== '') {
      annotation = '; ' + annotation;
    }
    annotationField.val(annotationField.val() + annotation);
    WS.Set(fieldingEditor.data('prefix') + 'Annotation', annotationField.val());
  }

  function processSkater(k, v) {
    var select = $('#FieldingEditor #skater');
    select.children('[value="' + k.Skater + '"]').remove();
    var prefix = 'ScoreBoard.Team(' + k.Team + ').Skater(' + k.Skater + ').';
    if (v !== null && WS.state[prefix + 'Role'] !== 'NotInGame') {
      var number = WS.state[prefix + 'RosterNumber'];
      var option = $('<option>').attr('number', number).val(k.Skater).text(number);
      _windowFunctions.appendAlphaSortedByAttr(select, option, 'number', 1);
    }
  }

  function processBoxTrip(k, v) {
    if (k.BoxTrip === null) { return; }
    var key = k.parts[3];
    var prefix = 'ScoreBoard.Team(' + teamId + ').BoxTrip(' + k.BoxTrip + ').';

    var row = $('#FieldingEditor .BoxTrip[id=' + k.BoxTrip + ']');
    if (v !== null && row.length === 0) {
      row = $('<tr>').addClass('BoxTrip').attr('id', k.BoxTrip).insertBefore('#FieldingEditor #tripFooter');
      $('<td>').append($('<button>').addClass('tripModify').text('-').button().on('click', function () {
        WS.Set(prefix + 'StartEarlier', true);
      })).append($('<span>').addClass('tripStartText')).append(
          $('<button>').addClass('tripModify').text('+').button()
          .on('click', function () {
            WS.Set(prefix + 'StartLater', true);
          })).appendTo(row);
      $('<td>').append($('<button>').addClass('tripModify').text('-').button().on('click', function () {
        WS.Set(prefix + 'EndEarlier', true);
      })).append($('<span>').addClass('tripEndText').text('ongoing')).append(
          $('<button>').addClass('tripModify').text('+').button()
          .on('click', function () {
            WS.Set(prefix + 'EndLater', true);
          })).appendTo(row);
      $('<td>').addClass('Col3').append($('<button>').addClass('tripRemove').text('Remove').button().on('click', function () {
        WS.Set(prefix + 'Delete', true);
      })).appendTo(row);
    }
    if (v === null) {
      if (key === 'Id') {
        row.remove();
      }
      if (key === 'Fielding') {
        row.removeClass(v);
      }
      return;
    }
    if ([ 'StartJamNumber', 'StartBetweenJams', 'StartAfterSP' ].includes(key)) {
      var between = isTrue(WS.state[prefix + 'StartBetweenJams']);
      var afterSP = isTrue(WS.state[prefix + 'StartAfterSP']);
      row.find('.tripStartText').text(
          (between ? 'Before ' : '') + 'Jam ' + WS.state[prefix + 'StartJamNumber'] +
          (afterSP ? ' after SP' : ''));
    }
    if ([ 'EndJamNumber', 'EndBetweenJams', 'EndAfterSP' ].includes(key)) {
      var between = isTrue(WS.state[prefix + 'EndBetweenJams']);
      var afterSP = isTrue(WS.state[prefix + 'EndAfterSP']);
      var jam = WS.state[prefix + 'EndJamNumber'];
      row.find('.tripEndText').text(
          (between ? ' After ' : ' ') + (jam === 0 ? 'ongoing' : 'Jam ' + jam) +
          (afterSP && !between ? ' after SP ' : ' '));
    }
    if (key === 'Fielding') {
      row.addClass(v);
    }
  }
}

//# sourceURL=controls\lt\lt-sheet.js
