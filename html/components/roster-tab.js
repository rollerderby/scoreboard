function createRosterTab(tab) {
  'use strict';
  var table;
  table = _crgUtils.createRowTable(2).appendTo(tab).attr('id', 'Teams', true);
  createRosterTable(table.children('tr').children('td:eq(0)'), 'ScoreBoard.CurrentGame.Team(1)');
  createRosterTable(table.children('tr').children('td:eq(1)'), 'ScoreBoard.CurrentGame.Team(2)');
}

function createRosterTable(element, teamPrefix) {
  'use strict';
  var teamTable = $('<table>')
    .addClass('Team')
    .append($('<tr><td colspan="3"/></tr>').addClass('Name'))
    .append($('<tr><td/><td/><td rowspan="3"></tr>').addClass('League'))
    .append($('<tr><td/><td/></tr>').addClass('LTeam'))
    .append($('<tr><td/><td/></tr>').addClass('Color'))
    .append($('<tr><td colspan="3"/></tr>').addClass('Skaters'))
    .appendTo(element);

  WSDisplay(teamPrefix + '.FullName', $('<span>')).appendTo(teamTable.find('tr.Name>td'));

  $('<span>').text('League: ').appendTo(teamTable.find('tr.League>td:eq(0)'));
  WSDisplay(teamPrefix + '.LeagueName', $('<span>')).appendTo(teamTable.find('tr.League>td:eq(1)'));
  $('<span>').text('Team: ').appendTo(teamTable.find('tr.LTeam>td:eq(0)'));
  WSDisplay(teamPrefix + '.TeamName', $('<span>')).appendTo(teamTable.find('tr.LTeam>td:eq(1)'));
  $('<span>').text('Uniform Color: ').appendTo(teamTable.find('tr.Color>td:eq(0)'));
  WSDisplay(teamPrefix + '.UniformColor', $('<span>')).appendTo(teamTable.find('tr.Color>td:eq(1)'));

  var logo = $('<img>').addClass('Logo').appendTo(teamTable.find('tr.League>td:eq(2)'));
  WS.Register(teamPrefix + '.Logo', function (k, v) {
    logo.attr('src', v);
  });

  var skatersTable = $('<table>')
    .addClass('Skaters Empty')
    .appendTo(teamTable.find('tr.Skaters>td'))
    .append('<col class="RosterNumber">')
    .append('<col class="Name">')
    .append('<col class="Pronouns">')
    .append('<col class="Flags">')
    .append('<thead/><tbody/>')
    .children('thead')
    .append('<tr><th></th><th class="Title">Skaters</th><th></th><th id="skaterCount"></th><th></th></tr>')
    .end();

  var updateSkaterCount = function () {
    var count = 0;
    skatersTable.find('tr.Skater td.Flags span').each(function (_, f) {
      var flag = $(f).attr('flag');
      if (flag === '' || flag === 'C' || flag === 'A') {
        count++;
      }
    });
    skatersTable.find('#skaterCount').text('(' + count + ' skating)');
  };
  updateSkaterCount();

  var handleSkaterUpdate = function (k, v) {
    var skaterRow = skatersTable.find('tr[skaterid="' + k.Skater + '"]');
    if (v == null) {
      skaterRow.remove();
      if (!skatersTable.find('tr[skaterid]').length) {
        skatersTable.children('tbody').addClass('Empty');
      }
      updateSkaterCount();
      return;
    }

    if (skaterRow.length === 0) {
      skatersTable.removeClass('Empty');
      skaterRow = $('<tr class="Skater">')
        .attr('skaterid', k.Skater)
        .append('<td class="RosterNumber">')
        .append('<td class="Name">')
        .append('<td class="Pronouns">')
        .append('<td class="Flags">');
      $('<span>').appendTo(skaterRow.children('td.RosterNumber'));
      $('<span>').appendTo(skaterRow.children('td.Name'));
      $('<span>').appendTo(skaterRow.children('td.Pronouns'));
      $('<span>').appendTo(skaterRow.children('td.Flags'));
      _windowFunctions.appendAlphaSortedByAttr(skatersTable.children('tbody'), skaterRow, 'skaternum');
    }

    if (k.field === 'Flags') {
      var position = v;
      switch (v) {
        case '':
          position = 'Skater';
          break;
        case 'ALT':
          position = 'Not Skating';
          break;
        case 'C':
          position = 'Captain';
          break;
        case 'A':
          position = 'Alt Captain';
          break;
        case 'BA':
          position = 'Bench Alt Captain';
          break;
        case 'B':
          position = 'Bench Staff';
          break;
      }
      skaterRow.children('td.Flags').children().attr('flag', v).text(position);
      updateSkaterCount();
    } else {
      skaterRow
        .children('td.' + k.field)
        .children()
        .text(v);
      if (k.field === 'RosterNumber') {
        skaterRow.attr('skaternum', v);
        _windowFunctions.appendAlphaSortedByAttr(skatersTable.children('tbody'), skaterRow, 'skaternum');
      }
    }
  };

  WS.Register(
    [
      teamPrefix + '.Skater(*).Flags',
      teamPrefix + '.Skater(*).Name',
      teamPrefix + '.Skater(*).RosterNumber',
      teamPrefix + '.Skater(*).Pronouns',
    ],
    handleSkaterUpdate
  );

  return teamTable;
}
