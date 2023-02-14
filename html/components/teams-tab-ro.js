function createTeamsTab(tab, gameId, teamId) {
  'use strict';
  var table;
  if (gameId != null) {
    // show teams for given game
    table = _crgUtils.createRowTable(2).appendTo(tab).attr('id', 'Teams', true);
    createReadTeamTable(table.children('tr').children('td:eq(0)'), 'ScoreBoard.Game(' + gameId + ').Team(1)', true);
    createReadTeamTable(table.children('tr').children('td:eq(1)'), 'ScoreBoard.Game(' + gameId + ').Team(2)', true);
  } else {
    // show prepared teams
    table = _crgUtils.createRowTable(1).appendTo(tab).attr('id', 'Teams', true);

    createReadTeamTable(table.children('tr').children('td:eq(0)'), 'ScoreBoard.PreparedTeam(' + teamId + ')', false);
  }
}

function createReadTeamTable(element, teamPrefix, isGameTeam) {
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
    var teamSelect = WSControl(teamPrefix + '.PreparedTeam', $('<select disabled>')).appendTo(teamTable.find('tr.Name>td'));
    $('<option value="">Custom Team</option>').appendTo(teamSelect);

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
  WSControl(teamPrefix + '.LeagueName', $('<input readonly type="text" class="Name" size="30">')).appendTo(teamTable.find('tr.League>td:eq(1)'));
  $('<span>').text('Team: ').appendTo(teamTable.find('tr.LTeam>td:eq(0)'));
  WSControl(teamPrefix + '.TeamName', $('<input readonly type="text" class="Name" size="30">')).appendTo(teamTable.find('tr.LTeam>td:eq(1)'));
  if (isGameTeam) {
    $('<span>').text('Uniform Color: ').appendTo(teamTable.find('tr.Color>td:eq(0)'));
    var colorSelectors = $('<span>').addClass('Hide').appendTo(teamTable.find('tr.Color>td:eq(1)'));
    var colorInput = WSControl(teamPrefix + '.UniformColor', $('<input readonly type="text" class="Name" size="15">'))
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
        selector = $('<select disabled>')
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
  }

  var controlTable = _crgUtils.createRowTable(3).appendTo(teamTable.find('tr.Control>td')).addClass('Control');
  var waitingOnUpload = '';
  // var logoSelect = $('<select>').append($('<option value="">No Logo</option>')).appendTo(controlTable.find('td:eq(0)'));
  // $('<img id="logo' + teamPrefix + '" src="/images/teamlogo/' + logoSelect.val() + '">').appendTo(controlTable.find('td:eq(0)'));
  // logoSelect.on('change', function () {
  //   WS.Set(teamPrefix + '.Logo', logoSelect.val() === '' ? '' : '/images/teamlogo/' + logoSelect.val());
  //   $('#logo' + teamPrefix).attr({
  //       'src': '/images/teamlogo/' + logoSelect.val()
  //   });
  // });

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
    .append('<tr class="AddSkater"><th/><th/><th/><th/><th/></tr>')
    .append('<tr><th colspan="4"><hr/></th></tr>')
    .end();

  var updateSkaterCount = function () {
    var count = 0;
    skatersTable.find('tr.Skater td.Flags select').each(function (_, f) {
      if (f.value === '' || f.value === 'C' || f.value === 'A') {
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
        var numberInput = $('<input readonly type="text" size="5">').appendTo(skaterRow.children('td.RosterNumber'));
        var nameInput = $('<input readonly type="text" size="20">').appendTo(skaterRow.children('td.Name'));
        nameInput.on('change', function () {
          WS.Set(skaterPrefix + '.Name', nameInput.val());
        });
        var skaterFlags = $('<select disabled>').appendTo(skaterRow.children('td.Flags'));
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
